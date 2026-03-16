import os

# spark-assist (replace OpenAI usage)
from spark_assist import SparkAssist  # spark-assist library

# Create Spark client (API key provided via env var)
client = SparkAssist(api_key=os.getenv("SPARK_API_KEY"))

# Read prompt template
with open("prompts/runbook_prompt.md", "r", encoding="utf-8") as f:
    prompt = f.read()

# Collect repository file list (basic example)
repo_files = ""
for root, dirs, files in os.walk("src"):
    for file in files:
        repo_files += f"{root}/{file}\n"

final_prompt = prompt + "\nRepository Files:\n" + repo_files

# Generate runbook with Spark Assist
# Note: parameter names may differ slightly depending on your spark-assist version.
result = client.generate(
    system="You are a senior SRE",
    prompt=final_prompt,
    # model="...",  # optional: set if your Spark deployment uses model names
    # temperature=0.2,  # optional
)

# Normalize output (depending on SDK return type)
html_output = result["content"] if isinstance(result, dict) else str(result)

os.makedirs("docs/runbooks", exist_ok=True)

with open("docs/runbooks/application_runbook.html", "w", encoding="utf-8") as f:
    f.write(html_output)

print("Runbook generated successfully")
