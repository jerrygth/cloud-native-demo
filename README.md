```markdown
# Cloud-Native Payments Platform (Demo)

A cloud-native platform reference implementation built using Spring Cloud, Quarkus, and React. This project demonstrates a modern microservices architecture featuring secure authentication, reactive systems, and scalable infrastructure patterns.

---

## 🚀 Key Features

* **BFF Authentication Pattern**
  * Secure session-based authentication using Auth0 + Spring Security OAuth2
  * No JWT exposed to the browser (HttpOnly cookies only)
* **Reactive Backend**
  * Non-blocking services built using Spring WebFlux + R2DBC
* **Microservices Architecture**
  * Service discovery via Eureka
  * API Gateway featuring routing, retries, and circuit breakers
* **Polyglot Services**
  * Spring Boot (User Service)
  * Quarkus (Payment Service)

---

## 🏗️ Architecture Overview

### 1. React Frontend
* **Stack:** React 18 + Vite + TypeScript
* **State Management & Forms:** Zustand, React Query, and React Hook Form + Zod (validation)
* **BFF Auth:** Uses HttpOnly session cookies (no JWT exposed to the browser)
* **CSRF Protection:** Reads the `XSRF-TOKEN` cookie and attaches the `X-XSRF-TOKEN` header for mutating requests

### 2. API Gateway (Spring Cloud Gateway)
* **OAuth2 Client:** Integrates with Auth0 to handle login/logout, storing tokens securely server-side within the session
* **BFF Boundary:** Ensures the browser never accesses the JWT
* **Routing:** Manages load-balanced routes (`lb://`) built with circuit breakers and retry policies
* **CSRF:** Utilizes `CookieServerCsrfTokenRepository` while excluding webhook endpoints

### 3. Service Registry
* Powered by Netflix Eureka Server.
* Secured via Spring Security (Basic Auth).
* Configured to allow Kubernetes health probes without authentication.

### 4. User Service
* Built on Spring Boot (WebFlux) as a fully reactive, non-blocking application.
* Uses R2DBC + PostgreSQL.
* Functions as an OAuth2 Resource Server that validates Auth0 JWTs via JWKS and maps the `sub` claim as the `externalId`.

### 5. Payment Service
* Built with Quarkus for fast startup times and a low memory footprint.
* Implements the Active Record pattern using Hibernate ORM Panache.
* Validates OIDC JWTs via SmallRye OIDC and handles Eureka registration via a community extension.

---

## 📊 System Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                        React SPA (Frontend)                     │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring Cloud)                   │
│  - OAuth2 Client (Session Management)                           │
│  - Rate Limiting                                                │
│  - Token Relay to Backend Services                              │
│  - CORS Configuration                                           │
└──────────────────┬──────────────┬───────────────────────────────┘
                   │              │             
                   ▼              ▼             
              ┌──────────┐   ┌─────────────┐
              │  User    │   │  Payment    │
              │ Account  │   │  Service    │
              │ (Spring) │   │ (Quarkus)   │
              └──────────┘   └─────────────┘
                    │               │         
                    ▼               ▼         
                ┌──────────┐   ┌──────────┐  
                │PostgreSQL│   │PostgreSQL│
                │   DB     │   │    DB    │
                └──────────┘   └──────────┘  
                            │
                            ▼
                    ┌──────────────┐
                    │ RazorPay API │
                    └──────────────┘
                            │
               All registered via Cloud Map/DNS
                            ▼
                  ┌──────────────────┐
                  │ Eureka Service   │
                  │    Registry      │
                  └──────────────────┘

```

---

## 🛠️ Local Development

### Prerequisites

* Java 21


* Node.js (v18+ recommended)


* Docker & Docker Compose



### Start All Services

```bash
# Navigate to the deployment directory and spin up the infrastructure
cd deployment/docker-compose
docker-compose up -d

```

### Environment Variables

Create a `.env` file inside your `docker-compose` folder and add the following configuration:

```env
AUTH0_CLIENT_ID=your_auth0_client_id
AUTH0_CLIENT_SECRET=your_auth0_client_secret
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=your_razorpay_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

```

### Local Service URLs

| Service | URL |
| --- | --- |
| **React Frontend** | http://localhost:3000 |
| **API Gateway** | http://localhost:8081 |
| **Eureka Dashboard** | http://localhost:8761 |
| **User Service** | http://localhost:8086 |
| **Payment Service** | http://localhost:8087 |

---

## ☁️ AWS Deployment

### Option 1: EC2 + Docker Compose (Free Tier)

All services run on a single EC2 `c7.large` instance using Docker Compose. This environment mirrors local development completely, except it points to an Amazon RDS instance instead of a local PostgreSQL container.

```text
Browser ──> EC2 Public IP:80 ──> Nginx (Reverse Proxy)
                                    ├── /* ──> React Build (Served from disk)
                                    └── /api/* ──> API Gateway Container (:8081)
                                                              │
                                                     (Docker Bridge Network)
                                                              │
                                                        RDS PostgreSQL

```

### Option 2: ECS Fargate Deployment

Each service runs as an independent, serverless ECS Fargate task. CloudFront routes static assets directly to S3 and forwards API traffic to the Application Load Balancer (ALB).

```text
Browser ──> CloudFront (d3cxjtievlz5zc.cloudfront.net)
               │
               ├── /* ──> S3 Bucket (React SPA via OAC)
               │
               └── /api/*, /oauth2/*, /login, /logout, /user/*
               │
               ▼
          ALB (microservices-alb:80)
               │
               ▼
     ┌────────────────────────────────────────────────────────┐
     │ ECS Cluster (microservices-cluster) - Region: ap-south-2│
     │                                                        │
     │  [api-gateway:4]       :8081  <-- ALB Target Group     │
     │  [serviceregistry:1]   :8761  <-- Cloud Map DNS        │
     │  [useraccount:2]       :8086  <-- Internal Only        │
     └────────────────────────────────────────────────────────┘
               │
               ▼
     RDS PostgreSQL (db.t3.micro)
     learning-postgres.cdkqcgmmqweh.ap-south-2.rds.amazonaws.com

```

> 💡 **ECR Pull Fix (Common Deployment Issue):**
> Fargate tasks require `assignPublicIp=ENABLED` and an outbound port `443` rule configured on their security group to reach the Amazon ECR API endpoint. Without both settings, tasks will fail with a `ResourceInitializationError: i/o timeout`—which mimics a permissions issue but is actually a network connectivity barrier.
>
>

```

```