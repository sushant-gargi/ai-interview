<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_AI-Integrated-6DB33F?style=for-the-badge&logo=spring&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/>
  <img src="https://img.shields.io/badge/OpenRouter-LLM-7C3AED?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Kokoro-TTS-06B6D4?style=for-the-badge"/>
</p>

<h1 align="center">🎯 AI Interview Platform</h1>

<p align="center">
  <strong>Production-Oriented AI Interview Automation Platform</strong>
</p>

<p align="center">
  Resume-Aware Interviews · Adaptive Questioning · Voice AI · Automated Evaluation · Structured Hiring Reports
</p>

<p align="center">
  <a href="#-architecture">Architecture</a> ·
  <a href="#-key-features">Features</a> ·
  <a href="#-tech-stack">Tech Stack</a> ·
  <a href="#-api-reference">API Reference</a> ·
  <a href="#-getting-started">Getting Started</a> ·
  <a href="#-system-design-decisions">Design Decisions</a>
</p>

---

## 🎯 Production Engineering Highlights

✔ AI-driven adaptive interviewing using Spring AI and OpenRouter

✔ Resume-aware and Job Description-aware conversational evaluation

✔ Scheduler-driven handling of no-shows, abandoned sessions, and lifecycle automation

✔ Structured LLM outputs using BeanOutputConverter for type-safe report generation

✔ Stateless JWT authentication with method-level role-based authorization

✔ Voice-enabled interview experience using Speech-to-Text and Kokoro TTS

✔ Idempotent report generation to prevent duplicate evaluation records

✔ PostgreSQL persistence with JSON column support and custom JPQL queries

---

## What It Does

AI Interview Platform is a production-oriented AI interviewing backend built with Spring Boot, Spring AI, and PostgreSQL. It automates the complete interview lifecycle—from session scheduling and resume processing to AI-driven interviewing, evaluation, and report generation.

Recruiters create interview sessions, candidates upload resumes and join through a voice-enabled interview lobby, and an AI interviewer dynamically adapts questions based on the candidate's responses, resume content, and job requirements.

The platform includes automated session lifecycle management, structured AI evaluation reports, PDF export capabilities, email notifications, and scheduler-driven handling of no-shows and abandoned interviews with minimal operational intervention.

The interview lifecycle is automated from scheduling through evaluation with minimal recruiter intervention.

---

## 📊 System Capabilities

- Automated session lifecycle management through scheduled background jobs
- Concurrent-safe and idempotent report generation workflows
- Stateless JWT authentication with role-based access control
- Resume-aware and JD-aware AI evaluation pipeline
- Voice-enabled interviewing using Speech-to-Text and Text-to-Speech
- PostgreSQL persistence with JSON-based structured evaluation reports
- Fault-tolerant processing with isolated email, AI, and TTS failures
- End-to-end interview automation with minimal human intervention

---

## ✨ Key Features

### Recruiter Side
- **Session Management** — Create, schedule, and delete interview sessions with validation (no past dates, no self-interview, no repeat candidates within 1 year)
- **Role-Based Access Control** — `RECRUITER` and `CANDIDATE` roles enforced at the method level via Spring Security's `@PreAuthorize`
- **Structured Evaluation Reports** — AI-generated reports with overall score (0–10), hiring recommendation (`STRONG_YES` / `YES` / `MAYBE` / `NO`), per-skill breakdown map, and per-question scoring
- **PDF Report Export** — Download evaluation reports as formatted PDFs (generated with Apache PDFBox)
- **Automated Email Notifications** — HTML email invites sent to candidates on session creation; report-ready alerts sent to recruiters on completion (via JavaMail + Mailtrap)

### Candidate Side
- **Resume Upload & Parsing** — PDF resumes parsed to plain text using PDFBox and injected into the AI's system context before the interview begins
- **Lobby Gate** — Resume must be uploaded before the interview can start (hard enforcement)
- **Live Voice Interview** — Browser-native Speech-to-Text (Web Speech API) converts the candidate's spoken answers to text; AI responses are synthesized to speech via Kokoro TTS and streamed as Base64-encoded MP3
- **Heartbeat Tracking** — Periodic heartbeats from the frontend update `lastActiveAt` so the backend knows the candidate is still present

### AI Interviewer Engine
- **Adaptive Questioning** — The AI is instructed to read each answer for depth, probe with follow-up questions ("Why did you choose that?", "What are the edge cases?"), and never repeat a static checklist
- **Resume + JD Aware** — System prompt includes the candidate's parsed resume and the full job description (truncated at 3000 chars each to respect context limits)
- **Time-Aware Prompting** — A transient system message is injected into every turn with `minutesRemaining`; the AI is instructed to issue a soft closure warning in the final 2 minutes
- **Voice-Optimised Format** — System rules enforce 2–3 sentence maximum responses with no markdown, no bullet lists — output designed for TTS playback
- **Structured Report Generation** — End-of-session evaluation uses `BeanOutputConverter<AiReportResult>` for type-safe JSON parsing of the LLM's scoring response

### Reliability & Automation
- **Scheduled Session Cleanup** — Two background jobs run every 60 seconds:
  - **No-Show Rule**: SCHEDULED sessions past their grace period are auto-expired with a no-show report
  - **Disconnect Safety Rule**: IN_PROGRESS sessions exceeding their scheduled end time are auto-completed with a partial evaluation
- **Idempotent Report Generation** — All report generation paths check for an existing report before writing; safe to call multiple times
- **Graceful Error Isolation** — Email failures, TTS failures, and report generation failures are caught and logged; they never roll back the primary business transaction

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT LAYER                           │
│   Browser (HTML/JS) — Voice STT + Audio Playback UI         │
│   Recruiter Dashboard · Candidate Interview Lobby           │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST + JWT Bearer
┌──────────────────────▼──────────────────────────────────────┐
│                   SPRING BOOT BACKEND (port 8181)           │
│                                                             │
│  Controllers: Auth · Chat · InterviewSession · Recruiter    │
│                                                             │
│  Services:                                                  │
│   ├─ AuthService         (JWT generation & validation)      │
│   ├─ InterviewSessionService (lifecycle, gates, RBAC)       │
│   ├─ ChatService         (LLM orchestration, history)       │
│   ├─ ReportService       (AI eval, idempotent write)        │
│   ├─ ResumeService       (PDF parse, session binding)       │
│   ├─ TtsService          (Kokoro speech synthesis)          │
│   ├─ EmailService        (HTML invite + report alerts)      │
│   └─ PdfReportService    (PDFBox report rendering)          │
│                                                             │
│  Scheduling:                                                │
│   ├─ cleanupExpiredGracePeriods()  → every 60s             │
│   └─ cleanupAbandonedSessions()    → every 60s             │
└──────┬─────────────────────┬───────────────────────────────┘
       │                     │
┌──────▼──────┐    ┌─────────▼────────────────────────────────┐
│ PostgreSQL  │    │          EXTERNAL SERVICES                │
│  (port 5455)│    │                                          │
│             │    │  OpenRouter / OpenAI-compatible LLM API  │
│ Tables:     │    │  (model: openrouter/owl-alpha)           │
│  users      │    │                                          │
│  interview_ │    │  Kokoro TTS (local, port 8801)           │
│   sessions  │    │  OpenAI-compatible /v1/audio/speech      │
│  conv_msgs  │    │                                          │
│  resumes    │    │  Mailtrap SMTP (sandbox email testing)   │
│  reports    │    │                                          │
└─────────────┘    └──────────────────────────────────────────┘
```

### Data Model

```
users (id, name, email, password, role[RECRUITER|CANDIDATE], createdAt)
  │
  └──< interview_sessions (id, recruiter_id, candidateEmail, resume_id,
                           jobRole, jobDescription, status, scheduledStart,
                           scheduledEnd, actualStart, actualEnd, lastActiveAt)
         │
         ├──< conversation_messages (id, session_id, role[SYSTEM|USER|ASSISTANT], content)
         │
         └──< reports (id, session_id, summary, overallScore, recommendation,
                       skillBreakdown[JSON], questionScores[JSON])

resumes (id, user_id, fileName, filePath, parsedText)
```

## ⚡ Key Engineering Challenges Solved

### 1. Preventing Duplicate Evaluation Reports

Three independent execution paths can generate reports:

- Candidate manually ends interview
- No-show scheduler expires session
- Disconnect scheduler auto-completes session

Without protection, concurrent execution could create duplicate reports.

**Solution**

- Report generation is idempotent
- Every report path checks for an existing report before persisting
- Multiple invocations safely converge to a single report

---

### 2. Maintaining Accurate Interview State Without WebSockets

The platform must detect candidate disconnects and abandoned interviews.

A WebSocket solution would introduce connection management complexity and infrastructure overhead.

**Solution**

- Lightweight heartbeat endpoint updates `lastActiveAt`
- Background scheduler evaluates inactivity windows
- Sessions are automatically completed when abandonment conditions are met

This provides reliable disconnect detection while keeping the architecture stateless.

---

### 3. Keeping AI Responses Suitable for Voice Conversations

Most LLMs naturally generate long-form responses.

Long responses create poor voice interview experiences and increase candidate cognitive load.

**Solution**

- System prompt enforces strict conversational constraints
- Responses are limited to short spoken interactions
- AI asks exactly one question at a time
- Output is optimized specifically for TTS playback

---

### 4. Time-Aware AI Without Storing Runtime State in Memory

The interviewer must know when an interview is approaching completion.

Persisting countdown state inside the conversation context introduces synchronization problems.

**Solution**

- Remaining time is calculated dynamically for every request
- A transient system instruction is injected into the prompt
- The AI always receives an accurate countdown without storing mutable interview state

---

### 5. Full Conversation Reconstruction Without In-Memory Sessions

Many chatbot systems maintain server-side conversation state.

This complicates horizontal scaling and recovery.

**Solution**

- Every conversation message is persisted
- ChatService remains stateless
- Conversation history is rebuilt from the database on each request
- Any application instance can continue the interview seamlessly

---

### 6. Graceful Failure of Non-Critical Services

Email delivery, TTS generation, and report creation involve external systems.

A failure in these services should never invalidate interview progress.

**Solution**

- External integrations are isolated behind service boundaries
- Failures are logged and contained
- Core interview state transitions always succeed independently

---

## 🛠 Tech Stack

| Layer | Technology | Purpose |
|:---|:---|:---|
| **Framework** | Spring Boot 3.x | Core application runtime |
| **AI** | Spring AI + OpenRouter API | LLM chat orchestration, `BeanOutputConverter` for structured output |
| **Security** | Spring Security + JJWT | Stateless JWT auth, method-level RBAC |
| **Database** | PostgreSQL 15 + Spring Data JPA | Persistence; Hibernate `ddl-auto: update` |
| **Scheduling** | Spring `@Scheduled` | Background session lifecycle management |
| **PDF** | Apache PDFBox | Resume text extraction + evaluation PDF generation |
| **TTS** | Kokoro (local) | AI interviewer voice synthesis, MP3 stream → Base64 |
| **Email** | JavaMail + Mailtrap | HTML interview invites + report-ready notifications |
| **Validation** | Jakarta Bean Validation | Input validation on all request DTOs |
| **Docs** | SpringDoc / Swagger UI | Auto-generated API docs at `/swagger-ui.html` |
| **Frontend** | Vanilla HTML/JS | Lightweight interview UI with Web Speech API STT |

---

## 🔄 Session Lifecycle

```
SCHEDULED ──(candidate uploads resume + joins within grace period)──► IN_PROGRESS
     │                                                                      │
     │ (no-show: grace period expires)                      (candidate ends │ or disconnect)
     ▼                                                                      ▼
  EXPIRED ◄──────────────────────────────────────────────────────────── COMPLETED
  [no-show report]                                                    [eval report]
```

**State Transition Rules (enforced server-side):**
- Only `CANDIDATE` role can call `startSession`
- Resume upload is required before `startSession` (hard gate)
- Session cannot be started before `scheduledStart` or after `scheduledStart + 5 min` (grace period)
- Recruiters cannot create sessions for themselves (email check)
- Candidates cannot complete an interview more than once per year
- Recruiters can only delete their own `SCHEDULED` sessions

---

## 📡 API Reference

### Auth
| Method | Endpoint | Role | Description |
|:---|:---|:---|:---|
| `POST` | `/api/auth/signup` | Public | Register as Recruiter or Candidate |
| `POST` | `/api/auth/login` | Public | Login and receive JWT token |

### Sessions (Candidate)
| Method | Endpoint | Role | Description |
|:---|:---|:---|:---|
| `GET` | `/api/sessions` | Candidate | List all my sessions |
| `POST` | `/api/sessions/{id}/resume` | Candidate | Upload resume PDF for a session |
| `POST` | `/api/sessions/{id}/start` | Candidate | Start the interview (validates all gates) |
| `POST` | `/api/sessions/{id}/heartbeat` | Candidate | Keep session alive (updates `lastActiveAt`) |

### Chat
| Method | Endpoint | Role | Description |
|:---|:---|:---|:---|
| `POST` | `/api/sessions/{id}/chat` | Candidate | Send a message; receive AI response + TTS audio |
| `GET` | `/api/sessions/{id}/chat` | Candidate | Retrieve full chat history (SYSTEM messages filtered) |
| `POST` | `/api/sessions/{id}/chat/end` | Candidate | End session and trigger report generation |

### Recruiter
| Method | Endpoint | Role | Description |
|:---|:---|:---|:---|
| `POST` | `/api/recruiter/sessions` | Recruiter | Create a new interview session |
| `GET` | `/api/recruiter/sessions` | Recruiter | List all sessions created by me |
| `DELETE` | `/api/recruiter/sessions/{id}` | Recruiter | Delete a SCHEDULED session |
| `GET` | `/api/recruiter/sessions/{id}/report` | Recruiter | Get structured JSON evaluation report |
| `GET` | `/api/recruiter/sessions/{id}/report/pdf` | Recruiter | Download evaluation report as PDF |

All protected endpoints require `Authorization: Bearer <token>` header.

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 15 (running on port `5455`)
- [Kokoro TTS](https://github.com/thewh1teagle/kokoro-onnx) (optional — voice synthesis, port `8801`)
- An [OpenRouter](https://openrouter.ai) API key (or any OpenAI-compatible endpoint)

### 1. Database Setup

```sql
CREATE DATABASE ai_interview;
```

Hibernate will auto-create all tables on first run (`ddl-auto: update`).

### 2. Configuration

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5455/ai_interview
    username: your_postgres_user
    password: your_postgres_password
  ai:
    openai:
      api-key: YOUR_OPENROUTER_API_KEY
      base-url: https://openrouter.ai/api
      chat:
        options:
          model: openrouter/owl-alpha   # or any OpenAI-compatible model

app:
  jwt:
    secret: YOUR_256_BIT_HEX_SECRET    # min 32 bytes
    expiration-ms: 86400000            # 24 hours
  tts:
    enabled: true                      # set false to disable voice
    url: http://localhost:8801/v1/audio/speech
  mail:
    enabled: true                      # set false to skip emails
    from: noreply@aiinterview.com
```

### 3. Run

```bash
./mvnw spring-boot:run
```

Server starts on **port 8181**. Swagger UI is available at:

```
http://localhost:8181/swagger-ui.html
```

### 4. Try the Interview UI

Open `src/main/resources/static/index.html` (or the served path) in **Google Chrome** or **Microsoft Edge** (required for Web Speech API).

1. Enter your candidate email + password + session ID
2. Click **Login & Start Interview**
3. The AI greeter loads automatically — click 🎤 to speak (auto-submits after 3 seconds of silence)

---

## 🧠 System Design Decisions

### Why Spring AI's `BeanOutputConverter` for reports?
Rather than prompting the LLM with "return JSON" and manually parsing, `BeanOutputConverter<AiReportResult>` injects the Jackson schema into the prompt and provides a typed `convert()` method. This eliminates brittle string parsing and gives compile-time safety on the report structure. An extra strip of markdown fences handles models that add code blocks around JSON.

### Why inject a transient time-warning into every chat turn?
The session's `scheduledEnd` is known at request time, so the backend calculates `minutesRemaining` and injects a fresh `SystemMessage` on every turn. This avoids storing time-state in the LLM's context window, keeps the system prompt immutable, and ensures the AI always has an accurate countdown for its soft-closure logic.

### Why idempotent report generation?
Three different paths can trigger report creation: the candidate explicitly ending the session, the disconnect scheduler, and the grace-period scheduler. Making `generateCompletedReport()`, `generateNoShowReport()`, and `generateAbandonedSessionReport()` all check for an existing report before writing prevents duplicate records even under race conditions.

### Why heartbeat tracking over WebSockets?
Heartbeats (a periodic `POST /heartbeat` from the frontend) update `lastActiveAt` on the session. The scheduler compares this against `scheduledEnd + ABANDON_BUFFER_MINUTES` to detect abandoned sessions. This is simpler than maintaining a WebSocket connection and more resilient to network blips — the candidate's connection can drop and reconnect without losing session state.

### Why a separate `SYSTEM` role in `conversation_messages`?
Storing the system prompt as a first-class database record — rather than regenerating it on every request — means the chat history is a complete, auditable source of truth for any session. It also keeps `ChatServiceImpl` stateless: it rebuilds the full Spring AI message list from the DB on every turn, with no in-memory conversation held between requests.

---

## 📂 Project Structure

```
src/main/java/com/aiinterview/ai_interview/
├── config/
│   ├── AiConfig.java              # Spring AI ChatClient bean with logging advisor
│   └── InterviewConstants.java    # Centralized grace/abandon timing constants
├── controller/
│   ├── AuthController.java
│   ├── ChatController.java
│   ├── InterviewSessionController.java
│   └── RecruiterController.java
├── dto/                           # Immutable Java records for all request/response types
│   ├── auth/, chat/, report/, resume/, session/
├── entity/
│   ├── User.java                  # Implements UserDetails
│   ├── InterviewSession.java      # Core session entity with DB indexes on status+date
│   ├── ConversationMessage.java
│   ├── Resume.java
│   └── Report.java                # skillBreakdown + questionScores as @JdbcTypeCode JSON
├── enums/
│   ├── SessionStatus.java         # SCHEDULED | IN_PROGRESS | COMPLETED | EXPIRED
│   └── HiringRecommendation.java  # STRONG_YES | YES | MAYBE | NO
├── error/
│   ├── GlobalExceptionHandler.java
│   ├── BadRequestException.java
│   └── ResourceNotFoundException.java
├── repository/                    # Spring Data JPA repositories with custom JPQL queries
├── scheduling/
│   └── SessionCleanupScheduler.java  # Two @Scheduled jobs for lifecycle enforcement
├── security/
│   ├── AuthUtil.java              # JWT generation + verification + principal extraction
│   ├── JwtAuthFilter.java         # OncePerRequestFilter with exception delegation
│   ├── JwtUserPrincipal.java
│   └── SecurityConfig.java        # Stateless, CORS-open, method security enabled
└── service/
    ├── impl/
    │   ├── AuthServiceImpl.java
    │   ├── ChatServiceImpl.java       # Core LLM orchestration logic
    │   ├── InterviewSessionServiceImpl.java
    │   ├── ReportServiceImpl.java     # AI evaluation + idempotent report writes
    │   ├── ResumeServiceImpl.java     # PDF upload + PDFBox text extraction
    │   ├── TtsServiceImpl.java        # Kokoro RestClient integration
    │   ├── EmailServiceImpl.java      # HTML email templates (invite + report-ready)
    │   └── PdfReportService.java      # PDFBox multi-page report renderer
    └── [interfaces for all above]
```

---

## 🔐 Security Notes

- Passwords are hashed with **BCrypt** via Spring Security's `PasswordEncoder`
- JWT tokens are signed with **HMAC-SHA** using a configurable secret (min 256-bit recommended)
- Email is normalized to **lowercase** at signup and on all lookups to prevent duplicate accounts
- Recruiters are prevented from interviewing themselves via an explicit email equality check at session creation
- File uploads are saved with **UUID-prefixed filenames** to prevent path traversal and filename collisions
- `SYSTEM` role messages are filtered out of the `/chat` history response — internal prompts are never exposed to the client
- Spring Security's `@PreAuthorize("hasRole('RECRUITER')")` enforces role separation at the method level

---

## Future Enhancements

- WebSocket-based live interviewer presence
- Vector database for long resume retrieval
- Multi-round interview workflows
- Recruiter analytics dashboard
- Real-time interview monitoring
- Interview recording and playback

---

## 🤝 Contributing

Pull requests are welcome. For significant changes, please open an issue first to discuss what you'd like to change.

---

<p align="center">
  Built with Spring Boot · Spring AI · PostgreSQL · Kokoro TTS · Apache PDFBox
</p>
