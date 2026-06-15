# Cloud-Native Payments Platform (Demo)

A cloud-native platform reference implementation built using Spring Cloud, Quarkus, and React.  
This project demonstrates modern microservices architecture with secure authentication, reactive systems, and scalable infrastructure patterns.

---

## Key Features

- **BFF Authentication Pattern**
   - Secure session-based authentication using Auth0 + Spring Security OAuth2
   - No JWT exposed to the browser (HttpOnly cookies only)

- **Reactive Backend**
   - Non-blocking services using Spring WebFlux + R2DBC

- **Microservices Architecture**
   - Service discovery via Eureka
   - API Gateway with routing, retries, and circuit breakers

- **Polyglot Services**
   - Spring Boot (User Service)
   - Quarkus (Payment Service)

---

## Architecture Overview

### 1. React Frontend
- React 18 + Vite + TypeScript
- State management:
   - Zustand
   - React Query
   - React Hook Form + Zod (validation)
- BFF Auth:
   - Uses HttpOnly session cookies (no JWT in browser)
- CSRF Protection:
   - Reads `XSRF-TOKEN` cookie
   - Sends `X-XSRF-TOKEN` header for mutating requests

---

### 2. API Gateway (Spring Cloud Gateway)
- OAuth2 Client (Auth0 integration)
   - Handles login/logout
   - Stores tokens server-side (session)
- BFF boundary
   - Browser never accesses JWT
- Routing
   - Circuit breakers
   - Retry policies
   - Load-balanced routes (`lb://`)
- CSRF
   - `CookieServerCsrfTokenRepository`
   - Webhook endpoints excluded

---

### 3. Service Registry
- Netflix Eureka Server
- Secured via Spring Security (Basic Auth)
- Kubernetes health probes allowed without authentication

---

### 4. User Service
- Spring Boot (WebFlux)
- Fully reactive, non-blocking
- R2DBC + PostgreSQL
- OAuth2 Resource Server:
   - Validates Auth0 JWT via JWKS
   - Uses `sub` claim as `externalId`

---

### 5. Payment Service
- Quarkus (fast startup, low memory footprint)
- Hibernate ORM Panache (Active Record pattern)
- OIDC JWT validation via SmallRye OIDC
- Eureka registration (community extension)

---

## Local Development

### Prerequisites
- Java 21
- Node.js (v18+ recommended)
- Docker & Docker Compose

---

### Start all services

```bash
# Start infrastructure + all services
cd deployment/docker-compose
docker-compose up -d

```

### Environment variables (copy to `.env` in docker-compose folder)

```env
AUTH0_CLIENT_ID=your_auth0_client_id
AUTH0_CLIENT_SECRET=your_auth0_client_secret
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=your_razorpay_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
```

### Service URLs (local)

| Service | URL |
|---|---|
| React Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8081 |
| Eureka Dashboard | http://localhost:8761  |
| User Service | http://localhost:8086 |
| Payment Service | http://localhost:8087 |

Architecture
┌─────────────────────────────────────────────────────────────────┐
│                        React SPA (Frontend)                     │
│                                         						  │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring Cloud)                   │
│  - OAuth2 Client (Session Management)                           │
│  - Rate Limiting                                                │
│  - Token Relay to Backend Services                              │
│  - CORS Configuration                                           │
└──────────────────┬──────────────┬───────────────────────────────┘
                   │              │             
                   ↓              ↓             
              ┌──────────┐   ┌─────────────┐
              │  User    │   │  Payment    │
              │ Account  │   │  Service    │
              │ (Spring) │   │ (Quarkus)   │
              └──────────┘   └─────────────┘
                    │               │         
                    ↓               ↓         
                ┌──────────┐   ┌──────────┐  
                │PostgreSQL│   │PostgreSQL│
                │   DB     │   │    DB    │
                └──────────┘   └──────────┘  
                            │
                            ↓
                    ┌──────────────┐
                    │ RazorPay API │
                    └──────────────┘
                    All registered with
                          ↓
                  ┌──────────────────┐
                  │ Eureka Service   │
                  │    Registry      │
                  └──────────────────┘

AWS Deployment:

EC2 + Docker Compose (Using Free Tier)
All services run on a single EC2 c7 large using Docker Compose — identical to local development, pointed at RDS instead of a local Postgres container.

Browser → EC2 public IP :80
│
Nginx (reverse proxy)
├── /* ──────────────▶ React build (served from disk)
└── /api/* ──────────▶ API Gateway container :8081
                                │
                            All other containers
                            (Docker bridge network)
                                │
                            RDS PostgreSQL

ECS Fargate Deployment
Each service runs as an independent ECS Fargate task — no EC2 instances to manage.
CloudFront routes static assets to S3 and API traffic to the Application Load Balancer.

Browser → CloudFront (d3cxjtievlz5zc.cloudfront.net)
                    │
                    ├── /* ──────────────▶ S3 (React SPA, private + OAC)
                    │
                    └── /api/* /oauth2/*
                    /login /logout /user/*
                            │
                            ▼
                ALB  microservices-alb  :80
                            │
                            ▼
                    ┌─────────────────────────────┐
                    │  ECS Cluster                │  ap-south-2
                    │  microservices-cluster      │
                    │                             │
                    │  [api-gateway:4]      :8081 │ ← ALB target group
                    │  [serviceregistry:1]  :8761 │ ← Cloud Map DNS
                    │  [useraccount:2]      :8086 │ ← internal only
                    └─────────────────────────────┘
                                │
                                ▼
                RDS PostgreSQL  (db.t3.micro)
                learning-postgres.cdkqcgmmqweh.ap-south-2.rds.amazonaws.com

ECR pull fix (common issue): Fargate tasks need assignPublicIp=ENABLED and an outbound port 443 rule on the security group to reach the ECR API endpoint.
Without both, tasks fail with ResourceInitializationError: i/o timeout — which looks like a permissions error but is a network connectivity problem.