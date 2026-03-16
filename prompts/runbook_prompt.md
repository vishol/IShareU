You are a senior Site Reliability Engineer and Software Architect.

Analyze the provided repository and generate a COMPLETE production runbook for the application.

Your output MUST be a single HTML file.

The runbook must be written for operations engineers who will support the application in production.

Follow the exact structure below.

----------------------------------
RUNBOOK STRUCTURE
----------------------------------

1. Application Name

2. Application Description
Explain the purpose of the application and what business problem it solves.

3. Application Overview
Provide a high level overview of the system including:
- main components
- services
- APIs
- databases
- external dependencies

4. Architecture Diagram
Generate a Mermaid diagram representing the architecture.

Example:

graph TD
Client --> API_Gateway
API_Gateway --> Application_Service
Application_Service --> Database
Application_Service --> External_API

5. Sequence Diagram
Create a Mermaid sequence diagram showing the primary workflow.

Example:

sequenceDiagram
User->>API: Request
API->>Service: Process request
Service->>Database: Query data
Database-->>Service: Return data
Service-->>API: Response
API-->>User: Result

6. Processing Steps
Provide step-by-step explanation of how the application processes requests.

7. Technology Stack
Automatically detect and list technologies used in the repository.

Include:
- programming languages
- frameworks
- databases
- messaging systems
- infrastructure tools

8. Division
If not available, infer from repository structure.

9. Group

10. Family

11. L1_Product

12. Product Manager

13. Scrum Team

If this information is unavailable, mark as "Unknown".

14. DataSet
List key datasets or databases used by the application.

15. Code Location
Provide repository path and major directories.

Example:

/src
/api
/services
/infrastructure

16. Deployment Architecture
Explain how the application is deployed.

Detect indicators such as:
- Docker
- Kubernetes
- Terraform
- CI/CD pipelines

17. Monitoring and Alerts
Describe possible monitoring tools and alerts.

18. Recent Issues Fixed
Analyze recent commits and summarize fixes.

19. Potential Code Improvements
Analyze the repository and suggest improvements.

Consider:
- performance
- scalability
- error handling
- architecture
- security

20. Troubleshooting Guide
Provide common issues and possible fixes.

Example sections:
- Service not starting
- Database connection failures
- API timeout issues

----------------------------------
HTML REQUIREMENTS
----------------------------------

The output must be valid HTML.

Include:

- Title
- Table of Contents
- Styled sections
- Code blocks
- Mermaid diagrams

Use simple CSS styling.

----------------------------------
IMPORTANT RULES
----------------------------------

1. Infer architecture from repository structure
2. Detect dependencies automatically
3. Generate diagrams using Mermaid syntax
4. Make reasonable assumptions if information is missing
5. Focus on operational support and troubleshooting

----------------------------------
OUTPUT FORMAT
----------------------------------

Return ONLY the HTML file.
Do not include explanations outside the HTML.
