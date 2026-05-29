# FinAnalytics Platform

Production-inspired fraud detection and secure transaction monitoring system built with Java 21 and Spring Boot 3.

## Status

🚧 Ongoing personal project

Currently focused on:

* Core backend architecture
* Fraud detection engine design
* Security layer implementation
* Transaction processing pipeline

Still in progress:

* Database schema implementation
* Full backend integration
* Postman API testing
* Docker orchestration
* Redis integration
* Frontend/dashboard layer
* Automated testing suite

---

## Overview

FraudGuard simulates how modern financial institutions process and monitor digital transactions at scale.

The project explores how banks and fintech platforms evaluate transaction risk in real time using layered fraud detection rules, behavioural analysis, secure authentication, and audit logging.

Inspired by the architecture and operational patterns used by South African financial institutions such as FNB, Absa, Capitec, and Standard Bank.

## Tech Stack

* Java 21
* Spring Boot 3
* Spring Security
* REST APIs
* JWT Authentication
* Maven
* MySQL/PostgreSQL *(planned)*
* Redis *(planned)*
* Docker *(planned)*

---

# Fraud Detection Engine

Multi-rule fraud scoring system that evaluates every transaction and assigns:

* Risk score (0–100)
* Fraud status
* Recommended action

### Actions

* ALLOW
* MONITOR
* REVIEW
* BLOCK

### Fraud Rules

* High-value transaction detection
* Transaction velocity checks
* Impossible travel detection
* Unknown device fingerprinting
* Unusual transaction hour detection
* Behavioural anomaly detection using z-score analysis
* New receiver account detection

---

# Authentication & Security

Planned security implementation includes:

* JWT access and refresh tokens
* BCrypt password hashing
* Role-based access control
* Account lockout protection
* Rate limiting using Bucket4j
* Input validation using Bean Validation
* SQL injection prevention via JPA
* Audit logging
* Secure API error handling
* CORS policy enforcement

---

# System Architecture

The project follows a layered Spring Boot architecture:

Controller → Service → Repository

## Components

### API Layer

* Authentication endpoints
* Transaction processing endpoints
* Analytics endpoints

### Security Layer

* JWT authentication filter
* Rate limiting filter
* Spring Security configuration

### Business Logic Layer

* Transaction orchestration service
* Fraud detection engine
* Behaviour profile service

### Data Layer

* Spring Data JPA repositories
* Transaction persistence
* User behavioural profiling

### Infrastructure

* Docker containers
* MySQL database
* Redis cache/rate-limit store

---

# Behavioural Profiling (Planned)

The system is designed to maintain a rolling behavioural profile for each user, including:

* Average transaction amount
* Transaction frequency
* Known devices
* Common countries
* Statistical anomaly thresholds

This profile will support more advanced fraud detection and anomaly scoring.

---

# Planned API Endpoints

| Method | Endpoint                      | Purpose                   |
| ------ | ----------------------------- | ------------------------- |
| POST   | `/api/auth/register`          | Register user             |
| POST   | `/api/auth/login`             | Login + JWT generation    |
| POST   | `/api/auth/refresh`           | Refresh access token      |
| POST   | `/api/transactions`           | Submit transaction        |
| GET    | `/api/transactions`           | Transaction history       |
| GET    | `/api/transactions/flagged`   | View flagged transactions |
| GET    | `/api/analytics/summary`      | Spending analytics        |
| GET    | `/api/analytics/risk-profile` | User fraud profile        |

---

# Planned Database Design

Database schema design has not yet been fully implemented.

Current planned entities:

* Users
* Transactions
* User Behaviour Profiles
* Roles
* Audit Logs

The schema will focus on:

* Transaction traceability
* Fraud analysis performance
* Secure data handling
* Auditability
* Scalable querying

---

# Planned Infrastructure

## Development Environment

* H2 in-memory database

## Production-Oriented Stack

* MySQL 8
* Redis 7
* Docker Compose
* Multi-stage Docker builds

---

# Learning Goals

This project was built to explore:

* Secure backend development
* Fintech system architecture
* Fraud detection design patterns
* Payment processing workflows
* Banking-grade API security
* Production-oriented Spring Boot architecture

---

# Future Improvements

* Machine learning fraud scoring
* Real-time streaming analytics
* Admin review dashboard
* SIEM/security monitoring integration
* Kafka event processing
* Advanced anomaly detection
* Cloud deployment (AWS)

---

# Running the Project

## Current Status

Setup instructions will be expanded as implementation progresses.

### Planned Development Run

```bash
./mvnw spring-boot:run
```

### Planned Docker Run

```bash
docker-compose up --build
```

---

# Notes

This is a personal learning project designed to simulate real-world fintech and fraud monitoring systems.

The goal is not only to build features, but to understand the architectural, security, and operational thinking behind modern payment infrastructure.
