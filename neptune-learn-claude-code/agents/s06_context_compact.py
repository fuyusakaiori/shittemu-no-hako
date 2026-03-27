#!/usr/bin/env python3
"""
s06_context_compact.py - Compact
Three-layer compression pipeline so the agent can work forever:
    Every turn:
    +------------------+
    | Tool call result |
    +------------------+
            |
            v
    [Layer 1: micro_compact]        (silent, every turn)
      Replace tool_result content older than last 3
      with "[Previous: used {tool_name}]"
            |
            v
    [Check: tokens > 50000?]
       |               |
       no              yes
       |               |
       v               v
    continue    [Layer 2: auto_compact]
                  Save full transcript to .transcripts/
                  Ask LLM to summarize conversation.
                  Replace all messages with [summary].
                        |
                        v
                [Layer 3: compact tool]
                  Model calls compact -> immediate summarization.
                  Same as auto, triggered manually.
Key insight: "The agent can forget strategically and keep working forever."
"""

import json
import os
import subprocess
import time

from pathlib import Path
from typing import Optional

from anthropic import Anthropic
from dotenv import load_dotenv

from agents.s02_tool_use import workdir

# 加载系统环境变量
load_dotenv(override=True)

# agent 工作目录
workdir = Path.cwd()
# claude 客户端
client = Anthropic(base_url=os.getenv("ANTHROPIC_BASE_URL"))
# 模型 id
model = os.getenv("MODEL_ID")
# 模型 prompt
system = f"""You are a coding agent at {workdir}. Use tools to solve tasks.""" # 提示词约束禁止多任务并行
# 模型调用工具的 schema
tools = [
    {"name": "bash", "description": "Run a shell command.",
     "input_schema": {"type": "object", "properties": {"command": {"type": "string"}}, "required": ["command"]}},
    {"name": "read_file", "description": "Read file contents.",
     "input_schema": {"type": "object", "properties": {"path": {"type": "string"}, "limit": {"type": "integer"}}, "required": ["path"]}},
    {"name": "write_file", "description": "Write content to file.",
     "input_schema": {"type": "object", "properties": {"path": {"type": "string"}, "content": {"type": "string"}}, "required": ["path", "content"]}},
    {"name": "edit_file", "description": "Replace exact text in file.",
     "input_schema": {"type": "object", "properties": {"path": {"type": "string"}, "old_text": {"type": "string"}, "new_text": {"type": "string"}}, "required": ["path", "old_text", "new_text"]}},
    {"name": "todo", "description": "Update task list. Track progress on multi-step tasks.",
     "input_schema": {"type": "object", "properties": {"items": {"type": "array", "items": {"type": "object", "properties": {"id": {"type": "string"}, "text": {"type": "string"}, "status": {"type": "string", "enum": ["pending", "in_progress", "completed"]}}, "required": ["id", "text", "status"]}}}, "required": ["items"]}},
    {"name": "compact", "description": "Trigger manual conversation compression.",
     "input_schema": {"type": "object", "properties": {"focus": {"type": "string", "description": "What to preserve in the summary"}}}},
]
# 模型调用的工具映射
tool_handlers = {
    # **kw 获取方法需要的入参; lambda 用于映射映射方法签名
    "bash": lambda **kw: run_bash(kw["command"]),
    "read_file": lambda **kw: run_read(kw["path"], kw.get("limit")),
    "write_file": lambda **kw: run_write(kw["path"], kw["content"]),
    "edit_file": lambda **kw: run_edit(kw["path"], kw["old_text"], kw["new_text"]),
    "compact": lambda **kw: "Manual compression requested."
}
# 活跃上下文阈值
context_threshold = 5000
# 不活跃上下文存储路径
transcript_dir = workdir / ".transcripts"

keep_recent = 3


# 任务状态管理器: 维护拆分的任务状态
class TodoManager:
    # 构造函数
    def __init__(self):
        # item: id, text, status
        self.todos = []
    # 更新任务状态
    def update(self, todos: list) -> str:
        # 判断拆分的任务数量是否超过限制
        if len(todos) > 20:
            raise ValueError("Exceeded maximum number of todos")
        in_progress_count = 0
        validate_todos = []
        # 循环遍历所有任务
        for index, todo in enumerate(todos):
            todo_id = str(todo.get("id", str(index + 1))) # 任务编号
            todo_status = str(todo.get("status", "pending")).lower() # 任务状态
            todo_content = str(todo.get("text", "")).strip() # 任务的内容
            # 判断任务内容是否为空
            if not todo_content:
                raise ValueError(f"Todo {todo_id}: content required")
            # 判断任务状态是否合法
            if todo_status not in ["pending", "completed", "in_progress"]:
                raise ValueError(f"Todo {todo_id}: invalid status '{todo_status}'")
            # 判断任务状态是否处于执行中
            if todo_status == "in_progress":
                in_progress_count += 1
            validate_todos.append({"id": todo_id, "status": todo_status, "text": todo_content})
        # 判断正在执行的任务是否唯一
        if in_progress_count > 1:
            raise ValueError("Only one task can be in_progress at a time")
        # 更新任务状态
        self.todos = validate_todos
        # 展示任务执行状态
        return self.render()

    # 展示任务执行状态
    def render(self ) -> str:
        if not self.todos:
            return "No todos"
        descriptions = []
        # 遍历所有任务
        for todo in self.todos:
            marker = {
                "pending": "[ ]",
                "in_progress": "[>]",
                "completed": "[x]",
            }[todo["status"]] # 任务状态标记
            descriptions.append(f"{marker} #{todo['id']}: {todo['text']}") # 任务状态描述
        done = sum(1 for todo in self.todos if todo["status"] == "completed") # 已完成任务数量
        descriptions.append(f"\n{done}/{len(self.todos)} completed")
        return "\n".join(descriptions)

todo = TodoManager()

# 估算上下文占用 token
def estimate_tokens(messages: list) -> int:
    """Rough token count: ~4 chars per token."""
    return len(str(messages)) // 4

# -- 第一层压缩: micro_compact - 将工具调用结果替换成占位符, 只需要知道调用了什么工具即可 --
def micro_compact(messages: list) -> list:
    tool_results = []
    # 1. 获取所有工具调用的执行结果
    for message in messages:
        if message["role"] == "user" and isinstance(message["content"], list):
            for content in message["content"]:
                tool_results.append(content) # tuple
    # 2. 如果工具调用不超过阈值, 那么不需要做压缩
    if len(tool_results) <= keep_recent:
        return messages
    # 3. 获取所有调用的工具
    tool_name_map = {}
    for message in messages:
        if message["role"] == "assistant" and isinstance(message["content"], list):
            for block in message["content"]:
                if hasattr(block, "type") and block.type == "tool_use":
                    tool_name_map[block.id] = block.name
    # 4. 压缩最早的部分上下文
    to_clear = tool_results[:-keep_recent]
    for result in to_clear:
        # 如果工具的调用结果长度大于 100, 那么就将其替换为工具方法名
        if isinstance(result.get("content", ""), str) and len(result.get("content", "")) > 100:
            tool_id = result.get("tool_use_id", "")
            tool_name = tool_name_map.get(tool_id, "unknown")
            result["content"] = f"[Previous: used {tool_name}]"
    return messages

# -- 第二层压缩: auto_compact - 将完整上下文保存到磁盘, 调用模型做上下文的摘要
def auto_compact(messages: list) -> list:
    # 1. 创建上下文存储的目录
    transcript_dir.mkdir(exist_ok=True)
    transcript_path = transcript_dir / f"transcript_{int(time.time())}.jsonl"
    # 2. 打开文件并写入上下文
    with open(transcript_path, "w") as file:
        for message in messages:
            file.write(json.dumps(message, default=str) + "\n")
    print(f"[transcript saved {transcript_path}]")
    # 3. 调用大模型重新总结
    context = json.dumps(messages, default=str)
    response = client.messages.create(
        model=model,
        messages=[
            {
                "role": "user",
                "content": "Summarize this conversation for continuity. Include: "
                           "1) What was accomplished, 2) Current state, 3) Key decisions made. "
                           "Be concise but preserve critical details.\n\n" + context
            }
        ],
        max_tokens=8000
    )
    # 4. 上下文替换为压缩后的总结
    summary = "no summary"
    for block in response.content:
        if hasattr(block, "text"):
            summary = block.text
            break
    return [
        {"role": "user", "content": f"[Conversation compressed. Transcript: {transcript_path}]\n\n{summary}"},
        {"role": "assistant", "content": "Understood. I have the context from the summary. Continuing."},
    ]

# -- 第三层压缩: manual_compact - 模型根据需要手动触发

# 路径沙箱: 确保工具访问的路径安全
def safe_path(path: str) -> Path:
    # 拼接路径并转换为绝对路径
    final_path = (workdir / path).resolve()
    # 判断是否为工作目录的子路径
    if not final_path.is_relative_to(workdir):
        raise ValueError(f"Path escape workspace: {path}")
    return final_path

# 工具调用: 执行命令
def run_bash(command: str) -> str:
    dangerous = ["rm -rf /", "sudo", "shutdown", "reboot", "> /dev/"]
    if any(d in command for d in dangerous):
        return "Error: Dangerous command blocked"
    try:
        # 执行 bash 命令
        result = subprocess.run(
            command, shell=True, cwd=os.getcwd(), capture_output=True, text=True, timeout=120)
        out = (result.stdout + result.stderr).strip()
        return out[:50000] if out else "(no output)"
    except subprocess.TimeoutExpired:
        return "Error: Timeout (120s)"

# 工具类型: read_file
def run_read(path: str, limit: Optional[int] = None) -> str:
    try:
        # 一次性读取文件内容
        content = safe_path(path).read_text()
        lines = content.splitlines()
        # 判断读取的行数是否小于文件总的行数: 如果是就仅读取一部分; 如果不是就读取全部
        if limit and limit < len(lines):
            lines = lines[:limit] + [f"... ({len(lines) - limit} more lines)"]
        return "\n".join(lines)[:50000]
    except Exception as exception:
        return f"Error: {exception}"

# 工具类型: write_file
def run_write(path: str, content: str) -> str:
    try:
        # 获取文件的绝对路径
        file_path = safe_path(path)
        # 创建文件的父目录: 确保文件所在的目录存在
        file_path.parent.mkdir(parents=True, exist_ok=True)
        # 将内容写入文件
        file_path.write_text(content)
        return f"Wrote {len(content)} bytes to {path}"
    except Exception as exception:
        return f"Error: {exception}"

# 工具类型: edit_file
def run_edit(path: str, old_text: str, new_text: str) -> str:
    try:
        # 获取文件的绝对路径
        file_path = safe_path(path)
        # 读取文件内容
        content = file_path.read_text()
        # 判断旧的内容是否存在于文本中
        if old_text not in content:
            return f"Error: {old_text} text not found in {path}"
        file_path.write_text(content.replace(old_text, new_text, 1))
        return f"Edited {path}"
    except Exception as exception:
        return f"Error: {exception}"

# 核心模式: 循环调用工具直到停止
def agent_loop(messages: list):
    # 1. 开始循环
    while True:
        # 2. 第一层压缩: micro_compact 压缩
        micro_compact(messages)
        # 3. 第二层压缩: auto_compact 压缩 - 如果 token 消耗大于阈值
        if estimate_tokens(messages) > context_threshold:
            print("[auto_compact triggered]")
            messages[:] = auto_compact(messages)
        # 4. 调用模型生成结果
        response = client.messages.create(
            model=model,system=system,messages=messages,tools=tools,max_tokens=8000
        )
        # 5. 将模型的响应结果添加到 messages
        messages.append({"role": "assistant", "content": response.content})
        # 6. 判断模型是否调用工具
        if response.stop_reason != "tool_use":
            break
        # 5. 执行每个工具调用
        results = []
        manual_compact = False
        for block in response.content:
            if block.type == "tool_use":
                if block.name == "manual_compact":
                    manual_compact = True
                    output = "Compressing..."
                else:
                    handler = tool_handlers.get(block.name)
                    try:
                        output = handler(**block.input) if handler else f"Unknown tool: {block.name}"
                    except Exception as e:
                        output = f"Error: {e}"
                print(f"> {block.name}: {output[:200]}")
                results.append({
                    "type": "tool_result",
                    "tool_use_id": block.id,
                    "content": output,
                })
        # 6. 将工具执行的结果添加到 messages
        messages.append({"role": "user", "content": results})
        # 7. 判断是否需要再次自动压缩
        if manual_compact:
            print("[manual compact]")
            messages[:] = auto_compact(messages)

if __name__ == '__main__':
    history = []
    # 1. 开始循环
    while True:
        try:
            query = input("\033[36ms06 >> \033[0m")
        except (EOFError, KeyboardInterrupt):
            break
        # 2. 判断是否接收到退出指令
        if query.strip().lower() in ("exit", "quit"):
            break
        # 3. 标准化输入内容
        history.append({"role": "user", "content": query})
        # 4. agent 执行循环
        agent_loop(history)
        # 5. 获取 agent 执行结果
        response_content = history[-1]["content"]
        # 6. 检查返回结果类型
        if isinstance(response_content, list):
            for block in response_content:
                if hasattr(block, "text"):
                    print(block.text)
        print()