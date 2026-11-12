# AAPS 4.0 – Phone overview redesign (v2)

Implementation brief for Claude Code. Read this whole file before touching any code.

The visual reference is in `mockup/` next to this file (see "Mockup files" at the end).
The mockup is a design reference only: never copy its HTML/JS. Rebuild everything in Compose.

---

## What changed in v2 (read this first)

Phases 0 to 4b are done and validated on the phone. v2 adds the **new Home layout** designed in the
mockups (board "Version D"), plus three behaviour changes and one bug fix:

1. **Home layout v2** (section 3, phase 5): round slots around the BG circle (alert, bell, Loop, Target),
   consumables bar moved **above** the graph, graph card **full width**, and a **2 × 2 grid of tiles
   at the bottom** (Profil, Glucides, Insuline active, Basale). Loop and Target are no longer tiles.
2. **Insuline active tile** (3.2.2, phase 6): a tap shows the IOB detail on **one line** and, below it,
   opens the **same insulin entry** as Traitements › Insuline.
3. **Basale tile** (3.2.3, phase 6): a tap shows the basal information, opens the **temporary basal
   editor** (the "Basale Temp" dialog) and offers **"Annuler la basale temp."** while a TBR runs.
4. **Carbs entry leaves the Traitements sheet** (3.2.4, phase 6): it stays reachable from the Glucides
   tile. Insulin and Bolus wizard stay.
5. **Consumables panel for a patch pump** (3.5.1, phase 7): the rows must mean one thing each:
   **Pompe Patch = time in use, Insuline = reservoir left, Capteur = time in use.** Today two rows show
   different reservoir counts and times (bug).

Everything that is not listed here keeps its current, validated behaviour. The bottom bar keeps its
current items, **including "Graphiques"** (which the mockup does not draw): see 2.3.

**Which document wins** when the mockup and this file disagree: this file. The mockup is older than
some decisions (separator signs, page title, bottom bar, priorities). Appendix B lists every known
divergence and the decision taken.

---

## 0. Ground rules (read first)

1. **UI only.** Do not change any algorithm, dosing, calculation, database, sync or pump code.
   Every value shown must come from the existing ViewModels / UI state. Every tap must open the
   same dialog or screen it opens today (except the three deliberate changes of v2 above, which
   still reuse the existing dialogs).
2. **Minimal footprint in upstream files.** This fork must keep rebasing cleanly on `master`.
   - Put all new code in a new package: `ui/src/commonMain/kotlin/app/aaps/ui/compose/redesign/`.
   - Touch existing files only at hook points, a few lines each, behind one switch:
     `RedesignFlags.ENABLED` (a `const val` in the new package, `true`).
     With the switch set to `false`, the app must behave exactly like stock 4.0.
   - When an existing composable needs a new behaviour, add a parameter **with a default value
     that keeps today's behaviour**, rather than editing its body.
   - Keep a running list of every upstream file you touched and why, in
     `docs/redesign/UPSTREAM_TOUCHES.md`. Update it at the end of each phase.
3. **Follow the repo's `CLAUDE.md`**, especially the Windows Bash rules (no `cd && ...`,
   use `./gradlew.bat`, etc.).
4. **Phone, portrait only, locked** (`OverviewScreenStacked` path). There is no landscape design.
   Tablet (`OverviewScreenTablet`) and split (`OverviewScreenSplit`) layouts stay untouched.
   **The lock**: with `RedesignFlags.ENABLED`, on a phone, the whole activity is locked to
   `SCREEN_ORIENTATION_USER_PORTRAIT` (every screen, settings included), set in
   `ComposeMainActivity.onCreate` before any content, so nothing is drawn in landscape first
   (`RedesignOrientation.lock`, one upstream line). The manifest is not changed, so with the flag off
   the app is stock. Tablets are not locked. In split screen the lock does not apply, and the graphs
   page fill height follows the window size.
5. **Work phase by phase** (section 9). At the start of each phase, explore the code and
   propose a short plan (files to create, upstream lines to touch). Wait for my approval before
   writing code. At the end of each phase: build, run the existing tests, and stop for me to test
   on the phone.
6. **Strings:** new user-visible strings go in new resource entries (English default + French).
   Do not edit or reorder existing string entries.
7. **Precedence:** this file > mockup. When you find a new divergence, follow this file and add it to
   Appendix B in your phase plan so I can confirm.

## 1. Target device and global layout

- Reference device: Samsung Galaxy S26+, **412 × 891 dp** (1440 × 3120 px, density 3.5).
- **Portrait only, locked** (see 0.4). Design and test for portrait; a phone never shows the
  redesign in landscape.
- **Edge-to-edge**: draw under the status bar and gesture bar, and apply
  `WindowInsets.safeDrawing` / `systemBars` paddings. Never hard-code bar heights.
- Content horizontal padding: **16 dp**. Radii (v2): **22 dp** tiles (48 dp high, nearly a pill),
  **18 dp** square slots, **42 dp** round slots (84 dp), **13 dp** consumables bar. The graph card is
  **full width, radius 0** (v2, 3.6).
- Fonts: system font (Roboto) is fine (the mockup uses Inter: do not add it). Use **tabular figures**
  for all numbers (`fontFeatureSettings = "tnum"`).
- Must fit without vertical scrolling on 412 × 891 dp at default font size (budget in Appendix A).
  With larger system font, content may scroll; nothing may be clipped or overlap.

## 2. Navigation structure

```
┌ Page 1: Home (simplified) ┐   Graphiques item   ┌ Page 2: Graphs (stock 4.0 graphs) ┐
└───────────────────────────┘ ⇄ (bottom bar) ⇄  └────────────────────────────────────┘
Bottom bar: Traitements | [Scènes] | Gestion | Graphiques | Menu
```

- The overview is a `HorizontalPager` with 2 pages. **No visible page indicator, no tabs, no swipe**
  (4b): the "Graphiques" item of the bottom bar switches page, and shows which page is open.
  Page names exist only as accessibility labels ("Accueil, page 1 sur 2", "Graphiques, page 2 sur 2").
- Tapping the BG circle on Home opens page 2 (the Graphiques item is then highlighted).
- Back on page 2 returns to Home.

### 2.1 Top bar (`main/MainTopBar.kt`)

- **Removed** when the redesign is enabled: no hamburger, no search bar, no gear, no version text.
  The hook goes in `main/MainScreen.kt` where `MainTopBar(...)` is rendered.
- Search moves to the Menu screen (section 6). Settings move to the Menu screen.
  The version moves to Menu › "À propos".

### 2.2 QuickLaunch toolbar (`quickLaunch/QuickLaunchToolbar.kt`)

- **Not rendered** on the overview when the redesign is enabled (hook in `main/MainScreen.kt`).
- Do not delete or change QuickLaunch code or its configuration screen.

### 2.3 Bottom bar (`main/MainNavigationBar.kt`)

- **Items (unchanged since 4b, keep exactly):** `Traitements | [Scènes] | Gestion | Graphiques | Menu`,
  at most 5. Keep every visibility rule: Scènes only when scenes or user-action automations exist;
  Gestion; Graphiques always; Menu always. **Objectifs, pump/BG setup, permissions and the open loop
  suggestion are NOT in the bar** (Objectifs, pump setup and permissions are rows of Menu › "À régler";
  the suggestion is the Loop slot).
- **The mockup draws 4 items (Traitements, Gestion, Objectifs, Menu) and no Graphiques. Do not follow
  that: the Graphiques item added in 4b stays, Objectifs stays in the Menu.**
- **"Menu" item** (icon: 3 lines) opens the Menu screen. Parameter `onMenuClick: (() -> Unit)? = null`
  (item hidden when null). It shows the dot only for the pump or permissions to set up (not Objectifs).
- **"Graphiques" item**: icon = trend line; selected while page 2 is shown; a second tap goes back
  to Home.
- **Visual style (v2, from the mockup), only when the redesign is enabled**, through one defaulted
  parameter (e.g. `redesignStyle: Boolean = false`) or a colour override, not by editing the body:
  bar background `#121318` (dark), 1 dp top border `#22252D`; items 60 dp high; icon 22 dp
  (stroke ≈ 1.9); label 12 sp medium, colour `#C9CED8`; **selected item**: label 12 sp semi-bold in
  the in-range accent `#56B4E9` and an indicator pill 56 × 30 dp (radius 15) `#1B2733` behind the
  icon. Light theme: use the equivalent tokens of section 7 (phase 8). Total height ≈ 87 dp including
  the gesture inset. With 5 items (each ≈ 82 dp wide) a label such as "Traitements" must shrink
  (down to 11 sp) rather than ellipsize.
- **BG source warning item** (the BG-source shortcut with the red quality badge): when the redesign
  is enabled, do not show it in the bar; it is shown instead in the left alert slot next to the BG
  circle (section 3.3). Implement via a parameter with a default that keeps today's behaviour.
- Drawer (`main/MainDrawer.kt`): no longer opened from the overview. Its entries move to the
  Menu screen. Keep the drawer code untouched.

## 3. Page 1 – Home (v2 layout)

New file(s) in `redesign/`. Hook: in `overview/OverviewScreen.kt`, on the stacked/phone path,
render the redesign pager instead of `OverviewScreenStacked` when `RedesignFlags.ENABLED`.
Phase 5 **replaces the arrangement** of the blocks built in phases 3 and 4 but **reuses their data
plumbing, state objects, dialogs and composables** wherever possible (tiles' content, BG circle,
consumables, graph canvas). Do not rewrite what works; re-parent and resize it.

### 3.1 Vertical layout

From top to bottom, inside the safe area. The page starts **12 dp below the status bar**
(`WindowInsets.statusBars`, already applied by `MainScreen`, plus 12 dp).

| Block | Content | Height |
|---|---|---|
| Scene | Active scene banner (**only when a scene runs**) | banner |
| A. Circle row | Left slots – BG circle **184 dp** – right slots. Two rows of 100 dp | **200** |
| B. Status | Consumables bar (3.5), width = screen − 12 dp | **26** |
| Client | AAPSClient status card (**AAPSClient build only**) | card |
| C. Graph | Full-width card: range buttons (40) + plot (≤ 290) + hour labels (16) + padding (10) + borders (2) | ≈ 358 |
| D. Tiles | 2 × 2 grid (3.2), tiles 48 dp, gap 16 dp (both directions), grid 316 dp wide, centred | 112 |
| | Sensitivity line (**only when active**, 3.4), 6 dp under the tiles | 24 |
| Bottom bar | existing `MainNavigationBar` (2.3) | ≈ 87 incl. gesture inset |

Gap rules (Appendix A has the arithmetic for the reference phone):

- Circle row → consumables bar: **flexible**, minimum 12 dp.
- Consumables bar → graph: **fixed 6 dp**.
- Graph → tiles: **flexible**, minimum 12 dp.
- Tiles → sensitivity line: fixed 6 dp. Last block → bottom bar: fixed 14 dp.
- The two flexible gaps **share the remaining height equally** (build with two weighted spacers).
- If the minimum gaps cannot be kept, the **plot shrinks first** (down to 160 dp, as in phase 4);
  below that the page scrolls. The plot never grows above 290 dp.
- It is fine that things move slightly when the sensitivity line or the AAPSClient card appears.

### 3.2 Tiles (2 × 2 grid, bottom of the page)

Reuse the data of the existing chips (`overview/chips/*`, `ChipsViewModel`, `ChipsUiState`).
Each tile is a **48 dp high card, radius 22 dp** (about 150 dp wide on the reference phone): an icon of
24 dp on the left, as high as the two text lines; line 1 = label 11 sp; line 2 = value 14 sp semibold.
Horizontal padding 14 dp (Profil, Insuline active) or 10 dp (Glucides, Basale). The time left (profile,
temporary basal) follows the label after a space, in the accent colour and semi-bold; **no separator
sign such as "·" anywhere on the home page**. Tap = same action as the corresponding chip today,
except Insuline active and Basale (3.2.2, 3.2.3).

```
Row 1:  Profil          | Glucides
Row 2:  Insuline active | Basale
```

**Loop and Target are no longer tiles**: they are the round slots of the circle row (3.3).

**Text must never overflow**: single line, and auto-shrink the font (down to ~11 sp) before
ellipsizing. Worst cases to test (tiles are narrower than before): "Normal (90%)" with the label
"Profil 9 min", "0 g 13 requis", "1,02 U/h 120 %" with the label "Temp. 25 min", "Insuline active".

States (dark theme colors in section 7; tile background tint / border / accent):

| Tile | Normal | Highlighted state |
|---|---|---|
| Profil | neutral, "Profil" / profile name | Temporary profile switch → yellow tint, label "Profil 9 min", value "Normal (90%)" (the stock name with its modifiers; the percentage is not repeated in small) |
| Glucides | "0 g" neutral | COB > 0 → light orange tint, value "24 g". Carbs required → stronger orange + border, value "0 g" + small "13 requis" |
| Insuline active | label "Insuline active" (new string), IOB value, neutral | IOB ≥ 1,0 U → insulin tint. Neutral below 1,0 U and when IOB is negative (as coded in phase 3, `REDESIGN_INSULIN_TINT_THRESHOLD_U`) |
| Basale | profile basal → neutral, flat-line icon, "Basale" / "0,85 U/h" | TBR running → teal tint, TBR icon, label "Temp. 25 min", value "0,00 U/h" + small "0 %" |

#### 3.2.1 Running mode (Loop slot, 3.3; same colours drive the graph band, 3.6)

| Mode | Icon | Slot caption | Tint (dark) | Band |
|---|---|---|---|---|
| Closed loop | loop arrow | "Fermée" | green `#132219`, border `#1F3A28`, accent `#6FD98B` | none |
| Open loop | loop arrow | "Ouverte" | blue `#14203A` / `#26406B` / `#8CB8F2` | `#4F8FD6` |
| LGS | loop arrow | "LGS" | purple `#1F1A33` / `#3A2F60` / `#B9A6F2` | `#8E7CE0` |
| Suspended | pause | "Pause" + "25 min" | yellow `#2A2512` / `#524716` / `#F0D86A` | `#F0D86A` |
| Pump disconnected | plug | "Déconnectée" + "30 min" | grey `#1E2026` / `#3A3E48` / `#B4BAC5` | `#8A909C` |
| Disabled | cross | "Désactivée" | red `#2C1616` / `#5A2626` / `#FF8A80` | `#E5534B` |
| Superbolus | (existing) | (existing text) | orange | orange (existing) |

The accessible name always carries the full stock text ("Boucle : Pause 25 min").

#### 3.2.2 Insuline active tile (new behaviour)

A tap opens **one écran** (the stock full-screen insulin screen) with two parts:

1. **IOB line, one line, on top.** The information of the stock IOB dialog (bolus IOB, basal IOB, IOB,
   screenshot "IOB: Bolus IOB 1.50 U / Basal IOB -1.30 U / IOB 0.20 U"), condensed to a single line,
   for example `Bolus 1,50 U + Basale −1,30 U = 0,20 U` (new string with placeholders; no "·"). Same
   numbers and same formatting as the stock dialog; **never recompute IOB**. The two parts are read in
   `RedesignHomeViewModel` (our file) with the same calls and the same rounding as the stock dialog
   (`ChipsViewModel.showIobInfo`); `ChipsViewModel` and `ChipsUiState` are not changed. Single line,
   auto-shrink to 11 sp, then ellipsize.
2. **Below it, the same insulin entry as Traitements › Insuline** (the existing insulin dialog,
   with its BG/IOB/COB strip, "Start Eating soon TT" and "Bolus will be recorded only" switches,
   the stepper, the +0,50 / +1,00 / +2,00 chips and OK). Same confirmations, same pump calls.

Implementation: reuse the existing insulin dialog composable and add **one defaulted parameter**
(`topInfo: (@Composable () -> Unit)? = null`, rendered under its BG/IOB/COB strip). `AppNavGraph`
passes the IOB line as `topInfo` when `RedesignFlags.ENABLED`, so the line is there from every entry
point, Traitements included (on purpose). Do not copy the dialog.

Permission: apply the **same block as the Treatments item on a client that is not paired**
(`masterOrPairedClient` and `role.allows(ElementType.INSULIN.role)`; check the exact names in the
code). When blocked, the tap only shows the stock IOB dialog (today's behaviour).

#### 3.2.3 Basale tile (new behaviour)

A tap opens **one écran** (the stock full-screen temp basal screen) with two parts:

1. **Basal information, one line, on top**: the data of the stock "Temp basal" dialog (rate, start time,
   elapsed / duration), written as in the mockup: **`0,00 U/h @15:58 5/60 min`** (locale decimal, 24 h or
   12 h time per the phone setting, no separator sign; new string with placeholders). When no TBR runs,
   the profile basal: "Basale du profil 0,85 U/h" (same source as the tile).
2. **Below it, the existing temporary basal editor** (the "Basale Temp" dialog: "Basal temp." in U/h
   with − / +, "Durée" in minutes with − / +, OK), unchanged, with the same pump-capability rules
   (absolute / percent, limits, duration steps).

3. **"Annuler la basale temp." button**, shown **only while a TBR is running** (not for the profile
   basal). It sits between the information line and the editor, as a full-width outlined button. It
   runs the **existing cancel-temp-basal action** (the one the stock app already offers, e.g. in
   Traitements / Gestion: find it, reuse it, no new pump logic) with the **same confirmation** as there,
   then closes the écran. Content description: "Annuler la basale temporaire en cours". New string FR + EN.

Implementation: same pattern as 3.2.2: one defaulted `topInfo` parameter on the existing temporary
basal dialog composable (the info line and the cancel button are both passed through it). Same permission
block as 3.2.2 (`ElementType` of the temp basal entry; when blocked, show only the stock information
dialog, no cancel button).

#### 3.2.4 Traitements sheet without the carbs entry

With the redesign enabled, `TreatmentBottomSheet` **no longer lists the carbs entry** (it is reached
from the Glucides tile, 4b). **Insulin and Bolus wizard stay**, and every other entry stays as it is
today. Implementation: one defaulted parameter (e.g. `showCarbs: Boolean = true`); the redesign passes
`false`. Name, icon and visibility rules of the "Traitements" bottom-bar item do not change. Do not
change what the carbs entry does elsewhere (stock behaviour with the flag off).

### 3.3 Circle row (BG circle and four slots)

Three columns: `[side column 1fr] [circle 184 dp] [side column 1fr]`. The row is **200 dp high**
(2 rows of 100 dp in each side column; the circle plus the 7 dp triangle overhang above and below
fits, 184 + 14 = 198). The row keeps the same height whatever the trend. Slots are 84 dp and centred
in their cell.

```
        ┌ left column ┐   ┌ circle ┐   ┌ right column ┐
row 1   │ alert slot  │   │        │   │ bell         │
row 2   │ Loop (round)│   │  133   │   │ Target(round)│
        └─────────────┘   └────────┘   └──────────────┘
```

- **Circle**: reuse the existing BG data (`BgInfoSection` state). Prefer reusing the phase 3 circle
  (`RedesignBgCircle.kt`) and **size it to 184 dp** (it was 200).
  - Ring: diameter 184 dp, stroke ≈ 7.4 dp (8 dp at 200 dp scale), BG-range colour at 22 % alpha.
    Trend arc on the ring: same colour, 100 % alpha, round caps.
  - **Trend triangles**: 1 to 3 triangles pointing outward at the trend arc, as on the stock circle
    (number = `toArcIndicator().triangleCount`: 1 for single/45°/flat, 2 for double, 3 for triple).
    Exact same logic as `BgInfoSection.kt`, block "Triangles pointing outward", already copied into
    `RedesignBgCircle.kt` (scale the sizes with the circle). The mockup draws a single triangle: keep the
    stock count. The triangles stand out of the ring by about 7 dp, so the circle is not clipped
    (only its touch area is round).
  - Inside, centred: delta (18 sp, semibold, trend arrow icon) / BG value (**72 sp**, bold,
    BG-range colour, tabular, letter-spacing about −2 sp) / age "il y a 1 min" (13 sp, secondary).
  - Keep the stock stale-data behaviour.
  - Tap → graphs page. Content description: "Glycémie 133 mg/dL, en baisse de 14, il y a
    1 minute. Ouvrir les graphiques".
- **Left column, row 1 – alert slot** (84 × 84 dp, radius 18 dp), priority order, only one shown:
  1. BG source warning (duplicates / flat data): red tint (`#331617` / `#7A2A2A` / `#FF7A6B`),
     drop icon 22 dp, line 1 = source name ("xDrip+", 11 sp), line 2 = reason ("Doublons", 12 sp bold).
     Tap = what the nav-bar shortcut does today.
  2. Eat reminder (replaces `EatReminderBanner` on Home): orange tint (`#2A2213` / `#5A4318` /
     `#F7B267`), timer icon 22 dp, line 1 "Repas", line 2 countdown "12 min". Tap opens a small sheet
     with the existing actions (Stop, +5 min / Dismiss when time is up). Reuse
     `EatReminderBannerContent` inside the sheet if possible. Content description: "Rappel repas dans
     12 minutes. Arrêter ou ajouter 5 minutes".
  3. Pump activity (replaces the floating `PumpActivityFab` on Home): shown while the pump
     communicates or an SMB is given. Blue tint (`#14203A` / `#26406B` / `#8CB8F2`), icon 24 dp, line 1
     "Pompe", line 2 "Injection" (or the stock wording of the state). Tap opens the existing pump
     activity dialog.

  Rule (**kept from the validated phase 3**; the mockup uses another order, see Appendix B):
  BG source warning > eat reminder > pump activity. When nothing applies the cell stays empty and keeps
  its size. On page 2 there are no floating buttons.
- **Left column, row 2 – Loop slot** (84 dp **round**, radius 42): colours per running mode (3.2.1).
  **Every mode, closed loop included**: icon about 30 dp + the caption(s) of 3.2.1 (12 sp bold,
  second line 11 sp, auto-shrink to 10 sp). Open loop suggestion waiting: blue open-loop colours, caption
  "Ouverte" and a count badge (18 dp) "1" at the top right; tap opens the existing sheet to accept it.
  Otherwise a tap opens the existing running-mode dialog (what the Boucle tile did). Content description:
  "Boucle : Fermée" (stock text of the state).
- **Right column, row 1 – notification button**: bell icon 36 dp, **no background**, 84 × 84 dp touch
  area (replaces the floating `NotificationFab` on Home), icon colour secondary text. Count badge 22 dp
  at the bottom right of the icon (right 20 dp, bottom 20 dp inside the cell) with a 3 dp ring in the
  page colour: **always red `#E5484D` with a white number**, whatever the importance of the
  notifications (no urgent / info distinction). Tap opens the existing `NotificationBottomSheet`.
  Hidden when no notifications (the cell stays empty). Content description: "2 notifications" /
  "1 notification".
- **Right column, row 2 – Target slot** (84 dp **round**): icon 32 dp + value 12 sp bold (tabular),
  e.g. "120–130". Neutral by default. Temporary target → orange tint, preset icon, value "140" and the
  time left "45 min" (11 sp) under it. Dynamic target adjustment → keep the existing colour logic of
  `TempTargetChip`. Tap = same action as the Cible tile had. Content description: "Cible : 120–130"
  (stock text of the state, with the time left).
- Nothing in these slots may overlap other content, and their appearance must not move the
  layout.

### 3.4 Sensitivity line (Autosens / dynamic ISF)

- Shown **only** when the existing `SensitivityChip` / `SensitivityChipBlock` would show
  something. Otherwise it takes no space.
- Placed **under the tile grid** (v2), centred, 24 dp, no container, 12 sp:
  `⇅ Sensibilité 80 %  ISF 36 → 45 mg/dL` (a gap, no separator sign) (labels secondary colour, values semibold).
- Tap = same dialog as the stock sensitivity chip.

### 3.5 Consumables bar

- Reuse `StatusViewModel` / `StatusUiState`, including warning (yellow) / critical (red)
  colours and the expanded panel (with its settings icon and Prime/Fill / Add buttons).
- **Position (v2): between the circle row and the graph.**
- Collapsed: a **32 dp** bar (radius 16, the same radius as the open patch card), **screen width − 12 dp** (6 dp from each screen edge),
  background and border = tile surface/border, items spread evenly: cannula age, reservoir, sensor age
  (+ pump battery when the pump has one), then a 44 × 24 dp chevron button.
- Font 13 sp (tabular), icons 14 dp, warning/critical colours on the value (e.g. insulin age in
  `#FF7A59`, patch age in yellow).
- Expanded: same panel as today, opened where the bar is (it may push the graph and the tiles down; the
  page scrolls). Keep the stock behaviour.

#### 3.5.1 Patch pump: what each row means, and the bug (phase 7)

**Decision (patch pump).** In the open panel and in the collapsed bar, each consumable means one thing:

| Row | Shows | Source |
|---|---|---|
| **Pompe Patch** | time in use (e.g. "2j17h") with its age bar; **no U count** | patch age |
| **Insuline** | **reservoir left** (e.g. "73 U") with its level bar; **no age** | reservoir level |
| **Capteur** | time in use (e.g. "2h"), with its use bar | sensor age |

**Layout of the three rows**: icon and name on the left; value and use bar **on the right, all bars
aligned on the same right edge and the same width**. The sensor row has **no "Ajouter" text button**: the
existing add-sensor action becomes a small round **(+) button (about 28 dp) to the right of the "Capteur"
label** (same action, same confirmation as the stock "Ajouter"; content description "Ajouter un capteur").

The collapsed bar shows the same three values in the same order: patch time, reservoir U, sensor time
(+ the pump battery when the pump has one). One value, one source, shown everywhere it appears.

**Bug seen on the phone** (panel open, rows "Pompe Patch", "Insuline", "Capteur"): the "Pompe Patch"
row showed **2j16h and 96 U**, the "Insuline" row showed **2j16h and 86 U**, and the collapsed bar showed
2j16h / **86 U** / 2h. The same reservoir appeared with two different values and the elapsed time was
shown twice (it can differ by a tick between the rows).

1. **Diagnose first**: reproduce with `RedesignFlags.ENABLED = false` (stock 4.0). Find where each row
   gets its reservoir level and its age (probably the pump driver's reported level vs the level from
   the last insulin change, and two timestamps). Tell me which one is stock and which one is ours, and
   which reservoir value is the right one (the "Insuline" row must use the value the collapsed bar uses,
   unless you find it is the wrong one).
2. **Target behaviour**: the table above. The "Pompe Patch" row loses its U count; the "Insuline" row loses
   its age. No duplicated value anywhere.
3. **Other pumps**: keep the stock rows for pumps that are not patch pumps, but check that they do not show
   the same inconsistency; if they do, tell me before changing them.
4. **Footprint**: if the cause or the row content is stock, change it in the smallest way (a defaulted
   parameter or an adapter on the redesign side of `StatusUiState`); never change pump, insulin or
   database code. Record it in `UPSTREAM_TOUCHES.md`. Add a unit/UI test with a patch-pump fake state
   (patch 2j17h, reservoir 73 U, sensor 2h).
5. **Open the pump menu from the pump line**: a tap on the **"Pompe Patch" row** (open panel; and on the pump
   item of the collapsed bar if it has one) opens the **pump's own menu** (the screen the pump plugin already
   provides for its management actions). Reuse the existing way to open it (the pump plugin's fragment or
   activity, or the action already used elsewhere in AAPS); do not rebuild it. Find it during the diagnosis
   and tell me which entry point you reuse. Keep the row readable: a chevron or similar hint at the right
   of the row, hit area at least 48 dp high, content description "Ouvrir le menu de la pompe". If the active
   pump has no menu of its own, the row stays non-clickable. Footprint: a defaulted `onPumpClick` parameter
   on the redesign side; record it in `UPSTREAM_TOUCHES.md`.

### 3.6 Home BG graph (simplified, full-width card)

Composable `RedesignHomeGraph` (a Compose `Canvas`; `BgGraphCompose.kt` is not changed). Data from the
existing graph data source (`GraphViewModel`: readings, the low and high marks, the running modes, the
target line). The maths is in `RedesignHomeGraphScale.kt`.

- **Card (v2)**: **full width** (edge to edge, radius 0), background `#14161B`, **1 dp border on the top
  and the bottom only** (`#22252D`), padding 4 dp top and 6 dp bottom. The canvas inside is
  (screen width − 34) dp wide, centred (378 dp on the reference phone).
- **Height**: preferred plot **290 dp**; the card takes what the gap rules of 3.1 leave, the plot keeps at
  least 160 dp (card 242 dp), and with less room the page scrolls.
- **Range buttons** at the top of the graph card: `3 h | 6 h | 12 h | 18 h | 24 h`, full width,
  equal widths, row 40 dp (padding 2 dp vertical, 6 dp horizontal), buttons 36 dp high, radius 10,
  13 sp semibold, **no background of their own** (they blend with the card). Selected = subtle fill
  (`#262A33`) + primary text (`#F2F4F7`); unselected text `#8A909C`. **Default 6 h.** The choice is kept
  in `RedesignOverviewState` (over a rotation and while other screens are open, not over a new start
  of the app; no preference). The buttons have a selected state and a spoken name ("6 heures").
- **Time window**: ends at the **next full hour** (e.g. now 00:55 → window ends 01:00). No scroll,
  no zoom, no predictions, no other series.
- **Hour labels** under the plot, including both edges (6 h at 00:55 → 19h … 1h), 11 sp, tertiary
  colour. Step: 1 h for 3 h and 6 h, 2 h for 12 h, 3 h for 18 h, 4 h for 24 h. Format "19h", "0h", "1h",
  from a string resource (`redesign_graph_hour`), the same in French and English.
- **Y axis**: fixed 40–280 mg/dL (adaptive, see below). Labels **on the left only**, 28 dp column,
  plot starting 6 dp after it:
  - Thresholds **56, 70, 170, 250** in their colour, semibold, with a line across the plot:
    250 dashed (very high colour), 170 solid (high colour), 70 solid (low colour), 56 dashed
    (very low colour). 70 and 170 come from the `OverviewLowMark` / `OverviewHighMark` preferences;
    56 / 250 are the fixed TIR limits `Constants.STATS_RANGE_VERY_LOW_MMOL` /
    `..._VERY_HIGH_MMOL` (3.1 / 13.9 mmol/L), the same as `BgRange`. Two coloured labels never
    overlap: they are moved apart when close.
  - **mmol/L users**: the same range in their unit: 2.2–15.5 mmol/L, lines at 3.1 / low mark /
    high mark / 13.9, grey steps every 2 mmol/L, labels with one decimal.
  - Secondary graduations: very faint lines, grey labels, **label hidden if it is closer than 14 dp to a
    threshold label** (in practice 50 is hidden).
  - Target range: faint band between the 70 and 170 lines (7 % of the in-range colour). Target line:
    faint neutral line. Faint vertical lines at each hour label.
- **Points and line**: a smooth line through all the readings, 1.5 dp, coloured by range, broken where
  more than 16 min are missing (no overshoot at peaks and dips). Dots on top: every reading on 3 h and
  6 h, one in 3 on 12 h, one in 6 on 18 h and 24 h, counted back from the newest. Dot radius ≈
  min(2.8 dp, 0.42 × spacing). Last reading: hollow circle (r 4.5 dp, stroke 2 dp, filled with the page
  colour), always drawn.
- **Treatment markers**: as on the stock BG graph, same colours. A manual bolus is a triangle pointing
  down at the reading from above; carbs a triangle pointing up at it from below; an SMB a small triangle
  pointing up, only on 3 h, 6 h and 12 h. A marker sits on the nearest reading (within 15 min), else in
  the middle of the target range.
- **Adaptive axis**: the axis is 40–280 mg/dL while the readings stay inside. A reading above 280 raises
  the top to the next multiple of 50; a reading below 40 lowers the bottom to the multiple of 10 under it
  (never below 0). The grey graduation step adapts: the smallest round step (10, 20, 25, 50, 100, 200,
  500 mg/dL; 1, 2, 5, 10 mmol/L) that leaves at least 32 dp between two graduations.
- **Target label**: the current target value is written on the left axis like the coloured lines (read
  with the real time, so a new temporary target shows at once), in the secondary text colour, and spread
  with them so no two labels overlap. When the target is not the profile target, the label and the step
  of the line in force take the colour of the Target slot (temporary target: orange; adjusted by the
  loop: the "adjusted" colour); past steps stay grey.
- **Tap**: a tap on the graph opens the graphs page (also an action for the screen reader). A long
  press is not a tap.
- **Running-mode band**: a 5 dp rounded bar at the top of the plot over the period where the mode
  was not closed loop, same colours as the stock 4.0 band (3.2.1).
- **Long press** (≈ 450 ms, the system long-press timeout) shows **the stock inspect pill,
  complete**: `GraphInspectPill` with all its lines, from `inspectDataAt`, both reused unchanged from
  `GraphInspect.kt` (no code copied; the mockup's 4-value pill is only a sketch). The pill sits at the top
  of the card, over the range buttons, centred on the reading and kept inside the card. Dashed vertical
  line + ring on the reading; follows the finger and snaps to readings like stock; the same haptics as
  stock; disappears 2 s after release. No predictions are passed. While held, the drag moves the line,
  not the page. A simple tap does nothing more than open the graphs page.
- **Spoken description**: "Glycémie sur les dernières 6 heures, actuellement 133 mg/dL. Appui long pour
  lire une valeur" (or "… Aucune valeur" without readings).

## 4. Page 2 – Graphs (stock 4.0 graphs, moved; edit mode redesigned in phase 8)

Mockups: `graphs-p8-normal.png`, `graphs-p8-edit.png`, `graphs-p8-menu.png`, `graphs-p8-added.png`.
The graphs themselves stay the **existing** `GraphsSection` (BG graph with predictions and markers,
secondary graphs, their axes, scroll, zoom and press-and-hold). Phase 8 changes the page around them,
their heights and the edit mode UI. Keep upstream changes to defaulted parameters of `GraphsSection`
(and `BgGraphCompose` for the scale); everything else lives in `redesign/`. If a point below needs more
than that, stop and tell me instead of doing it.

### 4.1 Page layout (normal mode)
- **Title row** (changed: the page now has a title): "Graphiques", 28 sp bold, 20 dp from the left,
  under the status bar. On the right (16 dp margin) the **edit button**: round, 44 dp (48 dp touch
  target), background `#1E2128`, 1 dp border `#3A4660`, pencil icon **grey** `#9AA0AC` (a rare action,
  it must not draw the eye). Content description "Modifier les graphiques". Hidden in **Simple Mode**.
- Under the title: the graphs, 12 dp between two graphs, 0 dp under the last one. No QuickLaunch space,
  **no floating buttons** (neither pump activity nor notifications). The long-press line stays at the
  same time in every graph.
- **Legends**: every secondary graph has its legend centred at the top, on its own line. **The BG graph
  has no legend** (decided after the phone test: it only added space above the treatment belt).
  Full series names (existing `graph_series_*` strings, in the app language), not the abbreviations.
  Legend line 32 dp for the secondary graphs. If the line is too long, the text shrinks down to
  **10 sp minimum**, then wraps to a second line; never cut.
- **Same BG scale as Home** (fixed 40–280 with the adaptive rule of 3.6, same threshold lines and left
  labels): wanted, only through existing parameters or one defaulted parameter on `BgGraphCompose`
  (phase 8c, optional).

### 4.2 Graph heights: Petit / Moyen / Grand
- Three sizes. **Petit = max(100 dp, (H − 3 × gap) / 4)**, H = height between the title row and the
  bottom bar, gap = the gap between graphs. **Moyen = 1.5 × Petit, Grand = 2 × Petit.** No maximum.
  So the page holds 4 units (Petit 1, Moyen 1.5, Grand 2) on every phone; above that the page scrolls.
  Recomputed on rotation, font size or window change.
- **Default**: BG Grand, IOB/basal Petit, every other graph Petit (G P P fills the page, like Home).
- Layouts that fill one page exactly: G G · G P P, P G P, P P G, M M P, M P M, P M M · P P P P. Smaller
  totals leave empty space at the bottom (**not** filled: the size the user picked is the size shown).
- **Saved value**: keep the stock saved height per graph and use it as a size code: **100 = Petit,
  175 = Moyen, 250 = Grand**; any other saved value is rounded to the nearest of the three. The real
  height is computed on the redesign side, so the saved format and the stock UI are unchanged.
- Replaces the former rule "the first three graphs grow to fill the page, max 250 dp".

### 4.3 Edit mode
- **Enter**: the title edit button (from phase 8a; until 8b it opens the current edit mode). It grows into
  the edit pill (container transform, ≤ 200 ms; a plain cross-fade is fine if the animation is costly).
  **Menu › "Mode édition" is removed** (section 6, phase 8a).
- **Edit pill**: floating, centred over the title row, 52 dp high, radius 26, background `#1E2128`,
  1 dp border `#3A4660`, shadow. Under it the title turns grey (`#3A3E48`). Content, left to right
  (close on the left, confirm on the right, the Android rule):
  `(✕)` · "Mode édition" (14 sp, `#C9CED8`) · `(+)` · `(✓)`. Buttons round 44 dp (48 dp touch);
  ✕ and + on `#2A2E38`, ✓ filled `#56B4E9` with icon `#06121B`. No red (red is for delete).
  Content descriptions: "Annuler", "Ajouter un graphique", "Terminer".
- The **bottom bar stays** as in normal mode (no edit bar).
- **Changes apply live.** `(✓)` keeps them and leaves edit mode. `(✕)` **and the Back gesture** restore
  the graphs as they were when edit mode started (redesign side snapshot of the graph settings) and
  leave edit mode. Tapping another bottom bar item = `(✓)` (changes kept).
- **Edit mode look**: curves and legends at 40 % opacity; touch, zoom, scroll of the time axis and
  press-and-hold on the graphs are **blocked** (the page itself still scrolls).
- **Controls on each graph** (all 44 dp, 48 dp touch target, background `#1E2128`, border `#3A4660`):
  - Right side, under the legend line: size control `− Petit +` (label 13 sp, 52 dp wide). `−` is
    greyed in Petit, `+` greyed in Grand. Content descriptions "Réduire", "Agrandir".
  - Just below it, right-aligned: the **✎ button** (icon `#56B4E9`), content description
    "Options du graphique". It opens the graph menu, same content as the stock menus today:
    - **BG graph**: Prédictions, Activité (check boxes). Text "Ce graphique ne peut pas être supprimé".
    - **IOB / basal graph**: Activité.
    - **Other graphs**: choice of the series (2), the stock selection.
  - **BG and IOB / basal are fixed**: always first and second, cannot be moved or deleted (no buttons).
  - **Other graphs**: bottom-left pill with `↑` `↓` and delete (trash icon `#E0603A`). `↑` greyed on the
    first movable graph (it cannot go above IOB / basal), `↓` greyed on the last. Content descriptions
    "Monter", "Descendre", "Supprimer le graphique". Delete is undone by `(✕)`, so no confirmation.
- **Add `(+)`**: the new graph is added **at the bottom, in Petit**; the other graphs keep their size;
  the page scrolls to it. `(+)` is greyed when the stock maximum number of graphs is reached (if there
  is none, no limit).
- Stock pencils and "Ajouter un graphique" of `GraphsSection` stay hidden (`editMode` parameter): the
  redesign controls above replace them and call the same stock actions.

## 5. States checklist (Home)

Each must be checked on the phone or in a Compose preview (the mockup has a switch for most of them):

- [ ] Each running mode of the table in 3.2.1 (Loop slot colour + caption + graph band).
- [ ] Open loop suggestion waiting (badge, tap opens the accept sheet).
- [ ] TBR running / not running (Basale tile).
- [ ] Temporary profile switch; temporary target (Target slot, with the time left).
- [ ] COB 0 / COB > 0 / carbs required.
- [ ] IOB below 1,0 U / above / negative.
- [ ] Sensitivity line on / off (it sits under the tiles).
- [ ] Notifications: none / one / several (red badge with the count).
- [ ] Eat reminder counting down / time is up.
- [ ] BG source warning (duplicates) at the same time as an eat reminder (warning wins).
- [ ] Pump activity alone; pump activity together with an eat reminder (eat reminder wins).
- [ ] BG in each range (colour of the circle, number and dots).
- [ ] Stale BG (stock behaviour preserved).
- [ ] AAPSClient build: no Objectives item, status card between the bar and the graph, layout otherwise identical.
- [ ] Largest system font: nothing clipped, tiles and slot captions auto-shrink.
- [ ] 412 × 891 dp: no scroll without the sensitivity line; with it, the plot shrinks (≥ 160 dp), no overlap.
- [ ] Tap on Insuline active: IOB line (one line) + insulin entry; tap on Basale: info line + temp basal editor.
- [ ] Traitements sheet: no carbs entry; Insulin and Bolus wizard present; carbs reachable from the Glucides tile.
- [ ] Not-paired AAPSClient: Glucides, Insuline active and Basale taps follow the same block as before.
- [ ] Patch pump, panel open: Pompe Patch = time only, Insuline = reservoir only, Capteur = time; the
      collapsed bar agrees (3.5.1).
- [ ] Basale écran while a TBR runs: "Annuler la basale temp." shown, confirmation, TBR cancelled; hidden
      with no TBR and on a blocked client.

## 6. Menu screen (new)

Opened from the "Menu" bottom-bar item. New file in `redesign/`. While it is open, the Menu item of the
bar is selected (2.3).

1. Title "Menu" (28 sp bold).
2. Search field: reuse the existing search (`ui/search/SearchBar.kt`, `M3SearchBar`,
   `SearchUiState`) with the same behaviour as in the old top bar. Mockup: 48 dp high, radius 14, label
   "Rechercher" (accessible), placeholder: the stock "Rechercher" (decided in 8a: the longer text of the mockup needed an upstream
   change of `M3SearchBar`, not worth it).
3. **"À régler"** (as coded, `redesignAttentionItems`): the items that left the bottom bar so it
   keeps at most 5 items. First in the Menu, under the search field, because they need the user;
   the whole section is hidden when it is empty. Each row keeps the label, icon, badge and action
   the item had in the bar, and shows under the same conditions. Order, most urgent first:
   - **Pump to set up** (the pump plugin's name, badge "!"): opens the pump plugin.
   - **Permissions** (badge "!"): opens the permissions sheet. Shown while one is missing.
   - **Objectifs, first row** (4b), with its progress badge (e.g. "9/10"), only while
     not all objectives are done.
   The open loop suggestion is **not** here: it is the Loop slot (a tap accepts it). Objectifs does not
   light the dot of the Menu item (a progress, not a problem); the pump and the permissions do.
   The BG source warning is **not** here: it is in the alert slot of the circle row (3.3).
   (The mockup has no "À régler" section and puts Objectifs in the bar: ignore that, Appendix B.)
4. One row of 3 large shortcuts (78 dp, radius 16, gap 10): **Paramètres** (existing settings screen),
   **Historique** (history browser), **Statistiques**. **"Mode édition" is removed** (phase 8a): edit mode
   is entered from the pencil button of the Graphiques page (4.3).
5. List "Outils" (rows 52 dp): Historique des traitements, Assistant profil, Maintenance, Assistant de
   configuration, Configuration. Use the **same actions** as the drawer entries
   (`MainDrawer.kt`), and keep any entry the drawer shows that this list misses.
6. Card: "À propos" with the version on the right (`MainViewModel.versionName`), then
   "Quitter AAPS" (same action as the drawer's Quit).

**Settings screen**: **not touched** (decided). Keep the stock 4.0 screen and its code. The mockup
("Paramètres avec recherche": a search field and restyled category cards) is **not implemented**; the
search stays in the Menu only (item 2 above).

## 7. Colours

Glucose palette (colour-blind friendly, Okabe-Ito). **Already in `core/ui/.../GeneralColors.kt`**
(`bgInRange` and friends) since phase 0 (branch `colorblind-colors`), so the stock graph on page 2
already matches. **Do not apply it again**: only check that the values there match this table.

| Range | Light | Dark |
|---|---|---|
| In range | `#0072B2` | `#56B4E9` |
| Target band | in-range at 16 % (redesign home: 7 %) | in-range at 25 % (redesign home: 7 %) |
| High | `#E69F00` | `#F0E442` |
| Very high | `#B8860B` | `#E69F00` |
| Low | `#D55E00` | `#FF6A3D` |
| Very low | `#8B2500` | `#C04000` |

Threshold **label text** must stay readable at 11 sp: on light theme use darker text variants
(250 `#8A6508`, 170 `#9A6A00`, 70 `#B34C00`, 56 `#8B2500`); on dark theme 56 uses `#E2602C`.

Redesign surfaces (put them in a small colour object in `redesign/`, not in the global theme):

| Token | Dark | Light |
|---|---|---|
| Page background | `#0E0F13` (AMOLED option: `#000000`) | `#F4F5F7` |
| Tile / pill surface | `#171920` | `#FFFFFF` |
| Tile / pill border | `#252932` | `#E1E4E9` |
| Graph card | `#14161B`, border `#22252D` | `#FFFFFF`, border `#E1E4E9` |
| Bottom bar (v2) | `#121318`, border `#22252D`, selected pill `#1B2733` | white, border `#E1E4E9`, light blue pill |
| Primary text | `#ECEEF2` | `#14161B` |
| Secondary text | `#9AA0AC` | `#5B616E` |
| Graph labels (secondary) | `#858B98` | `#6B717D` |
| Selected range button | `#262A33` | `#EEF0F3` |
| Insulin accent / tint | `#7FD6E6` / `#0F2530` + `#1C4352` | `#0A6388` / `#E6F1F8` + `#B8D7EA` |
| Carbs accent | `#F7B267` | `#9A5300` |
| Carbs required tint | `#33200E` + `#8A5A1E` | `#FDEFD8` + `#E8B160` |
| Carbs active tint | `#251B10` + `#4A3518` | lighter orange tint |
| TBR tint / accent | `#10292C` + `#1C4247` / `#8FD3DA` | `#E3F4F4` + `#ADDCDD` / `#0B6A70` |
| Profile temp (yellow) | `#2A2512` + `#524716` / `#F0D86A` | light yellow tint, dark yellow text |
| Target temp (orange) | `#2A1E10` + `#553A16` / `#F7A541` | light orange tint, dark orange text |
| BG warning slot (red) | `#331617` + `#7A2A2A` / `#FF7A6B` | light red tint, dark red text |
| Eat reminder slot (orange) | `#2A2213` + `#5A4318` / `#F7B267` | light orange tint, dark orange text |
| Pump activity slot (blue) | `#14203A` + `#26406B` / `#8CB8F2` | light blue tint, dark blue text |
| Notification badge | `#E5484D` (white text), one colour for every notification | same, with a ring in the page colour |

Light-theme tints for the running modes: same hues as the dark table, very light background,
mid border, dark accent (e.g. closed loop `#E9F6EC` / `#BFE3CA` / `#1D7238`).
All text must meet 4.5:1 contrast.

**AMOLED true black**: add a boolean option under Settings › Apparence ("Noir pur en mode
sombre"), default off. It only changes the redesign page background. If adding a preference
touches too many upstream files, skip it and tell me.

## 8. Accessibility

- Every tile, the circle, the slots and the range buttons have content descriptions in French
  (see the `aria-label` attributes in the mockup files for wording; the wording of 3.3 above is
  authoritative).
- The Loop and Target slots expose the full stock text (mode, time left, target, time left) even when
  the slot shows only an icon or a short caption.
- The IOB and Basale sheets read the one-line information first.
- Range buttons expose a selected state; the Graphiques and Menu bar items expose a selected state.
- The existing a11y behaviour of reused components must be preserved.

## 9. Phases

**État : phases 0 à 7 faites et validées sur le téléphone. Prochaine : phase 8 (alignement du menu et échelle de la page 2).**
(Old phases 5 "Themes" and 6 "Animations" are now phases 9 and 10.)

Each phase: plan → my approval → code → `./gradlew.bat :app:assembleFullDebug` (or the variant
I use) → existing `ui` tests (`OverviewScreenVariantsTest`, `OverviewSizeSweepTest`, chips tests…)
must still pass with `RedesignFlags.ENABLED = false`; adapt or add tests for the redesign only in
new test files → update `UPSTREAM_TOUCHES.md` → stop so I can test on the phone → commit.

0. **Palette daltonienne** (branche `colorblind-colors`) – faite.
1. **Chrome**: hide top bar and QuickLaunch, add the Menu item and the Menu screen (section 6) – faite.
2. **Pager**: two pages, page 2 = stock `GraphsSection`, edit mode (section 4) – faite.
3. **Home blocks A and B**: tiles, sensitivity line, consumables pill, BG circle and its side
   slots. Move notifications, eat reminder and pump activity – faite, with its corrections (triangles,
   12 dp under the status bar, page 2 fills its height, no floating buttons on page 2, portrait lock on
   the activity, "Mode édition").
4. **Home graph** (section 3.6) – faite.
4b. **Graphs button and carbs tile** – faite: "Graphiques" bar item, no swipe, Glucides tile opens carbs
   entry, bar of at most 5 items, Objectifs in Menu › "À régler".

5. **Home layout v2** (sections 3.1 to 3.6, appendix A) – faite. Re-arrange the existing blocks, do not rebuild them:
   - Circle row: circle 184 dp, BG 72 sp, two side columns of two 100 dp rows (alert slot / Loop slot on
     the left, bell / Target slot on the right). New `RedesignRoundSlot` for Loop and Target (state
     colours from the existing tile state, 3.2.1), keep the alert slot priority of 3.3. Move the bell
     and the alert logic into their new cells; the Loop and Target actions are the ones of the old tiles.
   - Consumables bar: 32 dp, 13 sp, placed under the circle row, width screen − 12 dp.
   - Graph card: full width, radius 0, top/bottom border only, canvas = screen − 34 dp, plot ≤ 290 dp.
   - Tiles: 2 × 2 grid at the bottom, radius 22, 150 dp wide, labels "Insuline active" and the others of
     3.2; sensitivity line under the grid.
   - Layout engine: a `Column` with two weighted spacers (flexible gaps) and the gap rules of 3.1; the
     plot height is derived from the remaining space (min 160, max 290).
   - Bottom bar style of 2.3 (selected pill, colours) through the defaulted parameter.
   - No change of tap actions in this phase: Insuline active and Basale keep their stock chip actions
     until phase 6. Loop and Target keep the actions of the old tiles.
   - Add Compose previews (or screenshot tests in new files) for: closed loop / suspended / disconnected /
     open loop with a suggestion, temp target, TBR, carbs required, alert slot ×3, notification ×3,
     sensitivity line, largest font.
6. **Tile actions and Traitements** (3.2.2, 3.2.3, 3.2.4) – faite: IOB + insulin sheet, Basale info + temp basal
   editor sheet with the "Annuler la basale temp." button, carbs entry removed from the Traitements sheet. Upstream touches expected (3 defaulted
   parameters): the insulin dialog (`topInfo`), the temporary basal dialog (`topInfo`),
   `TreatmentBottomSheet` (`showCarbs`). Add the string resources (FR + EN). Test the not-paired
   client blocks.
7. **Consumables panel for a patch pump** (3.5.1) – faite, with its corrections (own card radius 16, closed bar 32 dp / 13 sp / radius 16, "time / maximum" in the open panel, reservoir bar, pump menu from the patch row): row meanings (patch = time, insulin = reservoir,
   sensor = time), the inconsistency bug, and the tap on the "Pompe Patch" row that opens the pump menu.
   Can be done before phase 5 if you prefer (it is independent).
   Start with the diagnosis and tell me the cause before coding.
8a. **Graphiques page and heights** (4.1, 4.2, section 6): title row with the edit button, legends (BG
   included), Petit / Moyen / Grand heights with the 100 / 175 / 250 size code (height logic in
   `redesign/` with unit tests). The title button opens the **current** edit mode for now, and
   Menu › "Mode édition" is removed (the entry point moves in one go). Also the Menu finishing touches:
   placeholder text, sizes, selected Menu item of the bar. Low risk, no new upstream touch expected
   beyond the existing `redesignLayout` parameter.
8b. **New edit mode** (4.3). **Start with a diagnosis, before coding**: for each action (series menu of
   each graph, add, delete, **move up / down**, set the size, restore a snapshot for `(✕)`), say whether
   the stock code already offers it and through which call, and list the upstream touches it would need.
   Moving a graph may not exist in 4.0: if it needs more than a defaulted parameter or a small public
   function, stop and tell me. Then: the pill (✕) Mode édition (+) (✓), dimmed and locked graphs,
   controls on each graph, Back = cancel, new graph at the bottom in Petit.
8c. **Same BG scale on page 2** (4.1): optional. Only through existing parameters or one defaulted
   parameter on `BgGraphCompose`; otherwise drop it and tell me. Independent of 8a / 8b.
9. **Themes** (section 7): light theme, the redesign surfaces (including the v2 tokens), the true black
   option. The glucose palette is done (phase 0): only check its values. Also fix the stock temp basal
   colour of the light theme (`LightElementColors.tempBasal` = `#00FFFF`, light cyan, almost unreadable in
   the "Annuler : Basal temp." dialog; seen in the phase 6 test). Proposed: `#0B6A70`, the teal of the Basale tile. Check the light mockups
   (`Accueil, thème clair`, `Menu, thème clair`, `Graphiques, thème clair`, `Paramètres, thème clair`).
10. **Nice to have, last**: short animations (≤ 200 ms): BG number cross-fade on a new reading,
    trend arc sliding, new dot appearing. They run only when a real new reading arrives from the
    BG source (about once a minute or every 5 min), never on a timer: the mockup makes a new
    reading every 4 s only to show the animation.

## 10. Open questions

None at the moment. Ask me if you find one while exploring a phase.

**Decided (v2, by me):**
- Alert slot priority stays BG warning > eat reminder > pump activity.
- The notification badge is always red, no urgent / info distinction.
- The Basale sheet gets the "Annuler la basale temp." button.
- Patch pump rows: Pompe Patch = time in use, Insuline = reservoir left, Capteur = time in use; bars
  aligned on the right; "Ajouter" becomes a (+) to the right of the "Capteur" label.
- Loop and Target slots carry captions in every state, closed loop included ("Fermée").
- The Settings screen is not touched (no search field added): keep as much of the GitHub 4.0 code as possible.
- Page 2 uses the same BG scale as Home, only if that stays a minimal change.
- Graphiques page: title + grey edit button; edit pill (✕) Mode édition (+) (✓); Back = cancel; sizes
  Petit / Moyen / Grand proportional to the page (min 100 dp, no max), default G P P; BG and IOB / basal
  fixed; new graphs at the bottom in Petit; Menu › "Mode édition" removed.

## Mockup files

**Use the PNGs in `mockup/png/`** (412 × 891 dp drawn at 2×). They are the reference for v2. Open only the
ones the current phase needs. The `mockup/*.dc.html` files are older design sources: do not read them.
The notes under each image below are commentary, not UI text.

| File | Shows | Phase |
|---|---|---|
| `home.png` | Home v2, closed loop ("Fermée" caption), eat reminder, 1 notification, carbs required, TBR | 5 |
| `home-suspended-temptarget.png` | Loop "Pause 25 min", Target "140 / 45 min", yellow mode band | 5 |
| `home-disconnected.png` | Loop "Déconnectée 30 min", eat reminder slot | 5 |
| `home-openloop-bgwarn.png` | Loop "Ouverte" + suggestion badge "1"; BG source warning wins over the eat reminder | 5 |
| `home-pump.png` | Pump activity in the alert slot (mockup priority; the spec priority 3.3 wins) | 5 |
| `sheet-insulin.png` | Insuline active tap: IOB line (one line, new) above the stock insulin entry | 6 |
| `sheet-basal.png` | Basale tap: info line, "Annuler la basale temp." (only while a TBR runs), stock temp basal editor. Without a TBR the line reads "Basale du profil 0,85 U/h" | 6 |
| `sheet-treatments.png` | Traitements sheet without the carbs entry (other stock entries unchanged, not drawn) | 6 |
| `panel-patch.png` | Consumables panel, patch pump: Patch = time, Insuline = reservoir, Capteur = time + (+) | 7 |
| `menu.png` | Menu with "À régler" (Objectifs 9/10), shortcuts Paramètres / Historique / Statistiques in one row (no "Mode édition"), 4b bottom bar, Menu selected | 8a |
| `graphs-p8-normal.png` | Graphiques page: title + grey edit button, BG Grand, IOB Petit, COB Petit, legends on the secondary graphs | 8a |
| `graphs-p8-edit.png` | Edit mode: pill (✕) Mode édition (+) (✓), dimmed graphs, size control, ✎, ↑ ↓ delete | 8b |
| `graphs-p8-menu.png` | Edit mode, ✎ menu of the BG graph (Prédictions, Activité, cannot be deleted) | 8b |
| `graphs-p8-added.png` | A graph added: at the bottom in Petit, page scrolled, others unchanged | 8b |
| `menu-light.png`, `graphs-light.png`, `graphs-p8-edit-light.png` | Light theme of the Menu, the Graphiques page and its edit mode | 9 |

The gallery of every screen (old iterations included) is the artifact "AAPS redesign – mockups".

When the PNGs and this file disagree, **this file wins** (Appendix B).

---

## Appendix A – Home v2 geometry (reference phone 412 × 891 dp, default font)

Values read from the mockup source (board "Version D").

| Element | Value |
|---|---|
| Top padding | 46 dp = status bar 34 + 12 |
| Side padding of the page | 16 dp |
| Circle row | 200 dp (two rows of 100) |
| Circle | 184 dp; ring stroke 7.4 dp (8 at 200 scale); BG 72 sp bold, letter-spacing −2; delta 18 sp semibold; age 13 sp |
| Side slots | 84 × 84 dp; square radius 18, round radius 42; bell touch area 84 with a 36 dp icon |
| Alert slot content | icon 22, line 1 11 sp, line 2 12 sp bold, gap 2 |
| Round slot content | Loop icon 30 + caption 12 sp bold (+ 11 sp second line) / Target icon 32 + value 12 sp bold (+ 11 sp time left), gap 3 to 4 |
| Consumables bar | 32 dp, radius 16, 13 sp, icons 14, chevron 44 × 24, width 400 (screen − 12) |
| Graph card | full width, radius 0, borders 1 dp top and bottom, padding 4 / 6 |
| Range row | 40 dp; buttons 36 dp, radius 10, 13 sp semibold |
| Plot canvas | 378 × 290 dp, left label column 28, plot x from 34 to 366, y from 8 to 282 for 40–280 mg/dL |
| Hour labels | 16 dp row, 11 sp |
| Tile grid | 316 dp wide, 2 columns, gap 16 dp, rows 48 dp, radius 22 |
| Tile content | icon 24, gap 9, label 11 sp (line height 13), value 14 sp semibold, gap 2 |
| Sensitivity line | 24 dp, 12 sp, icon 13 dp |
| Bottom bar | 6 dp top padding + 60 dp items + 20 dp bottom (gesture) + 1 dp border ≈ 87 |

Vertical budget without the sensitivity line:
46 + 200 + 26 + 6 + 358 + 112 + 14 + 87 = **849 dp**; 891 − 849 = **42 dp** for the two flexible gaps
(21 dp each, minimum 12). With the sensitivity line (+ 30 dp): 12 dp left for the flexible gaps, which
is 12 dp short of the minimum, so the plot gives up about 12 dp (≈ 278 dp). With the AAPSClient card or a
larger font the plot shrinks further (min 160 dp), then the page scrolls.

## Appendix B – Divergences between the mockup and this file (decisions)

| Topic | Mockup | Decision (this file) |
|---|---|---|
| Bottom bar | Traitements, Gestion, **Objectifs**, Menu | Traitements, [Scènes], Gestion, **Graphiques**, Menu; Objectifs in Menu › "À régler" |
| Page 2 title | "Graphiques" 28 sp title | "Graphiques" title + grey round edit button (4.1) |
| Menu shortcut | "Modifier l'écran" | removed; edit mode from the Graphiques page (4.3) |
| Menu "À régler" | absent | kept (pump, permissions, Objectifs) |
| Separator "·" | "Profil · 9 min", "Cible · 45 min", "Temp. · 25 min", "Pompe · 30 min", "· 13 requis", sensitivity line | none: a space and the accent colour |
| Profile value | "Normal" + small "90 %" | "Normal (90%)" (stock name with modifiers) |
| Alert slot priority | pump > BG warning > eat reminder | BG warning > eat reminder > pump (decided) |
| Notification badge | always red | always red, same as the mockup (no urgent / info colour) |
| Loop slot | icon only | icon + caption in every mode, closed loop included (decided) |
| Target slot | icon + value | plus the time left for a temporary target (decided) |
| Trend triangles | one triangle | stock count (1 to 3) |
| Inspect pill | time, BG, IOB, basal | stock `GraphInspectPill`, complete |
| Font | Inter | system font |
| Insuline tile | label "Insuline active", value "2,33 U" | same label; value from the existing chip state |
| Page 2 graphs | simplified drawing | stock `GraphsSection` |
| Tap on Insuline active / Basale | none drawn | IOB line + insulin entry / basal line + temp basal editor + cancel TBR button (3.2.2, 3.2.3) |
| Consumables panel (patch pump) | not drawn | Pompe Patch = time, Insuline = reservoir left, Capteur = time (3.5.1) |
| Reservoir bar (patch pump) | not drawn | remaining / capacity, 0..1. No driver gives its capacity, so it is a 200 U constant (`PATCH_RESERVOIR_CAPACITY_U`, redesign side); the driver value is used if one is added later (3.5.1) |
| Consumables bar size | 26 dp, radius 13, 12 sp | 32 dp, radius 16, 13 sp, so the closed bar and the open patch card (radius 16) have the same corners. Applies to every pump (3.5) |
