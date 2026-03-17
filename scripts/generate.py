import os
import json
from openai import OpenAI
from datetime import datetime

# Load input data from package.json
with open("package.json", "r") as f:
    package_data = json.load(f)

# Build prompt from package.json data
app_name = package_data.get("name", "Application")
version = package_data.get("version", "N/A")
description = package_data.get("description", "No description provided")
scripts = package_data.get("scripts", {})
dependencies = package_data.get("dependencies", {})

prompt = f"""
You are a technical writer. Generate a detailed and professional runbook in HTML format for the following application.

Application Details:
- Name: {app_name}
- Version: {version}
- Description: {description}
- Scripts: {json.dumps(scripts, indent=2)}
- Dependencies: {json.dumps(dependencies, indent=2)}

The runbook must include the following sections:
1. Architecture Overview - describe the app structure based on its dependencies and scripts
2. Prerequisites - list tools, versions, and environment setup needed
3. Deployment Steps - step-by-step instructions to deploy the application
4. Rollback Procedures - how to roll back to a previous version if something goes wrong
5. Troubleshooting Guide - common issues and how to resolve them

Output ONLY valid HTML with inline CSS styling. Make it clean, readable, and professional.
Include the generation date: {datetime.now().strftime('%Y-%m-%d')}.
"""

# Call OpenAI API
client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

response = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "You are a technical documentation expert."},
        {"role": "user", "content": prompt}
    ],
    temperature=0.3
)

# Extract HTML content
html_content = response.choices[0].message.content

# Strip markdown code fences if present
if html_content.startswith("```"):
    html_content = html_content.split("```")[1]
    if html_content.startswith("html"):
        html_content = html_content[4:]

# Save to docs/runbook.html
os.makedirs("docs", exist_ok=True)
with open("docs/runbook.html", "w") as f:
    f.write(html_content)

print("✅ Runbook successfully generated at docs/runbook.html")
```
