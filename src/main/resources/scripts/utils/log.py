from rich.console import Console
from rich.markdown import Heading, Markdown
from rich.text import Text

console = Console()

def log(msg):
    msg = str(msg)
    lines = msg.splitlines()
    if msg.endswith("\n") and msg.strip():
        lines.append("")

    for line in lines:
        if line.startswith("# "):
            console.print(Text(line, style="bold"))
        else:
            console.print(Text(line))

def info(msg):
    log("INFO: " + str(msg))

def warn(msg):
    log("WARN: " + str(msg))

def err(msg):
    log("ERR: " + str(msg))
