# Upstream files touched by the redesign

Every change in a file that also exists on `master`, and why. All of them check the switch (step 1b):
`LocalRedesignEnabled` or `redesignEnabled(preferences)` (in `ui/.../compose/redesign/RedesignFlags.kt`), so with
the setting "New interface" off, or the build constant `RedesignFlags.ENABLED` set to `false`, the app behaves
like stock 4.0. New files in `redesign/` and new resource files are not
listed here.

## Main screen and bottom bar (current)

The whole redesign chrome lives in `redesign/RedesignMainScreen.kt` (content area Home | Graphs | Menu, its own
bottom bar `RedesignBottomBar`, sheets and dialogs). The older per-phase notes about `MainScreen` and
`MainNavigationBar` are removed: those hooks are gone.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainNavigationBar.kt`

No change. Identical to `master`.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainScreen.kt`

| Hook | Why |
|---|---|
| `if (redesignChromeActive()) { RedesignMainScreen(...); return }` at the start of the function | The one hook. The flag is on and the layout is the phone layout; split and tablet layouts keep the stock screen below it. |
| New parameter `bgQualityState: BgQualityCheck.State? = null` | Passed on to `RedesignMainScreen` (the BG source warning beside the glucose circle). Its caller (`main/OverviewScreen.kt`) passes it. |
| Three imports | `BgQualityCheck`, `RedesignMainScreen`, `redesignChromeActive`. |

The redesign screen has no drawer, no top bar, no QuickLaunch toolbar and no preview mode (the phone layout is
upright, so the height is always above the preview limit).

## Phase 1 - Chrome

## Phase 2 - Pager

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/graphs/GraphsSection.kt`

- New parameter `editMode: Boolean = true` (stock: always editable).
- `canEdit = !isSimpleMode && editMode` replaces `!isSimpleMode` at the four places that show a
  pencil or "Add graph". Two comments updated.

`OverviewScreenStacked.kt` had two parameters in phase 2 (page 1 was the stock screen without its
graphs). Phase 3 replaced page 1, so the file is back to stock.

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

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/OverviewScreen.kt`

- `MainScreen(bgQualityState = bgQualityState)`: one argument; the value was already collected there.

## Graphs page layout

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/graphs/GraphsSection.kt`

- **One** new parameter: `redesignLayout: GraphsRedesignLayout? = null`. Null (stock overview,
  history browser) keeps the stock layout exactly. The class lives in `redesign/` and holds
  `smallHeight`, `graphGap`, `topPadding`, `bottomPadding`, `quickLaunchSpace` and `legendHeight`.
- **Fixed block heights** (phase 8a, replaces the phase 3 "fill height"): with a layout, every graph block
  has a fixed height from its size. The saved height is a size code (100 Small, 175 Medium, 250 Grand,
  any other value rounded to the nearest) and `GraphHeights.ofSaved(...)` (in `redesign/`, unit tested)
  gives the dp. The height **includes the legend**. Nothing is measured: the phase 3 `fixedPart`,
  `graphScale`, `reportFilledPart`, `SectionBox`, `FILL_GRAPH_COUNT` and their imports are removed.
  `GraphConfig` and the saved format are not changed.
- **Legends**: with a layout, each graph below BG has its own legend line (`RedesignGraphLegend`, in `redesign/`) just above its plot, not drawn on it: 12 sp (font scale capped at 1.3), else 10 sp, else wrapped between entries. The BG graph has none (SPEC 4.1). The line height is measured and taken from the plot of the same graph. The name row stays only for the pencil in edit mode (empty name).
- **Pull-ups in the layout**: the BG box uses `pullUp(16 dp)` instead of `offset(y = -16.dp)` (the treatment belt overlap); the IOB and secondary boxes skip their stock `pullUp`. The internal padding of the stock graphs is accepted as it is. Without a layout the stock modifiers are used as before.
- **Gap**: `.padding(top = graphGap)` at the end of the modifier chain of the IOB graph and of each
  secondary graph, after the touch modifiers, so a drag in a gap still scrolls the graphs and does
  not swipe the page. Only vertical: the long-press line, the hour lines and the "now" line keep
  their x (each graph places them from the same time, its own scroll and its own left edge).
- **Top and bottom padding**: the section's padding is `topPadding` (instead of 8 dp) and `bottomPadding` (0 dp instead of 8), so the graphs end at the bottom bar.
- **No QuickLaunch space** (`quickLaunchSpace = false`): no 48 dp spacer at the end, and the
  "Add graph" button is not placed at all while it is hidden (stock keeps it as an invisible
  placeholder).
- Imports: `AapsTheme`, `GraphHeights`, `GraphLegendItem`, `GraphsRedesignLayout`, `RedesignGraphLegend`.

## Portrait only on a phone

### `app/src/main/kotlin/app/aaps/ComposeMainActivity.kt`

- `RedesignOrientation.lock(this, redesignOn)` in `onCreate`, right after `super.onCreate`, and its import
  (`redesignOn`: see "Switch between the new and the classic interface" below).
  The manifest is not changed.

`RedesignOrientation` (`ui/src/androidMain/.../redesign/`) sets `SCREEN_ORIENTATION_USER_PORTRAIT`
for the whole activity when the new interface is on and the device is a phone (smallest side of
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

## Phase 5 - Home layout v2

One upstream line, in one file. Everything else is in `redesign/` (new files `RedesignRoundSlot.kt`,
`RedesignConsumablesBar.kt`, `RedesignBottomBarStyle.kt`; the others are our own files from phases 3 and 4).

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

## Phase 6 - Tile actions and Traitements

New file `redesign/RedesignTileActions.kt`: the tap rules of the two tiles, the IOB line, the basal
line and its Cancel button. `RedesignHomeViewModel` (our file) gets `iobParts` (the same two calls and
the same rounding as `ChipsViewModel.showIobInfo`) and `tempBasalLine` (the data of the stock
"Temp basal" dialog in the new string `redesign_tbr_line`, "0,00 U/h @15:58 5/60 min").
`ChipsViewModel` and `ChipsUiState` are not touched.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/insulinDialog/InsulinDialogScreen.kt`

- New last parameter `topInfo: (@Composable () -> Unit)? = null` on `InsulinDialogScreen` and on
  `InsulinDialogContent`; the screen passes it on.
- `topInfo?.invoke()` right above `DialogStatusBar` (step 4: the mockup has the IOB line first; it was under it). Null (stock) draws nothing.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/tempBasalDialog/TempBasalDialogScreen.kt`

- The same parameter on `TempBasalDialogScreen` and `TempBasalDialogContent`; the screen passes it on.
- `topInfo?.invoke()` at the top of the scrolling column, above the editor card.

### `appshell/src/commonMain/kotlin/app/aaps/appshell/navigation/AppNavGraph.kt`

| Hook | Why |
|---|---|
| `InsulinDialogScreen(topInfo = if (LocalRedesignEnabled.current) { { RedesignIobLine() } } else null)` | The IOB line. Passed in the route, not by the tile, so it is there from every entry point (Traitements too, on purpose) and survives Back and the death of the process. |
| `TempBasalDialogScreen(topInfo = if (LocalRedesignEnabled.current) { { RedesignTbrLine(...) } } else null)` | The basal line and the Cancel button. After a confirmed cancel the screen closes; a failed cancel shows the stock "temp basal delivery error". |
| Imports | `LocalRedesignEnabled`, `RedesignIobLine`, `RedesignTbrLine`. |

Each line reads its own `RedesignHomeViewModel` (a new, light instance in the route). The Cancel
button uses its own `ManageViewModel`: `cancelTempBasal()`, the master's confirmation in
`ElementConfirmationDialog`, then `commitCancel`, exactly like `ManageSheetHost`.

## Phase 8a - Graphs page title, heights, Menu

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/graphs/GraphsSection.kt`

See "Graphs page layout" above: the fixed heights and legends all hang on the existing
`redesignLayout` parameter. No new parameter. With `redesignLayout == null` every stock modifier is the
same as on `master` (the `.then(if (redesignLayout == null) <stock> else <redesign>)` branches).

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/graphs/GraphViewModel.kt`

- One new function `migrateRedesignGraphHeights()`: it calls `RedesignGraphMigration.runOnce(...)` (in `redesign/`). Only the redesign graphs page calls it, so the stock screens never run it. One import.

### `core/keys/src/commonMain/kotlin/app/aaps/core/keys/BooleanNonKey.kt` and `app/src/testFull/resources/prefs-schema.txt`

- One new key `RedesignGraphHeightsMigrated` (`exportable = false`): the flag of the one-time BG height change (BG height 100 becomes 250). One line in the golden schema file.

## Step 1b - Switch between the new and the classic interface

The setting "New interface" (`BooleanKey.GeneralRedesign`, key `redesign_enabled`, on by default, exported with
the settings) picks the interface without a new build. It is in Settings › Appearance, and the Menu of the new
interface has a row to go back to the classic one (it asks first and says where to come back). The activity
reads the setting once in `onCreate` and starts again when it changes, so no screen ever sees it change.

| File | Hook | Why |
|---|---|---|
| `core/keys/.../BooleanKey.kt` | New key `GeneralRedesign`. | The setting. Its strings are in the new files `core/keys/src/androidMain/res/values/strings_redesign.xml` and `values-fr-rFR/strings_redesign.xml` (not in `crowdin.yml`). |
| `app/src/testFull/resources/prefs-schema.txt` | One line `redesign_enabled\|BooleanKey\|true`. | The golden list of keys. |
| `ui/.../search/BuiltInSearchables.kt` | `BooleanKey.GeneralRedesign` after the colour-blind mode in the Appearance screen. | The way back from the classic interface, which has no Menu. |
| `app/.../ComposeMainActivity.kt` | Field `redesignOn = redesignEnabled(preferences)` set in `onCreate`; `CompositionLocalProvider(LocalRedesignEnabled provides redesignOn)` around `MainContent()`; in `observePreferences`, a collector of the key that calls `recreate()` (with `rebuildingForRestart`, so the view models are new) when the value differs from `redesignOn`. Imports. | One value for the whole life of the activity, the same for every screen (main screen, sheets, portrait lock). |
| `appshell/.../AppNavGraph.kt` | `RedesignFlags.ENABLED` became `LocalRedesignEnabled.current` in the two `topInfo` hooks. | The insulin and basal lines follow the setting. |

## Resources

- `ui/src/androidMain/res/values/strings_redesign.xml` and `values-fr-rFR/strings_redesign.xml` are
  new files, not listed in `crowdin.yml`, so Crowdin never overwrites them.
