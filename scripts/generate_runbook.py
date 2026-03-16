import os
from openai import OpenAI

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# Read prompt template
with open("prompts/runbook_prompt.md", "r") as f:
    prompt = f.read()

# Collect repository file list (basic example)
repo_files = ""
for root, dirs, files in os.walk("src"):
    for file in files:
        repo_files += f"{root}/{file}\n"

final_prompt = prompt + "\nRepository Files:\n" + repo_files

response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[
        {"role": "system", "content": "You are a senior SRE"},
        {"role": "user", "content": final_prompt}
    ]
)

html_output = response.choices[0].message.content

os.makedirs("docs/runbooks", exist_ok=True)

with open("docs/runbooks/application_runbook.html", "w") as f:
    f.write(html_output)

print("Runbook generated successfully")
