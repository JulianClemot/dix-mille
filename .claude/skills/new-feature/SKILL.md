---
name: new-feature
description: Start the feature development workflow. Triggers collaborative spec refinement with BDD scenario writing. Use when the user wants to start building a new feature.
user-invocable: true
---

# New Feature Workflow — Step 1: Spec Refinement

You are starting the **DixMille feature development workflow**. This is Step 1 of 5.

## What you will do

1. Engage the `spec-refiner` agent to collaboratively define the feature
2. The agent will ask clarifying questions, then write strict Gherkin BDD scenarios to `docs/SPEC.md`
3. Once the spec is confirmed, guide the user to the next step

## Instructions

Invoke the `spec-refiner` agent with the user's feature description. Pass along the full context of what the user said.

The agent will:
- Ask 5+ clarifying questions
- Summarize its understanding for validation
- Write Gherkin scenarios to `docs/SPEC.md`
- Report what was written

## After the agent completes

Tell the user:
> "Spec written to `docs/SPEC.md`. Review the scenarios and confirm they capture your intent.
> When ready, run `/plan-increments` to break this into implementation steps."

## Workflow Map

```
/new-feature        ← YOU ARE HERE
    ↓
/plan-increments    ← Break spec into smallest increments
    ↓
/design-tests       ← Design test conditions per increment
    ↓
/tdd-step           ← Implement (red → green → refactor)
    ↓
/feature-review     ← Integration tests, E2E tests, commit
```
