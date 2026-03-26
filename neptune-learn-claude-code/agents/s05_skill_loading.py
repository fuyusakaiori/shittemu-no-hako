#!/usr/bin/env python3
"""
s05_skill_loading.py - Skills
Two-layer skill injection that avoids bloating the system prompt:
    Layer 1 (cheap): skill names in system prompt (~100 tokens/skill)
    Layer 2 (on demand): full skill body in tool_result
    skills/
      pdf/
        SKILL.md          <-- frontmatter (name, description) + body
      code-review/
        SKILL.md
    System prompt:
    +--------------------------------------+
    | You are a coding agent.              |
    | Skills available:                    |
    |   - pdf: Process PDF files...        |  <-- Layer 1: metadata only
    |   - code-review: Review code...      |
    +--------------------------------------+
    When model calls load_skill("pdf"):
    +--------------------------------------+
    | tool_result:                         |
    | <skill>                              |
    |   Full PDF processing instructions   |  <-- Layer 2: full body
    |   Step 1: ...                        |
    |   Step 2: ...                        |
    | </skill>                             |
    +--------------------------------------+
Key insight: "Don't put everything in the system prompt. Load on demand."
"""

import os
import re
import subprocess

from pathlib import Path
from typing import Optional

from anthropic import Anthropic
from dotenv import load_dotenv

# 加载系统环境变量
load_dotenv(override=True)

# agent 工作目录
workdir = Path.cwd()
# skill 目录
skill_dir = workdir / "skills"
# claude 客户端
client = Anthropic(base_url=os.getenv("ANTHROPIC_BASE_URL"))
# 模型 id
model = os.getenv("MODEL_ID")

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
    {"name": "load_skill", "description": "Load specialized knowledge by name.",
     "input_schema": {"type": "object", "properties": {"name": {"type": "string", "description": "Skill name to load"}}, "required": ["name"]}},
]

tool_handlers = {
    # **kw 获取方法需要的入参; lambda 用于映射映射方法签名
    "bash": lambda **kw: run_bash(kw["command"]),
    "read_file": lambda **kw: run_read(kw["path"], kw.get("limit")),
    "write_file": lambda **kw: run_write(kw["path"], kw["content"]),
    "edit_file": lambda **kw: run_edit(kw["path"], kw["old_text"], kw["new_text"]),
    "todo": lambda **kw: todo.update(kw["items"]), # 闭包延迟绑定
    "load_skill": lambda **kw: skill_loader.get_content(kw["name"])
}

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

# 技能加载器
class SkillLoader:
    def __init__(self, skills_dir: Path):
        self.skills_dir = skills_dir
        self.skills = {}
        self._load_all()
    # 加载所有 skill 名称
    def _load_all(self):
        # 1. 判断 skill 目录是否存在: 如果不存在, 那么返回为空
        if not self.skills_dir.exists():
            return
        # 2. 递归搜索 skill 目录下的所有技能
        for skill_file in sorted(self.skills_dir.rglob("SKILL.md")):
            # 读取 skill 的内容
            skill_content = skill_file.read_text()
            # 解析 skill 的元信息和实际内容
            skill_meta, skill_body = self._parse_frontmatter(skill_content)
            # 获取 skill 的名称: 默认使用 skill 目录的名称作为 skill 名称
            skill_name = skill_meta.get("name", skill_file.parent.name)
            # 添加 skill
            self.skills[skill_name] = {"meta": skill_meta, "body": skill_body, "path": str(skill_file)}

    # 解析 skill 内容: 解析 markdown frontmatter 内容
    """ frontmatter:
    title: hello
    date: 2024-01-01
    tags: python
    """
    def _parse_frontmatter(self, content: str) -> tuple:
        # 1. 定义正则表达式
        match = re.match(r"^---\n(.*?)\n---\n(.*)", content, re.DOTALL)
        # 2. 判断是否解析成功
        if not match:
            return {}, content
        meta = {}
        # 3. 遍历匹配到的 frontmatter 并按行切割
        for line in match.group(1).strip().splitlines():
            # 判断每行里是否含有冒号
            if ":" in line:
                # 根据冒号切割
                key, value = line.split(":", 1)
                # 将 frontmatter 的内容以 key value 的形式设置到元信息里
                meta[key.strip()] = value.strip()
        # 4. 返回元信息和实际内容
        return meta, match.group(2).strip()

    # 获取所有 skill 的描述
    def get_descriptions(self) -> str:
        # 1. 判断 skill 是否为空
        if not self.skills:
            return "(no skills available)"
        # 2. 遍历所有 skill
        lines = []
        for skill_name, skill in self.skills.items():
            desc = skill.get("meta", {}).get("description", "No description")
            tags = skill.get("meta", {}).get("tags", "")
            line = f"  - {skill_name}: {desc}"
            if tags:
                line += f" [{tags}]"
            lines.append(line)
        return "\n".join(lines)

    # 获取 skill 内容
    def get_content(self, name: str) -> str:
        skill = self.skills.get(name)
        if not skill:
            return f"Error: Unknown skill '{name}'. Available skills: {', '.join(sorted(self.skills.keys()))}"
        return f"<skill name=\"{name}\">{skill['body']}</skill>"

todo = TodoManager()

skill_loader = SkillLoader(skill_dir)

# 模型 prompt: 提醒模型需要使用 skill
system = f"""You are a coding agent at {workdir}.
Use load_skill to access specialized knowledge before tackling unfamiliar topics.
Skills available:
{skill_loader.get_descriptions()}"""

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
        # 3. 将模型的响应结果添加到 messages
        messages.append({"role": "assistant", "content": response.content})
        # 4. 判断模型是否调用工具
        if response.stop_reason != "tool_use":
            break
        # 5. 执行每个工具调用
        results = []
        for block in response.content:
            if block.type == "tool_use":
                handler = tool_handlers.get(block.name)
                try:
                    output = handler(**block.input) if handler else f"Unknown tool: {block.name}"
                except Exception as e:
                    output = f"Error: {e}"
                print(f"> {block.name}: {str(output)[:200]}")
                results.append({
                    "type": "tool_result",
                    "tool_use_id": block.id,
                    "content": output,
                })
        # 6. 将工具执行的结果添加到 messages
        messages.append({"role": "user", "content": results})

if __name__ == '__main__':
    history = []
    # 1. 开始循环
    while True:
        try:
            query = input("\033[36ms05 >> \033[0m")
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