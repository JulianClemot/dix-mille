---
name: new-feature
description: Start the feature development workflow. Triggers collaborative spec refinement with BDD scenario writing. Use when the user wants to start building a new feature.
user-invocable: true
effort: high
allowed-tools: Read, Agent
tags: [workflow, bdd, feature, spec, gherkin]
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

## What a Good Spec Looks Like

The agent should produce strict Gherkin under a `### BDD Scenarios` heading in `docs/SPEC.md`:

```gherkin
Feature: Three-Bust Penalty
  As a player
  I want my score to revert after three consecutive busts
  So that reckless play has a meaningful penalty

  Background:
    Given a game with player "Alice" who has a score of 500

  Scenario: Score reverts after three consecutive busts
    Given Alice has busted twice consecutively
    When Alice busts a third time
    Then Alice's score reverts to her score before the bust streak began

  Scenario: Bust streak resets after a successful turn
    Given Alice has busted twice consecutively
    When Alice scores 300 points
    Then Alice's consecutive bust count resets to 0

  Scenario: Skip does not count as a bust
    Given Alice has busted twice consecutively
    When Alice skips her turn
    Then Alice's consecutive bust count remains 2
```

**Quality signals for a good spec:**
- Every `Then` is explicit — no implicit failures
- `Background` used only for preconditions shared by ALL scenarios
- Domain vocabulary used throughout (bust, skip, entry threshold, final round)
- Edge cases have their own scenarios (not crammed into one)

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
