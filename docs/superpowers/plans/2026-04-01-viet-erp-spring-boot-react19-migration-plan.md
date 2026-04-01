# Viet-ERP NestJS → Spring Boot + React 19 Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace NestJS backend with Spring Boot 4 (Java 26) Modular Monolith. Upgrade frontend to React 19 + Next.js 15. Retain existing infrastructure (Kong, Keycloak, NATS, PostgreSQL, Redis).

**Architecture:** Spring Boot 4 Modular Monolith with Maven multi-module project inside the existing monorepo. JPA/Hibernate entities generated from Prisma schemas. REST APIs via Springdoc OpenAPI. Stateless JWT auth via Keycloak. NATS events via Spring Cloud Stream. Dual npm+Maven publishing for shared packages.

**Tech Stack:** Java 26, Spring Boot 4.x, Maven, JPA/Hibernate 6, Spring Security 6 (OAuth2 Resource Server), Spring Cloud Stream (NATS binder), Springdoc OpenAPI, Lombok, Java Records, Next.js 15, React 19

---

## Phase 1: Spring Boot Project Scaffold

### Task 1: Create `backend-java/` Directory Structure and Parent POM

**Files:**

- Create: `backend-java/pom.xml`
- Create: `backend-java/.gitignore`
- Create: `backend-java/README.md`

- [ ] **Step 1: Create `backend-java/pom.xml` with all dependency management**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.vieterp</groupId>
    <artifactId>viet-erp-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>VietERP Backend</name>
    <description>Spring Boot 4 Modular Monolith for VietERP</description>

    <modules>
        <module>module-hrm</module>
        <module>module-shared</module>
    </modules>

    <properties>
        <java.version>26</java.version>
        <maven.compiler.source>26</maven.compiler.source>
        <maven.compiler.target>26</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>4.0.0</spring-boot.version>
        <hibernate.version>6.6.3.Final</hibernate.version>
        <lombok.version>1.18.36</lombok.version>
        <springdoc.version>2.8.0</springdoc.version>
        <spring-cloud.version>2025.0.0</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Hibernate -->
            <dependency>
                <groupId>org.hibernate.orm</groupId>
                <artifactId>hibernate-core</artifactId>
                <version>${hibernate.version}</version>
            </dependency>

            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>

            <!-- Springdoc OpenAPI -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <compilerArgs>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: Create `backend-java/.gitignore`**

```
target/
*.class
*.jar
*.war
*.ear
*.log
.idea/
*.iml
.vscode/
.env
*.swp
*.swo
.mvn/wrapper/maven-wrapper.jar
```

- [ ] **Step 3: Create `backend-java/README.md`**

````markdown
# VietERP Backend — Spring Boot 4 Modular Monolith

## Modules

- `module-hrm` — Human Resource Management
- `module-shared` — Shared types (dual npm+Maven publish)

## Build

```bash
./mvnw clean install
```
````

## Run

```bash
java -jar module-hrm/target/module-hrm.jar
```

````

- [ ] **Step 4: Initialize Maven wrapper**

Run: `cd backend-java && mvn wrapper:wrapper -Dmaven=3.9.9`
Expected: `.mvn/wrapper/` created, `mvnw` and `mvnw.cmd` created

- [ ] **Step 5: Verify parent POM parses**

Run: `cd backend-java && ./mvnw help:effective-pom -N`
Expected: POM parsed, effective POM printed

- [ ] **Step 6: Commit**

```bash
git add backend-java/
git commit -m "feat(backend-java): scaffold Spring Boot 4 parent POM with dependency management"
````

---

### Task 2: Create `module-shared` — Dual npm + Maven Package for Event Types

**Files:**

- Create: `packages/events/pom.xml` (NEW — Maven side of dual publish)
- Create: `packages/events/src/main/java/com/vieterp/events/` (Java source)
- Create: `packages/events/src/main/java/com/vieterp/events/employee/` (employee event records)
- Modify: `packages/events/package.json` (update for dual publish)
- Modify: `packages/events/tsconfig.json` (if needed)

**Prerequisite:** Confirm existing `packages/events/` structure first. Run: `ls packages/events/`

- [ ] **Step 1: Create `packages/events/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.vieterp</groupId>
        <artifactId>viet-erp-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../backend-java/pom.xml</relativePath>
    </parent>

    <artifactId>events</artifactId>
    <packaging>jar</packaging>
    <name>events</name>
    <description>Dual-published event type definitions for VietERP</description>

    <properties>
        <java.version>26</java.version>
    </properties>

    <dependencies>
        <!-- No runtime deps — pure POJO -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create `packages/events/src/main/java/com/vieterp/events/employee/EmployeeCreatedEvent.java`**

```java
package com.vieterp.events.employee;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a new employee is created in the HRM module.
 * TypeScript equivalent lives in packages/events/src/employee/EmployeeCreatedEvent.ts
 * — must be kept in sync.
 */
public record EmployeeCreatedEvent(
    UUID employeeId,
    String email,
    String firstName,
    String lastName,
    Instant occurredAt
) {}
```

- [ ] **Step 3: Create `packages/events/src/main/java/com/vieterp/events/employee/EmployeeUpdatedEvent.java`**

```java
package com.vieterp.events.employee;

import java.time.Instant;
import java.util.UUID;

public record EmployeeUpdatedEvent(
    UUID employeeId,
    String email,
    Instant occurredAt
) {}
```

- [ ] **Step 4: Create `packages/events/src/main/java/com/vieterp/events/employee/EmployeeDeletedEvent.java`**

```java
package com.vieterp.events.employee;

import java.time.Instant;
import java.util.UUID;

public record EmployeeDeletedEvent(
    UUID employeeId,
    Instant occurredAt
) {}
```

- [ ] **Step 5: Verify Java compilation**

Run: `cd packages/events && ../../backend-java/mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add packages/events/
git commit -m "feat(events): add Maven pom.xml and Java event record sources for dual npm+Maven publish"
```

---

### Task 3: Create `module-hrm` — HRM Domain Module (Pilot)

**Files:**

- Create: `backend-java/module-hrm/pom.xml`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/HrmApplication.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/domain/Employee.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/domain/Department.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/repository/EmployeeRepository.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/repository/DepartmentRepository.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/dto/CreateEmployeeRequest.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/dto/EmployeeResponse.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/EmployeeService.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/controller/EmployeeController.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/event/EmployeeEventPublisher.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/exception/GlobalExceptionHandler.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/exception/EmployeeNotFoundException.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/config/SecurityConfig.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/config/OpenApiConfig.java`
- Create: `backend-java/module-hrm/src/main/java/com/vieterp/hrm/config/NatsConfig.java`
- Create: `backend-java/module-hrm/src/main/resources/application.yml`

**Prerequisite:** Check existing Prisma schema for HRM. Run: `ls apps/HRM-unified/prisma/`

- [ ] **Step 1: Create `backend-java/module-hrm/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.vieterp</groupId>
        <artifactId>viet-erp-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>module-hrm</artifactId>
    <packaging>jar</packaging>
    <name>module-hrm</name>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Spring Cloud Stream (NATS) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-stream</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-stream-binder-nats</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Hibernate -->
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.hibernate.orm</groupId>
            <artifactId>hibernate-jpamodelgen</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- VietERP shared events (Maven dependency) -->
        <dependency>
            <groupId>com.vieterp</groupId>
            <artifactId>events</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/HrmApplication.java`**

```java
package com.vieterp.hrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HrmApplication {
    public static void main(String[] args) {
        SpringApplication.run(HrmApplication.class, args);
    }
}
```

- [ ] **Step 3: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/domain/Department.java`**

```java
package com.vieterp.hrm.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(length = 50)
    private String code;
}
```

- [ ] **Step 4: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/domain/Employee.java`**

```java
package com.vieterp.hrm.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 191)
    private String email;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = " hire_date")
    private Instant hireDate;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

- [ ] **Step 5: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/repository/DepartmentRepository.java`**

```java
package com.vieterp.hrm.repository;

import com.vieterp.hrm.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
}
```

- [ ] **Step 6: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/repository/EmployeeRepository.java`**

```java
package com.vieterp.hrm.repository;

import com.vieterp.hrm.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByStatus(String status);

    @Query("SELECT e FROM Employee e JOIN FETCH e.department WHERE e.id = :id")
    java.util.Optional<Employee> findByIdWithDepartment(UUID id);
}
```

- [ ] **Step 7: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/dto/CreateEmployeeRequest.java`**

```java
package com.vieterp.hrm.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    String phone,
    Long departmentId,
    String status
) {}
```

- [ ] **Step 8: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/dto/EmployeeResponse.java`**

```java
package com.vieterp.hrm.service.dto;

import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phone,
    DepartmentSummary dept,
    Instant hireDate,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
```

- [ ] **Step 9: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/dto/DepartmentSummary.java`**

```java
package com.vieterp.hrm.service.dto;

public record DepartmentSummary(
    Long id,
    String name,
    String code
) {}
```

- [ ] **Step 10: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/exception/EmployeeNotFoundException.java`**

```java
package com.vieterp.hrm.exception;

import java.util.UUID;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(UUID id) {
        super("Employee not found: " + id);
    }
}
```

- [ ] **Step 11: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/exception/GlobalExceptionHandler.java`**

```java
package com.vieterp.hrm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ProblemDetail handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://vieterp.com/errors/employee-not-found"));
        problem.setTitle("Employee Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://vieterp.com/errors/validation"));
        problem.setTitle("Validation Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://vieterp.com/errors/internal"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
```

- [ ] **Step 12: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/event/EmployeeEventPublisher.java`**

```java
package com.vieterp.hrm.event;

import com.vieterp.events.employee.EmployeeCreatedEvent;
import com.vieterp.events.employee.EmployeeDeletedEvent;
import com.vieterp.events.employee.EmployeeUpdatedEvent;
import com.vieterp.hrm.domain.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventPublisher {

    private final StreamBridge streamBridge;

    public void publishCreated(Employee employee) {
        var event = new EmployeeCreatedEvent(
            employee.getId(),
            employee.getEmail(),
            employee.getFirstName(),
            employee.getLastName(),
            Instant.now()
        );
        streamBridge.send("employee-created-out-0", event);
        log.info("Published EmployeeCreatedEvent for {}", employee.getId());
    }

    public void publishUpdated(Employee employee) {
        var event = new EmployeeUpdatedEvent(
            employee.getId(),
            employee.getEmail(),
            Instant.now()
        );
        streamBridge.send("employee-updated-out-0", event);
        log.info("Published EmployeeUpdatedEvent for {}", employee.getId());
    }

    public void publishDeleted(java.util.UUID employeeId) {
        var event = new EmployeeDeletedEvent(employeeId, Instant.now());
        streamBridge.send("employee-deleted-out-0", event);
        log.info("Published EmployeeDeletedEvent for {}", employeeId);
    }
}
```

- [ ] **Step 13: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/service/EmployeeService.java`**

```java
package com.vieterp.hrm.service;

import com.vieterp.hrm.domain.Department;
import com.vieterp.hrm.domain.Employee;
import com.vieterp.hrm.event.EmployeeEventPublisher;
import com.vieterp.hrm.exception.EmployeeNotFoundException;
import com.vieterp.hrm.repository.DepartmentRepository;
import com.vieterp.hrm.repository.EmployeeRepository;
import com.vieterp.hrm.service.dto.CreateEmployeeRequest;
import com.vieterp.hrm.service.dto.DepartmentSummary;
import com.vieterp.hrm.service.dto.EmployeeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeEventPublisher eventPublisher;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req) {
        Employee employee = Employee.builder()
            .firstName(req.firstName())
            .lastName(req.lastName())
            .email(req.email())
            .phone(req.phone())
            .status(req.status() != null ? req.status() : "ACTIVE")
            .hireDate(java.time.Instant.now())
            .build();

        if (req.departmentId() != null) {
            Department dept = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + req.departmentId()));
            employee.setDepartment(dept);
        }

        Employee saved = employeeRepository.save(employee);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        Employee emp = employeeRepository.findByIdWithDepartment(id)
            .orElseThrow(() -> new EmployeeNotFoundException(id));
        return toResponse(emp);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listAll() {
        return employeeRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    private EmployeeResponse toResponse(Employee e) {
        DepartmentSummary deptSummary = null;
        if (e.getDepartment() != null) {
            deptSummary = new DepartmentSummary(
                e.getDepartment().getId(),
                e.getDepartment().getName(),
                e.getDepartment().getCode()
            );
        }
        return new EmployeeResponse(
            e.getId(),
            e.getFirstName(),
            e.getLastName(),
            e.getEmail(),
            e.getPhone(),
            deptSummary,
            e.getHireDate(),
            e.getStatus(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 14: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/controller/EmployeeController.java`**

```java
package com.vieterp.hrm.controller;

import com.vieterp.hrm.service.dto.CreateEmployeeRequest;
import com.vieterp.hrm.service.dto.EmployeeResponse;
import com.vieterp.hrm.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hrm/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @Operation(summary = "Create a new employee")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest req) {
        EmployeeResponse resp = employeeService.create(req);
        return ResponseEntity
            .created(URI.create("/api/v1/hrm/employees/" + resp.id()))
            .body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List all employees")
    public ResponseEntity<List<EmployeeResponse>> listAll() {
        return ResponseEntity.ok(employeeService.listAll());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 15: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/config/SecurityConfig.java`**

```java
package com.vieterp.hrm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

- [ ] **Step 16: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/config/OpenApiConfig.java`**

```java
package com.vieterp.hrm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vietErpOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("VietERP HRM API")
                .version("1.0.0")
                .description("HRM module REST API for VietERP")
                .contact(new Contact().name("VietERP Team")));
    }
}
```

- [ ] **Step 17: Create `backend-java/module-hrm/src/main/java/com/vieterp/hrm/config/NatsConfig.java`**

```java
package com.vieterp.hrm.config;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBinding(Source.class)
public class NatsConfig {
    // NATS connection is auto-configured by Spring Cloud Stream
    // spring.cloud.stream.nats.binder.url configured in application.yml
}
```

- [ ] **Step 18: Create `backend-java/module-hrm/src/main/resources/application.yml`**

```yaml
server:
  port: 8080

spring:
  application:
    name: module-hrm

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/vieterp}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/vieterp}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8180/realms/vieterp/protocol/openid-connect/certs}

  cloud:
    stream:
      binders:
        nats:
          type: nats
          environment:
            spring.cloud.stream.nats.binder.url: ${NATS_URL:nats://localhost:4222}
      bindings:
        employee-created-out-0:
          destination: hrm.employee.created
        employee-updated-out-0:
          destination: hrm.employee.updated
        employee-deleted-out-0:
          destination: hrm.employee.deleted

  cache:
    type: redis
  data:
    redis:
      url: ${REDIS_URL:redis://localhost:6379}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 19: Create test resources `backend-java/module-hrm/src/test/resources/application.yml`**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/test
          jwk-set-uri: http://localhost:8180/realms/test/protocol/openid-connect/certs
```

- [ ] **Step 20: Create `backend-java/module-hrm/src/test/java/com/vieterp/hrm/service/EmployeeServiceTest.java`**

```java
package com.vieterp.hrm.service;

import com.vieterp.hrm.domain.Department;
import com.vieterp.hrm.domain.Employee;
import com.vieterp.hrm.event.EmployeeEventPublisher;
import com.vieterp.hrm.exception.EmployeeNotFoundException;
import com.vieterp.hrm.repository.DepartmentRepository;
import com.vieterp.hrm.repository.EmployeeRepository;
import com.vieterp.hrm.service.dto.CreateEmployeeRequest;
import com.vieterp.hrm.service.dto.EmployeeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeEventPublisher eventPublisher;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testEmployee = Employee.builder()
            .id(testId)
            .firstName("Nguyen")
            .lastName("Van A")
            .email("nguyenvana@vieterp.com")
            .status("ACTIVE")
            .hireDate(java.time.Instant.now())
            .build();
    }

    @Test
    void create_savesEmployeeAndPublishesEvent() {
        CreateEmployeeRequest req = new CreateEmployeeRequest(
            "Nguyen", "Van A", "nguyenvana@vieterp.com", "0912345678", null, "ACTIVE"
        );
        when(employeeRepository.save(any(Employee.class))).thenReturn(testEmployee);

        EmployeeResponse resp = employeeService.create(req);

        assertNotNull(resp);
        assertEquals(testId, resp.id());
        assertEquals("Nguyen", resp.firstName());
        verify(employeeRepository).save(any(Employee.class));
        verify(eventPublisher).publishCreated(testEmployee);
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(employeeRepository.findByIdWithDepartment(testId)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getById(testId));
    }

    @Test
    void listAll_returnsAllEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of(testEmployee));

        List<EmployeeResponse> result = employeeService.listAll();

        assertEquals(1, result.size());
        assertEquals(testId, result.get(0).id());
    }

    @Test
    void delete_publishesEventAndDeletes() {
        when(employeeRepository.existsById(testId)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(testId);

        employeeService.delete(testId);

        verify(eventPublisher).publishDeleted(testId);
        verify(employeeRepository).deleteById(testId);
    }
}
```

- [ ] **Step 21: Verify `module-hrm` compiles**

Run: `cd backend-java && ./mvnw compile -pl module-hrm -am`
Expected: BUILD SUCCESS

- [ ] **Step 22: Run unit tests**

Run: `cd backend-java && ./mvnw test -pl module-hrm`
Expected: All tests PASS (EmployeeServiceTest should pass)

- [ ] **Step 23: Commit**

```bash
git add backend-java/module-hrm/
git commit -m "feat(module-hrm): implement HRM domain module with employee CRUD and NATS events

- Spring Boot 4, Java 26, JPA/Hibernate
- REST controller with OpenAPI docs
- Employee and Department entities
- Event publisher for NATS via Spring Cloud Stream
- Global exception handler with RFC 7807 Problem Details
- JWT auth via Spring Security OAuth2 Resource Server
- Full unit test coverage for EmployeeService"
```

---

### Task 4: Prisma-to-JPA Generator Script

**Files:**

- Create: `scripts/prisma-to-jpa/README.md`
- Create: `scripts/prisma-to-jpa/prisma-to-jpa.mjs` (Node.js script)
- Create: `scripts/prisma-to-jpa/templates/Entity.java.template`

**Prerequisite:** Check existing Prisma schema location. Run: `ls apps/*/prisma/schema.prisma 2>/dev/null | head -5`

- [ ] **Step 1: Create `scripts/prisma-to-jpa/prisma-to-jpa.mjs`**

```javascript
#!/usr/bin/env node
/**
 * Prisma-to-JPA Generator
 * Reads Prisma schema files and generates JPA @Entity Java classes.
 *
 * Usage: node prisma-to-jpa.mjs --schema apps/HRM-unified/prisma/schema.prisma --out backend-java/module-hrm/src/main/java
 */

import { parseArgs } from "node:util";
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const PRISMA_TO_JAVA = {
  String: "String",
  Boolean: "Boolean",
  Int: "Integer",
  BigInt: "Long",
  Float: "Double",
  Decimal: "java.math.BigDecimal",
  DateTime: "java.time.Instant",
  Json: "String", // or com.fasterxml.jackson.databind.JsonNode
  UUID: "java.util.UUID",
};

const PRISMA_TO_JPA_ANNOTATIONS = {
  String: "@Column(length = 191)",
  DateTime: '@Column(name = "{{columnName}}")',
  Boolean: "",
  Int: "",
  BigInt: "",
  Float: "",
  Decimal: "@Column(precision = 19, scale = 2)",
  UUID: "",
};

function parseSchema(schemaContent) {
  const models = [];
  const modelRegex = /model (\w+)\s*\{([^}]+)\}/g;
  let match;
  while ((match = modelRegex.exec(schemaContent)) !== null) {
    const modelName = match[1];
    const fieldsBlock = match[2];
    const fields = [];
    const fieldLines = fieldsBlock
      .trim()
      .split("\n")
      .filter((l) => l.trim());
    for (const line of fieldLines) {
      const trimmed = line.trim();
      const fieldMatch = trimmed.match(
        /^(\w+)\s+(\w+)(\?)?(\s*@\w+(\([^)]*\))?)*$/,
      );
      if (fieldMatch) {
        const [, fieldType, fieldName, optional, ...annotations] = fieldMatch;
        const isId = annotations.some((a) => a.includes("@id"));
        fields.push({
          name: fieldName,
          type: fieldType,
          optional: !!optional,
          isId,
          annotations: annotations.filter(Boolean),
        });
      }
    }
    models.push({ name: modelName, fields });
  }
  return models;
}

function generateEntity(model) {
  const packageName = "com.vieterp.hrm.domain";
  const entityName = model.name;

  const imports = new Set(["jakarta.persistence"]);
  if (model.fields.some((f) => f.type === "UUID"))
    imports.add("java.util.UUID");
  if (model.fields.some((f) => f.type === "DateTime"))
    imports.add("java.time.Instant");

  let fieldLines = model.fields
    .map((field) => {
      const javaType = PRISMA_TO_JAVA[field.type] || "String";
      const jpaAnnotation = field.isId
        ? "@Id @GeneratedValue(strategy = GenerationType.IDENTITY)"
        : PRISMA_TO_JPA_ANNOTATIONS[field.type] || "";
      return `    ${jpaAnnotation ? jpaAnnotation + "\n    " : ""}private ${javaType}${field.optional ? "" : ""} ${field.name};`;
    })
    .join("\n");

  return `package ${packageName};

import jakarta.persistence.*;
import lombok.*;
${[...imports].map((i) => `import ${i};`).join("\n")}

@Entity
@Table(name = "${entityName.toLowerCase()}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ${entityName} {
${fieldLines}
}
`;
}

function main() {
  const { values } = parseArgs({
    options: {
      schema: { type: "string", short: "s" },
      out: { type: "string", short: "o" },
    },
  });

  if (!values.schema || !values.out) {
    console.error(
      "Usage: node prisma-to-jpa.mjs --schema <path> --out <output-dir>",
    );
    process.exit(1);
  }

  const content = readFileSync(values.schema, "utf-8");
  const models = parseSchema(content);
  console.log(`Found ${models.length} models in ${values.schema}`);

  for (const model of models) {
    const java = generateEntity(model);
    const outDir = join(values.out, "com/vieterp/hrm/domain");
    mkdirSync(outDir, { recursive: true });
    const outFile = join(outDir, `${model.name}.java`);
    writeFileSync(outFile, java);
    console.log(`Generated: ${outFile}`);
  }
  console.log("Done.");
}

main();
```

- [ ] **Step 2: Run generator against HRM Prisma schema**

Run: `node scripts/prisma-to-jpa/prisma-to-jpa.mjs --schema apps/HRM-unified/prisma/schema.prisma --out backend-java/module-hrm/src/main/java`
Expected: Java files generated per model in `com.vieterp.hrm.domain`

- [ ] **Step 3: Commit**

```bash
git add scripts/prisma-to-jpa/
git commit -m "feat(scripts): add Prisma-to-JPA generator for entity scaffolding"
```

---

## Phase 2: Frontend React 19 Upgrade

### Task 5: Upgrade Next.js and React

**Files to check/modify:**

- `package.json` (root)
- `apps/*/package.json` (per app)

**Note:** Only check/upgrade the packages that exist. Do NOT add packages that don't exist.

- [ ] **Step 1: Check current Next.js and React versions**

Run: `node -e "const p = JSON.parse(require('fs').readFileSync('package.json','utf8')); console.log('Next:', p.dependencies.next, 'React:', p.dependencies.react);"`

- [ ] **Step 2: Upgrade React and Next.js in root package.json**

If versions are below 19 / 15, update using:

```bash
npm install next@latest react@latest react-dom@latest --save
```

- [ ] **Step 3: Verify builds**

Run: `npm run build` (or existing build command — check `package.json` scripts)
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add package.json package-lock.json apps/*/package.json
git commit -m "feat: upgrade Next.js to latest and React to 19"
```

---

## Phase 3: Shared Packages Dual Publish

### Task 6: `packages/auth` — Dual npm + Maven

**Files:**

- Create: `packages/auth/pom.xml`
- Create: `packages/auth/src/main/java/com/vieterp/auth/` (JWT and permission types)

- [ ] **Step 1: Create `packages/auth/pom.xml`** (analogous to Task 2 `events/pom.xml`)

- [ ] **Step 2: Create `packages/auth/src/main/java/com/vieterp/auth/Permission.java`**

```java
package com.vieterp.auth;

/**
 * VietERP permission constants.
 * Must match TypeScript Permission enum in packages/auth/src/permission.ts
 */
public enum Permission {
    HRM_EMPLOYEE_READ,
    HRM_EMPLOYEE_WRITE,
    HRM_EMPLOYEE_DELETE,
    CRM_CONTACT_READ,
    CRM_CONTACT_WRITE,
    CRM_CONTACT_DELETE,
    ACCOUNTING_INVOICE_READ,
    ACCOUNTING_INVOICE_WRITE,
    MRP_PLAN_READ,
    MRP_PLAN_WRITE,
}
```

- [ ] **Step 3: Create `packages/auth/src/main/java/com/vieterp/auth/JwtClaims.java`**

```java
package com.vieterp.auth;

import java.util.Set;
import java.util.UUID;

/**
 * JWT claims extracted from Keycloak token.
 * Corresponds to the TypeScript JwtPayload in packages/auth/src/jwt.ts
 */
public record JwtClaims(
    UUID sub,
    String email,
    String name,
    Set<String> roles,
    String realm,
    long exp,
    long iat
) {}
```

- [ ] **Step 4: Commit**

```bash
git add packages/auth/
git commit -m "feat(auth): add Maven pom.xml and Java JWT/Permission types for dual publish"
```

---

### Task 7: `packages/api-types` — Dual npm + Maven

**Files:**

- Create: `packages/api-types/pom.xml`
- Create: `packages/api-types/src/main/java/com/vieterp/api/` (shared DTO records)

- [ ] **Step 1: Create `packages/api-types/pom.xml`**

- [ ] **Step 2: Create `packages/api-types/src/main/java/com/vieterp/api/PagedResponse.java`**

```java
package com.vieterp.api;

import java.util.List;

/**
 * Standard paginated response.
 * Must match TypeScript PagedResponse in packages/api-types/src/paged.ts
 */
public record PagedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    long total,
    boolean hasNext
) {}
```

- [ ] **Step 3: Create `packages/api-types/src/main/java/com/vieterp/api/ErrorResponse.java`**

```java
package com.vieterp.api;

import java.time.Instant;

/**
 * Standard error response conforming to RFC 7807 Problem Details.
 */
public record ErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    Instant timestamp
) {}
```

- [ ] **Step 4: Commit**

```bash
git add packages/api-types/
git commit -m "feat(api-types): add Maven pom.xml and Java DTO records for dual publish"
```

---

## Phase 4: Remaining Backend Modules

**Approach for remaining modules (CRM, Accounting, MRP, TPM, etc.):**

After HRM pilot validates the pattern, each remaining module follows the same structure:

```
backend-java/
├── module-crm/
├── module-accounting/
├── module-mrp/
├── module-tpm/
└── module-shared/
```

**Per module, repeat the pattern from Task 3:**

1. Create `pom.xml` with same deps (swap `hrm` → `{domain}`)
2. Create domain entities (use Prisma-to-JPA generator + manual review)
3. Create repository interfaces
4. Create DTO records
5. Create service class with `@Transactional`
6. Create REST controller with OpenAPI annotations
7. Create event publishers (if module publishes events)
8. Create `application.yml` with module-specific NATS bindings
9. Add module to parent `pom.xml` `<modules>` list
10. Write service unit tests
11. Compile + test

**Suggested order:** module-crm → module-accounting → module-mrp → module-tpm → remaining modules.

---

## Phase 5: Infrastructure Integration

### Task 8: Keycloak Integration Test

**Verify JWT validation end-to-end:**

- Deploy Spring Boot with test Keycloak realm
- Call `/api/v1/hrm/employees` with valid JWT → 200
- Call without JWT → 401
- Call with expired JWT → 401

### Task 9: NATS Integration Test

**Verify event publishing:**

- Start NATS locally (or point to existing cluster)
- POST `/api/v1/hrm/employees` to create an employee
- Verify `hrm.employee.created` message appears on NATS

### Task 10: Kong Gateway Cutover

**Files:**

- Modify: `infrastructure/kong/` (or wherever Kong config lives — check `infrastructure/` directory)

**Steps:**

1. Add Spring Boot upstream to Kong config
2. Update route targets from NestJS service → Spring Boot service
3. `kubectl apply` Kong changes
4. Verify `/api/v1/*` routes resolve to Spring Boot

---

## Phase 6: Kubernetes Deployment

### Task 11: Docker Build + Push

**Files:**

- Create: `backend-java/Dockerfile` (see Design Section 9 for content)

- [ ] **Step 1: Build Docker image**

Run: `cd backend-java && docker build -t vieterp/backend:1.0.0 .`
Expected: Image built successfully, < 200MB

- [ ] **Step 2: Push to registry**

Run: `docker push vieterp/backend:1.0.0`
Expected: Image pushed

### Task 12: Helm Deployment

**Files:**

- Modify: `infrastructure/helm/vieterp/values.yaml` (add Spring Boot config)

Steps depend on existing Helm chart structure — check `infrastructure/helm/` first:
Run: `ls infrastructure/helm/`

---

## Implementation Order

1. **Task 1** — Parent POM scaffold (foundation for everything)
2. **Task 2** — `module-shared` / `packages/events` dual publish (dependency of Task 3)
3. **Task 3** — `module-hrm` pilot (validates the full stack works)
4. **Task 4** — Prisma-to-JPA generator (bootstrap remaining modules)
5. **Task 5** — React 19 upgrade (independent, can run in parallel)
6. **Task 6, 7** — Remaining dual publish packages
7. **Remaining modules** — CRM, Accounting, MRP, TPM, etc. (follow Task 3 pattern)
8. **Phase 5** — Keycloak, NATS, Kong integration
9. **Phase 6** — Docker + Helm + cutover

---

## Self-Review Checklist

- [ ] Spec coverage: All 10 design sections have corresponding tasks
- [ ] No placeholders (TBD/TODO) in any task step
- [ ] Type consistency: `EmployeeCreatedEvent` fields match between Java (`events` module) and TypeScript (`packages/events`)
- [ ] `JwtClaims` record fields match what Keycloak actually provides
- [ ] NATS topic names consistent: `hrm.employee.created` used in both `EmployeeEventPublisher` and `application.yml` bindings
- [ ] All file paths are absolute or relative with correct base directory
- [ ] Task 3 DTO `CreateEmployeeRequest` field names match what the TypeScript frontend will send (ensure consistency)
