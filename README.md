# stock_display_alpha_vantage — quickstart (informal)

This repo has a small Spring Boot backend that pulls stock data (AlphaVantage) and an Angular frontend.

Heads up: do NOT commit API keys into this repo. Use an environment variable called `FINANCIAL_API_KEY`.

Get your API key set (PowerShell):

Temporary (just for current terminal session):

```powershell
$env:FINANCIAL_API_KEY = 'your_api_key_here'
# then run the backend from repo root
cd C:\Users\..\..\stock_display_alpha_vantage
.\mvnw.cmd spring-boot:run
```

Make it persistent for your user (PowerShell):

```powershell
[Environment]::SetEnvironmentVariable('FINANCIAL_API_KEY','your_api_key_here','User')
# open a new terminal afterwards
```

Or pass the key once directly when running (maven or jar):

```powershell
# with maven
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--financial.api.key=your_api_key_here"

# or with the built jar
java -jar target\finansal_deneme-0.0.1-SNAPSHOT.jar --financial.api.key=your_api_key_here
```

How to run (dev)

Backend (spring boot):

```powershell
cd C:\..\..\..\stock_display_alpha_vantage
.\mvnw.cmd spring-boot:run
```

Frontend (angular):

```powershell
cd frontend
npm install
npm start # or ng serve
```

Notes / troubleshooting

- Project targets Java 21. Make sure you have JDK 21 installed.
- If you hit a Lombok/JDK compile problem, update `lombok.version` in `pom.xml` or wipe the local maven cache for Lombok and try again.
- In production prefer a real secrets manager (Azure Key Vault, GitHub/GitLab actions secrets, etc.).


