# Lite-SQLite

A **lightweight** SQLite implementation that's serious about being lightweight. Because sometimes you don't need a 200MB database—just a fun project to learn how databases work from the ground up (or should we say, from the *root* up?).

## What This Project Demonstrates

- Building a mini relational engine from scratch—spoonful by spoonful
- Translating SQL text into executable operations through parser visitors and command DTOs (the real "SQL to Java" pipeline)
- Designing a storage layer with slotted pages (basically Tetris for database rows :D) and record-level access
- Using a buffer pool + LRU strategy to reduce disk I/O
- Implementing and integrating B+ tree indexes for faster lookup paths (the indexing equivalent of taking the express lane)
- Structuring a multi-layer Java system that doesn't make your eyes water 💦💦💦.

I am refactoring this codebase (my brain) T_T

## Supported SQL

- `CREATE TABLE` — *Let's make this official*
- `INSERT` — *Adding rows like it's our job (it is)*
- `SELECT` — *The most popular kid at the query party*
- `UPDATE` — *Change is the only constant*
- `DELETE` — *When you really mean it*
- `CREATE INDEX` — *Because waiting is for the impatient*

## Architecture Overview

```text
lite.sqlite.cli/                   CLI layer (where users meet queries)
lite.sqlite.server.parser/         SQL parsing and visitor logic (linguistics for databases)
lite.sqlite.server.queryengine/    Query planning/execution orchestration (the conductor)
lite.sqlite.server.scan/           Record scan abstractions (reading between the lines)
lite.sqlite.server.storage/        File/page/buffer/table/record management (the basement)
lite.sqlite.server.datastructure/  B+ tree and supporting structures (branching out)
```

### Core Components

- `QueryEngineImpl` — Routes SQL commands to their execution paths.
- `MySqlStatementVisitor` — Converts the parser's AST into domain command models.
- `Table` — Table-level record and index interactions. 
- `SlottedRecordPage` — Page layout with slot directory + record serialization. 
- `BufferPool` — Caches pages with LRU behavior. The short-term memory your database wishes it had.
- `BplusTree` — Index data structure for keyed lookups. Balances between depth and breadth.

## Key Engineering Decisions

- **Slotted-page format** for variable-size records and efficient page reuse — *because nobody likes wasted space*
- **Explicit schema typing** (`INTEGER`, `VARCHAR`) to enforce conversion/validation at write time — *type safety is not a myth*
- **Layered package organization** to separate parsing, execution, and persistence concerns — *separation of church and state, database edition*
- **Index-aware selection path** so equality predicates can use indexes before fallback scans — *optimization: making queries great again*

## What I Learned

- How SQL text becomes executable operations through parser visitors and command DTOs
- Why storage layout decisions (offsets, free-space pointers, slot directories) matter for correctness
- How buffer management policies directly affect performance and I/O behavior
- How to reason about type-safety improvements (e.g., operator enums vs magic constants)
- The importance of refactoring for separation of concerns in growing codebases (and why god classes are not deities)

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

- Java 21 — *The chosen one*
- Gradle 8.8 — *Build automation that doesn't judge*
- ShardingSphere SQL Parser (MySQL dialect) — *Linguistic wizardry*
- JUnit 5 — *Testing, but make it official*
- Mockito — *Mocking without the salt*
- Guava — *Google's Swiss army knife*

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