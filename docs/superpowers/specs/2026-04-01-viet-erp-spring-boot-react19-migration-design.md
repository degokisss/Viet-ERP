# Viet-ERP Architecture Migration: NestJS → Spring Boot + React 19

## Status

Approved — 2026-04-01

## Overview

Migrate the Viet-ERP platform backend from NestJS (Node.js) to Spring Boot 4 (Java 26) while keeping the existing Next.js frontend (upgraded to React 19). The frontend calls Kong API Gateway, which routes to the new Java backend. Existing Kubernetes infrastructure (Kong, Keycloak, NATS, Redis, PostgreSQL) is retained.

**Total cutover** — NestJS is fully replaced. No dual-write period.

---

## 1. Repository & Project Structure

**Monorepo with two build systems.**

```
Viet-ERP/
├── apps/                    # React frontend apps (unchanged)
│   ├── HRM/
│   ├── CRM/
│   ├── Accounting/
│   ├── MRP/
│   ├── TPM/
│   └── ...
├── packages/                # Shared packages
│   ├── ui/                  # npm only (React components)
│   ├── dashboard/          # npm only
│   ├── auth/                # npm + Maven dual publish
│   ├── events/              # npm + Maven dual publish
│   ├── api-types/           # npm + Maven dual publish
│   ├── i18n/                # npm only (frontend only)
│   └── ... (other frontend-only packages)
├── backend-java/            # NEW — Spring Boot Modular Monolith
│   ├── pom.xml              # Parent POM (Maven)
│   ├── module-hrm/
│   ├── module-crm/
│   ├── module-accounting/
│   ├── module-mrp/
│   ├── module-tpm/
│   └── ...
└── docs/
```

**Backend is inside the same monorepo** — enables shared tooling, atomic cross-cutting changes, and unified CI.

---

## 2. Backend Architecture — Spring Boot 4 Modular Monolith

### Stack

- **Runtime**: Java 26
- **Framework**: Spring Boot 4.x
- **Build**: Maven (multi-module project)
- **ORM**: JPA / Hibernate 6 (with Hibernate 6 features)
- **DB**: Same PostgreSQL instance (entities reverse-engineered from Prisma schemas)
- **Auth**: Spring Security 6 + OAuth2 Resource Server (JWT)
- **Events**: Spring Cloud Stream + NATS binder
- **API**: REST (Springdoc OpenAPI for docs)
- **Caching**: Spring Cache + Redis (existing Redis infrastructure)
- **Logging**: Slf4j + Lombok `@Slf4j`

### Module Structure (per domain)

Each Maven module follows the same structure:

```
module-hrm/
├── pom.xml
├── src/main/java/com/vieterp/hrm/
│   ├── HrmApplication.java
│   ├── domain/
│   │   └── Employee.java          # JPA Entity (Lombok @Getter @Setter @Builder @NoArgsConstructor)
│   ├── repository/
│   │   └── EmployeeRepository.java  # Spring Data JPA
│   ├── service/
│   │   ├── EmployeeService.java      # @Transactional
│   │   └── dto/
│   │       ├── CreateEmployeeRequest.java   # Java record
│   │       └── EmployeeResponse.java          # Java record
│   ├── controller/
│   │   └── EmployeeController.java   # @RestController
│   └── event/
│       ├── EmployeeCreatedEvent.java  # Java record
│       └── EmployeeEventPublisher.java
└── src/test/java/...
```

### Java Language Features

- **Records** for all DTOs (request/response), event payloads, and immutable value objects
- **Lombok** for entities (`@Entity`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`) and logging (`@Slf4j`)
- **Sealed classes** for bounded type hierarchies (e.g., permission enums)
- **Pattern matching** (`instanceof`) in service layer where applicable
- **Virtual threads** (Java 21+) enabled via Spring Boot config for async endpoints

### No Lombok on:

- Records (they are already final, have equals/hashCode/toString)
- DTOs that are records
- Test classes where explicit is better

---

## 3. Data Layer

### Entity Generation

A code generator reads existing `prisma/schema.prisma` files and generates JPA `@Entity` classes:

- Prisma `String` → `@Column(length=191)` (UTF-8 compatibility)
- Prisma `DateTime` → `java.time.Instant` / `java.time.LocalDateTime`
- Prisma `Boolean` → `boolean` / `Boolean`
- Prisma `UUID` → `java.util.UUID`
- Prisma relations (`@relation`) → JPA `@ManyToOne`, `@OneToMany`, `@JoinColumn`

The generator is a one-time script (`scripts/prisma-to-jpa/`) — it produces source files that are checked in and maintained like any other code.

### Database

- **Same PostgreSQL instance** used by existing NestJS backend
- Prisma schema is the **source of truth** — all entities must match exactly
- Hibernate `validate` mode in production (schema already exists, no auto-DDL)
- Migration approach: generate JPA entities → validate against existing DB schema → cutover via Kong

### Transaction Boundaries

- All service methods use `@Transactional` (Spring)
- Read-only queries use `@Transactional(readOnly = true)`
- No distributed transactions (single DB, modular monolith)

---

## 4. API Layer

### REST Conventions

- Base path per module: `/api/v1/{domain}` (e.g., `/api/v1/hrm/employees`)
- OpenAPI 3 spec auto-generated via `springdoc-openapi`
- Standard HTTP semantics: `GET` (list/read), `POST` (create), `PUT/PATCH` (update), `DELETE`

### Request/Response DTOs (Records)

```java
// Request — immutable record
public record CreateEmployeeRequest(
    String firstName,
    String lastName,
    String email,
    Long departmentId
) {}

// Response — immutable record
public record EmployeeResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    DepartmentSummary dept
) {}

// List response with pagination
public record PagedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    long total
) {}
```

### Error Handling

- `GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions to RFC 7807 Problem Details
- Standard error response shape:

```java
public record ErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    Instant timestamp
) {}
```

---

## 5. Event Layer — NATS + Spring Cloud Stream

### Publishing Events

```java
// Event record
public record EmployeeCreatedEvent(
    UUID employeeId,
    String email,
    Instant occurredAt
) {}

// Publisher
@RequiredArgsConstructor
public class EmployeeEventPublisher {
    private final StreamBridge streamBridge;

    public void publishCreated(Employee employee) {
        var event = new EmployeeCreatedEvent(
            employee.getId(),
            employee.getEmail(),
            Instant.now()
        );
        streamBridge.send("employee-created-out-0", event);
    }
}
```

### Consuming Events

```java
@Bean
Consumer<EmployeeCreatedEvent> handleEmployeeCreated() {
    return event -> {
        log.info("Employee created: {}", event.employeeId());
        notificationService.sendWelcomeEmail(event.email());
    };
}
```

### Topic Naming Convention

`{domain}.{entity}.{action}` (e.g., `hrm.employee.created`, `crm.contact.updated`)

### NATS Configuration

- Binder: `spring-cloud-stream-binder-nats`
- Existing NATS JetStream cluster reused
- Dead-letter queue via NATS consumer configuration

---

## 6. Auth Layer — Keycloak + Spring Security

### Configuration

- Spring Security 6 with `spring-security-oauth2-resource-server`
- JWT validation: Keycloak JWKS endpoint (or Kong token introspection — Kong already validates tokens)
- Roles extracted from JWT claims (`realm_access.roles`, `resource_access.{client}.roles`)
- Spring Security `GrantedAuthority` populated from JWT roles

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

### Stateless JWT

- No server-side session (stateless JWT only)
- Token validated on every request by Spring Security
- Frontend continues using Keycloak-issued tokens (no frontend changes)

---

## 7. Shared Packages — Dual npm + Maven Publishing

### Packages to Dual-Publish

| Package     | npm | Maven | Purpose                                             |
| ----------- | --- | ----- | --------------------------------------------------- |
| `events`    | Yes | Yes   | Event class definitions (TypeScript + Java records) |
| `auth`      | Yes | Yes   | JWT claim types, permission constants               |
| `api-types` | Yes | Yes   | Shared request/response DTO types                   |
| `errors`    | Yes | Yes   | Error codes, exception types                        |

### Packages for Frontend Only

`ui`, `dashboard`, `admin`, `i18n`, `branding`, `master-data`, `notifications` (frontend components), `ai-copilot` (frontend UI)

### Publishing

- **npm**: GitHub Packages or Verdaccio private registry
- **Maven**: GitHub Packages or Artifactory/Nexus

Event class definitions must be identical between TypeScript and Java (same field names, types, ordering). A shared generator script ensures consistency.

---

## 8. Frontend — Next.js 14 + React 19

### Upgrade Path

1. Upgrade Next.js 14 → 15 (latest stable)
2. Upgrade React 18 → 19
3. Verify all packages compile (`npm run build`)
4. Migrate any Node.js-only shared packages to pure TypeScript

### What Stays the Same

- Next.js App Router (file-based routing)
- Tailwind CSS styling
- Kong API Gateway URL pattern (no frontend URL changes)
- Keycloak authentication (same tokens, same flow)
- Module structure per app (apps/HRM, apps/CRM, etc.)

### Shared Packages (Frontend)

- Existing TypeScript packages in `packages/` that work in browser remain npm-only
- No changes to `next.config.js`, `tsconfig.json`, or module resolution

---

## 9. Deployment

### Kubernetes

- Existing K8s cluster, namespaces, and Helm charts reused
- **Cutover**: Kong upstream target changed from NestJS service → Spring Boot service (one config update)
- Spring Boot application deployed as a single pod (modular monolith, not micro)
- HPA based on CPU/memory metrics

### Docker Image

```dockerfile
# Multi-stage build
FROM eclipse-temurin:26-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY module-hrm/ ./module-hrm/
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=builder /app/backend-java/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Target image size: < 200MB with `jlink` custom runtime.

### Health Checks

- `/actuator/health/liveness` → Liveness probe
- `/actuator/health/readiness` → Readiness probe
- Existing K8s probe patterns reused

### Environment Variables

Standard Spring Boot externalized config via `application.yml` / environment variables. Key ones:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` (Keycloak JWKS)
- `NATS_URL` (existing NATS cluster)
- `REDIS_URL` (existing Redis)

---

## 10. Migration Sequence

1. **Create `backend-java/` directory** with parent POM and module structure
2. **Write Prisma-to-JPA generator** (`scripts/prisma-to-jpa/`) — run against all `apps/*/prisma/schema.prisma`
3. **Generate JPA entities** for all modules, verify against existing DB schema
4. **Implement module by module** — start with one module (e.g., HRM) as pilot:
   - Repository + Entity
   - Service layer
   - REST controller
   - Event publisher
5. **Implement shared packages** dual-publishing (events, auth, api-types)
6. **Connect to Keycloak** — Spring Security Resource Server + JWT validation
7. **Connect to NATS** — Spring Cloud Stream publishers and consumers
8. **Upgrade frontend** to React 19 + Next.js 15, verify builds
9. **Deploy Spring Boot to K8s staging** — run alongside NestJS (read-only initially)
10. **Validate** — smoke tests, integration tests, load tests on staging
11. **Cutover** — update Kong upstream, scale NestJS to zero
12. **Monitor** — error rates, latency, NATS message lag for first 48 hours

---

## Open Questions (Not Yet Decided)

1. **Maven vs. Gradle** for Java backend — Maven chosen above but Gradle is viable if team prefers
2. **Prisma Studio** — will it still be used for DB browsing? May need a replacement (pgAdmin, DBeaver)
3. **CI/CD pipeline** — existing Jenkins/GitHub Actions will need updates for Maven build + Docker publish
