#!/usr/bin/env python3
"""
s02_tool_use.py - Tools
The agent loop from s01 didn't change. We just added tools to the array
and a dispatch map to route calls.
    +----------+      +-------+      +------------------+
    |   User   | ---> |  LLM  | ---> | Tool Dispatch    |
    |  prompt  |      |       |      | {                |
    +----------+      +---+---+      |   bash: run_bash |
                          ^          |   read: run_read |
                          |          |   write: run_wr  |
                          +----------+   edit: run_edit |
                          tool_result| }                |
                                     +------------------+
Key insight: "The loop didn't change at all. I just added tools."
"""

import os
import subprocess

from pathlib import Path
from typing import Optional

from anthropic import Anthropic
from dotenv import load_dotenv

# 加载系统环境变量
load_dotenv(override=True)

# agent 工作目录
workdir = Path.cwd()
# claude 客户端
client = Anthropic(base_url=os.getenv("ANTHROPIC_BASE_URL"))
# 模型 id
model = os.getenv("MODEL_ID")
# 模型 prompt
system = f"You are a coding agent at {os.getcwd()}. Use bash to solve tasks. Act, don't explain."
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
]

tool_handlers = {
    # **kw 获取方法需要的入参; lambda 用于映射映射方法签名
    "bash": lambda **kw: run_bash(kw["command"]),
    "read_file": lambda **kw: run_read(kw["path"], kw.get("limit")),
    "write_file": lambda **kw: run_write(kw["path"], kw["content"]),
    "edit_file": lambda **kw: run_edit(kw["path"], kw["old_text"], kw["new_text"]),
}

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
        # 2. 调用模型生成结果
        response = client.messages.create(
            model=model,system=system,messages=messages,tools=tools,max_tokens=8000
        )
        # 3. 追加助手响应(?)
        messages.append({"role": "assistant", "content": response.content})
        # 4. 判断模型是否调用工具
        if response.stop_reason != "tool_use":
            break
        # 5. 执行每个工具调用
        results = []
        for block in response.content:
            if block.type == "tool_use":
                # 6. 获取需要调用的工具
                handler = tool_handlers.get(block.name)
                # 7. 执行工具
                output = handler(**block.input)
                print(f"> {block.name}: {output[:200]}")
                results.append({
                    "type": "tool_result",
                    "tool_use_id": block.id,
                    "content": output,
                })
        messages.append({"role": "user", "content": results})

if __name__ == '__main__':
    history = []
    # 1. 开始循环
    while True:
        try:
            query = input("\033[36ms02 >> \033[0m")
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