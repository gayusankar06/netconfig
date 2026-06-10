# NetConfigAI — Enterprise Network Configuration Review Platform
WELCOME!
NOTE! (live frontend render running link: https://netconfigure.onrender.com/) & (Backend need more resources for deploy on render and other platform also like gcp cloud run, mig, gke  etc.. or aws ags, eks, ec2 etc.. or other hosting platforms as well, all we have only free tier, with in free tier account we cant able to deploy, thats why showcased the frontend alone) THANK YOU!

<div align="center">

![NetConfigAI](https://img.shields.io/badge/NetConfigAI-Enterprise-3B82F6?style=for-the-badge)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react)
![FastAPI](https://img.shields.io/badge/FastAPI-0.111-009688?style=for-the-badge&logo=fastapi)
![Gemini AI](https://img.shields.io/badge/Gemini_AI-Powered-4285F4?style=for-the-badge&logo=google)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**AI-powered network configuration change review with automated risk scoring, compliance checking, and enterprise approval workflows.**

[🚀 Live Demo](#running-locally) · [📄 Architecture](#architecture-overview) · [🧪 Tests](#running-tests) · [📹 Demo Video](#demo-video)

</div>

---

## 🎯 Problem Statement

Network engineers change firewall rules, security groups, and router configs daily. A single misconfiguration can expose critical infrastructure to attack. Manual peer review is slow, inconsistent, and doesn't scale. **NetConfigAI** automates this with Gemini AI to detect risks, check compliance, and manage the approval workflow — end to end.

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🤖 **AI Diff Analysis** | Gemini 1.5 Flash analyzes every configuration change and assigns a risk score 0–100 |
| 🛡️ **Compliance Checks** | CIS Benchmarks, NIST 800-53, PCI-DSS, SOC2 — automated Pass/Fail per control |
| ✅ **Approval Workflow** | Approve, Reject, or Escalate reviews with mandatory comment and audit trail |
| 📊 **Live Dashboard** | Real-time stat cards, risk distribution charts, compliance health bars |
| 📋 **Audit Log** | Immutable enterprise event trail for every action (login, approve, reject, download) |
| 🔐 **Google OAuth** | Sign in with Google — auto-registers new users as network engineers |
| 📄 **PDF Reports** | Download enterprise reports with risk charts, compliance findings, AI recommendations |
| 🎯 **Onboarding Tour** | 7-step guided tour for new users using react-joyride |
| 🔄 **Agent Loop** | Background Celery worker polls and triggers Gemini analysis asynchronously |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React + Vite)                       │
│  ┌─────────┐  ┌──────────┐  ┌─────────────┐  ┌──────────────────┐  │
│  │Dashboard│  │UploadPage│  │ReviewDetail │  │CompliancePage    │  │
│  │(recharts│  │(wizard)  │  │(diff+tabs)  │  │AuditLog HelpPage │  │
│  └────┬────┘  └────┬─────┘  └──────┬──────┘  └──────────────────┘  │
│       └────────────┴───────────────┘                                 │
│                     Axios + JWT Auth Headers                         │
└────────────────────────────┬────────────────────────────────────────┘
                             │ REST API
┌────────────────────────────▼────────────────────────────────────────┐
│                    BACKEND (FastAPI + Python)                         │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │Auth API  │  │Reviews API   │  │Reports API   │  │Dashboard   │  │
│  │/register │  │/approve      │  │/pdf          │  │/stats      │  │
│  │/login    │  │/reject       │  │/compliance   │  │            │  │
│  │/google   │  │/escalate     │  └──────────────┘  └────────────┘  │
│  └──────────┘  └──────┬───────┘                                      │
│                        │ Celery Task                                  │
│  ┌─────────────────────▼────────────────────────────────────────┐   │
│  │              AI AGENT LOOP (Celery + Redis)                   │   │
│  │  1. Receive config files  →  2. Parse format (JSON/YAML/IOS)  │   │
│  │  3. Diff Engine (DeepDiff + unified-diff)                      │   │
│  │  4. Risk Scorer (CIDR/port/credential detection)               │   │
│  │  5. Gemini 1.5 Flash (AI risk + compliance analysis)           │   │
│  │  6. Store results → 7. Notify frontend via polling             │   │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────────┬───────────────────────────────────────────────────────┘
               │
    ┌──────────▼──────────┐     ┌──────────────────────┐
    │  PostgreSQL 15       │     │  Redis 7             │
    │  - users            │     │  - Celery broker     │
    │  - reviews          │     │  - Celery results    │
    │  - diff_changes     │     │  - Session cache     │
    │  - audit_logs       │     └──────────────────────┘
    │  - workflow_steps   │
    └─────────────────────┘
                              ┌──────────────────────┐
                              │  Google Gemini API   │
                              │  gemini-1.5-flash    │
                              │  (External AI LLM)   │
                              └──────────────────────┘
```

### AI Agent Loop

The core AI capability is an **Agent Loop** — a background Celery worker that:
1. Receives a submitted config review task
2. Parses the config format (auto-detects JSON, YAML, Cisco IOS, Palo Alto)
3. Runs a 2-pass diff: semantic DeepDiff + unified text-diff fallback
4. Scores each change for risk (CIDR expansion, port exposure, credential changes)
5. Calls **Gemini 1.5 Flash** with a structured prompt for AI risk analysis + compliance evaluation
6. Persists all findings to PostgreSQL
7. Frontend polls `/api/v1/reviews/{id}/status` until complete

### MCP / External API Integration

- **Gemini API** (Google AI Studio) — External LLM for analysis
- **Google OAuth2 UserInfo API** — Social login token exchange
- **Recharts** — Real-time data visualization
- **ReportLab** (PDF generation) — Enterprise report generation

---

## 🛠️ Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | React + TypeScript + Vite | 18 + 5.4 |
| UI Library | Material UI | 5.x |
| Charts | Recharts | 2.x |
| Onboarding | react-joyride | 2.7 |
| Backend | FastAPI | 0.111 |
| AI/LLM | Google Gemini 1.5 Flash | via API |
| Task Queue | Celery + Redis | 5.x + 7.x |
| Database | PostgreSQL | 15 |
| ORM | SQLAlchemy (async) | 2.x |
| Auth | JWT + bcrypt + Google OAuth | — |
| Container | Docker + Docker Compose | — |
| Diff Engine | DeepDiff + difflib | — |
| PDF | ReportLab | — |
| Source Control | GitHub | — |

---

## 🚀 Setup & Run Instructions

### Prerequisites

- Docker Desktop (with Docker Compose v2)
- Git
- A Gemini API key (free at [aistudio.google.com](https://aistudio.google.com))

### 1. Clone the Repository

```bash
git clone https://github.com/gayusankar06/netconfig.git
cd netconfig
```

### 2. Configure Environment

```bash
# Copy and edit the env file
cp .env.example .env
```

Edit `.env` and set your Gemini API key:
```env
GEMINI_API_KEY=your_gemini_api_key_here
VITE_GOOGLE_CLIENT_ID=your_google_client_id_here   # Optional: for Google Sign-In
```

### 3. Start All Services

```bash
docker compose up -d
```

This starts: PostgreSQL, Redis, FastAPI backend, Celery worker, React frontend.

### 4. Access the Application

| Service | URL |
|---|---|
| **Frontend** | http://localhost:3000 |
| **Backend API** | http://localhost:8000 |
| **API Docs (Swagger)** | http://localhost:8000/docs |
| **Health Check** | http://localhost:8000/health/ready |

### 5. Login

| Email | Password | Role |
|---|---|---|
| `admin@netconfigdiff.com` | `Admin123!` | Admin |
| `admin@netconfig.ai` | `Admin123!` | Super Admin |
| `Demo@gmail.com` | `Admin123!` | Network Engineer |

---

## 🧪 Running Tests

### Backend Tests (pytest)

```bash
cd backend
pip install -r requirements.txt
pytest tests/ -v --tb=short
```

### Frontend Tests (Vitest)

```bash
cd frontend
npm install
npm run test
```

### Run All Tests via Docker

```bash
docker exec netconfig_backend pytest tests/ -v
docker exec netconfig_frontend npm run test -- --run
```

---

## 📁 Project Structure

```
netconfig/
├── backend/
│   ├── app/
│   │   ├── api/v1/          # FastAPI route handlers
│   │   │   ├── auth.py      # Login, register, Google OAuth
│   │   │   ├── reviews.py   # CRUD + approve/reject/escalate
│   │   │   ├── dashboard.py # Live stats
│   │   │   ├── audit.py     # Audit log trail
│   │   │   └── reports.py   # PDF generation
│   │   ├── models/          # SQLAlchemy ORM models
│   │   ├── services/
│   │   │   ├── diff_engine.py    # DeepDiff + unified-diff engine
│   │   │   ├── gemini_service.py # Gemini AI integration
│   │   │   └── report_service.py # PDF report builder
│   │   ├── workers/tasks.py  # Celery AI agent loop
│   │   └── main.py
│   ├── tests/               # pytest test suite
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Dashboard.tsx      # Live stats + charts
│   │   │   ├── UploadPage.tsx     # Config upload wizard
│   │   │   ├── ReviewDetailPage.tsx# Diff + workflow actions
│   │   │   ├── CompliancePage.tsx # Compliance overview
│   │   │   ├── AuditLogPage.tsx   # Audit trail
│   │   │   ├── HelpPage.tsx       # Documentation + FAQ
│   │   │   ├── OnboardingTour.tsx # react-joyride tour
│   │   │   ├── Sidebar.tsx        # Navigation sidebar
│   │   │   └── AuthPage.tsx       # Login + Google OAuth
│   │   └── App.tsx
│   └── Dockerfile
├── sample_data/             # Sample config files for demo
├── docs/                    # Documentation
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🤖 AI Capabilities Demonstrated

### ✅ Agent Loop
Background Celery worker continuously processes submitted reviews:
```
Submit → Parse → Diff → Risk Score → Gemini AI → Store → Notify
```

### ✅ External API Integration
- **Google Gemini 1.5 Flash** — LLM for risk analysis and compliance evaluation
- **Google OAuth2 UserInfo API** — Social authentication

### ✅ MCP-style Tool Consumption
- Structured tool-style prompting to Gemini with JSON schema response
- Config parser service auto-detects and normalizes multiple formats

---

## 📐 Assumptions & Limitations

| Item | Detail |
|---|---|
| **Gemini API** | Requires a valid Gemini API key (free tier available) |
| **Docker** | Requires Docker Desktop v2+ with Compose v2 |
| **Google OAuth** | Requires a Google Cloud OAuth client ID for social login |
| **SSL** | Not configured for local dev (HTTP only). Use a reverse proxy (Nginx/Caddy) for production |
| **Config Size** | Large configs (>1MB) may be truncated for Gemini prompt limits |
| **Analysis Time** | Gemini analysis takes 10–30 seconds per review |
| **PDF Reports** | Requires completed AI analysis (status: pending_review or approved) |
| **Port Exposure** | App runs on ports 3000, 8000, 5432, 6379 — ensure they are free |

---

## 📹 Demo Video

> 5–7 minute walkthrough recorded with OBS/Loom demonstrating:
> 1. Login (email + Google OAuth)
> 2. Upload Old and New config files
> 3. Watch AI analysis run in background
> 4. Review diff + risk score + AI summary
> 5. Approve/Reject/Escalate with comment
> 6. View Compliance findings
> 7. Download PDF report
> 8. Check Audit Log

**Demo Video Link:** [Submit after recording]

---

## 🏆 Challenge Criteria Checklist

| # | Criterion | Status |
|---|---|---|
| 1 | AI-Assisted Development | ✅ Gemini AI used throughout |
| 2 | Agent Loop | ✅ Celery async AI agent |
| 3 | MCP / External API | ✅ Gemini API + Google OAuth API |
| 4 | Working End-to-End | ✅ Full CRUD + workflow + reports |
| 5 | Code Quality | ✅ TypeScript + typed Python |
| 6 | Documentation | ✅ This README + docs/ folder |

---

## 📄 License

MIT License — Free to use, modify, and distribute.
