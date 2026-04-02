# Viet-ERP NestJS → Spring Boot + React 19 Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace NestJS backend with Spring Boot 3.4 (Java 21) Modular Monolith. Upgrade frontend to React 19 + Next.js 16. Retain existing infrastructure (Kong, Keycloak, NATS, PostgreSQL, Redis).

**Architecture:** Spring Boot 3.4 Modular Monolith with Maven multi-module project inside the existing monorepo at `backend-java/`. JPA/Hibernate entities from Prisma schemas. REST APIs via Springdoc OpenAPI. Stateless JWT auth via Keycloak. NATS events via Spring Cloud Stream. npm packages in `packages/` (TypeScript only); Maven artifacts in `backend-java/module-*/`.

**Tech Stack:** Java 21, Spring Boot 3.4.x, Maven, JPA/Hibernate 6, Spring Security 6 (OAuth2 Resource Server), Spring Cloud Stream 2024.0, Springdoc OpenAPI, Lombok, Java Records, Next.js 16, React 19

**Builder Pattern:** All DTO records use `@Builder` annotation for fluent construction. Entities use Lombok `@Builder`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`.

---

## Phase 1: Spring Boot Project Scaffold (COMPLETED)

### Task 1: ✅ Create `backend-java/` Directory Structure and Parent POM

- `backend-java/pom.xml` — Spring Boot 3.4.0, Java 21, Spring Cloud 2024.0.0, Hibernate 6.6.3, Lombok 1.18.38
- `backend-java/.gitignore`
- `backend-java/README.md`
- Maven wrapper initialized
- Commits: `545056e`

---

### Task 2: ✅ Create `backend-java/module-events/` — Event Types Maven Artifact

**Note:** Maven artifacts live in `backend-java/`, not `packages/`. `packages/events/` remains TypeScript-only.

- `backend-java/module-events/pom.xml` — parent: `viet-erp-parent`, artifactId: `module-events`
- `backend-java/module-events/src/main/java/com/vieterp/events/employee/EmployeeCreatedEvent.java`
- `backend-java/module-events/src/main/java/com/vieterp/events/employee/EmployeeUpdatedEvent.java` — includes firstName, lastName
- `backend-java/module-events/src/main/java/com/vieterp/events/employee/EmployeeDeletedEvent.java`
- `backend-java/module-events/src/test/java/...` — unit tests
- Commits: `5d3cd4f`, `eb51649`

---

### Task 3: ✅ Implement `backend-java/module-hrm/` — HRM Domain Module (Pilot)

- `backend-java/module-hrm/pom.xml` — depends on `module-events`
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/domain/` — Employee.java, Department.java
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/repository/` — Spring Data JPA repos
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/dto/` — CreateEmployeeRequest.java, EmployeeResponse.java (both with `@Builder`), DepartmentSummary.java
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/EmployeeService.java` — full CRUD including `update()`
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/controller/EmployeeController.java` — REST at `/api/v1/hrm/employees`
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/event/EmployeeEventPublisher.java` — StreamBridge NATS publisher
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/exception/` — GlobalExceptionHandler, EmployeeNotFoundException
- `backend-java/module-hrm/src/main/java/com/vieterp/hrm/config/` — SecurityConfig (JWT/OAuth2), OpenApiConfig, NatsConfig
- `backend-java/module-hrm/src/main/resources/application.yml` — PostgreSQL, Keycloak, Redis, NATS (StreamBridge only, no binder)
- `backend-java/module-hrm/src/test/` — EmployeeServiceTest (5 tests)
- Commits: `b2273bf`, `a66d159`

---

### Task 4: ✅ Prisma-to-JPA Generator

- `scripts/prisma-to-jpa/prisma-to-jpa.mjs` — Node.js script, parses Prisma schema → JPA entities
- `scripts/prisma-to-jpa/README.md`
- 182 models generated from HRM schema, tested
- Commits: `3bfb23b`

---

## Phase 2: Frontend React 19 Upgrade (COMPLETED)

### Task 5: ✅ Upgrade Next.js + React

- `package.json` — Next.js 16.2.2, React 19.2.4
- HRM app builds successfully
- Some apps (HRM-AI, ERP-landing-page, CRM) have webpack/Turbopack config issues needing manual resolution
- Commits: `62a37d0`

---

## Phase 3: Shared Modules — Dual npm + Maven (COMPLETED)

### Task 6: ✅ `backend-java/module-auth/` — Auth Maven Artifact

**Note:** Maven artifact in `backend-java/`, TypeScript package in `packages/auth/` unchanged.

- `backend-java/module-auth/pom.xml`
- `backend-java/module-auth/src/main/java/com/vieterp/auth/Permission.java` — enum with HRM, CRM, Accounting, MRP, TPM permissions
- `backend-java/module-auth/src/main/java/com/vieterp/auth/JwtClaims.java` — record with `@Builder`
- `backend-java/module-auth/src/test/` — unit tests
- Commits: `b5e2a0f`, `eb51649`

---

### Task 7: ✅ `backend-java/module-api-types/` — API Types Maven Artifact

**Note:** Maven artifact in `backend-java/`, TypeScript package in `packages/api-types/` unchanged.

- `backend-java/module-api-types/pom.xml`
- `backend-java/module-api-types/src/main/java/com/vieterp/api/PagedResponse.java` — record with `@Builder`, `of()` factory, overflow-safe `hasNext`
- `backend-java/module-api-types/src/main/java/com/vieterp/api/ErrorResponse.java` — record with `@Builder` (RFC 7807)
- `backend-java/module-api-types/src/test/` — unit tests
- Commits: `aa23f2e`, `eb51649`

---

## Phase 4: Remaining Backend Modules

**Pattern:** Each module follows the same structure as Task 3 (module-hrm).

**Per-module deliverables:**

1. `backend-java/module-{name}/pom.xml` — depends on `module-events`, `module-auth`, `module-api-types`
2. JPA entities (generated by Prisma-to-JPA script, then refined)
3. Spring Data JPA repositories
4. Service layer with `@Transactional` CRUD + update
5. REST controller at `/api/v1/{name}/`
6. Event publishers for NATS
7. DTO records with `@Builder`
8. Exception handlers
9. `application.yml` with module-specific config
10. Unit tests for service layer
11. **NestJS cleanup** — remove corresponding NestJS code after module validates (see below)

**Module order:** CRM → Accounting → MRP → TPM → remaining

---

## Phase 4: Completed Modules

### Task 8: ✅ `backend-java/module-crm/` — CRM Domain Module

- Company + Contact entities, repositories, services, REST controllers
- DTOs at `domain/dto/` with `@Builder`
- 10 unit tests (5 CompanyService + 5 ContactService)
- Commits: `b3724fd`, `c586325`

### Task 9: ✅ `backend-java/module-accounting/` — Accounting Domain Module

- Account entity with VAS/TT200/TT133 compliance enums
- AccountService with full CRUD
- DTOs at `domain/dto/` with `@Builder`
- 5 unit tests
- Commits: `fdaf253`

### Task 10: ✅ `backend-java/module-mrp/` — MRP Domain Module

- Part + BomHeader entities, services, REST controllers
- MRP enums (MakeOrBuy, LifecycleStatus, BomType)
- DTOs at `domain/dto/` with `@Builder`
- 10 unit tests
- Commit: `9044cf9`

### Task 11: ✅ `backend-java/module-tpm/` — TPM Domain Module

- Promotion + Claim entities, services, REST controllers
- TPM enums (PromotionStatus, ClaimStatus)
- DTOs at `domain/dto/` with `@Builder`
- 10 unit tests
- Commit: `40a27d4`

### Structural Refactor: ✅ DTOs moved to `domain/dto/`

- All DTOs moved from `service/dto/` to `domain/dto/` per layered architecture
- 9 DTOs moved across 3 modules (hrm, crm, accounting)
- 25 tests pass after refactor
- Commit: `0309d54`

### NestJS Cleanup

Per-module NestJS removal is **pending** — will proceed after Phase 5 (integration tests validate the modules).

---

## Phase 5: Infrastructure Integration

### Task: Keycloak Integration Test

- Deploy Spring Boot with test Keycloak realm
- Call `/api/v1/hrm/employees` with valid JWT → 200
- Call without JWT → 401

### Task: NATS Integration Test

- Start NATS locally or point to existing cluster
- POST to create employee → verify `hrm.employee.created` message on NATS

### Task: Kong Gateway Cutover ✅ DONE

- Kong routes updated from NestJS ports (3001-3008) to Spring Boot ports (8080-8084):
  - `hrm-service`: 3002 → 8080
  - `crm-service`: 3003 → 8081
  - `acc-service`: 3007 → 8082
  - `mrp-service`: 3001 → 8083
  - `tpm-service`: 3004 → 8084
- `infrastructure/kong/kong.yml` updated
- Commit: `f81882d`

### Task: module-shared ✅ DONE

- `backend-java/module-shared/` created with `BaseEntity.java` (common audit fields: createdAt, updatedAt, tenantId, @PrePersist/@PreUpdate)
- Commit: `f81882d`

### Task: Keycloak Integration Test

- Deploy Spring Boot with test Keycloak realm
- Call `/api/v1/hrm/employees` with valid JWT → 200
- Call without JWT → 401

### Task: NATS Integration Test

- Start NATS locally or point to existing cluster
- POST to create employee → verify `hrm.employee.created` message on NATS

---

## Phase 6: Kubernetes Deployment

### Task: Docker Build + Push

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY module-hrm/ ./module-hrm/
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/backend-java/module-hrm/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Task: Helm Deployment

- Modify `infrastructure/helm/vieterp/values.yaml` for Spring Boot image
- Cutover via Kong upstream update

---

## NestJS Cleanup — After Each Module Migration

**Rule:** After each module's Spring Boot implementation validates successfully on staging, remove the corresponding NestJS code.

**Per-module cleanup (run after module validates):**

1. Identify NestJS code for the migrated module:
   - `apps/TPM-api-nestjs/` — if it contains the module's backend
   - `apps/*/src/server/` — NestJS server directories
   - `apps/*/prisma/` — Prisma schema (DB stays, schema reviewed)

2. Remove only the NestJS-specific code. Keep:
   - `apps/*/prisma/schema.prisma` — source of truth for JPA entity generation
   - `apps/*/` frontend code (Next.js, React)
   - `packages/*/` that are frontend-only

3. Verify Kong routes updated (upstream target changed from NestJS to Spring Boot)

4. Commit with message: `chore: remove NestJS backend for {module} after Spring Boot migration`

**Current state:**

- `apps/TPM-api-nestjs/` — candidate for removal (TPM module)
- `apps/TPM-api/` — check if this is Node.js or has NestJS backend
- NestJS modules still present in `apps/` — do NOT remove until their Spring Boot replacement is validated

---

## Implementation Order

1. ~~Task 1~~ — Parent POM scaffold ✅
2. ~~Task 2~~ — `module-events` ✅
3. ~~Task 3~~ — `module-hrm` pilot ✅
4. ~~Task 4~~ — Prisma-to-JPA generator ✅
5. ~~Task 5~~ — React 19 / Next.js 16 upgrade ✅
6. ~~Task 6~~ — `module-auth` ✅
7. ~~Task 7~~ — `module-api-types` ✅
8. **Phase 4 modules** — CRM, Accounting, MRP, TPM (each: implement → validate → cleanup NestJS)
9. **Phase 5** — Keycloak, NATS, Kong integration
10. **Phase 6** — Docker + Helm + cutover

---

## Repo Structure After Migration

```
Viet-ERP/
├── apps/                    # React frontend apps (unchanged)
├── packages/                # npm packages (TypeScript only, no Maven)
│   ├── auth/                # npm only (TypeScript)
│   ├── events/             # npm only (TypeScript)
│   ├── api-types/          # npm only (TypeScript)
│   └── ...                # other frontend-only packages
├── backend-java/           # Spring Boot Modular Monolith
│   ├── pom.xml             # Parent POM
│   ├── module-hrm/         # HRM domain
│   ├── module-crm/         # CRM domain
│   ├── module-accounting/  # Accounting domain
│   ├── module-mrp/         # MRP domain
│   ├── module-tpm/         # TPM domain
│   ├── module-events/       # Event type definitions
│   ├── module-auth/        # Auth types (JWT, Permissions)
│   └── module-api-types/   # Shared API DTOs
└── infrastructure/         # K8s, Kong, Terraform (unchanged)
```
