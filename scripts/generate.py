import os
import json
from openai import OpenAI
from datetime import datetime

# ── 1. Load package.json ──────────────────────────────────────────────────────
with open("package.json", "r") as f:
    package_data = json.load(f)

app_name    = package_data.get("name", "Application")
version     = package_data.get("version", "N/A")
description = package_data.get("description", "No description provided")
scripts     = package_data.get("scripts", {})
dependencies = package_data.get("dependencies", {})

# ── 2. Crawl codebase ─────────────────────────────────────────────────────────
INCLUDE_EXTENSIONS = {".py", ".js", ".ts", ".jsx", ".tsx", ".yaml", ".yml", ".sh"}
EXCLUDE_DIRS       = {"node_modules", ".git", "__pycache__", ".venv", "venv", "dist", "build", "docs"}
MAX_FILE_SIZE_KB   = 20   # reduced from 50KB to 20KB per file
MAX_TOTAL_CHARS    = 50000  # hard cap on total codebase characters sent to GPT

def read_codebase(root="."):
    files_content = []
    total_chars = 0
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
        for filename in filenames:
            ext = os.path.splitext(filename)[1]
            if ext not in INCLUDE_EXTENSIONS:
                continue
            filepath = os.path.join(dirpath, filename)
            size_kb = os.path.getsize(filepath) / 1024
            if size_kb > MAX_FILE_SIZE_KB:
                print(f"⏭️ Skipping large file: {filepath} ({size_kb:.1f}KB)")
                continue
            try:
                with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                    content = f.read()
                # Stop if we hit total character limit
                if total_chars + len(content) > MAX_TOTAL_CHARS:
                    print(f"⚠️ Total character limit reached, stopping codebase scan")
                    return "\n\n".join(files_content)
                relative_path = os.path.relpath(filepath, root)
                files_content.append(f"### File: {relative_path}\n```\n{content}\n```")
                total_chars += len(content)
            except Exception as e:
                print(f"⚠️ Could not read {filepath}: {e}")
    return "\n\n".join(files_content)

print("📂 Reading codebase...")
codebase = read_codebase(".")
print(f"✅ Codebase loaded ({len(codebase)} characters)")

# ── 3. Build prompt ───────────────────────────────────────────────────────────
prompt = f"""
You are a technical writer. Generate a detailed and professional runbook in HTML format for the following application.

Application Details:
- Name: {app_name}
- Version: {version}
- Description: {description}
- Scripts: {json.dumps(scripts, indent=2)}
- Dependencies: {json.dumps(dependencies, indent=2)}

Below is the full codebase of the application. Use it to generate accurate, specific documentation:

{codebase}

The runbook must include the following sections:
1. Architecture Overview - describe the app structure based on the actual code
2. Prerequisites - list tools, versions, and environment setup needed
3. Deployment Steps - step-by-step instructions based on the actual scripts and code
4. Rollback Procedures - how to roll back to a previous version if something goes wrong
5. Troubleshooting Guide - common issues based on the actual code and how to resolve them

Output ONLY valid HTML with inline CSS styling. Make it clean, readable, and professional.
Keep the output concise and under 500KB.
Do NOT include large inline data, base64 images, or repeated content.
Include the generation date: {datetime.now().strftime('%Y-%m-%d')}.
"""

# ── 4. Call OpenAI API ────────────────────────────────────────────────────────
print("🤖 Calling GPT-4o...")
client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "You are a technical documentation expert."},
        {"role": "user", "content": prompt}
    ],
    temperature=0.3
)

# ── 5. Save runbook ───────────────────────────────────────────────────────────
html_content = response.choices[0].message.content
print(f"📄 HTML Content Preview:\n{html_content[:5000]}...")

# Strip markdown code fences if present
if html_content.startswith("```"):
    html_content = html_content.split("```")[1]
    if html_content.startswith("html"):
        html_content = html_content[4:]

os.makedirs("docs/runbooks", exist_ok=True)
output_path = f"docs/runbooks/runbook-{datetime.now().strftime('%Y-%m-%d')}.html"
with open(output_path, "w") as f:
    f.write(html_content)

print(f"✅ Runbook successfully generated at {output_path}")
