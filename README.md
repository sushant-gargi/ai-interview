<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_AI-2.0.0--M1-6DB33F?style=for-the-badge&logo=spring&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-Integrated-336791?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-JJWT_0.12.6-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/>
  <img src="https://img.shields.io/badge/OpenRouter-LLM-7C3AED?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Kokoro-TTS-06B6D4?style=for-the-badge"/>
</p>

<h1 align="center">🎯 AI Interview Platform</h1>

<p align="center">
  <strong>AI-Powered Technical Interview Automation — Built with Spring Boot 4, Spring AI, and PostgreSQL</strong>
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

## 🎯 Engineering Highlights

✔ AI-driven adaptive interviewing using Spring AI 2.0 and OpenRouter

✔ Resume-aware and Job Description-aware conversational evaluation (3000-char context truncation)

✔ Scheduler-driven lifecycle: no-show detection, abandoned session recovery, auto-reporting

✔ Structured LLM output using `BeanOutputConverter<AiReportResult>` for type-safe report generation

✔ Stateless JWT authentication with method-level role-based authorization (`@PreAuthorize`)

✔ Voice-enabled interview experience: Web Speech API STT + Kokoro TTS → Base64 MP3

✔ Idempotent report generation — all three report paths are safe to invoke multiple times

✔ PostgreSQL with `@JdbcTypeCode(SqlTypes.JSON)` columns and composite indexes on scheduler-critical queries

✔ Java 21 Records used for all DTOs — 17 immutable record types across the codebase

✔ `open-in-view: false` and `FetchType.LAZY` on all relations — JPA configured for production behaviour

---

## What It Does

AI Interview Platform automates the complete technical interview lifecycle. A recruiter creates a session with a job role, job description, and candidate email. The candidate receives an HTML email invite, uploads their resume in the lobby, and is interviewed by an AI that adapts its questions based on their resume, the job requirements, and each individual answer.

When the session ends — whether the candidate finishes normally, disconnects, or never shows up — the system automatically generates a structured evaluation report with an overall score, a hiring recommendation, a per-skill breakdown, and question-by-question scoring. A report-ready notification is emailed to the recruiter.

No human intervention is required after scheduling.

---

## 📊 System Capabilities

- Automated session lifecycle via two independent `@Scheduled` background jobs
- Idempotent report generation — three distinct paths, one report, no duplicates
- Stateless JWT authentication with RBAC enforced at the method and service layer
- Resume-aware and JD-aware AI evaluation pipeline with context-length management
- Voice-enabled interviewing: Speech-to-Text input + TTS audio output per response
- `@JdbcTypeCode(SqlTypes.JSON)` PostgreSQL columns for skill breakdown and Q&A scoring
- Fault-tolerant external integrations — email, TTS, and report failures never abort core state transitions
- Scheduler queries bounded to `TOP 100` to prevent memory pressure under high load

---

## ✨ Key Features

### Recruiter Side
- **Session Management** — Create, schedule, and delete interview sessions with validation: no past dates, no self-interview, no repeat candidates within 1 year (`hasCompletedSessionInLastYear` JPQL query)
- **Role-Based Access Control** — `RECRUITER` and `CANDIDATE` roles enforced at the controller level via `@PreAuthorize("hasRole('RECRUITER')")` and at the service level via `user.getRole() != Role.CANDIDATE`
- **Structured Evaluation Reports** — AI-generated reports with overall score (0–10), hiring recommendation (`STRONG_YES` / `YES` / `MAYBE` / `NO`), per-skill breakdown, and per-question scoring — parsed type-safely via `BeanOutputConverter<AiReportResult>`
- **PDF Report Export** — Multi-page evaluation PDFs generated with Apache PDFBox 3.0.3, with automatic page-break detection
- **Automated Email Notifications** — Responsive HTML email invites sent to candidates on session creation; report-ready summaries with score rings sent to recruiters on completion

### Candidate Side
- **Resume Upload & Parsing** — PDF resumes extracted to plain text using `PDFTextStripper`, UUID-prefixed on disk to prevent path traversal, and injected into the AI system prompt before the first question
- **Lobby Gate** — `session.getResume() == null` is a hard stop: the interview cannot start without an uploaded resume
- **Live Voice Interview** — Web Speech API (Chrome/Edge) converts spoken answers to text with continuous recognition and 3-second silence auto-submit; AI responses are synthesised by Kokoro TTS and returned as Base64 MP3 audio
- **Heartbeat Tracking** — `POST /heartbeat` updates `lastActiveAt`; the disconnect scheduler uses this to detect abandoned sessions

### AI Interviewer Engine
- **Adaptive Questioning** — System prompt instructs the AI to analyse each answer for depth, probe with targeted follow-ups ("Why did you choose that?", "What are the edge cases?"), and ask exactly one question at a time
- **Resume + JD Context** — Parsed resume text and job description are each truncated to 3000 characters and injected into the system prompt to keep the AI within LLM context limits
- **Time-Aware Prompting** — `minutesRemaining` is calculated dynamically on every turn and injected as a transient `SystemMessage`; the AI issues a soft closure warning in the final 2 minutes without persisting state
- **Voice-Optimised Output** — System rules enforce 2–3 sentence responses with no markdown or bullet lists — output is designed for TTS playback rather than reading
- **Structured Report Generation** — `BeanOutputConverter<AiReportResult>` injects the Jackson JSON schema into the evaluation prompt and provides typed `convert()` parsing; markdown fences in the response are stripped before parsing for compatibility with different models

### Reliability & Automation
- **Scheduled Session Cleanup** — Two `@Scheduled(fixedDelay = 60000)` jobs with staggered `initialDelay` (10s and 15s) to avoid startup contention:
  - **No-Show Rule**: SCHEDULED sessions past `scheduledStart + 5 minutes` are marked EXPIRED; a no-show report is generated
  - **Disconnect Safety Rule**: IN_PROGRESS sessions past their `scheduledEnd + 5 minutes` or with stale `lastActiveAt` are auto-completed with a partial evaluation
- **Idempotent Report Generation** — `generateCompletedReport()`, `generateNoShowReport()`, and `generateAbandonedSessionReport()` all check for an existing report before writing; safe to call from multiple paths
- **Graceful Error Isolation** — TTS, email, and report failures are caught and logged individually; they never propagate to the HTTP response or roll back the session state transition

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT LAYER                           │
│   Browser (HTML/JS) — Web Speech API STT + Kokoro TTS UI   │
│   Recruiter Dashboard · Candidate Interview Lobby           │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST + JWT Bearer
┌──────────────────────▼──────────────────────────────────────┐
│            SPRING BOOT 4.0.6 BACKEND (port 8181)            │
│                                                             │
│  Controllers: Auth · Chat · InterviewSession · Recruiter    │
│                                                             │
│  Services:                                                  │
│   ├─ AuthService              (BCrypt + JJWT 0.12.6)        │
│   ├─ InterviewSessionService  (lifecycle gates, RBAC)       │
│   ├─ ChatService              (Spring AI orchestration)     │
│   │    └─ ChatClient + SimpleLoggerAdvisor                  │
│   ├─ ReportService            (idempotent AI evaluation)    │
│   ├─ ResumeService            (PDFBox parse + UUID naming)  │
│   ├─ TtsService               (Kokoro RestClient)           │
│   ├─ EmailService             (HTML templates + JavaMail)   │
│   └─ PdfReportService         (multi-page PDFBox renderer)  │
│                                                             │
│  Scheduling (@Scheduled, fixedDelay=60s):                   │
│   ├─ cleanupExpiredGracePeriods()   [initialDelay=10s]      │
│   └─ cleanupAbandonedSessions()     [initialDelay=15s]      │
└──────┬─────────────────────┬───────────────────────────────┘
       │                     │
┌──────▼──────┐    ┌─────────▼────────────────────────────────┐
│ PostgreSQL  │    │          EXTERNAL SERVICES                │
│  (port 5455)│    │                                          │
│             │    │  OpenRouter (openrouter/owl-alpha)        │
│ Entities:   │    │  OpenAI-compatible chat API              │
│  users      │    │                                          │
│  interview_ │    │  Kokoro TTS (local, port 8801)           │
│   sessions  │    │  OpenAI-compatible audio/speech          │
│  conv_msgs  │    │                                          │
│  resumes    │    │  Mailtrap SMTP (sandbox.smtp.mailtrap.io)│
│  reports    │    │                                          │
│             │    └──────────────────────────────────────────┘
│ Indexes:    │
│  idx_status_start (status, scheduled_start)
│  idx_status_end   (status, scheduled_end)
└─────────────┘
```

### Data Model

```
users (id, name, email[unique], password, role[RECRUITER|CANDIDATE], createdAt)
  │
  └──< interview_sessions (id, recruiter_id, candidateEmail, resume_id,
                           jobRole, jobDescription, status, scheduledStart,
                           scheduledEnd, actualStart, actualEnd, lastActiveAt, createdAt)
         │                [indexed on (status, scheduledStart) and (status, scheduledEnd)]
         │
         ├──< conversation_messages (id, session_id, role[SYSTEM|USER|ASSISTANT], content[TEXT])
         │
         └──◇  reports (id, session_id[1:1], summary[TEXT], overallScore,
                        recommendation[ENUM], skillBreakdown[JSON], questionScores[JSON])

resumes (id, user_id, fileName, filePath[UUID-prefixed], parsedText[TEXT], uploadedAt)
```

---

## ⚡ Key Engineering Challenges Solved

### 1. Preventing Duplicate Evaluation Reports

Three independent paths can trigger report generation — the candidate manually ending the session, the no-show scheduler, and the disconnect scheduler. Without protection, concurrent execution could produce duplicate records.

**Solution:** Every report generation method checks for an existing report via `reportRepository.findBySessionId(sessionId).isPresent()` before writing. All three paths converge safely to a single record.

---

### 2. Detecting Abandoned Interviews Without WebSockets

The platform must know when a candidate disconnects mid-interview. A WebSocket approach would introduce connection management complexity and break the stateless architecture.

**Solution:** The frontend sends a lightweight `POST /heartbeat` every few seconds, updating `lastActiveAt` on the session. The background scheduler compares this timestamp against `scheduledEnd + ABANDON_BUFFER_MINUTES` and auto-completes stale sessions.

---

### 3. Keeping AI Responses Suitable for Voice

Most LLMs generate long-form, markdown-heavy responses by default. Long responses create poor TTS playback experiences and increase candidate cognitive load mid-interview.

**Solution:** The system prompt enforces strict output constraints — maximum 2–3 sentences, no markdown, no bullet lists, one question per response. The AI is designed to be a voice conversationalist, not a document generator.

---

### 4. Time-Aware AI Without Persisted State

The interviewer must know when the session is approaching its end time, but storing a countdown in the database or in-memory introduces synchronization problems.

**Solution:** `minutesRemaining` is calculated dynamically at request time (`Duration.between(Instant.now(), session.getScheduledEnd()).toMinutes()`) and injected as a transient `SystemMessage` that is never persisted. The system prompt remains immutable; the AI always receives an accurate countdown.

---

### 5. Stateless Conversation Reconstruction

Many chat systems keep an in-memory conversation state, which breaks horizontal scaling and recovery after restarts.

**Solution:** Every message — including `SYSTEM`, `USER`, and `ASSISTANT` — is persisted to `conversation_messages`. `ChatServiceImpl` is fully stateless: it rebuilds the Spring AI message list from the database on every turn. Any instance can handle any request.

---

### 6. Graceful Failure of Non-Critical Integrations

Email, TTS, and report generation each involve external systems that can fail independently. A failure in any of them should never invalidate the interview or the session state.

**Solution:** Each external call is wrapped in an isolated try-catch block. Failures are logged at `ERROR` level and swallowed. Core state transitions (session status, message persistence) are committed independently before external calls are made.

---

## 🛠 Tech Stack

| Layer | Technology | Details |
|:---|:---|:---|
| **Framework** | Spring Boot 4.0.6 | Core runtime, auto-configuration |
| **AI / LLM** | Spring AI 2.0.0-M1 + OpenRouter | `ChatClient`, `BeanOutputConverter`, `SimpleLoggerAdvisor` |
| **Security** | Spring Security + JJWT 0.12.6 | Stateless JWT, `@PreAuthorize`, `OncePerRequestFilter` |
| **Database** | PostgreSQL + Spring Data JPA | Hibernate `ddl-auto: update`, `open-in-view: false`, `FetchType.LAZY` |
| **Scheduling** | Spring `@Scheduled` | Two jobs, `fixedDelay=60s`, staggered `initialDelay` |
| **PDF** | Apache PDFBox 3.0.3 | Resume text extraction + multi-page report rendering |
| **TTS** | Kokoro (local, port 8801) | `RestClient` POST → Base64 MP3 response |
| **Email** | JavaMail + Mailtrap sandbox | `MimeMessageHelper`, responsive HTML templates |
| **Validation** | Jakarta Bean Validation | `@NotBlank`, `@Email`, `@Size` on all request records |
| **Utilities** | Lombok | `@Builder`, `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j`, `@FieldDefaults` |
| **API Docs** | SpringDoc OpenAPI 2.8.8 | Swagger UI at `/swagger-ui.html` |
| **Frontend** | Vanilla HTML/JS | Single-file UI, Web Speech API STT, Base64 audio playback |

---

## 🔄 Session Lifecycle

```
SCHEDULED ──(resume uploaded + candidate joins within grace period)──► IN_PROGRESS
     │                                                                       │
     │ [scheduler: scheduledStart + 5min elapsed, no join]   [candidate ends │ or scheduler:
     ▼                                                         scheduledEnd + │ 5min + stale heartbeat]
  EXPIRED                                                                    ▼
  [no-show report: score 0, recommendation NO]                          COMPLETED
                                                                    [AI evaluation report]
```

**State Transition Rules (enforced server-side):**
- Only users with `role = CANDIDATE` can call `startSession` (role check, not just email check)
- Resume upload required before `startSession` — hard gate with `BadRequestException`
- Session cannot start before `scheduledStart` or after `scheduledStart + 5 min`
- Recruiters cannot create sessions for themselves (email equality check)
- Candidates cannot complete an interview more than once per calendar year
- Recruiters can only delete their own `SCHEDULED` sessions (not IN_PROGRESS or COMPLETED)

---

## 📡 API Reference

### Auth
| Method | Endpoint | Auth | Description |
|:---|:---|:---|:---|
| `POST` | `/api/auth/signup` | Public | Register; `role` field defaults to `CANDIDATE`, accepts `RECRUITER` |
| `POST` | `/api/auth/login` | Public | Authenticate and receive signed JWT token |

### Sessions (Candidate)
| Method | Endpoint | Auth | Description |
|:---|:---|:---|:---|
| `GET` | `/api/sessions` | Bearer | List all sessions where `candidateEmail` matches current user |
| `POST` | `/api/sessions/{id}/resume` | Bearer | Upload resume PDF; session must be SCHEDULED |
| `POST` | `/api/sessions/{id}/start` | Bearer | Start interview; validates all gates, initialises AI context |
| `POST` | `/api/sessions/{id}/heartbeat` | Bearer | Update `lastActiveAt`; session must be IN_PROGRESS |

### Chat
| Method | Endpoint | Auth | Description |
|:---|:---|:---|:---|
| `POST` | `/api/sessions/{id}/chat` | Bearer | Send message; receives AI text + Base64 TTS audio |
| `GET` | `/api/sessions/{id}/chat` | Bearer | Retrieve history; SYSTEM messages filtered from response |
| `POST` | `/api/sessions/{id}/chat/end` | Bearer | Mark COMPLETED and trigger evaluation report |

### Recruiter
| Method | Endpoint | Auth | Description |
|:---|:---|:---|:---|
| `POST` | `/api/recruiter/sessions` | Recruiter | Create session; sends HTML invite email to candidate |
| `GET` | `/api/recruiter/sessions` | Recruiter | List all sessions created by authenticated recruiter |
| `DELETE` | `/api/recruiter/sessions/{id}` | Recruiter | Delete SCHEDULED session; cleans up resume file on disk |
| `GET` | `/api/recruiter/sessions/{id}/report` | Recruiter | Retrieve structured JSON evaluation report |
| `GET` | `/api/recruiter/sessions/{id}/report/pdf` | Recruiter | Download evaluation as multi-page PDF |

All protected endpoints require `Authorization: Bearer <token>` header.

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (required — `<java.version>21</java.version>` in pom.xml)
- PostgreSQL running on port `5455`
- [Kokoro TTS](https://github.com/thewh1teagle/kokoro-onnx) (optional — voice synthesis on port `8801`; set `app.tts.enabled: false` to skip)
- An [OpenRouter](https://openrouter.ai) API key (or any OpenAI-compatible LLM endpoint)

### 1. Database Setup

```sql
CREATE DATABASE ai_interview;
```

Hibernate with `ddl-auto: update` creates all tables on first boot.

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
    secret: YOUR_256_BIT_HEX_SECRET    # minimum 32 bytes
    expiration-ms: 86400000            # 24 hours
  tts:
    enabled: true                      # set false to disable Kokoro
    url: http://localhost:8801/v1/audio/speech
  mail:
    enabled: true                      # set false to skip email sending
    from: noreply@aiinterview.com
```

### 3. Run

```bash
./mvnw spring-boot:run
```

Server starts on **port 8181**. Swagger UI is at:

```
http://localhost:8181/swagger-ui.html
```

### 4. Interview UI

Open the served HTML page in **Google Chrome** or **Microsoft Edge** (required for Web Speech API).

1. Enter your candidate email, password, and session ID
2. Click **Login & Start Interview**
3. The AI greeting loads automatically — click 🎤 to speak (auto-submits after 3 seconds of silence)

---

## 🧠 System Design Decisions

### Why `BeanOutputConverter<AiReportResult>` instead of manual JSON parsing?

`BeanOutputConverter` injects the Jackson schema into the evaluation prompt using `converter.getFormat()`, so the LLM receives an exact structural contract rather than a vague "return JSON" instruction. The `convert()` method handles deserialisation with full type safety. A pre-processing step strips markdown code fences (```` ```json ```` blocks) that some models add, making the approach robust across different LLM providers on OpenRouter.

### Why inject a transient time warning on every chat turn?

`session.getScheduledEnd()` is available at request time, so `minutesRemaining` is computed fresh on every turn as `Duration.between(Instant.now(), scheduledEnd).toMinutes()`. This is injected as a new `SystemMessage` that is never persisted. The system prompt remains immutable and the AI always receives an accurate countdown — no stale cached state.

### Why idempotent report generation?

Three execution paths can reach the report creation logic: the candidate calling `POST /chat/end`, the grace-period scheduler expiring a no-show session, and the disconnect scheduler auto-closing an abandoned session. All three methods call `reportRepository.findBySessionId(id).isPresent()` before writing. Multiple invocations safely converge to one record.

### Why heartbeat polling over WebSockets?

WebSockets would introduce connection lifecycle management, reconnection logic, and stateful server infrastructure. A periodic `POST /heartbeat` requires none of this. It updates `lastActiveAt` on the session row; the scheduler checks whether `lastActiveAt` has gone stale relative to `scheduledEnd + ABANDON_BUFFER_MINUTES`. The candidate's connection can drop and reconnect without losing session state.

### Why store all messages — including SYSTEM — in the database?

Persisting the system prompt alongside `USER` and `ASSISTANT` messages means `ChatServiceImpl` is entirely stateless. It fetches the full message list from the database on every request, maps them to Spring AI types, and calls the LLM. Any server instance can handle any request without shared in-memory state. The `SYSTEM` role messages are filtered from the `/chat` history response so internal prompts are never exposed to clients.

### Why `open-in-view: false` and `FetchType.LAZY` everywhere?

`open-in-view: true` (the Spring Boot default) holds a database connection open for the full duration of the HTTP request, including view rendering. Setting it to `false` constrains JPA sessions to the service layer transaction boundary. Combined with `FetchType.LAZY` on all `@ManyToOne` and `@OneToOne` relations, this prevents unintended N+1 queries from crossing transaction boundaries.

---

## 📂 Project Structure

```
src/main/java/com/aiinterview/ai_interview/
├── config/
│   ├── AiConfig.java              # ChatClient bean; SimpleLoggerAdvisor for LLM call logging
│   └── InterviewConstants.java    # Centralised grace/abandon timing constants (5 min each)
├── controller/
│   ├── AuthController.java
│   ├── ChatController.java
│   ├── InterviewSessionController.java
│   └── RecruiterController.java   # @PreAuthorize("hasRole('RECRUITER')") class-level
├── dto/                           # 17 immutable Java 21 records across all request/response types
│   ├── auth/, chat/, report/, resume/, session/
├── entity/
│   ├── User.java                  # Implements UserDetails; single email unique constraint
│   ├── InterviewSession.java      # Composite DB indexes on (status,scheduledStart/End)
│   ├── ConversationMessage.java
│   ├── Resume.java
│   └── Report.java                # skillBreakdown + questionScores via @JdbcTypeCode(JSON)
├── enums/
│   ├── SessionStatus.java         # SCHEDULED | IN_PROGRESS | COMPLETED | EXPIRED
│   └── HiringRecommendation.java  # STRONG_YES | YES | MAYBE | NO
├── error/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice; handles 6 exception types
│   ├── BadRequestException.java
│   └── ResourceNotFoundException.java
├── repository/                    # Spring Data JPA; custom JPQL for scheduler queries + 1-year check
├── scheduling/
│   └── SessionCleanupScheduler.java  # Two @Scheduled jobs; staggered initialDelay
├── security/
│   ├── AuthUtil.java              # HMAC-SHA JWT generation + claim extraction
│   ├── JwtAuthFilter.java         # OncePerRequestFilter; delegates exceptions to HandlerExceptionResolver
│   ├── JwtUserPrincipal.java      # Java record; holds userId, email, authorities
│   └── SecurityConfig.java        # Stateless, method security enabled, CORS configured
└── service/
    ├── impl/
    │   ├── AuthServiceImpl.java          # BCrypt + dual duplicate-email guard
    │   ├── ChatServiceImpl.java          # LLM orchestration, message flush, TTS integration
    │   ├── InterviewSessionServiceImpl.java  # Session gates, role checks, scheduler triggers
    │   ├── ReportServiceImpl.java         # BeanOutputConverter, idempotent writes, email notify
    │   ├── ResumeServiceImpl.java         # UUID naming, PDFTextStripper, session binding
    │   ├── TtsServiceImpl.java            # RestClient → Kokoro → Base64
    │   ├── EmailServiceImpl.java          # HTML templates with inline CSS; IST timezone formatting
    │   └── PdfReportService.java          # PdfRenderer inner class with checkPageBreak()
    └── [service interfaces for all above]
```

---

## 🔐 Security Notes

- Passwords hashed with **BCrypt** via Spring Security's `PasswordEncoder`
- JWT tokens signed with **HMAC-SHA** using a configurable hex secret; `Keys.hmacShaKeyFor()` ensures minimum key length
- Duplicate email at signup guarded by **both** a pre-check (`existsByEmail`) and a `catch(DataIntegrityViolationException)` to handle the race condition between check and insert
- Email addresses normalised to **lowercase** at signup and on all session lookups to prevent account duplication via casing
- Recruiters blocked from creating sessions for themselves via explicit email equality check
- File uploads stored with **`UUID.randomUUID() + "_" + Paths.get(filename).getFileName()`** — prevents path traversal and filename collisions
- `SYSTEM` role messages filtered from the `GET /chat` response — internal prompts never reach the client
- `@PreAuthorize("hasRole('RECRUITER')")` on `RecruiterController` at class level; `role == CANDIDATE` check inside `startSession` service for defence-in-depth
- **Note (dev config):** CORS is currently `allowedOriginPatterns("*")` and `/actuator/**` is publicly accessible — appropriate for local development, should be locked down before production deployment

---

## Future Enhancements

- Docker Compose setup for one-command local dev (PostgreSQL + Kokoro + application)
- WebSocket-based live session monitoring for recruiters
- Vector database integration for semantic resume retrieval
- Multi-round interview workflows (phone screen → technical → system design)
- Recruiter analytics dashboard (score distributions, pass rates, time-to-hire)
- CORS and Actuator hardening for production deployment

---

## 🤝 Contributing

Pull requests are welcome. For significant changes, please open an issue first to discuss what you'd like to change.

---

<p align="center">
  Built with Spring Boot 4 · Spring AI 2.0 · Java 21 · PostgreSQL · Apache PDFBox 3 · Kokoro TTS · JJWT 0.12.6
</p>
