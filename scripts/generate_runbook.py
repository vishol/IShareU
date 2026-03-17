name: Generate Runbook

on:
  schedule:
    # Run every Monday at 9:00 AM UTC
    - cron: '0 9 * * 1'
  workflow_dispatch: # Allow manual trigger from GitHub UI

jobs:
  generate-runbook:
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install dependencies
        run: npm install

      - name: Generate runbook
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        run: node script.js

      - name: Commit and push runbook to main
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action"
          git add docs/runbook.html
          git commit -m "chore: auto-generate runbook $(date +'%Y-%m-%d')" || echo "No changes to commit"
          git push
