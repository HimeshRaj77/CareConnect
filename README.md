# CareConnect Lite

Live Hosted Link: _Add your deployed URL here_

## NGO Use-Case
CareConnect Lite is a lightweight triage and support portal for a local healthcare NGO. Patients or family members can submit urgent medical or support concerns through a simple form. The system stores requests centrally so volunteers and coordinators can prioritize outreach quickly.

## Smart Auto-Triage (AI Feature)
When a support request is submitted, the backend sends the message text to an LLM (Google Gemini API).
The AI returns:
- `urgency`: `High`, `Medium`, or `Low`
- `summary`: one concise sentence that captures the request

The application then stores both AI outputs with the original message in MySQL. If the LLM call fails (timeout, quota, parse issue), the request is still saved with safe fallback values so no user request is lost.

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Data JPA
- MySQL
- Vanilla HTML, CSS, JavaScript (served by Spring Boot static resources)
- Google Gemini REST API
- Maven
- Docker

## Project Structure
- `src/main/java` - Spring Boot backend (controller, service, repository, entity)
- `src/main/resources/application.properties` - production-style config using environment variables
- `src/main/resources/application-local.properties` - local development sample config
- `src/main/resources/static` - frontend files (`index.html`, `styles.css`, `app.js`)

## Architecture Notes
- Single-service deployment: frontend and backend are bundled together in one Spring Boot application.
- Service-layer AI integration: controller delegates triage logic to `SupportRequestService`.
- Resilient backend flow: failed AI calls do not block request persistence.
- Secrets externalized: database credentials and API keys are injected via environment variables.

## Local Setup Instructions
### 1. Prerequisites
- Java 17+
- Maven 3.9+
- MySQL running locally

### 2. Create Database
Run this once in MySQL:

```sql
CREATE DATABASE careconnect_db;
```

### 3. Configure Local Environment
Set environment variables in your terminal:

```bash
export DB_URL='jdbc:mysql://localhost:3306/careconnect_db?useSSL=false&serverTimezone=UTC'
export DB_USER='root'
export DB_PASS='your_mysql_password'
export API_KEY='your_gemini_api_key'
export LLM_MODEL='gemini-2.5-flash'
```

Alternative: keep values in `src/main/resources/application-local.properties` and run with local profile.

### 4. Run the App

```bash
mvn spring-boot:run
```

The frontend is available at:
- `http://localhost:8080/`

API endpoint:
- `POST http://localhost:8080/api/requests`

### 5. Build JAR

```bash
mvn clean package
java -jar target/careconnect-lite-0.0.1-SNAPSHOT.jar
```

## Docker Deployment
Build and run:

```bash
docker build -t careconnect-lite .
docker run -p 8080:8080 \
  -e DB_URL='jdbc:mysql://<host>:3306/careconnect_db?useSSL=false&serverTimezone=UTC' \
  -e DB_USER='<db_user>' \
  -e DB_PASS='<db_pass>' \
  -e API_KEY='<gemini_api_key>' \
  -e LLM_MODEL='gemini-2.5-flash' \
  careconnect-lite
```

## Internship Demo Tips
- Show one successful submission flow from UI to DB.
- Show AI-generated `aiUrgency` and `aiSummary` in the stored record.
- Explain fallback behavior (`UNASSIGNED`) for API failures to highlight reliability engineering.
