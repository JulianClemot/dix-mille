---
name: feature-review
description: Review a completed feature, then implement integration and E2E tests, run the full suite, and report a summary. Runs automatically with no approval gates and stops before commit. Final step of the feature development workflow.
user-invocable: true
effort: high
allowed-tools: Read, Grep, Glob, Bash, Write, Edit
tags: [review, testing, integration, e2e, workflow, automated]
---

# Feature Workflow — Feature Review (automated, stops before commit)

Final step of the automated DixMille feature pipeline. Runs with **no approval
gates**: it reviews the feature, implements the integration and E2E tests it
identifies, runs the full suite, and then stops with a summary. It does **not**
commit — the user runs `/commit` themselves.

## What you will do

1. Review all code written for this feature
2. Identify and implement integration tests
3. Identify and implement E2E tests (if the feature has UI)
4. Run the full suite and confirm green
5. Report a summary and stop — **do not invoke `/commit`**

## Step-by-Step

### 1. Code Review

Read all files modified or created for this feature. Summarize:
- What was built (domain, data, presentation layers affected)
- Key design decisions
- Any tech debt or known limitations introduced

### 2. Integration Tests

Identify scenarios where **multiple units interact** end-to-end through the layers:
- Use case → repository → storage
- ViewModel → use case → domain model
- Full game state transitions spanning multiple turns

For each, note briefly:
```
### Integration Test: <name>
**Tests the interaction between:** <LayerA> ↔ <LayerB>
**Scenario:** <what it validates>
**Why a unit test can't catch this:** <reason>
```

Then **implement all of them.** Do not ask for approval.

### 3. E2E Tests (UI features only)

If the feature includes Compose UI changes, identify end-to-end tests using
`composeTestRule`:
- Full user flow from screen entry to final state
- Each BDD scenario that has UI steps

For each, note briefly:
```
### E2E Test: <name>
**Screen(s):** <screens involved>
**User flow:** <what the user does>
**Assertion:** <what is verified on screen>
```

Then **implement all of them.** Do not ask for approval.

### 4. Run Tests

1. Run `./gradlew :composeApp:commonTest` (and any other relevant suite).
2. Confirm all pass. If something fails, fix it and re-run.
3. If a failure cannot be resolved, stop and report it clearly.

### 5. Final Summary — then stop

Present:
```
## Feature Complete: <feature name>

### Unit Tests
- X tests written, X passing

### Integration Tests
- X tests written, X passing

### E2E Tests
- X tests written, X passing (or "not applicable")

### Files changed
- <list>

### Assumptions made / open questions
- <list, or "none">
```

Then tell the user:
> "Pipeline complete — all tests green. Review the changes and run `/commit` when ready."

**Do not invoke `/commit`.** The workflow ends here.

## Workflow Map

```
/new-feature        ← Define BDD spec (asks questions once)
    ↓
/plan-increments    ← Break spec into increments
    ↓
/design-tests       ← Design test conditions
    ↓
/tdd-step           ← Implement (repeat per increment)
    ↓
/feature-review     ← YOU ARE HERE — implements integration + E2E tests, then STOP before /commit
```
