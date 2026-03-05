"""
s01_agent_loop.py - The Agent Loop
The entire secret of an AI coding agent in one pattern:
    while stop_reason == "tool_use":
        response = LLM(messages, tools)
        execute tools
        append results
    +----------+      +-------+      +---------+
    |   User   | ---> |  LLM  | ---> |  Tool   |
    |  prompt  |      |       |      | execute |
    +----------+      +---+---+      +----+----+
                          ^               |
                          |   tool_result |
                          +---------------+
                          (loop continues)
This is the core loop: feed tool results back to the model
until the model decides to stop. Production agents layer
policy, hooks, and lifecycle controls on top.
"""
import os
import subprocess

from anthropic import Anthropic
from dotenv import load_dotenv

# 加载系统环境变量
load_dotenv(override=True)

# claude 客户端
client = Anthropic(base_url=os.getenv("ANTHROPIC_BASE_URL"))
# 模型 ID
model = os.getenv("MODEL_ID")
# 系统提示词
system = f"You are a coding agent at {os.getcwd()}. Use bash to solve tasks. Act, don't explain."
# 调用工具
tools = [{
    "name": "bash",
    "description": "Run a shell command",
    "input_schema": {
        "type": "object",
        "properties": {"command": {"type": "string"}},
        "required": ["command"],
    }
}]

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
                print(f"\033[33m$ {block.input['command']}\033[0m")
                # 6. 调用工具
                output = run_bash(str(block.input["command"]))
                print(output[:200])
                results.append({
                    "type": "tool_result",
                    "tool_use_id": block.tool_use_id,
                    "content": output,
                })
        messages.append({"role": "user", "content": results})

if __name__ == '__main__':
    history = []
    # 1. 开始循环
    while True:
        try:
            query = input("提示词")
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
