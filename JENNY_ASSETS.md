# Jenny Avatar — Asset Guide

This document explains how to replace the placeholder SVG drawables with custom art for the Jenny 2.5D layered avatar.

---

## Layer Overview

The avatar is composed of 7 layers rendered bottom-to-top in a `225dp × 360dp` Box. Each layer is an independent image file that is animated independently (parallax, breathing, blink, hair sway).

| # | File | Role | Parallax |
|---|------|------|----------|
| 1 | `jenny_bg_bokeh` | Soft bokeh / atmospheric background | 25% |
| 2 | `jenny_body_base` | Face, neck, body, eyebrows, nose, gem | Anchored (breathing only) |
| 3 | `jenny_eyes_open` | Eyes — open state | 14% |
| 3b | `jenny_eyes_blink` | Eyes — closed state (shown during blink) | 14% |
| 4 | `jenny_mouth_neutral` | Mouth — resting / smile | 10% |
| 4b | `jenny_mouth_talk` | Mouth — open (looped during TALKING) | 10% |
| 5 | `jenny_hair_layer` | Hair — full flow | 70% + sway rotation |

---

## File Locations

All files are Android Vector Drawables placed in:

```
app/src/main/res/drawable/
```

---

## Viewport / Canvas Size

All SVG/vector files **must share the same viewport**:

```xml
android:width="200dp"
android:height="320dp"
android:viewportWidth="200"
android:viewportHeight="320"
```

Keeping the same viewport across all layers ensures that features align correctly (e.g., the mouth in `jenny_body_base` lines up with the mouth in `jenny_mouth_talk`).

---

## Format Options

### Option A — Android Vector Drawable (XML) ✅ Recommended
- Draw directly in the 200×320 viewport using `<path>`, `<group>`, `<clip-path>` elements.
- Fully scalable, no pixelation.
- Support for `android:fillAlpha`, `android:strokeColor`, `android:strokeAlpha`.
- Convert from SVG via Android Studio: `File → New → Vector Asset → Local SVG file`.

### Option B — PNG/WebP raster
- Place files as `jenny_bg_bokeh.png`, etc.
- Recommended resolution: **600×960 px** (3× of the 200×320 viewport) for `xxhdpi`.
- Provide `drawable-xxhdpi/` variants; add `drawable-xxxhdpi/` (800×1280 px) for high-density displays.

---

## Per-Layer Art Notes

### `jenny_bg_bokeh`
- Blurred / defocused bokeh circles, purple/indigo palette.
- Should fill the full viewport with color so no background bleeds through.
- Drawn scaled to 108% internally (prevents edge reveal during parallax shift).

### `jenny_body_base`
- Everything that isn't hair, eyes, or mouth: face shape, ears, neck, shoulders/body, eyebrows, nose, cheek blush, forehead gem.
- This layer receives the purple **glow/bloom** effect rendered in `jennyGlow()`.
- The face center is approximately at `(100, 100)` in the 200×320 viewport.

### `jenny_eyes_open` / `jenny_eyes_blink`
- Draw **only** the eye region on a transparent background.
- Open: full irises (purple/dark), pupils, catch-lights, upper lash lines.
- Blink: curved closed lash lines with decorative flicks.
- Eye centers are at roughly `(78, 92)` (left) and `(122, 92)` (right).

### `jenny_mouth_neutral` / `jenny_mouth_talk`
- Draw only the mouth region on a transparent background.
- Neutral: subtle closed smile, lip color with lower-lip highlight.
- Talk: open mouth cavity (dark red), visible teeth strip, upper lip, lower lip.
- Mouth center is at approximately `(100, 119)`.

### `jenny_hair_layer`
- Full hair drawn on a transparent background.
- Must cover the top of the head (crown) and flow down both sides of the face.
- The hair is rotated around `transformOrigin = (0.5, 0.0)` (top-center), so anchor the root of the hair there.
- Avoid painting any face features here — they're on lower layers.

---

## Replacing Placeholders

1. Design your art at **200×320 units** (SVG) or **600×960 px** (PNG @3×).
2. Export transparent-background PNG or convert to Android Vector XML.
3. Drop the file into `app/src/main/res/drawable/` with the exact filename listed above.
4. Rebuild the project — no code changes required.

---

## Palette Reference (current placeholder palette)

| Role | Color |
|------|-------|
| Hair dark | `#1E0840` |
| Hair mid | `#2A0A5C` |
| Hair shine | `#7B4CBF` |
| Skin base | `#F5D0C8` |
| Skin shadow | `#E8B4A8` |
| Eye iris | `#6A3AB0` |
| Eye pupil | `#1E0840` |
| Lip upper | `#E8A0A4` |
| Lip lower | `#D4878A` |
| Glow/accent | `#B060FF` |
| Bokeh circles | `#7B4CBF`, `#9B6BDF`, `#5A2A9F` |

---

## Animation States

The composable receives an `AlfredAvatarState` value that drives behavior:

| State | Effect |
|-------|--------|
| `IDLE` | Breathing only, random blinks, hair sway |
| `TALKING` | Mouth alternates between `jenny_mouth_neutral` / `jenny_mouth_talk` at ~70–170 ms |
| `THINKING` | Quick scale-up bounce (1.04×), eyes slightly displaced |
| `LISTENING` | Gentle scale pulse (1.03×) |

The accelerometer parallax, floating particles, and vignette overlay run continuously regardless of state.
