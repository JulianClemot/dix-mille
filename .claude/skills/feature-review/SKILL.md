---
name: feature-review
description: Review a completed feature, propose integration and E2E tests, then commit on user approval. Final step of the feature development workflow.
user-invocable: true
effort: high
allowed-tools: Read, Grep, Glob, Bash, Write, Edit
tags: [review, testing, integration, e2e, workflow]
---

# New Feature Workflow — Step 5: Feature Review

You are on the **final step** of the DixMille feature development workflow.

## What you will do

1. Review all code written for this feature
2. Propose integration tests
3. Propose E2E tests (if the feature has UI)
4. Wait for user approval on each test type
5. Implement approved tests and run them
6. On final approval, invoke `/commit`

## Step-by-Step

### 1. Code Review

Read all files modified or created for this feature. Summarize:
- What was built (domain, data, presentation layers affected)
- Key design decisions
- Any tech debt or known limitations introduced

### 2. Integration Test Proposal

Identify scenarios where **multiple units interact** end-to-end through the layers:
- Use case → repository → storage
- ViewModel → use case → domain model
- Full game state transitions spanning multiple turns

For each proposed integration test:
```
### Integration Test: <name>
**Tests the interaction between:** <LayerA> ↔ <LayerB>
**Scenario:** <what it validates>
**Why a unit test can't catch this:** <reason>
```

Ask the user: "Should I implement these integration tests?"

### 3. E2E Test Proposal (UI features only)

If the feature includes Compose UI changes, propose end-to-end tests using `composeTestRule`:
- Full user flow from screen entry to final state
- Each BDD scenario that has UI steps

For each proposed E2E test:
```
### E2E Test: <name>
**Screen(s):** <screens involved>
**User flow:** <what the user does>
**Assertion:** <what is verified on screen>
```

Ask the user: "Should I implement these E2E tests?"

### 4. Implement Approved Tests

For each approved test type:
1. Write the tests
2. Run `./gradlew :composeApp:commonTest` (or appropriate suite)
3. Confirm all pass
4. Report results

### 5. Final Sign-Off

Present a summary:
```
## Feature Complete: <feature name>

### Unit Tests
- X tests written, X passing

### Integration Tests
- X tests written, X passing (or "skipped by user")

### E2E Tests
- X tests written, X passing (or "not applicable" / "skipped by user")

### Files changed
- <list>
```

Ask: **"Ready to commit? I'll invoke `/commit` to format and create the commit."**

Only invoke `/commit` if the user explicitly confirms.

## Workflow Map

```
/new-feature        ← Define BDD spec
    ↓
/plan-increments    ← Break spec into increments
    ↓
/design-tests       ← Design test conditions
    ↓
/tdd-step           ← Implement (repeat per increment)
    ↓
/feature-review     ← YOU ARE HERE
```
