# Upstream files touched by the redesign

Every change in a file that also exists on `master`, and why. All of them check
`RedesignFlags.ENABLED` (in `ui/.../compose/redesign/RedesignFlags.kt`), so with the flag set to
`false` the app behaves like stock 4.0. New files in `redesign/` and new resource files are not
listed here.

## Phase 1 - Chrome

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainNavigationBar.kt`

- Three new parameters with defaults that keep the stock bar: `onMenuClick: (() -> Unit)? = null`,
  `menuSelected: Boolean = false` and `menuBadge: Boolean = false`.
- One call at the end of the bar: `RedesignMenuNavigationItem(...)`, only when `onMenuClick` is not null.
- One import.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainScreen.kt`

All hooks use `redesignChrome` = flag on **and** stacked phone layout (`isRedesignPhoneLayout`).
Split and tablet layouts keep the stock chrome.

| Hook | Why |
|---|---|
| `redesignChrome`, `showRedesignMenu` (`rememberSaveable`), `closeRedesignMenu` at the top | Menu state. Saveable, so Back from a screen opened in the Menu returns to the Menu. |
| `ModalNavigationDrawer(gesturesEnabled = !redesignChrome)` | The drawer is replaced by the Menu; the edge swipe must not open it (and would fight the pager in phase 2). |
| `hasToolbar = ... && !redesignChrome` | Hides the QuickLaunch toolbar and drops its 56 dp FAB offset. |
| `topBarHeight = if (redesignChrome) 0.dp else ...` | No top bar. A height measured before a rotation must not stay as a gap. |
| `OverviewScreen(modifier = clearAndSetSemantics {} while the Menu is open)` | The screen reader must not read the home screen under the Menu. |
| Search results overlay `&& !redesignChrome` | Search results are shown inside the Menu. |
| `VersionOverlay` only when `!redesignChrome` | The version moves to Menu > About. |
| `RedesignMenuScreen(...)` before the top bar | The Menu screen, over the overview, under the bottom bar. |
| Top bar `visible = showChrome && !redesignChrome` | Top bar removed. |
| `MainNavigationBar(onMenuClick = ..., menuSelected = ..., menuBadge = ...)` | Adds the Menu item (only with the redesign chrome), with a dot when something needs setting up. |
| `loopActionAvailable` collected at the top (was collected just before the bar) and `redesignAttentionItems(...)`, passed to the Menu | Loop accept, pump set-up and missing permissions are listed at the top of the Menu. (The BG source warning was there in phase 1 and moved beside the glucose circle in phase 3.) |
| `MainNavigationBar(bgSetupPlugin = null, loopActionAvailable = false, pumpSetupPlugin = null, permissionsMissing = false)` with the redesign chrome | Those four items leave the bar, which keeps at most five: Treatments, Scenes, Manage, Objectives, Menu. With all nine items shown, every label broke inside a word. In phase 3 the BG warning and loop accept also go next to the BG circle. |
| Imports | `rememberSaveable`, `LocalWindowInfo`, `clearAndSetSemantics`, redesign symbols. |

Not touched: `MainTopBar.kt`, `MainDrawer.kt`, `QuickLaunchToolbar.kt`, `overview/OverviewScreen.kt`,
`appshell`.

## Phase 2 - Pager

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/graphs/GraphsSection.kt`

- New parameter `editMode: Boolean = true` (stock: always editable).
- `canEdit = !isSimpleMode && editMode` replaces `!isSimpleMode` at the four places that show a
  pencil or "Add graph". Two comments updated.

`OverviewScreenStacked.kt` had two parameters in phase 2 (page 1 was the stock screen without its
graphs). Phase 3 replaced page 1, so the file is back to stock.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainScreen.kt`

| Hook | Why |
|---|---|
| `redesignOverview = viewModel { RedesignOverviewState() }` | Page and edit mode. A view model: kept over a rotation, but a new start of the app opens on the home page. |
| `LaunchedEffect(redesignChrome)` also ends the edit mode | Landscape shows the stock screen. |
| `SideEffect { redesignOverview.menuOpen = showRedesignMenu }` | While the Menu is open, Back is for the Menu, not for the graphs page under it. |
| `OverviewScreen(redesign = ...)` with the redesign chrome | The two pages. |
| `RedesignMenuScreen(onEditScreenClick = ...)`, null in simple mode | Menu > "Edit screen": graphs page in edit mode. |
| Imports | `SideEffect`, `viewModel`, `RedesignOverviewState`. |

## Phase 3 - Home blocks A and B

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/OverviewScreen.kt`

- New parameters `redesign: RedesignOverviewState? = null` (phase 2) and
  `redesignHome: RedesignHomeInputs = RedesignHomeInputs()`.
- One new branch before the stacked one: `else if (redesign != null) { RedesignOverview(...) }`.
  The stacked call is unchanged.
- `PumpActivityFab(visible = showPumpFab && redesign == null)` and
  `NotificationFab(notificationCount = if (redesign != null) 0 else ...)`: with the redesign there
  are no floating buttons. On the home page these two buttons are beside the glucose circle; the
  graphs page has none (correction 3.3), so the graphs reach the bottom bar. Their padding is stock.
  (The `fabBottomOffset = -56 dp` hook of phases 1–3 in `MainScreen` is gone for the same reason:
  `MainScreen` passes the stock value again.)
- Imports.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainScreen.kt`

| Hook | Why |
|---|---|
| New parameter `bgQualityState: BgQualityCheck.State? = null` | The reason ("Duplicates", "Flat data") on the BG source button. Only a spoken sentence was passed before, and a localized text must not be cut up. |
| `metroViewModel<RedesignHomeViewModel>()` with the redesign chrome | Tile values the stock state has not as separate texts: time left of profile and target, basal rate, temporary basal rate, percent and time left. It reads the same sources as `MainViewModel`, which is not touched. |
| `OverviewScreen(redesignHome = RedesignHomeInputs(...))` | BG source warning, eat reminder and open loop suggestion for the home page. |
| `redesignAttentionItems(...)` without the BG source | The BG source warning left the Menu for the home page. |
| Imports | `BgQualityCheck`, `metroViewModel`, redesign symbols. |

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/OverviewScreen.kt`

- `MainScreen(bgQualityState = bgQualityState)`: one argument; the value was already collected there.

## Graphs page layout

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/graphs/GraphsSection.kt`

- **One** new parameter: `redesignLayout: GraphsRedesignLayout? = null`. Null (stock overview,
  history browser) keeps the stock layout exactly. The class lives in `redesign/` and holds
  `fillHeight`, `graphGap`, `topPadding` and `quickLaunchSpace`.
- **Fill height**: the first three graphs (BG, IOB, the first secondary one) grow (never shrink) so
  they fill `fillHeight`, in the ratio of their saved heights, each at most
  `GraphConfig.MAX_GRAPH_HEIGHT_DP`, also in edit mode. Later graphs follow below at the same scale
  and are reached by scrolling. `GraphConfig` is not changed. `fixedPart` = the measured height down
  to the end of the filled part (the whole section for up to three graphs, the end of the third graph
  with `onGloballyPositioned` and `localPositionOf` for more) minus the graph heights applied, rounded
  down to whole dp. It is taken **once per layout**: the `remember` is keyed on `fillHeight`, the
  graph count, edit mode, gap, top padding, bars height and font size, and only a null value is set,
  so the measure cannot chase its own result (a first version that updated it on every change did not
  settle). `graphScale` = (`fillHeight` − `fixedPart`) / saved heights of the first three, at least 1.
  The four `.height(...)` calls (BG, IOB, secondary graphs) go through `graphHeight(saved)`, which
  returns `saved.dp` when the scale is 1.
- **Gap**: `.padding(top = graphGap)` at the end of the modifier chain of the IOB graph and of each
  secondary graph, after the touch modifiers, so a drag in a gap still scrolls the graphs and does
  not swipe the page. Only vertical: the long-press line, the hour lines and the "now" line keep
  their x (each graph places them from the same time, its own scroll and its own left edge).
- **Top and bottom padding**: the section's padding is `topPadding` (instead of 8 dp) and `bottomPadding` (0 dp instead of 8), so the graphs end at the bottom bar.
- **No QuickLaunch space** (`quickLaunchSpace = false`): no 48 dp spacer at the end, and the
  "Add graph" button is not placed at all while it is hidden (stock keeps it as an invisible
  placeholder).
- Imports: `onSizeChanged`, `LocalDensity`, `floor`, `GraphsRedesignLayout`.

The redesign passes `fillHeight` = height of page 2, and `null` in edit mode, so there the height
slider matches what is seen.

## Portrait only on a phone

### `app/src/main/kotlin/app/aaps/ComposeMainActivity.kt`

- `RedesignOrientation.lock(this)` in `onCreate`, right after `super.onCreate`, and its import.
  The manifest is not changed.

`RedesignOrientation` (`ui/src/androidMain/.../redesign/`) sets `SCREEN_ORIENTATION_USER_PORTRAIT`
for the whole activity when `RedesignFlags.ENABLED` and the device is a phone (smallest side of
`maximumWindowMetrics` under 600 dp, so split screen does not change the answer). Every screen of the
activity stays upright, settings included, and nothing is drawn in landscape first. Flag off or
tablet: nothing (a foldable opened to its tablet screen gets the lock taken back). A pump wizard's
`KeepScreenOnEffect` and `LockPortraitOrientation` put this lock back when they end.

Why the activity and not a manifest attribute: the manifest would lock with the flag off and on
tablets too (target SDK 35 obeys it there). Why not Compose code in `MainScreen` (the first try): it
runs after the first frame, so the app opened in landscape and then turned, and the other screens
(settings) could turn.

## Phase 4 - Home graph

No upstream file. The graph is new code in `redesign/` and reads `GraphViewModel` as it is. It
reuses `inspectDataAt` and `GraphInspectPill` (public in `GraphInspect.kt`), `beltColor` (internal in
`TreatmentBeltGraphCompose.kt`) and `BgRange.toColor` (internal in `BgInfoSection.kt`), all unchanged.

## Phase 4b - Graphs item, Carbs tile, Objectives in the Menu

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainNavigationBar.kt`

- Two new parameters with defaults that keep the stock bar: `onGraphsClick: (() -> Unit)? = null` (null
  hides the item) and `graphsSelected: Boolean = false`.
- One call just before the Menu item: `RedesignGraphsNavigationItem(...)`, only when `onGraphsClick`
  is not null. One import.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainScreen.kt`

| Hook | Why |
|---|---|
| `MainNavigationBar(onGraphsClick = ..., graphsSelected = ...)` with the redesign chrome | A tap shows the graphs page; while it is shown the same tap goes back to the home page and ends the edit mode. Selected while the page is shown and the Menu is closed. |
| `MainNavigationBar(objectivesSetupPlugin = null)` with the redesign chrome | Objectives leaves the bar (at most five items). |
| `redesignAttentionItems(objectivesSetupPlugin, objectivesProgressText, objectivesProgressDescription, ...)` | Objectives is the first row of "À régler". The open loop suggestion row is gone: the Loop tile does it. |
| `menuBadge = redesignAttentionItems.any { it.needsAction }` | Objectives is a progress and does not light the dot of the Menu item. |
| Imports | `GRAPHS_PAGE`, `HOME_PAGE`. |

Not touched: `TreatmentBottomSheet`, the Treatments item.

## Phase 5 - Home layout v2

One upstream line, in one file. Everything else is in `redesign/` (new files `RedesignRoundSlot.kt`,
`RedesignConsumablesBar.kt`, `RedesignBottomBarStyle.kt`; the others are our own files from phases 3 and 4).

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainScreen.kt`

| Hook | Why |
|---|---|
| `RedesignBottomBarStyle(redesignChrome) { MainNavigationBar(...) }` | The bar reads its colours from `MaterialTheme.colorScheme`, so the redesign colours (SPEC 2.3) and the 1 dp top line are set by a theme wrapper. `MainNavigationBar` has **no new parameter**. |
| Import | `RedesignBottomBarStyle`. |
| `onMenuClick`: when the Menu is open, the Menu button also sets `redesignOverview.page = HOME_PAGE` and ends the edit mode | Graphs > Menu > Menu comes back to the home page. Back from the Menu still returns to the page below (unchanged). |

What the theme cannot reach (the sizes are fixed inside the Material 3 navigation bar, and are not changed):
item height (60 dp), icon size (22 dp), indicator size (56 × 30 dp), semi-bold selected label and the
11 sp shrink of a long label.

### Phase 7: consumables panel of a patch pump (SPEC 3.5.1)

| File | Hook | Why |
|---|---|---|
| `overview/statusLights/StatusItem.kt` | Three new defaulted fields: `levelValue: Double?` (reservoir units as a number), `ageMax: String?` (critical age in short text, "3d"), `reservoirReadingLimit: Double?` (the driver's `maxReservoirReading`). | The patch panel shows "age / maximum" and draws a reservoir bar. The stock panel does not read them. |
| `overview/statusLights/StatusViewModel.kt` | `buildInsulinStatus`: `levelValue = reservoirLevel.takeIf { it > 0 }`, `reservoirReadingLimit = maxReading`. Sensor and cannula items: `ageMax = formatAgeMax(...)` (new private function, the critical age from the settings, same short form as the ages). `refreshState`: `pumpHasMenu = (activePlugin.activePumpInternal as? PluginBase)?.hasComposeContent() == true`. Import `PluginBase`. | Same value as the "U" text, no new reading. `pumpHasMenu` says if the Manage sheet "Pump" item would open a screen. It must ask `activePumpInternal`: `activePump` is a concentration wrapper, never a `PluginBase`. Test: `StatusViewModelPumpMenuTest` (new file). |
| `overview/statusLights/StatusUiState.kt` | New field `pumpHasMenu: Boolean = false`. | See above. |

`overview/OverviewStatusSection.kt` is stock again (the step 1 slot `expandedContent` is removed). The patch card is in `redesign/RedesignPatchPumpPanel.kt`: own card, radius 22, no title, settings sheet reusing the internal `StatusLightsSettingsContent`. Other pumps keep the stock panel. New strings: `redesign_age_max_days`, `redesign_value_of_max`.
The 96 U on the stock "Patch pump" row is the insulin used since the patch change (TDD), not a reservoir;
the redesign panel does not show it.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/OverviewScreen.kt`

| Hook | Why |
|---|---|
| `RedesignHomeStock(..., pumpStatusText = pumpStatusText)` (one more argument in the existing hook) | The pump activity slot shows what the pump does in the stock words of the pump status banner (temp basal, reading status, connecting...), not always "Injection". |

## Resources

- `ui/src/androidMain/res/values/strings_redesign.xml` and `values-fr-rFR/strings_redesign.xml` are
  new files, not listed in `crowdin.yml`, so Crowdin never overwrites them.
