#!/usr/bin/env python3
"""
s03_todo_write.py - TodoWrite
The model tracks its own progress via a TodoManager. A nag reminder
forces it to keep updating when it forgets.
    +----------+      +-------+      +---------+
    |   User   | ---> |  LLM  | ---> | Tools   |
    |  prompt  |      |       |      | + todo  |
    +----------+      +---+---+      +----+----+
                          ^               |
                          |   tool_result |
                          +---------------+
                                |
                    +-----------+-----------+
                    | TodoManager state     |
                    | [ ] task A            |
                    | [>] task B <- doing   |
                    | [x] task C            |
                    +-----------------------+
                                |
                    if rounds_since_todo >= 3:
                      inject <reminder>
Key insight: "The agent can track its own progress -- and I can see it."
"""
import json
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
# 执行计划 存放目录
tasks_dir = workdir / ".tasks"
# claude 客户端
client = Anthropic(base_url=os.getenv("ANTHROPIC_BASE_URL"))
# 模型 id
model = os.getenv("MODEL_ID")
# 模型 prompt
system = f"You are a coding agent at {workdir}. Use task tools to plan and track work."
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
    {"name": "task_create", "description": "Create a new task.",
     "input_schema": {"type": "object", "properties": {"subject": {"type": "string"}, "description": {"type": "string"}}, "required": ["subject"]}},
    {"name": "task_update", "description": "Update a task's status or dependencies.",
     "input_schema": {"type": "object", "properties": {"task_id": {"type": "integer"}, "status": {"type": "string", "enum": ["pending", "in_progress", "completed"]}, "addBlockedBy": {"type": "array", "items": {"type": "integer"}}, "addBlocks": {"type": "array", "items": {"type": "integer"}}}, "required": ["task_id"]}},
    {"name": "task_list", "description": "List all tasks with status summary.",
     "input_schema": {"type": "object", "properties": {}}},
    {"name": "task_get", "description": "Get full details of a task by ID.",
     "input_schema": {"type": "object", "properties": {"task_id": {"type": "integer"}}, "required": ["task_id"]}},
]

tool_handlers = {
    # **kw 获取方法需要的入参; lambda 用于映射映射方法签名
    "bash": lambda **kw: run_bash(kw["command"]),
    "read_file": lambda **kw: run_read(kw["path"], kw.get("limit")),
    "write_file": lambda **kw: run_write(kw["path"], kw["content"]),
    "edit_file": lambda **kw: run_edit(kw["path"], kw["old_text"], kw["new_text"]),
    "task_create": lambda **kw: task.create(kw["subject"], kw.get("description", "")),
    "task_update": lambda **kw: task.update(kw["task_id"], kw["status"], kw.get("addBlockedBy"), kw.get("addBlocks")),
    "task_list": lambda **kw: task.render(),
    "task_get": lambda **kw: task.get(kw["task_id"])
}

# 任务状态管理器: 维护拆分的任务状态并持久化存储
class TaskManager:
    # 构造函数
    def __init__(self, tasks_dir: Path):
        self.dir = tasks_dir
        self.dir.mkdir(exist_ok=True)
        self._next_id = self._max_id() + 1
    # 获取子任务中的最大 id
    def _max_id(self) -> int:
        ids = [int(file.stem.split("_")[1]) for file in self.dir.glob("task_*.json")]
        return max(ids) if ids else 0
    # 创建任务
    def create(self, subject: str, description: str = "") -> str:
        # 1. 初始化任务
        task = {
            "id": self._next_id,
            "subject": subject,
            "description": description,
            "status": "pending",
            "blocked_by": [], # 前置任务
            "blocks": [], # 后置任务
            "owner": ""
        }
        # 2. 持久化存储任务
        self._save(task)
        # 3. 最大任务 id 递增
        self._next_id += 1

        return json.dumps(task, indent=2)
    # 更新任务: 重构
    def update(self, task_id: int, status: str = None, add_blocked_by: list = None, add_blocks: list = None) -> str:
        # 1. 读取任务
        task = self._load(task_id)
        # 2. 判断任务状态是否合法
        if status:
            if status not in ("pending", "in_progress", "completed"):
                raise ValueError(f"Invalid status: {status}")
            task["status"] = status
            # 判断任务是否完成, 如果完成那么需要移除依赖
            if task["status"] == "completed":
                self._remove(task_id)
        # 3. 增加前置任务: 为什么这里不需要前置任务关联的后置任务呢?
        if add_blocked_by:
            task["blocked_by"] = list(set(task["blocked_by"] + add_blocked_by))
        # 4. 增加后置任务
        if add_blocks:
            task["blocks"] = list(set(task["blocks"] + add_blocks))
            # 需要同时更新后置任务关联的前置任务
            for blocked_task_id in add_blocks:
                try:
                    blocked_task = self._load(blocked_task_id)
                    # 判断当前任务是否在这些任务的前置任务列表中
                    if task_id not in blocked_task["blocked_by"]:
                        blocked_task["blocked_by"].append(task_id)
                        self._save(blocked_task)
                except ValueError:
                    pass
        # 5. 更新任务
        self._save(task)

        return json.dumps(task, indent=2)
    # 查询任务
    def get(self, task_id: int) -> str:
        return json.dumps(self._load(task_id), indent=2)
    # 存储任务
    def _save(self, task: dict):
        # 生成任务的存储路径
        path = self.dir / f"task_{task.get('id')}.json"
        path.write_text(json.dumps(task, indent=2))
    # 读取任务
    def _load(self, task_id: int) -> dict:
        # 1. 拼接需要读取的任务路径
        path = self.dir / f"task_{task_id}.json"
        # 2. 判断任务目录是否存在
        if not path.exists():
            raise ValueError(f"Task {task_id} not found")
        # 3. 读取任务
        return json.loads(path.read_text())
    # 删除任务依赖
    def _remove(self, completed_id: int):
        # 1. 读取所有任务
        for file in self.dir.glob("task_*.json"):
            task = json.loads(file.read_text())
            # 2. 移除将该已完成任务作为前置任务的依赖
            if completed_id in task.get("blocked_by", []):
                task["blocked_by"].remove(completed_id)
                self._save(task)
    # 格式化任务
    def render(self) -> str:
        tasks = []
        # 1. 读取任务
        for file in sorted(self.dir.glob("task_*.json")):
            tasks.append(json.loads(file.read_text()))
        # 2. 判断是否存在任务
        if not tasks:
            return "No tasks"
        # 3. 格式化任务
        lines = []
        for task in tasks:
            marker = {"pending": "[ ]", "in_progress": "[>]", "completed": "[x]"}.get(task["status"]) # 状态样式
            blocked = f"(blocked by: {task['blocked_by']})" if task["blocked_by"] else "" # 前置任务
            lines.append(f"{marker} #{task['id']}: {task['subject']}{blocked}")
        return "\n".join(lines)

task = TaskManager(tasks_dir)

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
    while True:
        response = client.messages.create(
            model=model, system=system, messages=messages,
            tools=tools, max_tokens=8000,
        )
        messages.append({"role": "assistant", "content": response.content})
        if response.stop_reason != "tool_use":
            return
        results = []
        for block in response.content:
            if block.type == "tool_use":
                handler = tool_handlers.get(block.name)
                try:
                    output = handler(**block.input) if handler else f"Unknown tool: {block.name}"
                except Exception as e:
                    output = f"Error: {e}"
                print(f"> {block.name}: {str(output)[:200]}")
                results.append({"type": "tool_result", "tool_use_id": block.id, "content": str(output)})
        messages.append({"role": "user", "content": results})

if __name__ == '__main__':
    history = []
    # 1. 开始循环
    while True:
        try:
            query = input("\033[36ms07 >> \033[0m")
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