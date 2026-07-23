# Ketch design guidelines — Material 3 Expressive

Source of truth for the app's visual language, distilled from the *Ketch M3 Redesign* canvas
(claude.ai/design project). Every surface uses a full M3 tonal scheme built on a warm amber
"departure board" seed: dynamic-color roles, tonal surfaces instead of shadows, one rounded
shape family, an 8dp spacing rhythm and emphasized type.

## 1. Color

Fixed brand scheme — **dynamic color is intentionally off** so the amber palette shows on all
devices. Tokens live in `ui/theme/Color.kt` (`LightColors` / `DarkColors`).

| Role | Light | Dark |
|---|---|---|
| primary / onPrimary | `#8A5100` / `#FFFFFF` | `#FFB868` / `#4A2800` |
| primaryContainer / on | `#FFDCBE` / `#2C1600` | `#693C00` / `#FFDCBE` |
| secondary / onSecondary | `#755846` / `#FFFFFF` | `#E4BFA6` / `#432B1B` |
| secondaryContainer / on | `#FFDCBE` / `#2A1707` | `#5B4130` / `#FFDCBE` |
| tertiary / onTertiary | `#59633A` / `#FFFFFF` | `#C1CC9A` / `#2B3410` |
| tertiaryContainer / on | `#DDE8B4` / `#171F02` | `#414B24` / `#DDE8B4` |
| error / onError | `#BA1A1A` / `#FFFFFF` | `#FFB4AB` / `#690005` |
| errorContainer / on | `#FFDAD6` / `#410002` | `#93000A` / `#FFDAD6` |
| surface & background / on | `#FFF8F4` / `#211A14` | `#19120C` / `#EEE0D4` |
| onSurfaceVariant | `#51443B` | `#D6C5B6` |
| surfaceContainerLowest | `#FFFFFF` | `#130D07` |
| surfaceContainerLow | `#FEF1E8` | `#211A13` |
| surfaceContainer | `#FBEBDF` | `#251E17` |
| surfaceContainerHigh | `#F5E5DA` | `#302821` |
| surfaceContainerHighest | `#EFE0D4` | `#3B322B` |
| outline / outlineVariant | `#84766A` / `#D6C5B6` | `#9F8F80` / `#51443B` |
| inverseSurface / inverseOn / inversePrimary | `#372F27` / `#FDEEE2` / `#FFB868` | `#EEE0D4` / `#372F27` / `#8A5100` |

### Semantic accent logic

- **primary** — the loudest voice, used sparingly: the duration pill, active selections
  (selected icon tile, day circles, switches, radio dots), section index headers, the pin and
  radius on the map, links.
- **tertiary** (olive) — "you're there": arrival times, the arrival dot/tick on timelines, the
  healthy `check_circle`, destination affordances. It never competes with primary.
- **secondaryContainer** — contextual tints: the permission banner, multi-select top bar,
  selected chips and segmented options.
- **error** — destructive delete and invalid fields only. Nowhere else.
- Cards sit on **surfaceContainer** tonal fills — **no drop shadows in-app**. The only
  intentional elevation: the FAB, a dragged card in reorder mode, and the home-screen widget
  panel (which must lift off arbitrary wallpaper).
- Resting/paused content drops to **surfaceContainerLow** with a dashed `outlineVariant`
  border; skeletons shimmer between surfaceContainerHigh and surfaceContainerHighest.

## 2. Typography

Roboto (system default; the mock uses Roboto Flex) via the M3 scale in `ui/theme/Type.kt`,
with emphasized weights: display/headlines **Bold** with −0.5sp tracking, titles and labels
**SemiBold**. Key uses:

- Home title "Ketch": 33sp / 700 / −0.5 (headlineLarge), subtitle 13sp onSurfaceVariant.
- Card titles 16sp / 600; card subtitles & captions 12sp onSurfaceVariant.
- Times: 15sp / 700, **tabular numerals** everywhere a time or line code appears.
- Stop names 11sp onSurfaceVariant, max 2 lines with ellipsis.
- Line-code chips 10–11sp / 600; duration pill 13sp / 600.
- Contextual bar titles 20sp / 600; settings screen title 26sp / 700.
- Settings group headers 13sp / 700 in **primary**; overlines in value fields 11sp.
- Buttons 13.5–15sp / 600.

## 3. Shape

One rounded family (`ui/theme/Shape.kt`): *"26px cards, 14px icon tiles, fully-pill chips &
badges."* Chips, badges, the Save button, segmented buttons and day circles are **full pills**.

| Element | Radius |
|---|---|
| Cards (home watcher cards, editor value cards) | 26dp / 16dp |
| Icon tiles 44dp / 40dp / 36dp / 30dp | 14 / 13 / 12 / 10dp (size-proportional) |
| Filled fields, inner info panels | 16dp |
| Grouped-row containers (settings) | 20dp |
| Full-width bottom buttons, map FAB | 18dp |
| FAB 64dp | 20dp |
| Bottom sheet (top corners only) | 28dp |
| Hero/empty-state tile 104dp, dialogs | 32dp |
| Widget panel / widget inner cards | 26dp / 18dp |

## 4. Spacing

8dp rhythm. List gap 12dp; card padding 15dp top / 16dp horizontal / 14dp bottom; card
internal section gap 13dp; list horizontal padding 16dp; header horizontal padding 20dp;
editor/settings content horizontal padding 18dp, section gap 22–24dp. Dividers inside cards
are 1dp `outlineVariant`; timeline rails and ticks are 2dp `outlineVariant`.

## 5. The journey timeline (signature element)

Connections read left-to-right like a departure board:

- **Horizontal timeline** (1–2 legs): grid `52dp | 1fr | 52dp (| 1fr | 52dp)`. Fixed columns
  hold times (15sp/700 tabular; departure left-aligned, transfer centered, **arrival
  right-aligned in tertiary**) with stop names beneath (11sp, 2-line clamp). Each `1fr` cell
  draws a 2dp `outlineVariant` rail with a **line-code chip** riding it: pill, card-colored
  fill (punches out the rail), 1dp `outlineVariant` border, 13dp transport icon in primary +
  code at 10sp/600.
- **Vertical timeline** (3+ legs): stops on the left (time 14sp/700, stop 10sp), 9dp primary
  node dots on a 2dp rail with card-color halos, leg chips to the right of the rail. Final
  time, tick and dot in **tertiary**.
- Card footer after a 1dp divider: `schedule` 16dp + "Arrives HH:MM", then `sync_alt` +
  "n transfer(s)" or `trending_flat` + "Direct", 12.5sp onSurfaceVariant.
- The **duration pill** (primary fill, `schedule` 15dp + "NN min", 5×11dp padding) is the
  loudest element on every card.

## 6. Per-screen rules

### Home
- No top app bar chrome: a 48dp end-aligned action row (`sync`, `settings` icon buttons,
  44dp circular, onSurfaceVariant) above the large "Ketch" title + "N watchers · updated H:MM"
  subtitle.
- Watcher cards per §5. FAB 64dp / 20dp radius, primaryContainer, `add` 28dp.
- **Modes swap the app bar, not the layout**: reorder & multi-select promote a contextual
  top bar (close + title/count + action) so the list stays put. Multi-select bar is
  secondaryContainer edge-to-edge; selected rows invert to primaryContainer with primary
  `check_circle`; delete confirm is a bottom error button stating its count ("Delete 2
  watchers").
- Loading: shimmer skeleton cards (1.3s linear) + spinning `progress_activity` in the
  subtitle slot ("Finding connections…").
- Empty: 104dp `alt_route` primaryContainer tile, "No watchers yet" 22sp/700, body copy,
  bottom full-width primary button "Create your first watcher" (18dp radius).
- No connection: inline surfaceContainerHigh panel (16dp radius) with `event_busy`,
  reassurance copy + "open in Google Maps" link, right-aligned "Try again" text button.
  A dead end never ends the card.
- Resting watcher: surfaceContainerLow card, dashed outlineVariant border, desaturated tile
  (surfaceContainerHighest / onSurfaceVariant), switch off, `bedtime` status strip
  ("Resting — outside 07:00–09:00 window").
- Permissions: dismissible secondaryContainer banner card ("Finish setup") with a primary
  `my_location` 40dp tile, bolded permission names, "Not now" text + "Grant" filled pill.

### Watcher editor
- Pinned top bar: back, title 20sp/600, **Save pill** (primary filled with `check` icon;
  disabled = surfaceContainerHighest/onSurfaceVariant, no icon).
- Sections are **icon-led groups on a single scroll — no nested cards-in-cards**: 20dp
  Material Symbol in primary (destination header in tertiary) + 15sp/600 title.
- Icon picker: 46dp tiles, 14dp radius, selected = primary/onPrimary.
- Filled-style fields: surfaceContainer, 16dp radius, 2dp bottom underline (primary when
  focused, outline at rest), floating 12sp label.
- Day selectors are equal-width **circles** (weight 1 + aspect ratio 1), selected =
  primary/onPrimary; chips are pills; the whole form is one rounded family.
- Time window: two equal 16dp-radius cards, overline From/To 11sp, value 20sp/600 tabular.
- Limits: stepper row (32dp outlined circle − / value / +) and value fields with overline +
  "min" suffix. Preferred connection: M3 filter chips (selected = secondaryContainer with
  check).
- Validation is **inline per-field, not a blocking dialog**: 2dp error border on the field,
  `error` 16dp icon + 12.5sp helper in error color.
- Toggles share one 16dp container with 1dp dividers; icons in primary.

### Settings
- Title 26sp/700. Group headers 13sp/700 **primary**, sitting above one rounded container
  per section (20dp); no loose full-width dividers — whitespace separates groups.
- Theme picker is a true M3 segmented button (pill, secondaryContainer selection).
- Radio groups: rows with `radio_button_checked` in primary / unchecked in outline; a
  selected row with helper text carries a surfaceContainerHigh tint; helper text lives right
  under the option it explains.
- API key: masked monospace value with a `visibility` reveal affordance; helper below the card.
- Defaults: same circular day chips (12sp) + Window / Radius value cards.
- Footer: centered 12sp "Ketch vX.Y · Made by …" with the author link in primary.

### Map picker
- Full-bleed themed map (light paper / dark ink styles). Floating 56dp pill search field
  (back arrow, query, 40dp primaryContainer search circle).
- The pin (`location_on` 52dp), radius ring (2dp primary stroke, primary @ 16% fill), center
  dot and "150 m" `radar` chip all speak **primary** and share one anchor.
- 56dp / 18dp-radius my-location FAB above the sheet.
- Bottom confirm sheet rounds only its top corners (28dp): drag handle, 44dp primaryContainer
  context tile (`trip_origin` / `place`), title + "address · leave radius N m" subtitle,
  full-width primary "Use this location" (18dp radius, `check` icon). Everything reachable in
  the thumb zone.

### Widget (Glance)
- Raised 26dp panel (surfaceContainerLow light / surfaceContainerHigh dark) — the one place
  elevation is intentional. Header: 34dp primary logo tile (`sailing`), "Ketch" 14sp/700,
  32dp refresh circle.
- One watcher per 18dp-radius inner card: 30dp icon tile, name 13sp/600, primary duration
  pill, then the same departure-board timeline (13sp/700 times, 9sp line chips, arrival in
  tertiary with "arrive" label).
- The dark widget uses **fixed white / white-alpha text** so it stays legible on any
  wallpaper.

### Notification
- **Ketch owns the words, not the chrome.** Stock system notification: `setColor` = brand
  primary tints the small icon; title = watcher name; body carries the whole decision —
  departure, stop, line, arrival, transfers — expanded via BigTextStyle. Buttons, grouping,
  sound and heads-up priority come from the channel and OS.

## 7. Motion

Expressive M3: mode changes feel like one surface morphing (swap the app bar, keep the
list). Skeleton shimmer 1.3s linear; loader spin 1s linear; content enters with a short
rise-and-fade. Use spring-based `animate*AsState`/`AnimatedContent` defaults over bespoke
curves.
