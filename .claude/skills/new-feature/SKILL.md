---
name: new-feature
description: Run the full DixMille feature development pipeline automatically — spec, increments, test design, TDD implementation, and review — from one feature description. Asks clarifying questions once up front, then runs to completion with no further prompts, stopping just before the commit. Use when the user wants to build a new feature.
user-invocable: true
effort: high
allowed-tools: Read, Grep, Glob, Bash, Write, Edit, Agent
tags: [workflow, bdd, feature, spec, gherkin, tdd, automated]
---

# Feature Development Workflow — Fully Automated

This runs the **entire** DixMille feature pipeline end to end from a single feature
description. There is **exactly one** interaction point: the `spec-refiner` agent's
clarifying questions at the very start. Once the user answers those, every remaining
step runs automatically — no "confirm this?", no "ready for the next step?" — until
the pipeline stops after the review step. The final `/commit` is left to the user.

## Pipeline

```
1. Spec refinement       (spec-refiner agent)       ← asks clarifying questions ONCE
2. Increment planning     (increment-planner agent)
3. For each increment, in order:
     a. Test design        (inline — design-tests skill logic)
     b. TDD implementation  (tdd-engineer agent)
4. Feature review         (inline — feature-review skill logic)
5. STOP — report summary, do NOT commit
```

## Instructions

### Step 1 — Spec refinement (the only interactive step)

Invoke the `spec-refiner` agent with the user's feature description and the full
context of what they said.

The agent asks its clarifying questions and waits for the user's answers **once**.
After the user answers (or tells it to proceed with stated assumptions), the agent
writes strict Gherkin BDD scenarios to `docs/SPEC.md` and reports what it wrote —
with no separate "confirm my understanding" gate.

As soon as the agent reports the spec is written, continue straight to Step 2. Do
not ask the user to review it.

### Step 2 — Increment planning

Invoke the `increment-planner` agent. It reads the BDD scenarios from `docs/SPEC.md`,
decomposes them into an ordered list of smallest-shippable increments with acceptance
criteria, and reports the plan.

Do not ask the user to confirm the ordering. Note the plan and continue to Step 3.

### Step 3 — Per-increment loop

For each increment in the plan, in order:

#### 3a. Test design

Produce the complete list of test conditions for this increment using the format in
the `design-tests` skill — happy path, boundary values, invalid inputs, state
preconditions, side effects, and the completeness checklist. Record the conditions.
Do not ask whether they are complete.

#### 3b. TDD implementation

Invoke the `tdd-engineer` agent, passing the increment number, its BDD scenarios, and
the test conditions you just designed. The agent runs the red-green-refactor loop,
runs `./gradlew :composeApp:commonTest` itself, and reports what it implemented.

When it reports back, move directly to the next increment. Repeat 3a + 3b until every
increment is done.

### Step 4 — Feature review

Follow the `feature-review` skill logic with all approval gates removed:

1. Read every file created/modified for the feature. Summarise what was built (layers
   touched, key design decisions, any tech debt introduced).
2. Identify the integration tests that exercise cross-layer interactions
   (use case ↔ repository ↔ storage, ViewModel ↔ use case ↔ domain, multi-turn game
   state). **Implement them.** Run them.
3. If the feature touches Compose UI, identify the E2E tests with `composeTestRule`
   for each BDD scenario that has UI steps. **Implement them.** Run them.
4. Run the full suite (`./gradlew :composeApp:commonTest`) and confirm it is green.

Do not ask "should I implement these tests?" — implement them.

### Step 5 — Stop (do not commit)

Present the final summary:

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

## Rules

- The **only** time you stop for the user is the `spec-refiner` clarifying questions
  in Step 1. Everywhere else, proceed on your own judgement.
- Stop and report **only** on a hard failure you cannot resolve: a broken build, a
  test condition that cannot be satisfied, or a spec contradiction. Never continue
  silently past a red suite.
- Domain increments always precede presentation increments (the `increment-planner`
  enforces this).
- All tests live in `commonTest`. Test naming: backtick sentence style
  `` `Should X when Y` ``.

## What a Good Spec Looks Like

Strict Gherkin under a `### BDD Scenarios` heading in `docs/SPEC.md`:

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

**Quality signals:**
- Every `Then` is explicit — no implicit failures
- `Background` only for preconditions shared by ALL scenarios
- Domain vocabulary throughout (bust, skip, entry threshold, final round)
- Edge cases get their own scenarios

## Individual re-entry points

Each stage remains separately invocable to resume partway through:
`/plan-increments`, `/design-tests`, `/tdd-step`, `/feature-review`. Each of those
also runs automatically through to the end of the pipeline, stopping before commit.

## Workflow Map

```
/new-feature        ← full automated pipeline (spec → increments → tests → TDD → review)
    │
    ├─ spec-refiner       (asks questions once)
    ├─ increment-planner
    ├─ per increment: test design → tdd-engineer
    └─ feature review     → STOP before /commit
```
