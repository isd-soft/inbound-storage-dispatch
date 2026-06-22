# ISD — Inbound, Storage & Dispatch

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)](https://vuejs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-28.0-blue.svg)](https://www.docker.com/)

**ISD** is a lightweight Warehouse Management System (WMS) designed to simulate real-world warehouse operations — inbound logistics, inventory storage, replenishment, and dispatch. Built as a full-stack distributed system, it serves as a practical training ground for modern enterprise software development.

> **Note:** This project was developed during a 4-week internship (June 2–26, 2026) using Agile Scrum methodology with 4 sprints.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running Locally](#running-locally)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Contributors](#contributors)
- [License](#license)

---

## Overview

ISD implements a client-server architecture:

- **Backend** (`wmsBack/`): Java 21 + Spring Boot REST API with JWT authentication, business logic, and PostgreSQL persistence.
- **Frontend** (`wmsFront/`): Vue 3 + Vite single-page application with:
  - Desktop-oriented dashboard for **Supervisors**
  - Mobile-friendly console for **Operators**
- **Database**: PostgreSQL 16 with versioned schema migrations.
- **Deployment**: Docker Compose for containerized orchestration.

The system supports two primary user roles:

| Role | Responsibilities |
|------|------------------|
| **Supervisor** | Product management, location management, inventory adjustment, task creation, user management, dashboard monitoring |
| **Operator** | Execute assigned replenishment and picking tasks via mobile-optimized interface |

---

## Key Features

| Feature | Description |
|---------|-------------|
| **Authentication & Authorization** | JWT-based login with role-based access control (DEV, SUPERVISOR, OPERATOR) |
| **Product Management** | CRUD operations with categories, search/filter, and soft delete |
| **Warehouse Location Management** | Zone-based location codes (e.g., `RECEIVING-01`, `PICK-01`) |
| **Inventory Management** | Stock tracking by SKU/location with manual adjustment and history logging |
| **Replenishment Workflow** | Create requests → generate tasks → operators execute → stock updates automatically |
| **Picking Workflow** | Order creation → picking tasks → operators confirm quantities → inventory updates |
| **Task Monitoring** | Supervisors view active/completed/cancelled tasks filtered by status, operator, or type |
| **Inventory History** | Full audit trail: product, SKU, quantity delta, before/after, locations, user, timestamp |
| **Supervisor Dashboard** | Total inventory, open tasks, completed tasks, low stock alerts |
| **User Management** | Create/disable users and assign roles |
| **Mobile-Friendly Operator Console** | Responsive UI with large actionable controls for task execution |
| **AI Assistant (Bonus)** | Chatbot interface for supervisors to query warehouse insights (e.g., "Show products with low stock") |

---

## Tech Stack

### Backend
- **Java 21**
- **Spring Boot 3.4**
- **Spring Security** with JWT authentication
- **Spring Data JPA** (Hibernate)
- **PostgreSQL** (via JDBC)
- **Maven** (build tool)
- **Flyway** or **Liquibase** (database migrations — verify which is used)
- **Swagger/OpenAPI** (API documentation)

### Frontend
- **Vue 3** with Composition API
- **Vite** (build tool)
- **Vue Router** (navigation)
- **Axios** (HTTP client)
- **ESLint** (code linting)

### Infrastructure
- **Docker** & **Docker Compose**
- **Git** (hosted on GitHub)

## Project Structure
inbound-storage-dispatch/
├── wmsBack/ # Backend (Spring Boot)
│ ├── src/
│ │ ├── main/
│ │ │ ├── java/com/isd/wms/
│ │ │ │ ├── config/ # Security, CORS, Swagger configs
│ │ │ │ ├── controller/ # REST endpoints (Product, Inventory, Task, etc.)
│ │ │ │ ├── dto/ # Data Transfer Objects
│ │ │ │ ├── entity/ # JPA entities (Product, Location, Inventory, Task, etc.)
│ │ │ │ ├── enums/ # Role, TaskStatus, OperationType
│ │ │ │ ├── exception/ # Custom exceptions & global handler
│ │ │ │ ├── job/ # Scheduled jobs (e.g., shortage detection)
│ │ │ │ ├── mapper/ # MapStruct mappers (Entity ↔ DTO)
│ │ │ │ └── repository/ # Spring Data JPA repositories
│ │ │ └── resources/
│ │ │ ├── application.properties
│ │ │ └── db/migration/ # Flyway/Liquibase migration scripts
│ │ └── test/ # Unit & integration tests
│ ├── .mvn/wrapper/ # Maven wrapper
│ ├── pom.xml # Maven dependencies
│ └── mvnw / mvnw.cmd # Maven wrapper scripts
│
├── wmsFront/ # Frontend (Vue 3 + Vite)
│ ├── public/ # Static assets
│ ├── src/
│ │ ├── api/ # Axios API calls (backend integration)
│ │ ├── assets/ # Images, fonts, styles
│ │ ├── components/ # Reusable Vue components
│ │ ├── composables/ # Vue composables (shared logic)
│ │ ├── dynamic/ # Dynamic form/table components
│ │ ├── layouts/ # Page layouts (Supervisor, Operator)
│ │ ├── router/ # Vue Router configuration
│ │ └── services/ # Business logic services
│ ├── .editorconfig
│ ├── .gitignore
│ ├── index.html
│ ├── package.json # NPM dependencies
│ └── vite.config.js # Vite configuration
│
├── logs/ # Application logs (generated at runtime)
├── .env # Environment variables
├── .gitattributes
├── .gitignore
├── docker-compose.yaml # Multi-container orchestration
└── HELP.md # Additional setup help


---

## Getting Started

### Prerequisites

- **Docker** & **Docker Compose** (recommended)
- **Java 21** (for local backend development)
- **Node.js 18+** & **npm** (for local frontend development)
- **PostgreSQL 16** (if running without Docker)

### Running with Docker Compose

1. **Clone the repository:**
   ```bash
   git clone https://github.com/isd-soft/inbound-storage-dispatch.git
   cd inbound-storage-dispatch
Configure environment variables (edit .env if needed):

env
# Database
POSTGRES_DB=wmsdb
POSTGRES_USER=wmsuser
POSTGRES_PASSWORD=wmspass
# Backend
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/wmsdb
SPRING_DATASOURCE_USERNAME=wmsuser
SPRING_DATASOURCE_PASSWORD=wmspass
JWT_SECRET=your-jwt-secret-key
Build and start all services:

bash
docker-compose up --build
Access the application:

Frontend: http://localhost:5173

Backend API: http://localhost:8080

Swagger UI: http://localhost:8080/swagger-ui.html

Default login credentials (check database seed or create via registration):

Supervisor: supervisor@isd.com / password

Operator: operator@isd.com / password

Running Locally (Development)
Backend (Spring Boot)
bash
cd wmsBack
./mvnw spring-boot:run
Frontend (Vue 3)
bash
cd wmsFront
npm install
npm run dev
API Documentation
The REST API is documented using Swagger/OpenAPI. Once the backend is running, visit:

Swagger UI: http://localhost:8080/swagger-ui.html

OpenAPI JSON: http://localhost:8080/v3/api-docs

Key Endpoints
Method	Endpoint	Description
POST	/api/auth/login	Authenticate and receive JWT token
GET	/api/products	List all products (with filters)
POST	/api/products	Create a new product
GET	/api/inventory	View current inventory
POST	/api/inventory/adjust	Manual stock adjustment
POST	/api/replenishment	Create replenishment request
GET	/api/tasks	List warehouse tasks (with status filters)
POST	/api/tasks/{id}/execute	Execute a task (operator)
GET	/api/history	View inventory movement history
GET	/api/dashboard/stats	Supervisor dashboard metrics
POST	/api/users	Create/update users (supervisor only)
POST	/api/chat	AI assistant query (bonus)
Testing
The backend includes unit and integration tests covering:

Service layer logic

REST API endpoints

Critical business flows (replenishment, picking, inventory updates)

Run tests with:

bash
cd wmsBack
./mvnw test
Contributors
This project was developed by a team of 4 interns during the June 2026 internship session:

MarcencoDasuka (Marcenco Yamahaha)

Whatys (Cristian)

stanislav-ciobanu (Ciobanu Stanislav)

Alisizationed (Linage)

License
This project is open-source and available under the MIT License.

Acknowledgments
Built as part of the Inther Software Development internship program.

Special thanks to mentors and supervisors for guidance throughout the Agile development process.

text

---

**Instrucțiuni:**

1. Copiază **Partea 1** într-un fișier nou numit `README.md`.
2. Copiază **Partea 2** și lipește-l la sfârșitul aceluiași fișier (după `---`).
3. Salvează fișierul.

Astfel, chiar dacă selecția se întrerupe, ai ambele părți separate și le poți uni manual. Succes!
- **Trello** or **Jira** (task board — used during development)

---
