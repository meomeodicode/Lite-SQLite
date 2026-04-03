# Lite-SQLite

A lightweight SQLite implementation. Sometimes you don't need a 200MB database—just a project to learn how databases work from the ground up.

## What This Project Demonstrates

- Building a mini relational engine from scratch
- Translating SQL text into executable operations through parser visitors and command DTOs
- Designing a storage layer with slotted pages (basically Tetris for database rows :D) and record-level access
- Using a buffer pool + LRU strategy to reduce disk I/O
- Implementing and integrating B+ tree indexes for faster lookup paths
- Structuring a multi-layer Java system that doesn't make your eyes water 💦💦💦.

I am refactoring this codebase (my brain) T_T

## Supported SQL

- `CREATE TABLE`
- `INSERT`
- `SELECT`
- `UPDATE`
- `DELETE`
- `CREATE INDEX`

## Architecture Overview

```text
lite.sqlite.cli/                   CLI layer
lite.sqlite.server.parser/         SQL parsing and visitor logic
lite.sqlite.server.queryengine/    Query planning and execution
lite.sqlite.server.scan/           Record scan abstractions
lite.sqlite.server.storage/        File, page, buffer, table, and record management
lite.sqlite.server.datastructure/  B+ tree and supporting structures
```

### Core Components

- `QueryEngineImpl` — Routes SQL commands to their execution paths.
- `MySqlStatementVisitor` — Converts the parser's AST into domain command models.
- `Table` — Table-level record and index interactions. 
- `SlottedRecordPage` — Page layout with slot directory + record serialization. 
- `BufferPool` — Caches pages with LRU behavior.
- `BplusTree` — Index data structure for keyed lookups.

## Key Engineering Decisions

- **Slotted-page format** for variable-size records and efficient page reuse
- **Explicit schema typing** (`INTEGER`, `VARCHAR`) to enforce conversion/validation at write time
- **Layered package organization** to separate parsing, execution, and persistence concerns
- **Index-aware selection path** so equality predicates can use indexes before fallback scans

## What I Learned

- How SQL text becomes executable operations through parser visitors and command DTOs
- Why storage layout decisions (offsets, free-space pointers, slot directories) matter for correctness
- How buffer management policies directly affect performance and I/O behavior
- How to reason about type-safety improvements (e.g., operator enums vs magic constants)
- The importance of refactoring for separation of concerns in growing codebases

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew runCli
```

### Example

```sql
CREATE TABLE students (id INTEGER, name VARCHAR(50), grade INTEGER);
INSERT INTO students (id, name, grade) VALUES (1, 'Alice', 85);
INSERT INTO students (id, name, grade) VALUES (2, 'Bob', 92);
SELECT * FROM students;
CREATE INDEX idx_students_id ON students(id);
SELECT name FROM students WHERE id = 2;
```

## Test

```bash
./gradlew test
./gradlew testRecordPage
./gradlew testQueryEngine
```

## Tech Stack

- Java 21
- Gradle 8.8
- ShardingSphere SQL Parser (MySQL dialect)
- JUnit 5
- Mockito
- Guava

## Project Structure

```text
Lite-SQLite/
+-- app/
│   +-- src/
│   │   +-- main/java/lite/sqlite/
│   │   │   +-- App.java
│   │   │   +-- cli/
│   │   │   +-- server/
│   │   │       +-- queryengine/
│   │   │       +-- parser/
│   │   │       +-- storage/
│   │   │       +-- datastructure/
│   │   +-- test/
│   +-- build.gradle
+-- gradle/wrapper/
+-- build.gradle
```