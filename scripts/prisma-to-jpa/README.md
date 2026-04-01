# Prisma-to-JPA Generator

A Node.js script that reads Prisma schema files and generates JPA `@Entity` Java classes for bootstrapping backend services.

## Overview

This tool parses Prisma schema files (`.prisma`) and generates corresponding JPA entity classes. It is designed to help bootstrap JPA entities for all modules after the HRM pilot, ensuring consistency across the codebase.

## Requirements

- Node.js 18+

## Usage

```bash
node prisma-to-jpa.mjs --schema <path> --out <output-dir> --package <java.package.name>
```

### Parameters

| Parameter   | Description                                                                    |
| ----------- | ------------------------------------------------------------------------------ |
| `--schema`  | Path to the Prisma schema file (e.g., `apps/HRM-unified/prisma/schema.prisma`) |
| `--out`     | Output directory for generated Java files                                      |
| `--package` | Java package name for the entities (e.g., `com.vieterp.hrm.domain`)            |

### Example

```bash
node scripts/prisma-to-jpa/prisma-to-jpa.mjs \
  --schema apps/HRM-unified/prisma/schema.prisma \
  --out backend-java/module-hrm/src/main/java \
  --package com.vieterp.hrm.domain
```

## Output

The script generates one `.java` file per Prisma model in the specified output directory, organized by package structure.

For example, for a `model Employee { ... }` with package `com.vieterp.hrm.domain`, the output would be:

```
backend-java/module-hrm/src/main/java/com/vieterp/hrm/domain/Employee.java
```

## Type Mapping

| Prisma Type | Java/JPA Type                             |
| ----------- | ----------------------------------------- |
| `String`    | `String` (with `@Column(length = 191)`)   |
| `Boolean`   | `Boolean`                                 |
| `Int`       | `Integer`                                 |
| `BigInt`    | `Long`                                    |
| `Float`     | `Double`                                  |
| `Decimal`   | `java.math.BigDecimal`                    |
| `DateTime`  | `java.time.Instant`                       |
| `UUID`      | `java.util.UUID`                          |
| `Json`      | `com.fasterxml.jackson.databind.JsonNode` |

## Decorator Mapping

| Prisma Decorator                        | JPA Annotation                                            |
| --------------------------------------- | --------------------------------------------------------- |
| `@id`                                   | `@Id` + `@GeneratedValue(strategy = GenerationType.UUID)` |
| `@unique`                               | `@Column(unique = true)`                                  |
| `@default(cuid())` / `@default(uuid())` | `@GeneratedValue(strategy = GenerationType.UUID)`         |
| `@map("column_name")`                   | `@Column(name = "column_name")`                           |
| `@updatedAt`                            | `@UpdateTimestamp`                                        |
| `@createdAt`                            | `@CreationTimestamp`                                      |

## Generated Entity Template

```java
package {packageName};

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "{tableName}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {EntityName} {
    // Fields with appropriate JPA annotations
}
```

## Notes

- Enum fields are generated as `String` type. Enum classes should be created separately.
- Field ordering is preserved from the Prisma schema.
- Optional fields get `nullable = true` in the `@Column` annotation.
- String fields (non-ID) get `@Column(length = 191)` for database compatibility.

## Use Cases

This generator is particularly useful for:

1. Bootstrapping JPA entities for new modules (CRM, Accounting, MRP, TPM)
2. Ensuring consistency in entity structure across modules
3. Quick initial entity generation that can be refined manually

## Future Enhancements

- Support for generating enum classes alongside entities
- Generation of repository interfaces
- Support for relationship fields (`@relation`)
- Batch processing of multiple schema files
