# Ketch design guidelines — Material 3 Expressive

Source of truth for the app's visual language, distilled from the *Ketch M3 Redesign* canvas
(claude.ai/design project). Every surface uses a full M3 tonal scheme: dynamic-color roles,
tonal surfaces instead of shadows, one rounded shape family, an 8dp spacing rhythm and
emphasized type.

## 1. Color

**Eight dark seeds, one tonal system.** Ketch is **dark-only**. A seed overrides colour *roles
only*: shape, elevation, type and spacing are identical across all eight, so a new seed ships
without re-checking a single layout. Every screen reads the same roles.

Tokens live in `ui/theme/Color.kt`; the choice is persisted as `ColorPalette` and applied by
`KetchTheme`, which cross-fades roles over 200ms so switching needs no restart.

| Palette | Identity | primary | tertiary | surface | surfaceContainer |
|---|---|---|---|---|---|
| `WALLPAPER` | Material You — reads the wallpaper (API 31+, falls back to Steel) | — | — | — | — |
| `STEEL` **(default)** | Night Platform · cold steel blue | `#8ECDFF` | `#7FD6C4` | `#0E1418` | `#182027` |
| `AURORA` | Indigo periwinkle · teal arrivals | `#BEC5FF` | `#8FD7D2` | `#121318` | `#1E1F25` |
| `PHOSPHOR` | Departure-board green · ice blue | `#7CE0A6` | `#A2CEDD` | `#0D1310` | `#171E19` |
| `ICE_VIOLET` | Cool lilac · mint arrivals | `#D9BDFF` | `#8AD5C6` | `#141218` | `#201E25` |
| `GRAPHITE` | Neutral graphite · electric cyan | `#5FE0E6` | `#C6D0D6` | `#0F1112` | `#1A1D1E` |
| `AMBER` | Departure Board · warm amber, olive arrivals | `#FFB868` | `#C1CC9A` | `#19120C` | `#251E17` |
| `MONO` | True Black · greys only, AMOLED | `#E8E8E8` | `#FFFFFF` | `#000000` | `#121212` |

Every palette shares one error set — destructive actions read the same everywhere:
`error #FFB4AB` / `onError #690005` / `errorContainer #93000A` / `onErrorContainer #FFDAD6`.
`surfaceVariant` follows each seed's `outlineVariant`. See `Color.kt` for the full role set.

**The one light exception.** The home-screen widget may be asked to sit on a bright wallpaper,
so `lightCounterpart()` renders a seed light. Nothing there is invented: it reads the light
roles back out of the dark scheme using M3's tonal symmetry — a dark `inversePrimary` *is* the
light primary (T40), a dark `onPrimaryContainer` (T90) is the light `primaryContainer`, and
`inverseSurface` / `inverseOnSurface` are the light surface and its text. The amber seed's
original light scheme confirms the mapping exactly.

**Why fixed seeds at all.** Material You is the natural default, but a departure board is read
at a glance in bad light: the fixed seeds guarantee a duration pill that survives a pale
wallpaper, and True Black exists for AMOLED lock-screen glances at night.

Map tiles sit outside the M3 role set. Each palette carries `KetchMapColors`
(land / line / road / water); wallpaper palettes derive theirs from the live scheme. The map
picker builds its Google Maps style JSON from these (`ui/theme/MapStyle.kt`) rather than a
fixed raw resource, so the tiles re-tint with the rest of the app.

### Semantic accent logic

- **primary** — the loudest voice, used sparingly: the duration pill, active selections
  (selected icon tile, day circles, switches, radio dots), section index headers, the pin and
  radius on the map, links.
- **tertiary** — "you're there": arrival times, the arrival dot/tick on timelines, the healthy
  `check_circle`, destination affordances. Every seed pitches it away from primary (mint against
  steel, teal against indigo, olive against amber) so the two never compete.
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
- **Vertical timeline** (3+ legs): a `1fr | 22dp | 1fr` grid with the **rail on the card's
  centre line**. Times and stops pin right in the left half (time 15sp/700 tabular, stop
  10.5sp), an 11dp node dot sits centred on the rail with a tick reaching it from the text, and
  leg chips branch off to the right. The rail is inset half a row top and bottom so it starts
  and ends inside the first and last dots. Final time, tick and dot in **tertiary**.
- Card footer after a 1dp divider: `schedule` 16dp + "Arrives HH:MM", then `sync_alt` +
  "n transfer(s)" or `trending_flat` + "Direct", 12.5sp onSurfaceVariant.
- The **duration pill** (primary fill, `schedule` 15dp + "NN min", 5×11dp padding) is the
  loudest element on every card.

## 6. Per-screen rules

### Home
- No top app bar chrome and **no opaque band**: the header floats over the list. On API 31+ it
  draws a blurred copy of the cards passing underneath (the list records itself into a
  `GraphicsLayer` that the header re-draws through a `BlurEffect`), masked by a vertical fade so
  the blur and the tonal wash both die out before the bottom edge — the list stays continuous
  rather than emerging from under a bar. Below API 31 the gradient carries it alone.
- The large "Ketch" title (33sp) shrinks to 19sp once the list has moved. Only "Finding
  connections…" occupies the second line; there is no watcher count.
- Watcher cards per §5. FAB 64dp / 20dp radius, primaryContainer, `add` 28dp.
- Double tapping a card hands the route to Google Maps as public transport directions to the
  watcher destination, when the gesture is enabled in Settings → Gestures.
- **The header menu is overflow, not a settings trip**: the four list-level actions (Refresh
  all, Reorder, Show/Hide resting, Delete) sit above a divider, with Settings and Help below.
  The header `sync` icon and pull-to-refresh follow the refresh-scope setting; **"Refresh all"
  is the only way to poll resting watchers**, so the expensive sweep is one deliberate step
  away. "Show resting" toggles inline with the menu staying open.
- Resting watchers (enabled, outside their window) drop to 70% opacity and **sort below active
  ones**, each group keeping the user's own order.
- **Modes swap the app bar, not the layout**: reorder and multi-select promote a **floating
  pill** on surfaceContainerHigh (40dp surfaceContainer close circle + title/count + action),
  not an edge-to-edge band, so entering a mode reads as one surface morphing.
- **Reorder**: rows collapse to a single 24dp row each — `drag_handle` in outline, 36dp icon
  tile, name over "NN min · HH:MM → HH:MM" — so a five-watcher list stays visible while
  dragging. The lifted card goes to surfaceContainerHighest with a 2dp primary outline, a
  shadow, 1.03 scale, a primary handle and tile, and reads "Dragging · position N of M"; the
  slot it will drop into shows as a dashed outline over a primary @8% wash. The bar's action is
  a filled primary **Done** pill, and a `swipe_vertical` chip at the bottom says the order
  saves on drop. **Long-pressing anywhere on a row lifts it** — the handle is an affordance,
  not the only target.
- Both contextual bars are pinned to one shared height, so switching modes never changes the
  bar's size.
- **Multi-select**: selected rows take a **secondaryContainer** tint with a 2dp primary
  outline, a 22dp square checkbox (7dp radius, filled primary with a check) and an icon tile
  that flips to surfaceContainerLowest with a primary glyph. The bar carries a live count in
  tabular figures, `select_all`, and a filled **error** circle — the only place error appears at
  full strength. Delete confirm is a bottom error pill stating its count ("Delete 2 watchers")
  over "Undo stays available for 5 s", and deleting posts a snackbar held open for exactly that
  long rather than asking first.
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
- Every control applies immediately — there is no Save.
- **Appearance** is a single "Color palette" row: `palette` icon, the active palette's name
  and subtitle, the three tones that carry the UI (primary, tertiary, card surface) as
  overlapping 22dp swatches, and a chevron. Helper beneath: "Ketch is dark-only — the palette
  sets its tones."
- **Palette picker** is a bottom sheet, not a page, so the change is visible behind it:
  "Applies instantly · dark tones only", then one list with **Wallpaper first** (conic sweep of
  the live dynamic tones + `wallpaper` glyph) followed by the seven fixed seeds, each previewed
  by its own three tones. Selected row = surfaceContainerHigh + `radio_button_checked`.
- **Gestures**: "Double-tap opens in Google Maps" switch with explanatory body copy. Turning it
  off disables the home card's double-tap handoff.
- Radio groups (Editing, Refresh): rows with `radio_button_checked` in primary / unchecked in
  outline; a selected row carries a surfaceContainerHigh tint; helper text lives right under
  the option it explains.
- API key: masked monospace value with a `visibility` reveal affordance; helper below the card.
- Defaults: same circular day chips (12sp) + Window / Radius value cards.
- **Support**: one row to Help & feedback ("FAQ, troubleshooting, report an issue").
- Footer: centered 12sp "Ketch vX.Y" linking to the GitHub repo, in primary.

### Help & feedback
- **Answers first, contact second.** Search and the two getting-started cards sit above the
  fold; the outbound rows come after the FAQ, so most questions resolve without writing to a
  one-person dev team.
- Search is a 52dp pill on surfaceContainer and **filters the FAQ as you type**; the
  getting-started cards hide while filtering.
- The two entry cards use **primaryContainer** and **tertiaryContainer** — the same pairing the
  journey cards use for departure vs. arrival, so the palette stays legible across screens.
- FAQ is one 20dp container; **only one answer stays expanded**, on surfaceContainerHigh with
  its chevron in primary.
- Outbound rows are marked `open_in_new` and hand off to GitHub — nothing is collected in-app.

### Map picker
- Full-bleed map styled from the active palette's map tokens — a desaturated ink palette so
  the pin, ring and route read first. Floating 56dp pill search field
  (back arrow, query, 40dp primaryContainer search circle).
- The pin, radius ring (2dp primary stroke, primary @ 20% fill), center dot and "150 m" `radar`
  chip all speak **primary** and share one anchor. The map pin is the stock marker recoloured to
  the palette hue — the SDK's default red is reserved here for destructive actions.
- 56dp / 18dp-radius my-location FAB above the sheet.
- Bottom confirm sheet rounds only its top corners (28dp): drag handle, 44dp primaryContainer
  context tile (`trip_origin` / `place`), title + "address · leave radius N m" subtitle,
  full-width primary "Use this location" (18dp radius, `check` icon). Everything reachable in
  the thumb zone.

### Widget (Glance)
- Raised 26dp panel on surfaceContainerHigh — the one place elevation is intentional, since a
  widget must lift off arbitrary wallpaper. It follows the **app's palette** (Glance renders
  outside the app's composition, so wallpaper palettes fall back to the default seed). Header: 34dp primary logo tile (`sailing`) that opens the widget
  configuration, "Ketch" 14sp/700, 32dp refresh circle.
- **One connection owns the panel.** The watcher fills an 18dp-radius inner card on
  surfaceContainerHighest: an identity row (30dp primaryContainer tile, name 13sp/600, primary
  **duration pill**) over the journey laid out as a departure board — times on the outside,
  line chips riding between them, arrival in **tertiary** under an "arrive" label.
- The panel pages via the indicators alone, as in the spec: the active page is a 16×6 primary
  pill, the rest 6dp dots, each with padding folded into its tap target so it jumps straight to
  its connection. *The spec shows a swipe. RemoteViews has no pager and receives no gesture
  callbacks, so a home-screen widget cannot be swiped — the dots are the navigation.*
- **The widget is sized in tiers**, not drawn once and clipped: full (tile, stop names, page
  dots), medium (no tile, no stop names), and compact for a 2×1 cell (no header either, tighter
  padding and type, just the name, the duration and the two ends).
- Refresh is a plain clickable box rather than `CircleIconButton`, whose hit area did not match
  what it drew, and it swaps in a "Refreshing…" label while the lookup runs, since a widget
  cannot animate a spinner.
- Glance renders in a remote process and can only use drawable resources, so the watcher icons
  and vehicle glyphs have resource twins under `res/drawable/ic_watcher_*` and `ic_vehicle_*`.
  The header logo, notification glyph and adaptive icon are all the one Ketch mark.
- The widget follows its resize (`SizeMode.Exact`): the wordmark drops below 200dp wide and the
  journey line count follows the available height.
- The dark widget uses **fixed white / white-alpha text** so it stays legible on any
  wallpaper.
- Widget configuration (reached by tapping the logo tile) picks which watchers page, plus a
  **"Show only active"** switch: resting watchers are skipped in the pager until their window
  opens, so a two-watcher evening cycles between two pages, not five.
- **Widget theme** is a System / Light / Dark segmented control, independent of the app — the
  one place a palette is rendered light (see §1). System hands both schemes to Glance; Light
  and Dark pin it regardless of the device setting.
- "Nothing picked" and "everything picked is resting" are different empty states and say so:
  the second reads *"Every watcher is resting — the pager resumes when a window opens"* rather
  than telling you to pick watchers you already picked.

### Notification
- **Ketch owns the words, not the chrome.** Stock system notification: `setColor` = the active
  palette's primary tints the small icon; title = watcher name; body carries the whole decision
  — `"16:00 Praha hl.n. (R41) 🚆 → arrives 17:00 · 1 transfer"`, plus a "leave within N min"
  countdown when the departure is within the hour — expanded via BigTextStyle to one boarding
  per line. Buttons, grouping,
  sound and heads-up priority come from the channel and OS.

## 7. Motion

Expressive M3: mode changes feel like one surface morphing (swap the app bar, keep the
list). Skeleton shimmer 1.3s linear; loader spin 1s linear; content enters with a short
rise-and-fade. Use spring-based `animate*AsState`/`AnimatedContent` defaults over bespoke
curves.
