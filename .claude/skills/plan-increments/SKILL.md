---
name: plan-increments
description: Break confirmed BDD scenarios into the smallest independently shippable implementation increments. Use after /new-feature spec is confirmed.
user-invocable: true
effort: medium
allowed-tools: Read, Agent
tags: [workflow, bdd, planning, increments, tdd]
---

# New Feature Workflow — Step 2: Increment Planning

You are on **Step 2 of 5** of the DixMille feature development workflow.

## What you will do

1. Invoke the `increment-planner` agent to decompose the BDD scenarios into ordered increments
2. Each increment is the smallest unit that compiles, passes tests, and produces a product change
3. Present the ordered plan to the user for confirmation

## Instructions

Invoke the `increment-planner` agent. It will:
- Read `docs/SPEC.md` to find the feature's BDD scenarios
- Identify which architecture layers each scenario touches (domain / data / presentation)
- Produce an ordered increment list with acceptance criteria
- Ask the user to confirm the ordering

## After the agent completes

Once the user confirms the increment plan, tell them:
> "Increment plan confirmed. Run `/design-tests` to generate test conditions for Increment 1."

If the user wants to adjust the plan, ask the agent to revise.

## What a Good Increment Plan Looks Like

Each increment must compile, pass tests, and produce a visible product change. Example for the three-bust penalty feature:

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

## Reminder: Increment Sizing Rules

- Domain model change = 1 increment
- Use case = 1 increment per distinct behavior
- Validation rule = 1 increment
- ViewModel state change = 1 increment
- UI component = 1 increment
- Full screen = 2–4 increments minimum

## Workflow Map

```
/new-feature        ← Define BDD spec
    ↓
/plan-increments    ← YOU ARE HERE
    ↓
/design-tests       ← Design test conditions per increment
    ↓
/tdd-step           ← Implement (red → green → refactor)
    ↓
/feature-review     ← Integration tests, E2E tests, commit
```
