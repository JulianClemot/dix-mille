---
name: plan-increments
description: Break confirmed BDD scenarios into the smallest independently shippable implementation increments. Use after /new-feature spec is confirmed.
user-invocable: true
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
