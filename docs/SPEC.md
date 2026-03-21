# Dix Mille (10,000) — Score Sheet App Specification

## Project Overview

**Dix Mille** is a digital score sheet for the traditional French dice game "10,000". Players roll
physical dice and use the app to track scores, enforce rules, and determine the winner.

### App Type
- **Score sheet only** — Manual entry, players roll physical dice
- **Strict rule enforcement** — App prevents invalid moves
- **Local persistence** — Games saved across app restarts
- **Fixed turn order** — Players always go in creation order
- **Turn history tracking** — Complete turn-by-turn history for all players
- **Auto-validate UX** — ViewModel automatically commits valid turns on score entry

### Platform
- Kotlin Multiplatform Mobile (KMM)
- Android & iOS
- Compose Multiplatform UI
- Material 3 Design

---

## Scoring Rules (Reference)

### Basic Scoring

| Dice | Points | Notes |
|------|--------|-------|
| Single 1 | 100 | Always scores |
| Single 5 | 50 | Must re-roll if only one 5 in entire throw |

### Triplets (Three of a Kind)

| Dice | Points | Formula |
|------|--------|---------|
| Three 1s | 1,000 | Special case |
| Three 2s | 200 | Face × 100 |
| Three 3s | 300 | Face × 100 |
| Three 4s | 400 | Face × 100 |
| Three 5s | 500 | Face × 100 |
| Three 6s | 600 | Face × 100 |

### Multiples (Four, Five, Six of a Kind)

| Count | Multiplier | Example |
|-------|------------|---------|
| Four (Carré) | 2× triple value | Four 2s = 400 |
| Five (Quinte) | 4× triple value | Five 2s = 800 |
| Six (Sixte) | 8× triple value | Six 2s = 1,600 |

**Special case for 1s:** Three 1s = 1,000 / Four 1s = 2,000 / Five 1s = 4,000 / Six 1s = 8,000

---

## Domain Model

### Game
```kotlin
data class Game(
    val id: String,
    val players: List<Player>,
    val targetScore: Int = 10_000,
    val currentPlayerIndex: Int = 0,
    val gamePhase: GamePhase = GamePhase.IN_PROGRESS,
    val triggeringPlayerId: String? = null,
    val createdAt: Long,
    val turnHistory: List<TurnRecord> = emptyList(),
    val roundNumber: Int = 1,
    val rules: GameRules = GameRules()
)
```

### GameRules
```kotlin
data class GameRules(
    val targetScore: Int = 10_000,
    val entryMinimumScore: Int = 500,
    val consecutiveBustsForPenalty: Int = 3,    // min 2
    val enableBustPenalty: Boolean = true,
    val enableFinalRound: Boolean = true,
    val minPlayers: Int = 2,
    val maxPlayers: Int = 6
)
```

### Player
```kotlin
data class Player(
    val id: String,
    val name: String,
    val totalScore: Int = 0,
    val hasEnteredGame: Boolean = false,
    val currentTurn: Turn? = null,
    val hasPlayedFinalRound: Boolean = false,
    val consecutiveBusts: Int = 0
)
```

### Turn
```kotlin
data class Turn(
    val id: String,
    val entries: List<ScoreEntry> = emptyList(),
    val isBusted: Boolean = false
) {
    val turnTotal: Int get() = if (isBusted) 0 else entries.sumOf { it.points }
}
```

### ScoreEntry
```kotlin
data class ScoreEntry(
    val id: String,
    val points: Int,
    val type: ScoreType = ScoreType.PRESET,
    val label: String? = null
)

enum class ScoreType {
    PRESET,  // Quick-tap preset score
    CUSTOM   // Manually entered
}
```

### GamePhase
```kotlin
enum class GamePhase {
    IN_PROGRESS,   // Normal gameplay
    FINAL_ROUND,   // Someone hit target, others get final turn
    ENDED          // Game complete
}
```

### TurnOutcome
```kotlin
enum class TurnOutcome {
    SCORED,     // Player scored points — resets bust counter
    BUST,       // No scoring dice — counts toward bust penalty
    SKIP,       // Voluntary skip — does NOT count as bust
    COLLISION   // Score reverted due to collision — does NOT count as bust
}
```

### TurnRecord
```kotlin
data class TurnRecord(
    val roundNumber: Int,
    val playerId: String,
    val points: Int,
    val outcome: TurnOutcome,
    val previousScore: Int
)
```

### PresetScore
```kotlin
data class PresetScore(
    val points: Int,
    val label: String
)
```

### ValidationResult
```kotlin
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val error: ValidationError) : ValidationResult()
}

sealed class ValidationError {
    data class InsufficientPointsToEnter(val minimum: Int = 500) : ValidationError()
    data object InvalidScoreValue : ValidationError()
    data object GameAlreadyEnded : ValidationError()
    data object NotPlayersTurn : ValidationError()
    data object MustScoreToCommit : ValidationError()
    data object TurnAlreadyBusted : ValidationError()
    data object ScoreExceedsTarget : ValidationError()
}
```

---

## Preset Score Values

| Points | Label | Common Combinations |
|--------|-------|---------------------|
| 50 | "One 5" | 5 |
| 100 | "One 1" | 1 |
| 150 | "1 + 5" | 1, 5 |
| 200 | "Two 1s / Three 2s" | 1, 1 or 2, 2, 2 |
| 250 | "Two 1s + 5" | 1, 1, 5 |
| 300 | "Three 1s / Three 3s" | 1, 1, 1 or 3, 3, 3 |
| 350 | "Three 3s + 5" | 3, 3, 3, 5 |
| 400 | "Four 1s / Three 4s" | 1, 1, 1, 1 or 4, 4, 4 |
| 450 | "Three 4s + 5" | 4, 4, 4, 5 |
| 500 | "Five 1s / Three 5s" | 1×5 or 5, 5, 5 |
| 600 | "Six 1s / Three 6s" | 1×6 or 6, 6, 6 |
| 750 | "Three 5s + Two 1s" | 5, 5, 5, 1, 1 |
| 1000 | "Three 1s" | 1, 1, 1 |
| 1500 | "Three 1s + 5" | 1, 1, 1, 5 |
| 2000 | "Four 1s" | 1, 1, 1, 1 |
| 2500 | "Four 1s + 5" | 1, 1, 1, 1, 5 |
| **Custom** | User enters any value | Manual entry |

---

## BDD Scenarios

---

### Feature: Game Creation

```gherkin
Feature: Game Creation
  As a group of players
  I want to create a new game with a list of players
  So that I can start tracking scores

  Scenario: Create a game with valid player count
    Given I want to start a game with 3 players named "Alice", "Bob", "Carol"
    When I create the game
    Then the game is created with 3 players in order: Alice, Bob, Carol
    And all players start with a total score of 0
    And all players have not entered the game
    And the current player is Alice
    And the game phase is IN_PROGRESS
    And the round number is 1

  Scenario: Create a game with custom target score
    Given I want to start a game with target score 5000
    When I create the game with 2 players
    Then the game target score is 5000

  Scenario: Create a game with minimum player count
    Given I want to create a game with 2 players
    When I create the game
    Then the game is created successfully

  Scenario: Create a game with maximum player count
    Given I want to create a game with 6 players
    When I create the game
    Then the game is created successfully

  Scenario: Fail to create a game with too few players
    Given I want to create a game with 1 player
    When I create the game
    Then the game creation fails with an invalid player count error

  Scenario: Fail to create a game with too many players
    Given I want to create a game with 7 players
    When I create the game
    Then the game creation fails with an invalid player count error
```

---

### Feature: Score Entry

```gherkin
Feature: Score Entry
  As the current player
  I want to add a score entry to my current turn
  So that I can accumulate points before committing my turn

  Background:
    Given a game in progress with players Alice and Bob
    And it is Alice's turn
    And Alice has entered the game

  Scenario: Add a valid preset score
    When Alice adds a score entry of 100 points
    Then Alice's current turn contains 1 entry of 100 points
    And Alice's current turn total is 100

  Scenario: Add multiple entries in the same turn
    When Alice adds 100 points
    And Alice adds 200 points
    Then Alice's current turn total is 300

  Scenario: Add a custom score entry
    When Alice adds a custom score of 750 points
    Then Alice's current turn contains 1 entry of 750 points (custom)

  Scenario: Reject an invalid score value
    When Alice tries to add a score entry of 0 points
    Then the entry is rejected with an InvalidScoreValue error
    And Alice's current turn is unchanged

  Scenario: Reject a negative score value
    When Alice tries to add a score entry of -50 points
    Then the entry is rejected with an InvalidScoreValue error

  Scenario: Reject a score entry when the game has ended
    Given the game phase is ENDED
    When Alice tries to add a score entry
    Then the entry is rejected with a GameAlreadyEnded error

  Scenario: Reject a score entry that would exceed the target score
    Given Alice's total score is 9800
    And the target score is 10000
    When Alice tries to add a score entry of 300 points
    Then the entry is rejected with a ScoreExceedsTarget error
    And Alice's current turn is unchanged

  Scenario: Reject an entry from a player who is not the current player
    Given it is Alice's turn
    When Bob tries to add a score entry
    Then the entry is rejected with a NotPlayersTurn error

  Scenario: Reject a score entry on a busted turn
    Given Alice's current turn has been marked as busted
    When Alice tries to add a score entry
    Then the entry is rejected with a TurnAlreadyBusted error
```

---

### Feature: Turn Commitment

```gherkin
Feature: Turn Commitment
  As the current player
  I want to commit my turn total to my score
  So that my points are saved and the next player can go

  Background:
    Given a game in progress with players Alice and Bob
    And it is Alice's turn
    And Alice has entered the game

  Scenario: Commit a valid turn
    Given Alice's current turn total is 300
    When Alice commits her turn
    Then Alice's total score increases by 300
    And Alice's current turn is cleared
    And it is now Bob's turn
    And a SCORED turn record is added to history

  Scenario: Reject committing a turn with zero points
    Given Alice's current turn is empty
    When Alice tries to commit her turn
    Then the commit is rejected with a MustScoreToCommit error
    And Alice's total score is unchanged

  Scenario: Reject committing when the game has ended
    Given the game phase is ENDED
    When Alice tries to commit her turn
    Then the commit is rejected with a GameAlreadyEnded error

  Scenario: Advance to next player after commit
    Given there are 3 players: Alice, Bob, Carol
    And it is Alice's turn
    When Alice commits a turn with 200 points
    Then it is Bob's turn

  Scenario: Wrap around to first player after last player commits
    Given there are 2 players: Alice, Bob
    And it is Bob's turn
    When Bob commits a turn with 200 points
    Then it is Alice's turn
    And the round number increments

  Scenario: Bust counter resets to 0 on successful commit
    Given Alice has 2 consecutive busts
    When Alice commits a turn with 300 points
    Then Alice's consecutive bust count is 0
```

---

### Feature: Entry Rule (500-Point Minimum)

```gherkin
Feature: Entry Rule (500-Point Minimum)
  As a player who has not yet entered the game
  I want the app to enforce the 500-point entry minimum
  So that I must score at least 500 in a single turn to start accumulating points

  Background:
    Given a game in progress with players Alice and Bob
    And it is Alice's turn
    And Alice has NOT entered the game
    And Alice's total score is 0

  Scenario: Enter the game with exactly 500 points
    Given Alice's current turn total is 500
    When Alice commits her turn
    Then Alice's total score is 500
    And Alice has entered the game

  Scenario: Enter the game with more than 500 points
    Given Alice's current turn total is 750
    When Alice commits her turn
    Then Alice's total score is 750
    And Alice has entered the game

  Scenario: Fail to enter the game with less than 500 points
    Given Alice's current turn total is 400
    When Alice tries to commit her turn
    Then the commit is rejected with an InsufficientPointsToEnter error
    And Alice's total score remains 0
    And Alice has not entered the game

  Scenario: Once entered, no minimum applies on future turns
    Given Alice has previously entered the game (total score 600)
    And Alice's current turn total is 100
    When Alice commits her turn
    Then Alice's total score is 700
    And no entry error is raised

  Scenario: Entry minimum is configurable
    Given the game uses a custom entry minimum of 300
    And Alice's current turn total is 350
    When Alice commits her turn
    Then Alice's total score is 350
    And Alice has entered the game

  Scenario: Entry minimum blocks commit even with custom minimum
    Given the game uses a custom entry minimum of 300
    And Alice's current turn total is 250
    When Alice tries to commit her turn
    Then the commit is rejected with an InsufficientPointsToEnter error
```

---

### Feature: Bust Turn

```gherkin
Feature: Bust Turn
  As the current player
  I want to record a bust when no dice scored
  So that my turn points are lost and the next player can go

  Background:
    Given a game in progress with players Alice and Bob
    And it is Alice's turn
    And Alice has entered the game
    And Alice's total score is 1000

  Scenario: Bust a turn with no accumulated points
    Given Alice's current turn is empty
    When Alice busts her turn
    Then Alice's total score remains 1000
    And Alice's consecutive bust count is 1
    And it is now Bob's turn
    And a BUST turn record is added to history

  Scenario: Bust a turn with accumulated turn points
    Given Alice's current turn total is 300
    When Alice busts her turn
    Then Alice's total score remains 1000
    And the accumulated turn points are discarded
    And Alice's consecutive bust count is 1

  Scenario: Bust increments consecutive bust counter
    Given Alice has 1 consecutive bust
    When Alice busts again
    Then Alice's consecutive bust count is 2

  Scenario: Bust does not apply when player hasn't entered the game
    Given Alice has NOT entered the game
    And Alice's consecutive bust count is 0
    When Alice busts her turn
    Then Alice's consecutive bust count is 1
    And Alice's total score remains 0
```

---

### Feature: Skip Turn

```gherkin
Feature: Skip Turn
  As the current player
  I want to voluntarily skip my turn
  So that I score 0 points without it counting as a bust

  Background:
    Given a game in progress with players Alice and Bob
    And it is Alice's turn
    And Alice has entered the game

  Scenario: Skip a turn records 0 points and advances to next player
    When Alice skips her turn
    Then Alice's total score is unchanged
    And it is now Bob's turn
    And a SKIP turn record is added to history with 0 points

  Scenario: Skip does NOT increment the consecutive bust counter
    Given Alice has 1 consecutive bust
    When Alice skips her turn
    Then Alice's consecutive bust count remains 1

  Scenario: Skip does NOT reset the consecutive bust counter
    Given Alice has 2 consecutive busts
    When Alice skips her turn
    Then Alice's consecutive bust count remains 2

  Scenario: Skip is distinct from bust in the turn record
    When Alice skips her turn
    Then the turn record outcome is SKIP, not BUST
```

---

### Feature: Three-Bust Penalty

```gherkin
Feature: Three-Bust Penalty
  As a player
  I want the three-consecutive-bust rule to be enforced
  So that repeated busting is penalised by reverting my score

  Background:
    Given a game in progress with players Alice and Bob
    And Alice has entered the game

  Scenario: Score reverts to previousScore of last scored turn on 3rd bust
    Given Alice's turn history is:
      | round | outcome | points | previousScore |
      |   1   | SCORED  | 500    | 0             |
      |   2   | SCORED  | 300    | 500           |
      |   3   | BUST    | 0      | 800           |
      |   4   | BUST    | 0      | 800           |
    And Alice's current total score is 800
    When Alice busts a 3rd consecutive time
    Then Alice's total score reverts to 500 (previousScore of last SCORED record)
    And Alice's consecutive bust count resets to 0

  Scenario: Score reverts to 0 when no prior scored turn exists
    Given Alice has entered the game with 500 points in round 1
    And Alice busted in rounds 2 and 3
    And Alice's turn history shows only 1 SCORED record with previousScore 0
    When Alice busts a 3rd consecutive time
    Then Alice's total score reverts to 0
    And Alice's consecutive bust count resets to 0

  Scenario: Bust counter resets after penalty is applied
    Given Alice has just received the three-bust penalty
    When Alice busts once more in a later turn
    Then Alice's consecutive bust count is 1 (not 4)

  Scenario: Skip between busts does NOT prevent the penalty
    Given Alice has 2 consecutive busts
    When Alice skips her turn (bust count stays at 2)
    And Alice busts again
    Then Alice does NOT receive the three-bust penalty (counter was 2, not 3 before bust)

  Scenario: Scoring between busts resets the counter
    Given Alice has 2 consecutive busts
    When Alice scores 200 points (bust counter resets to 0)
    And Alice busts once
    Then Alice's consecutive bust count is 1
    And the penalty is not triggered

  Scenario: Bust penalty is skipped when disabled via rules
    Given the game has bust penalty disabled (enableBustPenalty = false)
    And Alice has 2 consecutive busts
    When Alice busts a 3rd consecutive time
    Then Alice's total score is unchanged (no reversion)
    And Alice's consecutive bust count is 3

  Scenario Outline: Configurable bust penalty threshold
    Given the game uses a bust threshold of <threshold>
    And Alice has <threshold - 1> consecutive busts
    When Alice busts one more time
    Then Alice's score reverts to previousScore of her last scored turn

    Examples:
      | threshold |
      | 2         |
      | 3         |
      | 4         |
```

---

### Feature: Score Collision

```gherkin
Feature: Score Collision
  As a player
  I want score collisions to be automatically resolved
  So that no two players can share the same total score

  Background:
    Given a game in progress with players Alice, Bob, and Carol

  Scenario: Collision reverts the other player to their previousScore
    Given Bob's total score is 1500
    And Bob's last scored turn had previousScore 1200
    And Alice's total score is 1000
    When Alice scores 500 points (total becomes 1500)
    Then Bob's total score reverts to 1200
    And a COLLISION turn record is created for Bob
    And Alice's total score remains 1500

  Scenario: Scoring player is immune to collision
    Given Alice's total score is 1500
    And Bob's total score is 1000
    When Alice scores 500 points (Alice stays at 1500, but wait — Alice was already at 1500)
    Given Alice's total score is 1000
    And Bob's total score is 1000
    When Carol scores 0 points — invalid
    Given Alice's total score is 1000
    And Bob's total score is 500
    When Alice scores 500 points (total becomes 1000, matching Bob's score before)
    Then Bob reverts, Alice is unaffected

  Scenario: Collision cascades to a third player
    Given Alice's total score is 1000
    And Bob's total score is 700 (last scored turn had previousScore 500)
    And Carol's total score is 500 (last scored turn had previousScore 200)
    When Alice scores 300 points (total becomes 1000 — no match)
    Given Alice's total score is 700
    And Bob's total score is 500 (last scored turn had previousScore 200)
    And Carol's total score is 200
    When Alice scores 300 points (total becomes 1000)
    Then Alice stays at 1000
    And no collision (no one else at 1000)

    Given Alice's total score is 700
    And Bob's total score is 1000 (last scored turn had previousScore 700)
    And Carol's total score is 700 (last scored turn had previousScore 400)
    When Alice scores 300 points (total becomes 1000, matching Bob)
    Then Bob reverts to 700 (matching Carol)
    And Carol reverts to 400 (no further match)
    And Alice stays at 1000

  Scenario: Collision at score 0 is ignored
    Given Bob's total score is 0
    And Alice's total score is 300
    When Alice busts (total stays at 300, not 0)
    Then no collision is triggered for Bob

  Scenario: Collision does NOT increment bust counter
    Given Bob's total score is 500 and has 1 consecutive bust
    And Alice's total score is 200
    When Alice scores 300 points (total becomes 500, matching Bob)
    Then Bob's consecutive bust count remains 1
    And Bob's hasEnteredGame status is unchanged

  Scenario: Collision only triggers on SCORED turns
    Given Bob's total score is 500
    And Alice suffers the three-bust penalty which reverts her score to 500
    Then no collision turn record is created for Bob

  Scenario: Collision does NOT affect hasEnteredGame
    Given Bob has entered the game with total score 500
    And Alice scores to match Bob's score of 500
    When Bob reverts to his previousScore of 200
    Then Bob's hasEnteredGame remains true
```

---

### Feature: Final Round

```gherkin
Feature: Final Round
  As a player
  I want the final round to be triggered when someone reaches the target
  So that every other player gets exactly one more turn before the game ends

  Background:
    Given a game with players Alice, Bob, Carol
    And the target score is 10000

  Scenario: Final round triggers when a player reaches the target score
    Given Alice's total score is 9500
    When Alice commits a turn of 500 points (total becomes 10000)
    Then the game phase becomes FINAL_ROUND
    And Alice is recorded as the triggering player
    And it is Bob's turn

  Scenario: Triggering player does not get another turn in the final round
    Given the game is in FINAL_ROUND
    And Alice is the triggering player
    And Bob and Carol have not yet played their final round turns
    When Bob plays and Carol plays
    Then the game ends
    And Alice does not play again

  Scenario: Each non-triggering player gets exactly one final round turn
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob has not played his final round turn
    When Bob commits a turn
    Then Bob's hasPlayedFinalRound becomes true
    And Bob cannot take another turn in this final round

  Scenario: Game ends when all non-triggering players have played
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob and Carol each play their final round turn
    When the last player (Carol) plays
    Then the game phase becomes ENDED

  Scenario: Final round is skipped when disabled via rules
    Given the game has final round disabled (enableFinalRound = false)
    And Alice's total score is 9500
    When Alice commits a turn of 500 points (total becomes 10000)
    Then the game phase becomes ENDED immediately
    And no final round turns are played

  Scenario: Player cannot act after playing their final round turn
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob has already played his final round turn
    When Bob tries to add a score entry
    Then the action is rejected

  Scenario: Bust or skip during final round still counts as the player's turn
    Given the game is in FINAL_ROUND with triggering player Alice
    And it is Bob's turn
    When Bob busts
    Then Bob's hasPlayedFinalRound becomes true
    And it advances to Carol's turn
```

---

### Feature: Game End and Winner Determination

```gherkin
Feature: Game End and Winner Determination
  As a player
  I want the game to declare the winner correctly
  So that the player with the highest score after the final round wins

  Background:
    Given a 3-player game in progress: Alice, Bob, Carol

  Scenario: Winner is the player with the highest score
    Given the game has ended with final scores: Alice 12500, Bob 10800, Carol 9200
    Then Alice is declared the winner

  Scenario: No further actions allowed after game ends
    Given the game phase is ENDED
    When any player tries to add a score entry
    Then the action is rejected with a GameAlreadyEnded error
```

---

### Feature: Undo Last Entry

```gherkin
Feature: Undo Last Entry
  As the current player
  I want to remove the last score entry from my current turn
  So that I can correct a mistake before committing

  Background:
    Given a game in progress with players Alice and Bob
    And it is Alice's turn
    And Alice has entered the game

  Scenario: Remove the last entry from a turn with multiple entries
    Given Alice's current turn has entries: 100, 200, 300
    When Alice undoes her last entry
    Then Alice's current turn has entries: 100, 200
    And Alice's current turn total is 300

  Scenario: Remove the only entry from a turn
    Given Alice's current turn has one entry of 500 points
    When Alice undoes her last entry
    Then Alice's current turn is empty
    And Alice's current turn total is 0

  Scenario: Undo last entry does not affect total score
    Given Alice's total score is 800
    And Alice's current turn has entries: 100, 200
    When Alice undoes her last entry
    Then Alice's total score remains 800
```

---

### Feature: Undo Last Turn

```gherkin
Feature: Undo Last Turn
  As a player
  I want to undo the most recently committed turn
  So that mistakes made after committing can be corrected

  Background:
    Given a game in progress with players Alice and Bob

  Scenario: Undo a scored turn restores the previous player's score
    Given Alice committed a turn of 300 points (total went from 500 to 800)
    And it is now Bob's turn
    When the last turn is undone
    Then Alice's total score reverts to 500
    And it is Alice's turn again
    And the SCORED turn record is removed from history

  Scenario: Undo reverts hasEnteredGame if the undone turn was the entry turn
    Given Alice had not entered the game
    And Alice committed a turn of 600 points (entered the game)
    When the last turn is undone
    Then Alice's total score reverts to 0
    And Alice's hasEnteredGame becomes false

  Scenario: Undo a turn with trailing collision records removes those records first
    Given Alice committed 500 points causing Bob to collide (Bob reverted)
    And the turn history ends with: [Alice SCORED, Bob COLLISION]
    When the last turn is undone
    Then Bob's collision record is removed first
    And Bob's score is restored to before the collision
    And Alice's scored record is then removed
    And Alice's score is restored to before her turn

  Scenario: Undo resets hasPlayedFinalRound for the undone player
    Given the game is in FINAL_ROUND
    And Bob committed his final round turn (hasPlayedFinalRound = true)
    When the last turn is undone
    Then Bob's hasPlayedFinalRound becomes false
    And it is Bob's turn again

  Scenario: Undo reverts from ENDED back to FINAL_ROUND
    Given the game has just ended (phase = ENDED)
    When the last turn is undone
    Then the game phase reverts to FINAL_ROUND

  Scenario: Undo re-derives consecutive bust counter from history
    Given Alice's history shows: SCORED, BUST, BUST, BUST (penalty applied), SCORED
    And the last turn was the final SCORED (which is now being undone)
    When the last turn is undone
    Then Alice's consecutive bust count is re-derived from remaining history

  Scenario: Undo a bust turn restores the bust state
    Given Alice busted (bust count went from 1 to 2)
    When the last turn is undone
    Then Alice's consecutive bust count reverts to 1
    And Alice's total score is unchanged (bust didn't add points)
    And it is Alice's turn again
```

---

### Feature: Auto-Commit (ViewModel / UX Layer)

```gherkin
Feature: Auto-Commit on Score Entry
  As a player using the app
  I want score entries to be committed automatically
  So that I don't need to tap an "End Turn" button

  Note: This behavior is implemented in the ViewModel layer.
  The domain uses AddScoreEntryUseCase and CommitTurnUseCase independently.
  The ViewModel chains them automatically on a valid score entry.

  Scenario: Adding a valid preset score auto-commits the turn
    Given it is Alice's turn and Alice has entered the game
    When Alice taps a preset score button (e.g. 200 points)
    Then the ViewModel calls AddScoreEntryUseCase
    And the ViewModel immediately calls CommitTurnUseCase
    And Alice's total score increases
    And the turn advances to the next player

  Scenario: Adding an invalid score does NOT auto-commit
    Given it is Alice's turn and Alice has NOT entered the game
    When Alice taps a preset score button that results in a turn total < 500
    Then AddScoreEntryUseCase succeeds
    But CommitTurnUseCase returns InsufficientPointsToEnter
    And an error is shown to the user
    And the turn is NOT committed

  Scenario: Adding a custom score auto-commits when valid
    Given it is Alice's turn and Alice has entered the game
    When Alice types "750" in the custom score field and taps "Add"
    Then the ViewModel adds and commits in sequence
    And Alice's total score increases by 750
```

---

### Feature: Configurable Game Rules

```gherkin
Feature: Configurable Game Rules
  As a game organiser
  I want to customise the game rules when creating a game
  So that players can choose rule variants

  Scenario: Create a game with a custom target score
    Given I set the target score to 5000
    When I create the game
    Then the game ends when a player reaches 5000 points

  Scenario: Create a game with bust penalty disabled
    Given I disable the bust penalty (enableBustPenalty = false)
    And a player busts 3 consecutive times
    When the 3rd bust is recorded
    Then the player's score is NOT reverted
    And the bust counter continues incrementing

  Scenario: Create a game with a custom bust penalty threshold
    Given I set consecutiveBustsForPenalty to 2
    And a player busts twice in a row
    When the 2nd bust is recorded
    Then the player's score reverts to their previousScore

  Scenario: Bust penalty threshold must be at least 2
    Given I try to set consecutiveBustsForPenalty to 1
    When I create the game
    Then the game creation fails with a validation error

  Scenario: Create a game with final round disabled
    Given I disable the final round (enableFinalRound = false)
    When a player reaches the target score
    Then the game ends immediately without a final round

  Scenario: Create a game with a custom entry minimum
    Given I set the entry minimum score to 300
    And a player's first turn totals 350 points
    When the player commits their turn
    Then the player enters the game successfully
```

---

## Implementation Phases

### Phase 1: Domain Models ✅
- [x] `Game` data class
- [x] `Player` data class
- [x] `Turn` data class
- [x] `ScoreEntry` data class
- [x] `GamePhase` enum
- [x] `TurnOutcome` enum
- [x] `TurnRecord` data class
- [x] `PresetScore` data class and constants
- [x] `ValidationResult` / `ValidationError` sealed classes
- [x] `GameRules` data class with configurable rules

### Phase 2: Validation & Rules ✅
- [x] `ScoreValidator` — score entry validation, target validation
- [x] 500-point entry rule validation
- [x] Final round trigger logic
- [x] Winner determination

### Phase 3: Use Cases ✅
- [x] `CreateGameUseCase`
- [x] `AddScoreEntryUseCase`
- [x] `CommitTurnUseCase` — bust counter reset, collision resolution, final round trigger
- [x] `BustTurnUseCase` — bust penalty, three-bust reversion
- [x] `SkipTurnUseCase`
- [x] `UndoLastEntryUseCase`
- [x] `UndoLastTurnUseCase` — collision record removal, bust counter re-derivation
- [x] `GetCurrentGameUseCase`

### Phase 4: Data Layer ✅
- [x] `GameRepository` interface
- [x] `GameRepositoryImpl` with local storage
- [x] `LocalStorage` expect/actual (SharedPreferences / NSUserDefaults)
- [x] JSON serialization for `Game` state

### Phase 5: Presentation — ViewModel ✅
- [x] `GameUiState` data class
- [x] `GameEvent` sealed class (user actions)
- [x] `GameViewModel` with StateFlow

### Phase 6: Presentation — UI Screens ✅
- [x] `HomeScreen`
- [x] `GameSetupScreen`
- [x] `ScoreSheetScreen`
- [x] `GameEndScreen`
- [x] Navigation between screens

### Phase 7: Presentation — Components ✅
- [x] `PlayerScoreCard`
- [x] `TurnSummaryCard`
- [x] `PresetScoreButtons`
- [x] `CustomScoreDialog`
- [x] `ConfirmDialog`

### Phase 8: UX Improvements — Turn History & Undo 🚧
- [ ] Add `UndoLastTurn` event to `GameEvent`
- [ ] Update `GameUiState` with `canUndoTurn` property
- [ ] Implement auto-commit logic in `GameViewModel` (chains AddScoreEntry + CommitTurn)
- [ ] Update repository serialization for new model fields

### Phase 9: UI Redesign — History Table & Streamlined Controls 📋
- [ ] `ScoreHistoryTable` component — turn totals per player
- [ ] `CustomScoreInput` component — inline text field (not dialog)
- [ ] Redesign `ScoreSheetScreen` with sticky bottom bar
- [ ] Remove `EndTurnDialog` (auto-commit replaces it)
- [ ] Add "Undo Turn" button
- [ ] Simplify PresetScoreButtons (remove Custom button, now inline)

### Phase 10: Dark Theme 🎨
- [ ] `DixMilleTheme.kt` with custom color schemes
- [ ] `Color.kt`: deep navy (#1A1A2E), soft purple (#7F5AF0), mint green (#2CB67D), warm orange (#FF8906)
- [ ] Apply `DixMilleTheme` in `App.kt`

### Phase 11: Winner Screen & Animations 🎉
- [ ] `GameEndScreen.kt` — full-screen winner display
- [ ] Trophy/crown animation with scale effect
- [ ] Confetti particles animation
- [ ] Winner name scale-in animation
- [ ] Final scores ranking
- [ ] Navigate to `GameEndScreen` on game end

---

## UI Flow

```
┌─────────────────┐
│  Game Setup     │
│  - Add players  │
│  - Set target   │
│  - Start game   │
└────────┬────────┘
         │
         ▼
┌──────────────────────────────────────────────────────┐
│ 🎲 Dix Mille                          Round 5        │
├──────────────────────────────────────────────────────┤
│ Score History Table (scrollable)                     │
│ Turn │ Alice │ Bob  │ Carol                          │
│  1   │ +500  │  0   │  -                             │
│  2   │ +300  │ +650 │ BUST                           │
│  3   │ +200  │ +150 │ +500                           │
│ TOTAL│ 1600  │ 850  │ 500                            │
├──────────────────────────────────────────────────────┤
│                                             ← STICKY │
│ Custom Score: [______] [Add]        ← Inline input  │
│ [50][100][150][200][250][300][400][500]...           │
│ [🔄 Undo Turn]                      [💥 Bust]       │
└──────────────────────────────────────────────────────┘
         │
         ▼ (when game ends)
┌──────────────────────────────────────────────────────┐
│                                                      │
│              👑 ALICE WINS! 👑                       │
│           (animated crown + confetti)                │
│                  12,500 points                       │
│                                                      │
│  ────────────────────────────────────────────        │
│  🥇 Alice ............ 12,500                        │
│  🥈 Bob .............. 10,800                        │
│  🥉 Carol ............ 9,200                         │
│  ────────────────────────────────────────────        │
│                                                      │
│              [🎮 New Game]                           │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## Technical Decisions

### Architecture
- **Clean Architecture** — Domain, Data, Presentation layers
- **MVVM** — one ViewModel per screen
- **Unidirectional data flow** — events up, state down

### State Management
- `StateFlow` for ViewModel state
- Immutable state with `.copy()` updates
- Single source of truth in `Game` domain model

### Persistence
- `LocalStorage` expect/actual (SharedPreferences on Android, NSUserDefaults on iOS)
- JSON serialization via kotlinx-serialization
- Auto-save on every state change

### Testing Strategy
- Domain layer: 90%+ coverage
- Use Cases: 90%+ coverage
- ViewModels: 80%+ coverage
- Repositories: 70%+ coverage
- UI: Critical paths only

### Dependency Injection
- Koin with modular setup: `dataModule`, `domainModule`, `presentationModule`, `platformModule`

---

## File Structure

```
composeApp/src/commonMain/kotlin/com/julian/dixmille/
├── domain/
│   ├── model/
│   │   ├── Game.kt
│   │   ├── GameRules.kt
│   │   ├── GamePhase.kt
│   │   ├── Player.kt
│   │   ├── Turn.kt
│   │   ├── ScoreEntry.kt
│   │   ├── TurnOutcome.kt
│   │   ├── TurnRecord.kt
│   │   └── PresetScore.kt
│   ├── repository/
│   │   └── GameRepository.kt
│   ├── usecase/
│   │   ├── CreateGameUseCase.kt
│   │   ├── AddScoreEntryUseCase.kt
│   │   ├── CommitTurnUseCase.kt
│   │   ├── BustTurnUseCase.kt
│   │   ├── SkipTurnUseCase.kt
│   │   ├── UndoLastEntryUseCase.kt
│   │   ├── UndoLastTurnUseCase.kt
│   │   └── GetCurrentGameUseCase.kt
│   └── validation/
│       ├── ScoreValidator.kt
│       └── ValidationResult.kt
├── data/
│   ├── repository/
│   │   └── GameRepositoryImpl.kt
│   └── source/
│       └── LocalStorage.kt (expect/actual)
└── presentation/
    ├── viewmodel/
    │   └── GameViewModel.kt
    ├── screen/
    │   ├── HomeScreen.kt
    │   ├── GameSetupScreen.kt
    │   ├── ScoreSheetScreen.kt
    │   └── GameEndScreen.kt
    ├── component/
    │   ├── PlayerScoreCard.kt
    │   ├── TurnSummaryCard.kt
    │   ├── PresetScoreButtons.kt
    │   └── CustomScoreDialog.kt
    ├── model/
    │   └── GameUiState.kt
    ├── navigation/
    │   └── Navigator.kt
    └── theme/
        ├── DixMilleTheme.kt
        └── Color.kt
```

---

## Future Enhancements (Post-Launch)

- Multiple concurrent games
- Game statistics (average score, win rate)
- Sound effects
- Export game results
- Multi-language support
- Game replay / review mode

---

## Notes

- **Manual scoring**: Players roll physical dice, app does not simulate dice
- **Trust-based**: App assumes players enter correct scores
- **Single game**: MVP supports one active game at a time
- **No accounts**: Local device only, no cloud sync

---

**Version**: 3.0
**Last Updated**: 2026-03-21
**Status**: Phases 1-7 Complete ✅ | Phase 8 In Progress 🚧
