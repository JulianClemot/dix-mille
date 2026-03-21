---
name: commit
description: Format and create git commits using gitmoji convention with explicit, descriptive messages. Use when the user asks to commit, create a commit, or says /commit.
user_invocable: true
effort: low
allowed-tools: Bash, Read
tags: [git, commit, gitmoji, version-control]
---

## When to Use

- User says `/commit` or "commit this", "create a commit", "commit my changes"
- After completing an increment or feature, to persist the work
- Do **not** commit proactively — always wait for an explicit request

## Commit Message Format

Every commit message MUST follow this format:

```
<gitmoji> <type>(<scope>): <subject>

<body>
```

### Rules

1. **Gitmoji**: Use the emoji directly (not the `:shortcode:`), placed at the start of the subject line
2. **Type**: Lowercase keyword matching the gitmoji category
3. **Scope**: The module or area affected (e.g., `domain`, `presentation`, `data`, `navigation`, `di`, `build`, `theme`)
4. **Subject**: Imperative mood, lowercase after the colon, no period at the end, max 72 characters total for the first line
5. **Body**: Explain **what** changed and **why**. Be explicit — list the key changes. Wrap at 72 characters. Separate from subject with a blank line.

### Gitmoji Reference

| Emoji | Code | Type | When to use |
|-------|------|------|-------------|
| ✨ | `:sparkles:` | feat | New feature |
| 🐛 | `:bug:` | fix | Bug fix |
| ♻️ | `:recycle:` | refactor | Code refactoring (no behavior change) |
| 🎨 | `:art:` | style | UI/style improvements |
| ✅ | `:white_check_mark:` | test | Adding or updating tests |
| 📝 | `:memo:` | docs | Documentation changes |
| 🔧 | `:wrench:` | chore | Config files, build scripts, tooling |
| ⬆️ | `:arrow_up:` | deps | Upgrading dependencies |
| ⬇️ | `:arrow_down:` | deps | Downgrading dependencies |
| 🔥 | `:fire:` | remove | Removing code or files |
| 🚚 | `:truck:` | move | Moving or renaming files/resources |
| 💄 | `:lipstick:` | ui | Cosmetic UI changes (spacing, colors) |
| 🏗️ | `:building_construction:` | arch | Architectural changes |
| 🗃️ | `:card_file_box:` | data | Data layer or storage changes |
| 🔒 | `:lock:` | security | Security fixes |
| ⚡ | `:zap:` | perf | Performance improvements |
| 🩹 | `:adhesive_bandage:` | fix | Simple fix for a non-critical issue |
| 🚀 | `:rocket:` | deploy | Deployment related changes |
| 🎉 | `:tada:` | init | Initial commit / project setup |
| 💚 | `:green_heart:` | ci | CI/CD fixes |
| 🔀 | `:twisted_rightwards_arrows:` | merge | Merge branches |

### Examples

```
✨ feat(domain): add three-bust penalty reversion logic

Add score reversion when a player busts three consecutive times.
The player's score reverts to the value it was before the first
of the three busts, using previousScore from TurnRecord history.
```

```
🐛 fix(presentation): fix score history table not scrolling on iOS

The ScoreHistoryTable was missing a verticalScroll modifier on
the inner Column, causing the table to clip on iOS when rounds
exceeded the max height of 200dp.
```

```
♻️ refactor(usecase): extract final round validation into ScoreValidator

Move final round trigger check and game end check from
CommitTurnUseCase into ScoreValidator for consistency with
other validation logic. No behavior change.
```

```
✅ test(domain): add edge case tests for BustTurnUseCase

Cover scenarios: bust when player has not entered game,
bust on final round turn, and three-bust penalty when
player scored between busts.
```

```
🔧 chore(build): update Kotlin to 2.3.1 and Compose to 1.10.2

Bump Kotlin and Compose Multiplatform versions in
libs.versions.toml. No breaking API changes.
```

## Workflow

When the user invokes /commit:

1. Run `git status` and `git diff --staged` to see what's staged, and `git diff` to see unstaged changes
2. If nothing is staged, identify the relevant changed files and stage them (ask user if ambiguous)
3. Analyze ALL staged changes carefully to understand the nature of the change
4. Choose the most appropriate gitmoji based on the primary change
5. Write an explicit commit message following the format above
6. Show the proposed message to the user and ask for confirmation before committing
7. Create the commit using a HEREDOC for proper formatting
8. Run `git status` to verify success
