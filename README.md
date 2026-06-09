# AI Network Config Diff Reviewer

![Architecture](https://via.placeholder.com/800x400?text=AI+Network+Config+Diff+Reviewer)

## Project Overview

An enterprise-grade AI-powered platform that compares two versions of network configuration files (Terraform, JSON, YAML, AWS/GCP/Azure exports), identifies configuration changes, performs intelligent security risk analysis using a locally hosted Llama 3 LLM via Ollama, validates against compliance frameworks, and produces approval-ready reports.

## Prerequisites

- **Docker Desktop** (or Docker Engine + Docker Compose)
- **Ollama** (installed locally or running via Docker)
- **Node.js 20+** (for local frontend development)
- **Python 3.11+** (for local backend development)

## Quick Start

Run these 5 commands to get the application running locally:

```bash
# 1. Clone/navigate to repository
cd d:/web_netconfig/ai-network-config-diff-reviewer

# 2. Setup environment variables
cp .env.example .env

# 3. Start services
make up

# 4. Wait for database, then run migrations and seed data
make migrate
make seed

# 5. Access the application
# Frontend: http://localhost:3000
# Backend API Docs: http://localhost:8000/docs
```

## Detailed Setup Guide

The application uses Docker Compose to run all services locally. The `docker-compose.yml` file defines the following services:
- `backend`: FastAPI application
- `frontend`: React/Vite development server
- `postgres`: PostgreSQL 16 database
- `redis`: Redis 7.2 (cache & Celery broker)
- `ollama`: Local LLM server
- `celery_worker`: Background task worker
- `celery_beat`: Background task scheduler
- `prometheus`: Metrics collection
- `grafana`: Metrics visualization

### Pulling the Llama 3 Model

The Ollama container needs the `llama3` model to perform AI analysis. You can pull it manually:

```bash
docker exec -it ai-network-config-diff-reviewer-ollama-1 ollama pull llama3
```

## Configuration Reference

Key `.env` variables:

- `DATABASE_URL`: PostgreSQL connection string.
- `REDIS_URL`: Redis connection string.
- `OLLAMA_BASE_URL`: URL to the Ollama server (e.g., `http://ollama:11434`).
- `OLLAMA_MODEL`: The LLM model to use (default: `llama3`).
- `JWT_SECRET_KEY`: Secret key for signing JWT tokens.

## API Documentation

Once the backend is running, the interactive API documentation (Swagger UI) is available at:
`http://localhost:8000/docs`

## Sample Usage Walkthrough

1. Login using the default admin credentials (created via `make seed`).
2. Navigate to the "Upload Config" page.
3. Select "AWS Security Group" as the configuration type.
4. Upload `sample-configs/aws-sg-old.json` and `sample-configs/aws-sg-new.json`.
5. Review the structural differences in the Diff Viewer.
6. Click "Run AI Analysis" to trigger risk scoring and Llama 3 evaluation.
7. Review the compliance violations and AI recommendations.
8. Approve or reject the change.
9. Download the PDF report.

## Architecture Decisions

- **FastAPI**: Chosen for its async support, performance, and built-in OpenAPI generation.
- **React + Vite**: Chosen for fast development builds and modern component ecosystem.
- **Ollama**: Allows running LLMs locally to ensure sensitive network configurations never leave the enterprise network.
- **Celery + Redis**: Used to offload long-running tasks like AI analysis and report generation to background workers.

## Future Enhancements Roadmap

- Multi-Cloud Risk Correlation
- Azure Support
- ServiceNow Integration
- GitHub Pull Request Analysis
- AI Remediation Suggestions
