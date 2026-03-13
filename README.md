# RecoFlow — UPI Reconciliation SaaS

> Multi-tenant SaaS that auto-reconciles UPI/bank statement transactions with invoices using an explainable matching engine.

---

## 🚀 Quick Start (Docker — recommended)

### Prerequisites
- Docker Desktop installed and running
- Ports 5432, 8080, 3000 available

```bash
# 1. Clone the repo
git clone https://github.com/your-username/recoflow.git
cd recoflow

# 2. Copy env file
cp .env.example .env

# 3. Start everything
docker compose up --build

# Backend API:  http://localhost:8080
# Frontend UI:  http://localhost:3000
# Swagger docs: http://localhost:8080/swagger-ui.html
```

---

## 🛠️ Local Development (without Docker)

### Prerequisites
- Java 21 (install via [SDKMAN](https://sdkman.io): `sdk install java 21-tem`)
- Node.js 20+ (`nvm install 20`)
- PostgreSQL 16 running locally

### Backend

```bash
cd backend

# Set env vars (or export them in your shell)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/recoflow
export SPRING_DATASOURCE_USERNAME=recoflow
export SPRING_DATASOURCE_PASSWORD=recoflow
export JWT_SECRET=c2VjcmV0LWtleS1mb3ItcmVjb2Zsb3ctc2Fhcy1kZW1v

# Run
./mvnw spring-boot:run
```

> Flyway will automatically create all tables on first run.

### Frontend

```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:3000
```

---

## 📁 Project Structure

```
recoflow/
├── backend/                        # Spring Boot 3 + Java 21
│   ├── src/main/java/com/recoflow/
│   │   ├── config/                 # SecurityConfig, TenantContext, AsyncConfig
│   │   ├── controller/             # REST controllers (Auth, Customer, Invoice, etc.)
│   │   ├── dto/                    # Request/Response DTOs
│   │   ├── entity/                 # JPA entities
│   │   ├── enums/                  # InvoiceStatus, ReconStatus, FileStatus
│   │   ├── exception/              # GlobalExceptionHandler
│   │   ├── filter/                 # JwtAuthFilter
│   │   ├── repository/             # Spring Data JPA repositories
│   │   ├── security/               # UserDetailsServiceImpl
│   │   ├── service/                # Business logic + MatchingEngine
│   │   └── util/                   # JwtUtil
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/           # Flyway V1–V4 SQL migrations
│
├── frontend/                       # React + Vite + Tailwind
│   └── src/
│       ├── pages/                  # All page components
│       ├── components/layout/      # Sidebar layout
│       ├── services/api.js         # Axios API client
│       └── store/authStore.js      # Zustand auth state
│
├── seed/
│   └── sample_statement.csv        # Demo CSV for testing
├── docker-compose.yml
└── .env.example
```

---

## 🔐 Authentication Flow

1. **Register** → `POST /api/auth/register` → creates tenant + ADMIN user → returns JWT
2. **Login** → `POST /api/auth/login` (requires `tenantId` from registration) → returns JWT
3. All subsequent requests → `Authorization: Bearer <token>`

### RBAC Roles

| Role | Permissions |
|------|------------|
| `ROLE_ADMIN` | Full access including user management + audit logs |
| `ROLE_ACCOUNTANT` | Invoices, statements, reconciliation, reports |
| `ROLE_STAFF` | View only (invoices, customers, reports) |

---

## 🔄 Core Workflow

```
1. Register org → Login
2. Create Customers → Create Invoices (status: DRAFT → SENT)
3. Upload CSV bank statement → auto-parsed in background
4. Matching engine runs → creates SUGGESTED reconciliations with confidence score
5. Review suggestions → Confirm / Reject / Manual link
6. Invoice status auto-updates (PARTIAL / PAID)
7. View Reports → Daily collections, Outstanding invoices
```

---

## 🧠 Matching Engine

Scoring formula (max = 100 points):

| Factor | Points | Logic |
|--------|--------|-------|
| Amount match | 40 | Exact=40, ±1%=20, else=0 |
| Time window | 20 | Same day=20, ±1 day=10, ±3 days=5 |
| Remark similarity | 25 | Invoice no in remark=25, customer name=15, partial=5 |
| VPA hint match | 15 | customer.vpaHint matches payer_vpa=15 |

Confidence levels: **HIGH** ≥ 0.80 · **MEDIUM** ≥ 0.50 · **LOW** < 0.50

---

## 📊 API Endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET/POST | `/api/customers` | STAFF+ |
| GET/POST | `/api/invoices` | STAFF+ |
| PUT | `/api/invoices/{id}/status` | ACCOUNTANT+ |
| POST | `/api/statements/upload` | ACCOUNTANT+ |
| GET | `/api/statements/{id}/status` | STAFF+ |
| GET | `/api/reconciliations` | STAFF+ |
| POST | `/api/reconciliations/{id}/confirm` | ACCOUNTANT+ |
| POST | `/api/reconciliations/{id}/reject` | ACCOUNTANT+ |
| POST | `/api/reconciliations/manual` | ACCOUNTANT+ |
| GET | `/api/reports/daily-collections` | STAFF+ |
| GET | `/api/reports/outstanding` | STAFF+ |
| GET | `/api/audit-logs` | ADMIN |

Full docs: `http://localhost:8080/swagger-ui.html`

---

## 🧪 Running Tests

```bash
cd backend

# Unit tests only (no DB needed)
./mvnw test -Dtest=MatchingEngineTest

# All tests including integration (requires Docker for Testcontainers)
./mvnw verify
```

---

## 🚢 CSV Format for Import

```csv
date,time,amount,utr,remark,payer_vpa,type
20/01/2025,14:32:00,5000.00,UTR123456789,Payment INV-2025-0042,user@upi,CREDIT
```

A sample file is at `seed/sample_statement.csv`.

---

## 🔧 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/recoflow` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `recoflow` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `recoflow` | DB password |
| `JWT_SECRET` | (base64 string) | JWT signing secret — **change in prod!** |
| `VITE_API_URL` | `http://localhost:8080` | Backend URL for frontend |

---

## 🛣️ Roadmap

- [ ] PDF statement parsing
- [ ] Duplicate/refund detection
- [ ] WhatsApp/SMS reminders for outstanding invoices
- [ ] Tally export
- [ ] Analytics: revenue trends, customer risk scoring
- [ ] Mobile app for field collections

---

## 📄 License

MIT
