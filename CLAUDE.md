# Lite-SQLite agent instructions

Spec: `docs/spec-vector-search-wal.md`. Decisions: `docs/decisions.md`.

## Stack and map

Java 21 (Gradle toolchain), JUnit 5, Mockito, Lombok, Kafka client for change events. No CI yet.

`App` → `QueryEngineImpl` (`server/queryengine`) → `Table` (`storage/table`) → `SlottedRecordPage` (`storage/record`) → `BufferPool` (`storage/buffer`) → `BasicFileManager` (`storage`) → `<table>.tbl`. Catalog persistence: `server/catalog`.

Before changing a path, read its comprehension memo in `docs/learning/` (restart path: `comprehension-slice2.md`). Do not delete or overwrite anything in `app/database/` without looking at it first.

## Verification rule of thumb

Writing code is cheap. Knowing it is correct is the bottleneck. Every change ships with a check the agent can run itself, and the human reviews the evidence.

1. **Check before code.** Before implementing a slice, define what "done" means as a runnable check: an E2E scenario, a property, or a comparison with an oracle. Restate it in one line before coding.
2. **The implementer does not grade itself.** The user writes the check or predicts its result before implementation. Otherwise a fresh-context reviewer audits the tests against the spec's acceptance criteria.
3. **Pick the check from the spectrum:**
   - **E2E (ground truth):** SQL through `QueryEngineImpl`, with a close and reopen inside the scenario when durability is involved. Keep few of them, all high value.
   - **Property (rules that never break):** round-trip (encode then decode), invariant (no wrong data ever loads; zero permission leaks), idempotency (replaying the WAL twice equals replaying it once), equivalence with a reference (`BplusTree` vs `TreeMap`). Use a seeded `Random` loop in JUnit. A failing seed becomes a regression test.
   - **Differential (an oracle decides):** exact search for HNSW recall, `TreeMap` for the B+tree, and real SQLite for SQL results (needs the `sqlite-jdbc` test dependency, ask first). Ignore row order unless there is an ORDER BY.
4. **The test must tell a correct implementation from a plausible wrong one.** Ask what you could delete and still pass. If deleting it changes nothing, the test is missing or the code is unneeded.
5. **No misleading green.** Never edit, weaken or delete an existing test to make it pass without saying so in the summary. Never drop behavior to satisfy a check.
6. **Fast and deterministic loop.** Use seeded randomness, no sleeps, isolated `@TempDir` state, and quiet output that shows failures only. A flaky test is quarantined and reported, never retried until it passes.
7. **Repeated correction → mechanical check.** If the same mistake is corrected twice, turn it into a test, a hook or a CI check instead of a line of prose.

### Verification tiers

| Tier | When | Command |
|---|---|---|
| Fast | After every edit | `./gradlew :app:test --tests '<TouchedTest>' -q` |
| Slice | End of a slice | `./gradlew :app:test` |
| Phase | Phase checkpoint in the spec | Full suite, crash tests (`SimulatedDisk`, then the kill -9 harness, T1.x), benchmarks, and a fresh-context review against the acceptance criteria |

### Known verification gaps

- `CatalogStoreTest` passes even if `channel.force(true)` is removed. Only a simulated power loss can tell the difference. `SimulatedDisk` (T1.x) closes this gap.

## Session start

A SessionStart hook (`.claude/settings.json`) prints `docs/STATUS.md` and `git log --oneline -5`. Then:

1. Run the Fast tier on the tests of the current slice (listed in STATUS.md) before any change.
2. One slice per session. The user runs `/clear` between tasks. Use subagents for wide exploration and plan mode for design.
3. One commit per slice, only when the user approves.
4. At slice end, update STATUS.md: flip `passes` in the slice list, move the "Now" line. Do not rewrite other entries.

## Learning loop

The user owns decisions and learns by building. Per slice:

1. **One prediction round** before code: problem, knowledge card, 1–2 questions. Hard cap of 2 rounds; after round 2, explain the gap and proceed.
2. **Hints, not answers.** When the user is stuck, give the smallest hint that unblocks (a file:line, a question, a failing case). Give the answer only on a second ask.
3. **User writes the core.** Mark the 20–40 lines that carry the slice's idea with `// TODO(human): <what and why>` and let the user write them. Claude writes scaffolding, tests and glue.
4. **Retrieval cards.** At slice end, write 3–5 question/answer cards to the Obsidian vault (`Minh's Notebook/Backend/Database`, tag `Backend`). Cards replace long knowledge docs.
