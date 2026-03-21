---
name: design-tests
description: Design exhaustive test conditions for the current increment before writing any code. Covers happy paths, edge cases, and error states. Use after /plan-increments, before /tdd-step.
user-invocable: true
effort: medium
allowed-tools: Read
tags: [testing, tdd, bdd, test-design, workflow]
---

# New Feature Workflow — Step 3: Test Design

You are on **Step 3 of 5** of the DixMille feature development workflow.

## What you will do

Produce a complete list of test conditions for the current increment — before writing any implementation code. This is the thinking phase that makes the TDD loop fast and focused.

## Instructions

Ask the user which increment they are on (or infer from context). Then produce a test condition table:

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

Test method name: should_<expectedBehavior>_when_<condition>
```

## Completeness Checklist

Before presenting the list, verify:
- [ ] Happy path covered
- [ ] Empty/null inputs covered (where applicable)
- [ ] Minimum valid input covered
- [ ] Maximum valid input covered
- [ ] Already-in-that-state scenario (idempotency) covered if relevant
- [ ] Failure path covered (what happens when dependencies fail)
- [ ] Domain rule interactions covered (busts, entry threshold, final round, score collision)

## After presenting the test conditions

Ask the user: "Do these test conditions fully cover the increment? Any cases to add or remove?"

Once confirmed, tell the user:
> "Test conditions locked. Run `/tdd-step` to start the red-green-refactor cycle for this increment."

## Workflow Map

```
/new-feature        ← Define BDD spec
    ↓
/plan-increments    ← Break spec into increments
    ↓
/design-tests       ← YOU ARE HERE
    ↓
/tdd-step           ← Implement (red → green → refactor)
    ↓
/feature-review     ← Integration tests, E2E tests, commit
```
