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

### Feature: Internationalization (English / French)

```gherkin
Feature: Internationalization (English / French)
  As a player
  I want the app to display in my language (French or English)
  So that I can understand all labels, messages, and game terms without translation effort

  # --- Locale Detection ---

  Scenario: App displays in French when device locale is French
    Given the device locale is "fr"
    When the app launches
    Then all visible strings are displayed in French

  Scenario: App displays in English when device locale is English
    Given the device locale is "en"
    When the app launches
    Then all visible strings are displayed in English

  Scenario: App falls back to English for unsupported locales
    Given the device locale is "de"
    When the app launches
    Then all visible strings are displayed in English

  Scenario: App uses language from locale even with regional variant
    Given the device locale is "fr-CA"
    When the app launches
    Then all visible strings are displayed in French

  Scenario: App falls back to English for English regional variants
    Given the device locale is "en-GB"
    When the app launches
    Then all visible strings are displayed in English

  # --- App Title Translation ---

  Scenario: App title is "Ten Thousand" in English
    Given the device locale is "en"
    When the app displays the title
    Then the title reads "Ten Thousand"

  Scenario: App title is "Dix Mille" in French
    Given the device locale is "fr"
    When the app displays the title
    Then the title reads "Dix Mille"

  # --- Game Term Translation ---

  Scenario: Game terms are translated to French
    Given the device locale is "fr"
    When the app displays game action labels
    Then "Bust" is displayed as "Perdu"
    And "Skip" is displayed as "Passer"
    And "Undo" is displayed as "Annuler"
    And "New Game" is displayed as "Nouvelle partie"

  Scenario: Game terms are displayed in English
    Given the device locale is "en"
    When the app displays game action labels
    Then "Bust" is displayed as "Bust"
    And "Skip" is displayed as "Skip"
    And "Undo" is displayed as "Undo"
    And "New Game" is displayed as "New Game"

  # --- Screen Titles and Labels ---

  Scenario: Home screen labels are translated
    Given the device locale is "fr"
    When the Home screen is displayed
    Then the "Continue Game" button reads "Reprendre la partie"
    And the "New Game" button reads "Nouvelle partie"

  Scenario: Game setup screen labels are translated
    Given the device locale is "fr"
    When the Game Setup screen is displayed
    Then the "Add Player" label reads "Ajouter un joueur"
    And the "Target Score" label reads "Score cible"
    And the "Start Game" button reads "Commencer la partie"

  Scenario: Score sheet screen labels are translated
    Given the device locale is "fr"
    When the Score Sheet screen is displayed
    Then the "Round" label reads "Manche"
    And the "Custom Score" label reads "Score personnalisé"

  Scenario: Game end screen labels are translated
    Given the device locale is "fr"
    When the Game End screen is displayed
    Then the winner announcement uses French phrasing
    And the "New Game" button reads "Nouvelle partie"

  # --- Number Formatting ---

  Scenario: Scores use French number formatting
    Given the device locale is "fr"
    And a player has a total score of 10000
    When the score is displayed
    Then it is formatted as "10 000"

  Scenario: Scores use English number formatting
    Given the device locale is "en"
    And a player has a total score of 10000
    When the score is displayed
    Then it is formatted as "10,000"

  Scenario: Target score uses locale-specific formatting
    Given the device locale is "fr"
    And the target score is 10000
    When the target score is displayed
    Then it is formatted as "10 000"

  Scenario: Small scores below 1000 display without separators
    Given the device locale is "en"
    And a player has a total score of 500
    When the score is displayed
    Then it is formatted as "500"

  # --- Dynamic Strings with Parameters ---

  Scenario: Entry threshold error message includes formatted minimum
    Given the device locale is "fr"
    And the entry minimum is 500
    When a player fails to meet the entry threshold
    Then the error message includes "500" formatted according to French locale

  Scenario: Final round announcement includes triggering player name
    Given the device locale is "fr"
    And Alice triggers the final round
    When the final round announcement is displayed
    Then the message includes "Alice" and is phrased in French

  Scenario: Score collision message names the affected player
    Given the device locale is "en"
    And Bob's score is reverted due to a collision
    When the collision notification is displayed
    Then the message includes "Bob" and is phrased in English

  Scenario: Three-bust penalty message includes reverted score
    Given the device locale is "fr"
    And Alice's score reverts to 500 due to the three-bust penalty
    When the penalty notification is displayed
    Then the message includes "500" formatted as French locale
    And the message is phrased in French

  # --- Preset Score Labels ---

  Scenario: Preset score labels are translated to French
    Given the device locale is "fr"
    When the preset score buttons are displayed
    Then "One 5" is displayed as "Un 5"
    And "One 1" is displayed as "Un 1"
    And "Three 1s" is displayed as "Trois 1"

  Scenario: Preset score labels are displayed in English
    Given the device locale is "en"
    When the preset score buttons are displayed
    Then "One 5" is displayed as "One 5"
    And "One 1" is displayed as "One 1"
    And "Three 1s" is displayed as "Three 1s"

  # --- Error Messages ---

  Scenario: Validation error messages are translated to French
    Given the device locale is "fr"
    When a GameAlreadyEnded error occurs
    Then the error message is displayed in French

  Scenario: Validation error messages are displayed in English
    Given the device locale is "en"
    When a GameAlreadyEnded error occurs
    Then the error message is displayed in English

  Scenario Outline: All validation errors are translated
    Given the device locale is "<locale>"
    When a <error_type> error occurs
    Then the error message is displayed in "<language>"

    Examples:
      | locale | error_type                 | language |
      | en     | InvalidScoreValue          | English  |
      | fr     | InvalidScoreValue          | French   |
      | en     | InsufficientPointsToEnter  | English  |
      | fr     | InsufficientPointsToEnter  | French   |
      | en     | GameAlreadyEnded           | English  |
      | fr     | GameAlreadyEnded           | French   |
      | en     | ScoreExceedsTarget         | English  |
      | fr     | ScoreExceedsTarget         | French   |
      | en     | MustScoreToCommit          | English  |
      | fr     | MustScoreToCommit          | French   |
      | en     | TurnAlreadyBusted          | English  |
      | fr     | TurnAlreadyBusted          | French   |
      | en     | NotPlayersTurn             | English  |
      | fr     | NotPlayersTurn             | French   |

  # --- Game Phase Indicators ---

  Scenario: Game phase labels are translated
    Given the device locale is "fr"
    When the game is in FINAL_ROUND phase
    Then the phase indicator reads "Dernière manche"

  Scenario: Game phase labels are displayed in English
    Given the device locale is "en"
    When the game is in FINAL_ROUND phase
    Then the phase indicator reads "Final Round"

  # --- Turn Outcome Labels in History ---

  Scenario: Turn outcome labels in history are translated to French
    Given the device locale is "fr"
    When the score history table is displayed
    Then BUST outcomes are labeled "Perdu"
    And SKIP outcomes are labeled "Passé"
    And COLLISION outcomes are labeled "Collision"

  Scenario: Turn outcome labels in history are displayed in English
    Given the device locale is "en"
    When the score history table is displayed
    Then BUST outcomes are labeled "Bust"
    And SKIP outcomes are labeled "Skip"
    And COLLISION outcomes are labeled "Collision"
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

### Phase 12: Internationalization (English / French) 🌍
- [ ] Locale detection (expect/actual: device locale on Android/iOS)
- [ ] String resource system with English and French translations
- [ ] Translate all static labels (screen titles, button labels, game terms)
- [ ] Translate all dynamic strings with parameter interpolation
- [ ] Translate preset score labels
- [ ] Translate validation error messages
- [ ] Translate game phase indicators and turn outcome labels
- [ ] Locale-aware number formatting (French: space separator, English: comma separator)
- [ ] Fallback to English for unsupported locales

### Phase 13: Player Library 📋
- [ ] `SavedPlayer` domain model (`id`, `name`, `createdAt`, `lastPlayedAt`)
- [ ] Room 3 database setup (`androidx.room3`) with `SavedPlayerEntity`, `SavedPlayerDao`
- [ ] `SavedPlayerRepository` interface and implementation
- [ ] Use cases: `GetSavedPlayersUseCase`, `AddSavedPlayerUseCase`, `UpdateLastPlayedAtUseCase`
- [ ] Case-insensitive duplicate name validation
- [ ] Player selector bottom sheet UI (checkboxes, search, quick-add)
- [ ] Subtitle logic (ALREADY IN GAME / LAST PLAYED X DAYS AGO / AVAILABLE)
- [ ] Max 6 player cap enforcement (disable checkboxes, hide + ADD PLAYER)
- [ ] FAB enabled only with 2+ checked players
- [ ] Game Setup screen: selected player chips with remove (x), alphabetical order
- [ ] Update `lastPlayedAt` when starting a game
- [ ] Search/filter: case-insensitive "contains" on name

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

## Final Round System Overhaul

This feature overhauls the final round to clarify undo behavior, disable score collision during the final round, and define precise ranking rules for game end.

### Key Rules

- **Undo triggering turn**: If the player who triggered the final round has their turn undone, the phase reverts from `FINAL_ROUND` back to `IN_PROGRESS`.
- **Undo non-triggering turn**: If a non-triggering player's final round turn is undone, the phase stays `FINAL_ROUND`.
- **Score collision disabled in final round**: The "Hit" rule (score collision) does not apply during `FINAL_ROUND`. Two players may share the same score.
- **Exceeding target is a bust**: During the final round, a score entry that would bring a player's total above the target score is treated as a bust. The turn is not committed and the bust counter increments.
- **Ranking**: Players who reached the target score are ranked by who got there first. Players below target are ranked by score descending. Ties below target are acceptable.
- **Entry threshold unchanged**: The 500-point entry threshold still applies during the final round. Un-entered players cannot score unless they meet the threshold.

### BDD Scenarios

```gherkin
Feature: Final Round System Overhaul
  As a player
  I want the final round to handle undo, collision, ranking, and entry correctly
  So that the endgame is fair and predictable

  Background:
    Given a game with players Alice, Bob, Carol
    And the target score is 10000

  Scenario: Undo the triggering player's turn reverts phase to IN_PROGRESS
    Given Alice's total score is 9500
    And Alice commits a turn of 500 points (total becomes 10000)
    And the game phase is FINAL_ROUND
    And Alice is recorded as the triggering player
    When the last turn is undone
    Then the game phase reverts to IN_PROGRESS
    And Alice's total score reverts to 9500
    And Alice is no longer recorded as the triggering player
    And it is Alice's turn again

  Scenario: Undo a non-triggering player's final round turn keeps phase as FINAL_ROUND
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob commits his final round turn of 800 points
    And Bob's hasPlayedFinalRound is true
    When the last turn is undone
    Then the game phase remains FINAL_ROUND
    And Bob's hasPlayedFinalRound becomes false
    And Bob's score reverts to before his final round turn
    And it is Bob's turn again

  Scenario: Score collision is disabled during FINAL_ROUND
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob's total score is 7000
    And Carol's total score is 7000
    And it is Bob's turn
    When Bob commits a turn of 0 points (skip or bust, total stays 7000)
    Then Carol's score remains 7000
    And no collision event is emitted

  Scenario: Score collision is disabled when a player reaches another player's score during FINAL_ROUND
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob's total score is 6500
    And Carol's total score is 7000
    And it is Bob's turn
    When Bob commits a turn of 500 points (total becomes 7000)
    Then Bob's total score is 7000
    And Carol's total score remains 7000
    And no collision event is emitted

  Scenario: Score collision still applies during IN_PROGRESS phase
    Given the game phase is IN_PROGRESS
    And Alice's total score is 2000
    And Bob's total score is 2500
    When Bob commits a turn of score that brings Bob to 2000
    Then Alice's score reverts to her previous score before she reached 2000
    And a collision event is emitted for Alice

  Scenario: Game ends after all non-triggering players have played their final round turn
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob has not played his final round turn
    And Carol has not played her final round turn
    When Bob commits his final round turn
    And Carol commits her final round turn
    Then the game phase becomes ENDED

  Scenario: Ranking when multiple players reach the target score — ordered by who got there first
    Given the game has ended
    And Alice reached 10000 on turn 10 (triggering player)
    And Bob reached 10000 on turn 11 (during final round)
    And Carol's final score is 8500
    Then the ranking is: 1st Alice, 2nd Bob, 3rd Carol

  Scenario: Ranking when players are below target score — ordered by score descending
    Given the game has ended
    And Alice reached 10000 (triggering player)
    And Bob's final score is 9200
    And Carol's final score is 8500
    Then the ranking is: 1st Alice, 2nd Bob, 3rd Carol

  Scenario: Ranking with ties below target score — ties are acceptable
    Given the game has ended
    And Alice reached 10000 (triggering player)
    And Bob's final score is 8500
    And Carol's final score is 8500
    Then the ranking is: 1st Alice, 2nd Bob (tied), 2nd Carol (tied)

  Scenario: Ranking with two players at target score — ordered by turn order (who got there first)
    Given a game with players Alice, Bob, Carol, Dave
    And the game has ended
    And Alice reached 10000 on turn 12 (triggering player)
    And Carol reached 10000 on turn 14 (during final round)
    And Bob's final score is 9500
    And Dave's final score is 7000
    Then the ranking is: 1st Alice, 2nd Carol, 3rd Bob, 4th Dave

  Scenario: Exceeding the target score during the final round counts as a bust
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob's total score is 9800
    And it is Bob's turn
    When Bob tries to commit a turn that would bring his total to 10300
    Then the turn is treated as a bust
    And Bob's total score remains 9800
    And Bob's bust count increments

  Scenario: Mixed ranking — target-reaching players always rank above non-reaching players
    Given a game with players Alice, Bob, Carol
    And the game has ended
    And Alice reached 10000 (triggering player)
    And Bob's final score is 9500 (busted when trying to exceed 10000)
    And Carol's final score is 8500
    Then the ranking is: 1st Alice, 2nd Bob, 3rd Carol

  Scenario: Entry threshold still applies during final round — un-entered player needs 500+
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob has not entered the game (hasEnteredGame = false)
    And Bob's total score is 0
    And it is Bob's turn
    When Bob tries to commit a turn of 400 points
    Then the turn is rejected because it does not meet the entry threshold of 500
    And Bob's total score remains 0

  Scenario: Un-entered player can enter during final round with 500+ points
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob has not entered the game (hasEnteredGame = false)
    And Bob's total score is 0
    And it is Bob's turn
    When Bob commits a turn of 500 points
    Then Bob's total score becomes 500
    And Bob's hasEnteredGame becomes true
    And Bob's hasPlayedFinalRound becomes true

  Scenario: Un-entered player who busts during final round stays at score 0
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob has not entered the game (hasEnteredGame = false)
    And Bob's total score is 0
    And it is Bob's turn
    When Bob busts
    Then Bob's total score remains 0
    And Bob's hasEnteredGame remains false
    And Bob's hasPlayedFinalRound becomes true
    And it advances to the next player

  Scenario: Un-entered player who skips during final round stays at score 0
    Given the game is in FINAL_ROUND with triggering player Alice
    And Bob has not entered the game (hasEnteredGame = false)
    And Bob's total score is 0
    And it is Bob's turn
    When Bob skips
    Then Bob's total score remains 0
    And Bob's hasEnteredGame remains false
    And Bob's hasPlayedFinalRound becomes true
    And it advances to the next player
```

---

## Player Library

This feature introduces persistent player storage so that players are saved across games and can be reused. The Game Setup screen is enhanced with a player selector bottom sheet, search, quick-add, and subtitle grouping.

### Data Model

```kotlin
data class SavedPlayer(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastPlayedAt: Long? = null
)
```

Persisted using Room 3 (`androidx.room3:room3-runtime:3.0.0-alpha03`). All DAO functions are `suspend` or `Flow`. KSP-only compilation.

### Key Rules

- **Unique names**: Player names must be unique, case-insensitive. "alice" and "Alice" are considered duplicates.
- **Max 6 selected**: No more than 6 players can be selected for a game.
- **Min 2 to start**: The "START GAME" button and the bottom sheet FAB are disabled until 2+ players are selected/checked.
- **Quick-add auto-selects**: A newly created player is immediately checked in the bottom sheet.
- **Alphabetical display**: Selected players on the Game Setup screen are displayed in alphabetical order.
- **Subtitles**: Players in the bottom sheet show contextual subtitles:
  - "ALREADY IN GAME" — player is currently selected for this game.
  - "LAST PLAYED X DAYS AGO" — player has a `lastPlayedAt` timestamp.
  - "AVAILABLE" — player has no play history (no `lastPlayedAt`).
- **No deletion**: Players cannot be deleted in this version.
- **Search**: Case-insensitive "contains" matching on name only.
- **"+ ADD PLAYER" hidden at cap**: The button to open the bottom sheet is hidden when 6 players are already selected.

### BDD Scenarios

```gherkin
Feature: Player Library
  As a game organiser
  I want to save players and reuse them across games
  So that I don't have to re-enter player names every time

  # --- Game Setup Screen: Viewing Selected Players ---

  Scenario: Game Setup screen shows no players initially
    Given I navigate to the Game Setup screen
    And no players have been selected yet
    Then the selected players list is empty
    And the "+ ADD PLAYER" button is visible
    And the "START GAME" button is disabled

  Scenario: Selected players are displayed in alphabetical order
    Given I have selected players "Charlie", "Alice", "Bob"
    When I view the Game Setup screen
    Then the players are displayed in order: Alice, Bob, Charlie
    And each player is shown as a chip with a remove (x) button

  Scenario: Remove a player via the x button on Game Setup screen
    Given I have selected players "Alice", "Bob", "Charlie"
    When I tap the remove (x) button on "Bob"
    Then "Bob" is removed from the selected players list
    And the remaining players are: Alice, Charlie
    And the "+ ADD PLAYER" button is visible

  Scenario: Removing a player below the minimum disables START GAME
    Given I have selected exactly 2 players: "Alice" and "Bob"
    When I tap the remove (x) button on "Alice"
    Then only "Bob" remains in the selected players list
    And the "START GAME" button is disabled

  Scenario: START GAME is enabled with 2 or more players selected
    Given I have selected players "Alice" and "Bob"
    Then the "START GAME" button is enabled

  Scenario: START GAME is enabled with 6 players selected
    Given I have selected 6 players
    Then the "START GAME" button is enabled

  # --- "+ ADD PLAYER" Button Visibility ---

  Scenario: ADD PLAYER button is visible when fewer than 6 players are selected
    Given I have selected 5 players
    Then the "+ ADD PLAYER" button is visible

  Scenario: ADD PLAYER button is hidden when 6 players are selected
    Given I have selected 6 players
    Then the "+ ADD PLAYER" button is not visible

  Scenario: ADD PLAYER button reappears after removing a player from a full selection
    Given I have selected 6 players
    And the "+ ADD PLAYER" button is not visible
    When I tap the remove (x) button on one of the selected players
    Then the "+ ADD PLAYER" button becomes visible

  # --- Opening the Player Selector Bottom Sheet ---

  Scenario: Tapping ADD PLAYER opens the player selector bottom sheet
    Given I am on the Game Setup screen
    When I tap the "+ ADD PLAYER" button
    Then the player selector bottom sheet opens
    And the search field is empty
    And all saved players from the database are listed

  Scenario: Bottom sheet shows previously selected players as pre-checked
    Given I have selected players "Alice" and "Bob" on the Game Setup screen
    When I open the player selector bottom sheet
    Then "Alice" has a checked checkbox
    And "Bob" has a checked checkbox
    And all other players have unchecked checkboxes

  # --- Selecting and Deselecting Players ---

  Scenario: Select a player by checking their checkbox
    Given the player selector bottom sheet is open
    And "Carol" has an unchecked checkbox
    When I check the checkbox next to "Carol"
    Then "Carol" is marked as selected

  Scenario: Deselect a player by unchecking their checkbox
    Given the player selector bottom sheet is open
    And "Alice" has a checked checkbox
    When I uncheck the checkbox next to "Alice"
    Then "Alice" is no longer marked as selected

  Scenario: Checkboxes become disabled at max player cap (6)
    Given the player selector bottom sheet is open
    And 6 players are currently checked
    Then all unchecked players' checkboxes are disabled
    And the user cannot select additional players

  Scenario: Unchecking a player re-enables other checkboxes when at cap
    Given the player selector bottom sheet is open
    And 6 players are currently checked
    And all unchecked players' checkboxes are disabled
    When I uncheck one of the selected players
    Then all unchecked players' checkboxes become enabled again

  # --- FAB (Confirm Selection) ---

  Scenario: FAB is disabled with fewer than 2 players checked
    Given the player selector bottom sheet is open
    And only 1 player is checked
    Then the confirm selection FAB is disabled

  Scenario: FAB is disabled with 0 players checked
    Given the player selector bottom sheet is open
    And no players are checked
    Then the confirm selection FAB is disabled

  Scenario: FAB is enabled with 2 or more players checked
    Given the player selector bottom sheet is open
    And 2 players are checked
    Then the confirm selection FAB is enabled

  Scenario: FAB is enabled with 6 players checked
    Given the player selector bottom sheet is open
    And 6 players are checked
    Then the confirm selection FAB is enabled

  Scenario: Confirming selection closes the bottom sheet and updates Game Setup
    Given the player selector bottom sheet is open
    And I have checked "Alice", "Charlie", and "Bob"
    When I tap the confirm selection FAB
    Then the bottom sheet closes
    And the Game Setup screen shows selected players in order: Alice, Bob, Charlie
    And the "START GAME" button is enabled

  # --- Quick-Add Player ---

  Scenario: Quick-add a new player saves to DB and auto-selects
    Given the player selector bottom sheet is open
    And no player named "Diana" exists in the database
    When I type "Diana" in the quick-add field
    And I tap the add button
    Then "Diana" is saved to the database
    And "Diana" appears in the player list with a checked checkbox
    And the quick-add field is cleared

  Scenario: Quick-add trims whitespace from the name
    Given the player selector bottom sheet is open
    When I type "  Diana  " in the quick-add field
    And I tap the add button
    Then a player named "Diana" is saved to the database (trimmed)
    And "Diana" appears in the player list with a checked checkbox

  Scenario: Quick-add is blocked when 6 players are already checked
    Given the player selector bottom sheet is open
    And 6 players are currently checked
    When I type "NewPlayer" in the quick-add field
    Then the add button is disabled
    And no new player can be quick-added until a player is unchecked

  # --- Duplicate Name Prevention ---

  Scenario: Quick-add rejects a duplicate name (exact match)
    Given the player selector bottom sheet is open
    And a player named "Alice" exists in the database
    When I type "Alice" in the quick-add field
    And I tap the add button
    Then an error is shown indicating the name already exists
    And no duplicate player is created

  Scenario: Quick-add rejects a duplicate name (case-insensitive)
    Given the player selector bottom sheet is open
    And a player named "Alice" exists in the database
    When I type "alice" in the quick-add field
    And I tap the add button
    Then an error is shown indicating the name already exists
    And no duplicate player is created

  Scenario: Quick-add rejects a duplicate name (case-insensitive with different casing)
    Given the player selector bottom sheet is open
    And a player named "Bob" exists in the database
    When I type "BOB" in the quick-add field
    And I tap the add button
    Then an error is shown indicating the name already exists
    And no duplicate player is created

  Scenario: Quick-add rejects a blank name
    Given the player selector bottom sheet is open
    When I type "" in the quick-add field
    Then the add button is disabled

  Scenario: Quick-add rejects a whitespace-only name
    Given the player selector bottom sheet is open
    When I type "   " in the quick-add field
    Then the add button is disabled

  # --- Search / Filter ---

  Scenario: Search filters players by name (case-insensitive contains)
    Given the player selector bottom sheet is open
    And the database contains players "Alice", "Alicia", "Bob", "Carol"
    When I type "ali" in the search field
    Then the player list shows only "Alice" and "Alicia"
    And "Bob" and "Carol" are not visible

  Scenario: Search with no results shows an empty list
    Given the player selector bottom sheet is open
    And the database contains players "Alice", "Bob"
    When I type "xyz" in the search field
    Then the player list is empty
    And a "no results" message is displayed

  Scenario: Clearing the search field restores the full player list
    Given the player selector bottom sheet is open
    And I have typed "ali" in the search field showing filtered results
    When I clear the search field
    Then all saved players are displayed again

  Scenario: Search preserves check state of filtered-out players
    Given the player selector bottom sheet is open
    And "Alice" and "Bob" are checked
    When I type "ali" in the search field (only "Alice" is visible)
    And I clear the search field
    Then "Alice" is still checked
    And "Bob" is still checked

  # --- Player Subtitle Logic ---

  Scenario: Subtitle shows "ALREADY IN GAME" for currently selected players
    Given the player selector bottom sheet is open
    And "Alice" is currently checked (selected for this game)
    Then "Alice" displays the subtitle "ALREADY IN GAME"

  Scenario: Subtitle shows "LAST PLAYED X DAYS AGO" for players with play history
    Given the player selector bottom sheet is open
    And "Bob" has a lastPlayedAt timestamp from 3 days ago
    And "Bob" is not currently checked
    Then "Bob" displays the subtitle "LAST PLAYED 3 DAYS AGO"

  Scenario: Subtitle shows "AVAILABLE" for players with no play history
    Given the player selector bottom sheet is open
    And "Carol" has no lastPlayedAt timestamp (null)
    And "Carol" is not currently checked
    Then "Carol" displays the subtitle "AVAILABLE"

  Scenario: Subtitle updates from "AVAILABLE" to "ALREADY IN GAME" when checked
    Given the player selector bottom sheet is open
    And "Carol" has no lastPlayedAt timestamp and displays "AVAILABLE"
    When I check the checkbox next to "Carol"
    Then "Carol" displays the subtitle "ALREADY IN GAME"

  Scenario: Subtitle updates from "ALREADY IN GAME" to prior subtitle when unchecked
    Given the player selector bottom sheet is open
    And "Bob" was checked and displayed "ALREADY IN GAME"
    And "Bob" has a lastPlayedAt timestamp from 5 days ago
    When I uncheck the checkbox next to "Bob"
    Then "Bob" displays the subtitle "LAST PLAYED 5 DAYS AGO"

  Scenario: Subtitle shows "LAST PLAYED TODAY" for a player who played today
    Given the player selector bottom sheet is open
    And "Eve" has a lastPlayedAt timestamp from today
    And "Eve" is not currently checked
    Then "Eve" displays the subtitle "LAST PLAYED TODAY"

  Scenario: Subtitle shows "LAST PLAYED 1 DAY AGO" for a player who played yesterday
    Given the player selector bottom sheet is open
    And "Frank" has a lastPlayedAt timestamp from 1 day ago
    And "Frank" is not currently checked
    Then "Frank" displays the subtitle "LAST PLAYED 1 DAY AGO"

  # --- Interaction with Existing Game Rules ---

  Scenario: Starting a game with selected players creates the game correctly
    Given I have selected players "Alice", "Bob", "Charlie" on the Game Setup screen
    When I tap "START GAME"
    Then a new game is created with players Alice, Bob, Charlie
    And each player's lastPlayedAt is updated to the current timestamp in the database
    And the game follows all existing rules (entry threshold, bust penalty, etc.)

  Scenario: Bottom sheet shows empty list when no players exist in the database
    Given the database contains no saved players
    When I open the player selector bottom sheet
    Then the player list is empty
    And the quick-add field is visible for creating the first player
```

---

## Future Enhancements (Post-Launch)

- Multiple concurrent games
- Game statistics (average score, win rate)
- Sound effects
- Export game results
- ~~Multi-language support~~ (specified in Phase 12)
- Game replay / review mode

---

## Notes

- **Manual scoring**: Players roll physical dice, app does not simulate dice
- **Trust-based**: App assumes players enter correct scores
- **Single game**: MVP supports one active game at a time
- **No accounts**: Local device only, no cloud sync

---

**Version**: 4.0
**Last Updated**: 2026-05-03
**Status**: Phases 1-7 Complete ✅ | Phase 8 In Progress 🚧 | Phase 13 Specified 📋
