---
name: download-icon
description: Download a Material Design icon from Google's official repository and save it as an Android Vector Drawable XML in the Compose Multiplatform resources folder. Use when the user says /download-icon or asks to add a Material icon to the project.
user_invocable: true
effort: medium
allowed-tools: WebFetch, Bash, Write, Read
tags: [icons, compose, android, material-design, ui]
---

## When to Use

- User says `/download-icon` or "add a Material icon", "download an icon", "I need a Material icon as a drawable"
- User wants a Material Design icon as an Android Vector Drawable XML (not a Compose `ImageVector`) for use via `painterResource(Res.drawable.ic_…)`
- Do **not** invoke for Compose `androidx.compose.material.icons.Icons.*` references — those don't need a download

## Inputs to Collect

Ask the user for the following, in order. Do not proceed until you have all three.

1. **Icon name** — e.g. `alarm`, `home`, `arrow_back`. Tell the user:
   > "If you're unsure of the exact name, browse https://fonts.google.com/icons. Names use underscores, not hyphens (e.g. `arrow_back`, not `arrow-back`)."
2. **Icon style** — present this numbered menu and accept either the number or the name:
   1. Filled (default)
   2. Outlined
   3. Rounded
   4. Sharp
   5. Two-toned
3. **Output filename** — default to `ic_{icon_name}.xml`. Confirm with the user (or accept the default if they say "default" or skip).

## Style → Folder Mapping

The Google repo stores each style in a fixed subfolder. **The "Rounded" folder is named `materialiconsround` — without the trailing `ed`.** Get this wrong and every fetch 404s.

| Style       | Folder name              |
|-------------|--------------------------|
| Filled      | `materialicons`          |
| Outlined    | `materialiconsoutlined`  |
| Rounded     | `materialiconsround`     |
| Sharp       | `materialiconssharp`     |
| Two-toned   | `materialiconstwotone`   |

## Workflow

### Step 1 — Find the icon's category

Icons in the repo live at `src/<category>/<icon_name>/<style_folder>/24px.svg`. The category is not part of the user's input, so we look it up from the official index:

```
https://raw.githubusercontent.com/google/material-design-icons/master/update/current_versions.json
```

This file is a flat JSON object with keys of the form `"<category>::<icon_name>"`. Fetch it once with WebFetch and ask for any keys ending in `::<icon_name>`.

- **Found exactly one match** → use that category.
- **Found multiple matches** (rare — same name across categories) → list them and ask the user to pick.
- **Found none** → tell the user the icon was not found and suggest checking https://fonts.google.com/icons for the exact name. Stop.

### Step 2 — Verify the style variant exists

Hit the GitHub Contents API for the icon folder to confirm which style variants are available:

```
https://api.github.com/repos/google/material-design-icons/contents/src/<category>/<icon_name>
```

This returns a JSON array of folder entries (one per available style). If the user's chosen style folder is not present, list the styles that **are** available and ask them to pick a different one.

### Step 3 — Download the SVG

The current repo (as of the time of writing this skill) ships only `24px.svg` per style — there are **no** pre-built Android XML files under `drawable-anydpi-v21`. Download the SVG directly:

```
https://raw.githubusercontent.com/google/material-design-icons/master/src/<category>/<icon_name>/<style_folder>/24px.svg
```

Use WebFetch with a prompt like "Return the raw SVG contents verbatim, with no commentary or markdown fences." If WebFetch wraps it in a code fence, strip the fence before parsing.

If for any reason a pre-built Android XML is found at a `drawable-anydpi-v21/<prefix>_<icon_name>_24dp.xml` path (prefixes: `baseline`, `outline`, `round`, `sharp`, `twotone`), prefer it over the SVG and skip Step 4. This is unlikely with the current repo layout, but check before falling back.

### Step 4 — Convert SVG to Android Vector Drawable XML

Material `24px.svg` files are simple and follow a predictable shape:

```xml
<svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 0 24 24" width="24">
  <path d="M0 0h24v24H0z" fill="none"/>
  <path d="M12 2..." />
</svg>
```

Convert to:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M12,2..."
        android:fillColor="#FF000000"/>
</vector>
```

#### Conversion rules

1. **viewport** — parse the SVG `viewBox` (4 numbers separated by spaces). Use the 3rd as `android:viewportWidth` and the 4th as `android:viewportHeight`. Always set `android:width="24dp"` and `android:height="24dp"` regardless of the source `width`/`height` (Material icons are always 24dp at baseline).
2. **paths** — emit one `<path>` per SVG `<path>` element, in the same order:
   - `d` → `android:pathData`. **Keep the value verbatim** — don't reformat or strip whitespace.
   - `fill` → `android:fillColor`. Mapping:
     - missing or `currentColor` → `#FF000000`
     - `none` → **skip the path entirely** (these are transparent placeholder rects like `<path d="M0 0h24v24H0z" fill="none"/>`; emitting them adds an invisible bounding box but no value, and AAPT will warn)
     - any other value (`#xxx`, `#xxxxxx`, named colour) → use as-is, expanding 3-digit hex to 6-digit and prefixing `#FF` if no alpha
   - `fill-rule="evenodd"` or `fillRule="evenodd"` → add `android:fillType="evenOdd"`
   - `fill-opacity="X"` (common on two-toned icons for the secondary tone) → fold the opacity into the alpha channel of `android:fillColor`. E.g. `fill="#000" fill-opacity="0.3"` → `android:fillColor="#4D000000"` (0.3 × 255 ≈ 77 → `0x4D`).
3. **groups** — if a `<g fill="…" fill-opacity="…">` wraps paths, apply those attributes to every child path that doesn't override them.
4. **gradients / clip paths** — if the SVG contains `<defs>` with `<linearGradient>`, `<radialGradient>`, `<clipPath>`, `<mask>`, or `<filter>`, prepend a comment to the output:
   ```xml
   <!-- gradient/clip not fully supported — simplified -->
   ```
   and emit the visible paths with their solid fill (or `#FF000000` if none). Don't try to translate gradients — Material 24px icons rarely use them.
5. **other elements** (`<rect>`, `<circle>`, `<polygon>`, `<line>`) — Material 24px icons use only `<path>`. If you encounter another shape primitive, log a warning to the user and skip it; tell them to verify the result.

#### Two-toned specifics

Two-toned icons typically have two paths: one at full opacity (the outline) and one at 30% opacity (the fill tint). Preserve the SVG's exact `fill` and `fill-opacity` per path so both tones survive the conversion. Do not collapse them to a single colour.

### Step 5 — Resolve the save path and write the file

#### 5a — Discover the project structure

Run the following to find all `composeResources/drawable` directories that exist under source sets (ignore `build/` trees):

```bash
find . -type d -name "drawable" -path "*/composeResources/drawable" -not -path "*/build/*"
```

#### 5b — Pick the target directory

**Mono-module** (exactly one result): use that path directly — no need to ask the user.

**Multi-module** (two or more results): infer the best module from context:

1. Look at the icon name and what the user said they need it for (e.g. "statistics section", "home screen", "game setup").
2. Map that to one of the discovered modules by matching the module name or path segment against the use-case (e.g. `feature/score_sheet` for a score-related icon, `feature/game_setup` for a setup icon, `core/presentation` or the shared app module for icons used across multiple features).
3. Propose the chosen path to the user in one line: `"I'll save to <path> — does that look right?"` and wait for confirmation or a correction before writing.

If the target `drawable/` directory does not yet exist in the chosen module, create it with `mkdir -p` before writing.

#### 5c — Write the file

Write `<filename>.xml` into the resolved directory. If a file with that name already exists, ask the user whether to overwrite.

### Step 6 — Report back

Tell the user:

1. The absolute path the file was saved to.
2. How to reference it in Compose:
   ```kotlin
   import dixmille.composeapp.generated.resources.Res
   import dixmille.composeapp.generated.resources.<filename_without_ext>
   import org.jetbrains.compose.resources.painterResource

   Icon(
       painter = painterResource(Res.drawable.<filename_without_ext>),
       contentDescription = null,
   )
   ```
3. If the SVG had gradients or clip paths, remind them to verify the visual result and possibly hand-tweak the XML.

## Error Handling

| Situation | Response |
|-----------|----------|
| `current_versions.json` has no `::<icon_name>` key | "Icon `<name>` was not found in the Material Design Icons repository. Check the exact name at https://fonts.google.com/icons (names use underscores, e.g. `arrow_back`)." Stop. |
| Multiple categories match the name | List them and ask the user to pick. |
| Chosen style folder is missing for that icon | List the styles that are available (from the GitHub Contents API response in Step 2) and ask the user to choose one of those. |
| GitHub API returns 403 (rate limit) | "GitHub's unauthenticated API limit (60 req/h) was hit. Wait an hour or set `GITHUB_TOKEN` in your environment and retry." Stop. |
| WebFetch returns the SVG wrapped in markdown fences | Strip the fences before parsing. |
| SVG contains `<rect>`, `<circle>`, etc. | Warn the user; skip those elements; ask them to verify the result. |
| Output file already exists | Ask the user whether to overwrite. |

## Notes

- Icon names use **underscores**, not hyphens (e.g. `arrow_back`, `keyboard_arrow_down`).
- The repo's `master` branch is the source of truth — pin to `master` in URLs.
- Material 24px icons are always 24×24 viewport — do not infer width/height from the source's `width`/`height` attributes (some are missing or wrong); always emit `24dp`/`24` for the four `android:width|height|viewportWidth|viewportHeight` attributes.
- The `materialiconsround` folder is named without the `ed` suffix. Double-check this when writing the URL.
- Pre-built Android XML drawables (`drawable-anydpi-v21/*.xml`) are no longer present in the current repo layout — expect to convert from SVG every time.
