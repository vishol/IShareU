const Anthropic = require("@anthropic-ai/sdk");
const fs = require("fs");

const client = new Anthropic();

const runbookPrompt = `You are a technical documentation expert. Generate a comprehensive runbook in HTML format for a web application.

The runbook must include ALL of the following sections in this exact order:

1. Application Name
2. Application Description (2-3 sentences)
3. Application Overview (detailed explanation of purpose and key features)
4. Architecture Diagram (create an ASCII art or detailed text-based diagram showing components, services, and data flow)
5. Sequence Diagram (show interaction flow between major components)
6. Processing Steps (numbered step-by-step flow of how the application processes requests)
7. Technology Stack (list of technologies, frameworks, databases, tools)
8. Division (organizational division)
9. Group (team or group responsible)
10. Family (product family/category)
11. L1_Product (primary product identifier)
12. Product Manager (name or role)
13. Scrum Team (team name or members)
14. DataSet (data sources or datasets used)
15. Code Location (repository URL or code location)
16. Recent Issues Fixed (list of recent bug fixes or improvements)
17. Potential Code Improvements (recommendations for future enhancements)
18. Deployment Instructions (step-by-step deployment process)
19. Troubleshooting Guide (common issues and solutions)
20. Rollback Procedures (how to rollback if deployment fails)
21. Monitoring & Alerts (key metrics to monitor and alert thresholds)

Format the output as a complete, standalone HTML5 document with:
- Professional styling with a clean, modern design
- Proper heading hierarchy (h1, h2, h3)
- Table of contents at the top with internal links
- Responsive layout that works on desktop and mobile
- Code blocks for configuration, deployment commands, or code snippets
- Clear visual separation between sections
- Footer with generation timestamp

Use semantic HTML and inline CSS. The document should be self-contained and ready to view in any modern browser.

Start generating the runbook now:`;

async function generateRunbook() {
  try {
    console.log("Generating runbook...");

    const message = await client.messages.create({
      model: "claude-3-5-sonnet-20241022",
      max_tokens: 4000,
      messages: [
        {
          role: "user",
          content: runbookPrompt,
        },
      ],
    });

    const htmlContent = message.content[0].text;

    // Write to file
    fs.writeFileSync("docs/runbook.html", htmlContent, "utf-8");
    console.log("Runbook generated successfully at docs/runbook.html");

    process.exit(0);
  } catch (error) {
    console.error("Error generating runbook:", error);
    process.exit(1);
  }
}

generateRunbook();
