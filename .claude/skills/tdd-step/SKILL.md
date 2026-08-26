---
name: tdd-step
description: Execute the TDD red-green-refactor cycle for the current increment via the tdd-engineer agent, then automatically design and implement the remaining increments and run the feature review. Use to start or resume the pipeline from the implementation stage.
user-invocable: true
effort: high
allowed-tools: Read, Grep, Glob, Bash, Write, Edit, Agent
tags: [tdd, workflow, testing, red-green-refactor, implementation, automated]
---

# Feature Workflow — TDD Implementation (auto-continues through all increments + review)

This is a re-entry point into the automated DixMille feature pipeline. It runs the TDD
loop for the current increment and then keeps going — designing tests for and
implementing every remaining increment, then running the feature review — with no
confirmation gates, stopping before commit.

## What you will do

For the current increment, invoke the `tdd-engineer` agent. It will:

1. Pick the next test condition (simplest first)
2. Write a failing test in `commonTest`
3. **Run `./gradlew :composeApp:commonTest`** to confirm it fails (RED)
4. Implement the minimum production code to pass
5. **Run `./gradlew :composeApp:commonTest`** again to confirm it passes (GREEN)
6. Refactor if needed, run tests again
7. Repeat for each remaining test condition
8. Run the full suite at the end to confirm no regressions

## Instructions

Invoke the `tdd-engineer` agent. Pass the current increment number, its BDD scenarios,
and the designed test conditions. If test conditions for this increment have not been
designed yet, design them first (see the `design-tests` skill) — do not stop to ask.

The agent runs tests automatically — no manual intervention during the loop.

## RED phase is mandatory

If a test passes before any implementation is written, the test is wrong. The agent
will catch this and fix the test before proceeding.

## What a Healthy RED→GREEN Cycle Looks Like

**RED** — the test fails for the right reason:
```
> Task :composeApp:compileKotlinMetadata FAILED

e: BustTurnUseCaseTest.kt:42: Unresolved reference: consecutiveBusts

BUILD FAILED — 1 error
```
This is correct: the property doesn't exist yet. Proceed to implementation.

**GREEN** — minimum code passes:
```
> Task :composeApp:commonTest

BustTurnUseCaseTest > Should increment consecutive busts when player busts PASSED

BUILD SUCCESSFUL — 1 test passed
```

**Wrong RED** (test passes immediately → test is wrong):
```
BustTurnUseCaseTest > Should revert score when third consecutive bust PASSED
```
Stop. The test asserts nothing meaningful. Fix the assertion before proceeding.

## Common Failure Modes to Watch

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Test passes before impl | Assertion always true | Strengthen the assertion |
| Compile error in wrong file | Import missing | Add the import, re-run |
| Test fails after GREEN | Regression from refactor | Revert refactor, step smaller |
| `runTest` hangs | Missing `advanceUntilIdle()` | Add after the action |

## After the agent completes

The agent reports which tests pass and what was implemented. Then, without asking the
user:

- **If more increments remain:** design test conditions for the next increment
  (`design-tests` logic) and invoke `tdd-engineer` again. Repeat until every increment
  is done.
- **When all increments are done:** run the feature review (`feature-review` logic) —
  implement integration and E2E tests, run the full suite, present the summary, and
  stop. **Do not invoke `/commit`.**

Stop early only on a hard failure you cannot resolve (broken build, an impossible test
condition, a spec contradiction).

## Workflow Map

```
/new-feature        ← Define BDD spec (asks questions once)
    ↓
/plan-increments    ← Break spec into increments
    ↓
/design-tests       ← Design test conditions
    ↓
/tdd-step           ← YOU ARE HERE — implements this increment, then auto-loops the rest + review
    ↓
feature review      → STOP before /commit
```
