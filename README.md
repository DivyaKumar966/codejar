# CodeJar

A modern Java and Python coding platform built with React and Spring Boot.

## Features

- Java and Python code execution only
- Standard input and live console output
- Responsive dark/light modern workspace UI
- Existing JWT authentication APIs retained
- 5-second execution timeout, temporary run workspaces, request validation

## Run locally

Start the backend (requires PostgreSQL configured in `backend/src/main/resources/application.properties`):

```powershell
cd backend
mvn spring-boot:run
```

Then start the React app in another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## Secure execution setup

Code runs inside locked-down Docker containers, never on the host runtime. Install/start Docker Desktop, then pull the permitted images once:

```powershell
docker pull eclipse-temurin:21-jdk
docker pull python:3.12-alpine
```

The runner disables networking, uses a read-only filesystem and ephemeral `/tmp`, limits memory/CPU/processes, and enforces a 5-second timeout.
