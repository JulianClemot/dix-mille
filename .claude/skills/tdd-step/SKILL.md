---
name: tdd-step
description: Execute one TDD red-green-refactor cycle for the current increment. Writes a failing test, runs it, implements minimum code, runs tests again. Use after /design-tests for each increment.
user-invocable: true
---

# New Feature Workflow — Step 4: TDD Implementation

You are on **Step 4 of 5** of the DixMille feature development workflow.

## What you will do

Execute the TDD loop for the current increment using the `tdd-engineer` agent. The agent will:

1. Pick the next test condition (simplest first)
2. Write a failing test in `commonTest`
3. **Run `./gradlew :composeApp:commonTest`** to confirm it fails (RED)
4. Implement the minimum production code to pass
5. **Run `./gradlew :composeApp:commonTest`** again to confirm it passes (GREEN)
6. Refactor if needed, run tests again
7. Repeat for each remaining test condition
8. Run the full suite at the end to confirm no regressions

## Instructions

Invoke the `tdd-engineer` agent. Pass the current increment number, the BDD scenario, and the designed test conditions.

The agent runs tests automatically — no manual intervention needed during the loop.

## RED phase is mandatory

If a test passes before any implementation is written, the test is wrong. The agent will catch this and fix the test before proceeding.

## After the agent completes

The agent will report which tests are passing and what was implemented.

- If **more increments remain**: tell the user to run `/design-tests` for the next increment, then `/tdd-step`
- If **all increments are done**: tell the user to run `/feature-review`

## Workflow Map

```
/new-feature        ← Define BDD spec
    ↓
/plan-increments    ← Break spec into increments
    ↓
/design-tests       ← Design test conditions
    ↓
/tdd-step           ← YOU ARE HERE (repeat per increment)
    ↓
/feature-review     ← Integration tests, E2E tests, commit
```

## Loop reminder

For a feature with N increments:
```
/design-tests  →  /tdd-step    (Increment 1)
/design-tests  →  /tdd-step    (Increment 2)
...
/design-tests  →  /tdd-step    (Increment N)
/feature-review
```
