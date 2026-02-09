# Dix Mille (10,000) - Score Sheet App Specification

## Project Overview

**Dix Mille** is a digital score sheet for the traditional French dice game "10,000". Players roll physical dice and use the app to track scores, enforce rules, and determine the winner.

### App Type
- **Score sheet only** - Manual entry, players roll physical dice
- **Strict rule enforcement** - App prevents invalid moves
- **Local persistence** - Games saved across app restarts
- **Fixed turn order** - Players always go in creation order
- **Turn history tracking** - Complete turn-by-turn history for all players
- **Auto-validate UX** - Automatically commits valid turns on score entry

### Platform
- Kotlin Multiplatform Mobile (KMM)
- Android & iOS
- Compose Multiplatform UI
- Material 3 Design

---

## Game Rules (French: "Dix Mille")

### Objective
Be the first player to reach 10,000 points (configurable target).

### Players
2-6 players

### Equipment
- 6 physical dice (rolled by players)
- This app (for score tracking)

---

## Scoring Rules

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

**Special case for 1s:**
- Three 1s = 1,000
- Four 1s = 2,000
- Five 1s = 4,000
- Six 1s = 8,000

---

## Game Flow Rules

### 1. Entry Rule (500 Point Minimum)
- Player must score **at least 500 points in a single turn** to "enter the game"
- Until entered, turn points are lost (treated like a bust)
- Once entered, all future turn points count toward total

### 2. Turn Structure
1. Player rolls all 6 dice (physical dice, not app)
2. Player identifies scoring dice and adds points to turn via app
3. Player chooses:
   - **Add more points**: Roll remaining dice, add more points to turn
   - **End turn**: Commit turn total to player score
   - **Bust**: No scoring dice available, lose all turn points

### 3. Full Hand (Main Pleine)
- If all 6 dice score, player must re-roll all 6 dice
- Points accumulate in the same turn

### 4. Bust Rule
- If no dice score in a roll, player loses ALL turn points
- Turn advances to next player

### 5. Auto-Commit on Score Entry
- When a player adds a score (preset or custom), the turn is immediately committed
- Turn automatically moves to the next player
- No manual "End Turn" action required

### 6. Skip vs Bust
- **Skip**: Player voluntarily skips their turn (0 points, does NOT count as bust)
- **Bust**: No scoring dice rolled (0 points, COUNTS toward 3-bust penalty)

### 7. Three-Bust Penalty Rule
- If a player busts **3 times in a row**, their score reverts to what it was before the first of those 3 busts
- Bust counter resets to 0 after penalty is applied
- Bust counter resets to 0 when player scores points (successful turn)
- Skip does NOT count as a bust and does NOT reset the bust counter

### 8. Cannot Commit Zero
- Player must score at least one point to end their turn

### 9. Winning & Final Round
- When any player reaches the target (10,000), the game enters **Final Round**
- Each other player gets exactly **one more turn**
- Triggering player does NOT get another turn
- Highest score after final round wins

---

## Preset Score Values

To simplify manual entry, the app provides quick-tap preset scores:

| Points | Label | Common Combinations |
|--------|-------|---------------------|
| 50 | "One 5" | 5 |
| 100 | "One 1" | 1 |
| 150 | "1 + 5" | 1, 5 |
| 200 | "Two 1s / Three 2s" | 1, 1 or 2, 2, 2 |
| 250 | "Two 1s + 5" | 1, 1, 5 |
| 300 | "Three 1s / Three 3s" | 1, 1, 1 or 3, 3, 3 |
| 400 | "Four 1s / Three 4s" | 1, 1, 1, 1 or 4, 4, 4 |
| 500 | "Five 1s / Three 5s" | 1×5 or 5, 5, 5 |
| 600 | "Six 1s / Three 6s" | 1×6 or 6, 6, 6 |
| 1000 | "Three 1s (first roll)" | 1, 1, 1 (special) |
| **Custom** | User enters any value | Manual entry dialog |

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
    val turnNumber: Int = 1
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
    val hasPlayedFinalRound: Boolean = false
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

### PresetScore
```kotlin
data class PresetScore(
    val points: Int,
    val label: String
)
```

### TurnRecord
```kotlin
data class TurnRecord(
    val turnNumber: Int,
    val playerId: String,
    val points: Int,
    val wasBust: Boolean
)
```

---

## Validation Rules

### ValidationResult
```kotlin
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val error: ValidationError) : ValidationResult()
}

sealed class ValidationError {
    data object InsufficientPointsToEnter : ValidationError()  // < 500 on first scoring turn
    data object InvalidScoreValue : ValidationError()          // Not a valid score
    data object GameAlreadyEnded : ValidationError()
    data object NotPlayersTurn : ValidationError()
    data object MustScoreToCommit : ValidationError()          // Cannot commit 0 points
    data object TurnAlreadyBusted : ValidationError()
}
```

### Entry Rule Validation
```
IF player.hasEnteredGame == false
  AND turn.turnTotal < 500
  THEN cannot commit turn (points lost, like a bust)
```

### Commit Turn Validation
- Turn total must be > 0
- If player hasn't entered game, turn total must be ≥ 500
- Game must not be ended

### Final Round Validation
- Triggering player does NOT get another turn
- Each other player gets exactly one turn
- After all players have played final round, game ends

---

## Implementation Phases

### Phase 1: Domain Models ✅
- [x] Create `Game` data class
- [x] Create `Player` data class
- [x] Create `Turn` data class
- [x] Create `ScoreEntry` data class
- [x] Create `GamePhase` enum
- [x] Create `PresetScore` data class and constants
- [x] Create `ValidationResult` sealed class

### Phase 2: Validation & Rules ✅
- [x] Implement `ScoreValidator` - validate score entries
- [x] Implement 500-point entry rule validation
- [x] Implement final round trigger logic
- [x] Implement winner determination

### Phase 3: Use Cases ✅
- [x] `CreateGameUseCase` - create game with players
- [x] `AddScoreEntryUseCase` - add score to current turn
- [x] `CommitTurnUseCase` - validate & commit turn points
- [x] `BustTurnUseCase` - handle bust, advance turn
- [x] `UndoLastEntryUseCase` - remove last entry
- [x] `GetCurrentGameUseCase` - get active game

### Phase 4: Data Layer ✅
- [x] Define `GameRepository` interface
- [x] Implement `GameRepositoryImpl` with local storage
- [x] Create expect/actual for `LocalStorage`
- [x] Implement JSON serialization for Game state

### Phase 5: Presentation - ViewModel ✅
- [x] Create `GameUiState` data class
- [x] Create `GameEvent` sealed class (user actions)
- [x] Implement `GameViewModel` with state flow

### Phase 6: Presentation - UI Screens ✅
- [x] Create `GameSetupScreen` - add players, start game
- [x] Create `ScoreSheetScreen` - main gameplay screen
- [x] Implement navigation between screens

### Phase 7: Presentation - Components ✅
- [x] Create `PlayerScoreCard` - player info & status
- [x] Create `TurnSummaryCard` - current turn display
- [x] Create `PresetScoreButtons` - quick score buttons
- [x] Create `CustomScoreDialog` - manual entry
- [x] Create `ConfirmDialog` - bust/end turn confirmation

### Phase 8: UX Improvements - Turn History & Undo 🚧
- [ ] Create `TurnRecord` data class with turnNumber, playerId, points, wasBust
- [ ] Add `turnHistory: List<TurnRecord>` and `turnNumber: Int` to `Game` model
- [ ] Update `CommitTurnUseCase` to record turn history and increment turn number
- [ ] Update `BustTurnUseCase` to record bust turns in history
- [ ] Create `UndoLastTurnUseCase` to revert last committed turn
- [ ] Update `GameEvent` with `UndoLastTurn` event
- [ ] Update `GameUiState` with `canUndoTurn` computed property
- [ ] Update `GameViewModel` with auto-commit logic and undo turn handling
- [ ] Update repository serialization to handle new fields

### Phase 9: UI Redesign - History Table & Streamlined Controls 📋
- [ ] Create `ScoreHistoryTable` component - compact table showing turn totals per player
- [ ] Create `CustomScoreInput` component - inline text field + validate button (not dialog)
- [ ] Redesign `ScoreSheetScreen` with sticky bottom bar layout
- [ ] Remove `EndTurnDialog` from dialogs (auto-commit on score entry)
- [ ] Update `PresetScoreButtons` to remove "Custom" button (now inline)
- [ ] Add "Undo Turn" button to revert last committed turn
- [ ] Simplify UI flow - remove unnecessary confirmation dialogs

### Phase 10: Dark Theme 🎨
- [ ] Create `presentation/theme/DixMilleTheme.kt` with custom color schemes
- [ ] Create `presentation/theme/Color.kt` with pastel dark colors
  - Deep navy background (#1A1A2E)
  - Soft purple primary (#7F5AF0)
  - Mint green secondary (#2CB67D)
  - Warm orange tertiary (#FF8906)
- [ ] Update `App.kt` to use `DixMilleTheme` instead of `MaterialTheme`

### Phase 11: Winner Screen & Animations 🎉
- [ ] Create `GameEndScreen.kt` with full-screen winner display
- [ ] Implement trophy/crown animation with scale effect
- [ ] Implement confetti particles animation
- [ ] Add winner name scale-in animation
- [ ] Display final scores ranking
- [ ] Update `App.kt` navigation to route to `GameEndScreen` when game ends
- [ ] Remove `GameEndDialog` from dialogs (replaced by full screen)

---

## File Structure

```
composeApp/src/commonMain/kotlin/com/julian/dixmille/
├── domain/
│   ├── model/
│   │   ├── Game.kt
│   │   ├── Player.kt
│   │   ├── Turn.kt
│   │   ├── ScoreEntry.kt
│   │   ├── GamePhase.kt
│   │   └── PresetScore.kt
│   ├── repository/
│   │   └── GameRepository.kt
│   ├── usecase/
│   │   ├── CreateGameUseCase.kt
│   │   ├── AddScoreEntryUseCase.kt
│   │   ├── CommitTurnUseCase.kt
│   │   ├── BustTurnUseCase.kt
│   │   ├── UndoLastEntryUseCase.kt
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
    │   ├── GameSetupScreen.kt
    │   ├── ScoreSheetScreen.kt
    │   └── GameEndScreen.kt
    ├── component/
    │   ├── PlayerScoreCard.kt
    │   ├── TurnSummaryCard.kt
    │   ├── PresetScoreButtons.kt
    │   └── CustomScoreDialog.kt
    └── model/
        └── GameUiState.kt
```

---

## UI Flow (Updated - Phase 9)

### Game Setup → Score Sheet → Winner Screen

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
│ 🎲 Dix Mille                          Turn 5         │
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
│ [50][100][150][200][250][300][400][500]...          │
│ [🔄 Undo Turn]                     [💥 Bust]        │
└──────────────────────────────────────────────────────┘
         │
         ▼ (when game ends)
┌──────────────────────────────────────────────────────┐
│                                                       │
│              👑 ALICE WINS! 👑                       │
│            (animated crown + confetti)               │
│                   12,500 points                      │
│                                                       │
│  ─────────────────────────────────────────────       │
│  🥇 Alice ............ 12,500                        │
│  🥈 Bob .............. 10,800                        │
│  🥉 Carol ............ 9,200                         │
│  ─────────────────────────────────────────────       │
│                                                       │
│              [🎮 New Game]                           │
│                                                       │
└──────────────────────────────────────────────────────┘
```

### Key UX Changes (Phases 8-11)

**Auto-Validate Behavior:**
- When user adds ANY valid score, turn is automatically committed
- No "End Turn" button needed
- Streamlined one-tap scoring flow

**Turn History:**
- Compact table showing turn totals for each player
- Shows turn number, points per player per turn, and "BUST" indicators
- Running total row at bottom
- NOT showing individual score entries within turns (simplified view)

**Undo Turn:**
- Can undo the most recent committed turn only (single undo)
- Reverts player to previous state before that turn
- Restores previous player's turn if needed

**Inline Custom Score:**
- Text field integrated into bottom bar (not a dialog)
- Enter value + tap "Add" button
- Immediately validates and commits if valid

**Winner Screen:**
- Full-screen celebration (not a dialog)
- Animated trophy/crown with scale effect
- Confetti particle animation
- Winner name with scale-in animation
- Ranked final scores display
- "New Game" button to restart

---

## Key User Stories

### As a player, I want to...
1. ✅ Start a new game with 2-6 players
2. ✅ Quickly add common scores (50, 100, 200, etc.)
3. ✅ Enter custom scores for unusual combinations
4. ✅ See which players have "entered the game" (≥500)
5. ✅ See whose turn it is
6. ✅ See each player's total score
7. ✅ Undo the last score entry in case of mistake
8. ✅ Mark a turn as "busted"
9. ✅ End my turn and commit points to my total
10. ✅ Be prevented from entering invalid scores
11. ✅ Know when final round has started
12. ✅ See the winner at the end
13. ✅ Resume an interrupted game (persistence)

---

## Technical Decisions

### Architecture
- **Clean Architecture** with Domain, Data, Presentation layers
- **MVVM** pattern for presentation layer
- **Unidirectional data flow** (events up, state down)

### State Management
- `StateFlow` for ViewModel state
- Immutable state with `copy()` updates
- Single source of truth

### Persistence
- Local storage (SharedPreferences/UserDefaults via expect/actual)
- JSON serialization for Game state
- Auto-save on every state change

### Testing Strategy
- Domain layer: 90%+ coverage (business logic)
- Use Cases: 90%+ coverage
- ViewModels: 80%+ coverage
- UI: Critical paths only

---

## Future Enhancements (Post-Launch)

- [ ] Multiple concurrent games
- [ ] Game statistics (average score, win rate)
- [ ] Sound effects
- [ ] Animations for score changes
- [ ] Export game results
- [ ] Configurable rules variants
- [ ] Multi-language support
- [ ] Game replay/review mode

---

## Notes

- **Manual scoring**: Players roll physical dice, app doesn't simulate dice
- **Trust-based**: App assumes players enter correct scores
- **Single game**: MVP supports one active game at a time
- **No accounts**: Local device only, no cloud sync

---

**Version**: 2.0  
**Last Updated**: 2026-02-07  
**Status**: Phases 1-7 Complete ✅ | Phase 8 In Progress 🚧
