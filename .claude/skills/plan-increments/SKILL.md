---
name: plan-increments
description: Break confirmed BDD scenarios into the smallest independently shippable implementation increments, then run each increment (test design + TDD) and the feature review automatically. Use to start or resume the pipeline from the increment-planning stage.
user-invocable: true
effort: high
allowed-tools: Read, Grep, Glob, Bash, Write, Edit, Agent
tags: [workflow, bdd, planning, increments, tdd, automated]
---

# Feature Workflow — Increment Planning (auto-continues to the end)

This is a re-entry point into the automated DixMille feature pipeline. It assumes the
BDD spec already exists in `docs/SPEC.md`. It plans the increments and then runs the
rest of the pipeline automatically — no confirmation gates — stopping before commit.

## What you will do

1. Invoke the `increment-planner` agent to decompose the BDD scenarios into ordered
   increments. Each increment is the smallest unit that compiles, passes tests, and
   produces a product change.
2. Note the ordered plan (no user confirmation).
3. For each increment, in order: design its test conditions (`design-tests` logic),
   then implement it with the `tdd-engineer` agent.
4. Run the feature review (`feature-review` logic): implement integration and E2E
   tests, run the full suite.
5. Report the final summary and stop. **Do not invoke `/commit`.**

## Instructions

Invoke the `increment-planner` agent. It will:
- Read `docs/SPEC.md` to find the feature's BDD scenarios
- Identify which architecture layers each scenario touches (domain / data / presentation)
- Produce an ordered increment list with acceptance criteria

Do not wait for the user to confirm the ordering. As soon as the plan is reported,
proceed through every increment and then the review, exactly as the `new-feature`
skill describes for Steps 3–5.

## What a Good Increment Plan Looks Like

Each increment must compile, pass tests, and produce a visible product change. Example
for the three-bust penalty feature:

```
Increment 1 — Domain model: add consecutiveBusts and scoreBeforeStreak to Player
  Layers: domain/model
  Acceptance: Player data class has both fields, existing tests still pass

Increment 2 — BustTurnUseCase: increment consecutiveBusts on bust
  Layers: domain/usecase
  Acceptance: consecutiveBusts increments correctly, resets on score

Increment 3 — BustTurnUseCase: revert score on third consecutive bust
  Layers: domain/usecase
  Acceptance: score reverts to scoreBeforeStreak when consecutiveBusts reaches 3

Increment 4 — ViewModel: expose bust count in UiState
  Layers: presentation/viewmodel
  Acceptance: ScoreSheetUiState.consecutiveBusts reflects player state

Increment 5 — UI: show bust warning indicator on second consecutive bust
  Layers: presentation/screen
  Acceptance: warning icon visible after 2 busts, gone after scoring
```

**Rules a good plan follows:**
- Domain increments always come before presentation increments
- Each increment touches as few files as possible
- No increment combines a model change with a use case change

## Increment Sizing Rules

- Domain model change = 1 increment
- Use case = 1 increment per distinct behavior
- Validation rule = 1 increment
- ViewModel state change = 1 increment
- UI component = 1 increment
- Full screen = 2–4 increments minimum

## Workflow Map

```
/new-feature        ← Define BDD spec (asks questions once)
    ↓
/plan-increments    ← YOU ARE HERE — plans, then auto-runs every increment + review
    ↓
(per increment)  test design → tdd-engineer
    ↓
feature review      → STOP before /commit
```
