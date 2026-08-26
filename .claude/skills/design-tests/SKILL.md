---
name: design-tests
description: Design exhaustive test conditions for the current increment, then implement it via TDD and continue the pipeline automatically. Covers happy paths, edge cases, and error states. Use to start or resume the pipeline from the test-design stage.
user-invocable: true
effort: high
allowed-tools: Read, Grep, Glob, Bash, Write, Edit, Agent
tags: [testing, tdd, bdd, test-design, workflow, automated]
---

# Feature Workflow — Test Design (auto-continues to TDD and review)

This is a re-entry point into the automated DixMille feature pipeline. It designs the
test conditions for the current increment, then hands straight to the `tdd-engineer`
agent and keeps going through the remaining increments and the feature review — no
confirmation gates — stopping before commit.

## What you will do

Produce a complete list of test conditions for the current increment — before writing
any implementation code — then proceed directly to implementation.

## Instructions

Determine the current increment from context (the increment plan and which increments
are already implemented). Then produce the test condition list.

### For each BDD scenario in this increment:

1. **Happy path** — the scenario works as specified
2. **Boundary values** — minimum/maximum valid inputs
3. **Invalid inputs** — what the system should reject and why
4. **State preconditions** — scenarios where prior state affects behavior
5. **Side effects** — what else should (or should not) change as a result

### Output format for each test condition:

```
### Test N: <short description>

**Type:** happy path | boundary | error | side effect
**Given:** <precondition state>
**When:** <action performed>
**Then:** <expected outcome>
**Edge:** <why this case matters>

Test method name: `Should <expectedBehavior> when <condition>`
```

## Completeness Checklist

Before moving on, verify:
- [ ] Happy path covered
- [ ] Empty/null inputs covered (where applicable)
- [ ] Minimum valid input covered
- [ ] Maximum valid input covered
- [ ] Already-in-that-state scenario (idempotency) covered if relevant
- [ ] Failure path covered (what happens when dependencies fail)
- [ ] Domain rule interactions covered (busts, entry threshold, final round, score collision)

## After designing the test conditions

Do not ask the user whether the conditions are complete. Record them and continue
immediately:

1. Invoke the `tdd-engineer` agent with this increment's number, its BDD scenarios,
   and these test conditions.
2. When it reports back, if more increments remain, repeat this test-design step for
   the next increment and invoke `tdd-engineer` again.
3. When all increments are done, run the feature review (`feature-review` logic):
   implement integration and E2E tests, run the full suite, report the summary.
4. Stop. **Do not invoke `/commit`.**

## Workflow Map

```
/new-feature        ← Define BDD spec (asks questions once)
    ↓
/plan-increments    ← Break spec into increments
    ↓
/design-tests       ← YOU ARE HERE — designs conditions, then auto-runs TDD + review
    ↓
(per increment)  tdd-engineer
    ↓
feature review      → STOP before /commit
```
