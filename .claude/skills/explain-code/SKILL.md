---
name: explain-code
description: Explains code with visual diagrams and analogies. Use when explaining how code works, teaching about a codebase, or when the user asks "how does this work?"
effort: medium
allowed-tools: Read, Grep, Glob
tags: [explanation, teaching, diagrams, documentation]
---

## When to Use

- User asks "how does this work?" or "what does X do?"
- Teaching a pattern or concept to someone unfamiliar with it
- Explaining a non-obvious design decision in the codebase
- Onboarding to a new area of the code

## When NOT to Use

- User needs to *change* code → switch to the relevant implementation skill
- The answer is a one-liner where an analogy would over-explain

## How to Explain

Always include these four elements:

1. **Start with an analogy**: Compare the code to something from everyday life
2. **Draw a diagram**: Use ASCII art to show the flow, structure, or relationships
3. **Walk through the code**: Explain step-by-step what happens
4. **Highlight a gotcha**: What's a common mistake or misconception?

Keep explanations conversational. For complex concepts, use multiple analogies.

## Diagram Types

Pick the right shape for the concept:

### Flow / sequence (use for: call chains, data pipelines)
```
User taps "Add Score"
        │
        ▼
  ViewModel.onEvent(AddScore)
        │
        ▼
  AddScoreEntryUseCase.invoke()
        │
        ▼
  GameRepository.saveGame()
        │
        ▼
  LocalStorage.put(key, json)
```

### Layer (use for: architecture, dependency direction)
```
┌──────────────────────────┐
│      Presentation        │  ← ViewModel, Composables
├──────────────────────────┤
│        Domain            │  ← Use Cases, Models, Validation
├──────────────────────────┤
│         Data             │  ← Repository, LocalStorage
└──────────────────────────┘
         ↑ depends on ↑
```

### State machine (use for: turn lifecycle, game phases)
```
[Idle] ──addScore──▶ [Turn Active]
                          │
              commitTurn ─┤─ bust
                          │
                    ┌─────┴──────┐
                    ▼            ▼
              [Next Player]  [Bust Recorded]
```

### Data model (use for: entity relationships)
```
Game
 ├── players: List<Player>
 │    └── name, score, consecutiveBusts
 ├── currentPlayerIndex: Int
 └── currentTurn: Turn
       └── entries: List<ScoreEntry>
                     └── points, category
```

## Worked Example

**User:** "How does the three-bust penalty work?"

**1. Analogy**
> Think of it like a bowling house rule: three gutter balls in a row and you lose points you'd already banked — not just this frame, but your whole streak since the last time you scored.

**2. Diagram**
```
Turn N-2: Bust → consecutiveBusts = 1, snapshot previousScore = 500
Turn N-1: Bust → consecutiveBusts = 2
Turn N  : Bust → consecutiveBusts = 3 → REVERT score to 500
```

**3. Walk-through**
> `BustTurnUseCase` is called. It increments `consecutiveBusts` on the current player. If the count reaches 3, it reads `scoreBeforeStreak` — the player's score from before the first of the three busts — and resets `score` to that value. The bust counter then resets to 0.

**4. Gotcha**
> The reversion only triggers on **3 consecutive** busts. If the player scores (even 0 points via a skip) between two busts, the streak resets. A voluntary skip does **not** count as a bust.