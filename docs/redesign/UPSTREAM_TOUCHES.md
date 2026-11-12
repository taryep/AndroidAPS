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
| `OverviewScreen(fabBottomOffset = -56 dp)` with the redesign chrome | The notification button sits 16 dp above the bottom bar instead of 72 dp, and the pump activity button 72 dp instead of 128 dp, so they do not float over the graphs. |
| `OverviewScreen(modifier = clearAndSetSemantics {} while the Menu is open)` | The screen reader must not read the home screen under the Menu. |
| Search results overlay `&& !redesignChrome` | Search results are shown inside the Menu. |
| `VersionOverlay` only when `!redesignChrome` | The version moves to Menu > About. |
| `RedesignMenuScreen(...)` before the top bar | The Menu screen, over the overview, under the bottom bar. |
| Top bar `visible = showChrome && !redesignChrome` | Top bar removed. |
| `MainNavigationBar(onMenuClick = ..., menuSelected = ..., menuBadge = ...)` | Adds the Menu item (only with the redesign chrome), with a dot when something needs setting up. |
| `loopActionAvailable` collected at the top (was collected just before the bar) and `redesignAttentionItems(...)`, passed to the Menu | BG source warning, loop accept, pump set-up and missing permissions are listed at the top of the Menu. |
| `MainNavigationBar(bgSetupPlugin = null, loopActionAvailable = false, pumpSetupPlugin = null, permissionsMissing = false)` with the redesign chrome | Those four items leave the bar, which keeps at most five: Treatments, Scenes, Manage, Objectives, Menu. With all nine items shown, every label broke inside a word. In phase 3 the BG warning and loop accept also go next to the BG circle. |
| Imports | `rememberSaveable`, `LocalWindowInfo`, `clearAndSetSemantics`, redesign symbols. |

Not touched: `MainTopBar.kt`, `MainDrawer.kt`, `QuickLaunchToolbar.kt`, `overview/OverviewScreen.kt`,
`appshell`.

## Phase 2 - Pager

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/graphs/GraphsSection.kt`

- New parameter `editMode: Boolean = true` (stock: always editable).
- `canEdit = !isSimpleMode && editMode` replaces `!isSimpleMode` at the four places that show a
  pencil or "Add graph". Two comments updated.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/OverviewScreenStacked.kt`

- New parameters `showGraphs: Boolean = true` and `bgModifier: Modifier = Modifier`.
- `if (showGraphs)` before `GraphsSection(...)`; `modifier = bgModifier` on `BgInfoSection(...)`.
  The redesign uses the stock stacked screen without graphs as page 1 until phase 3.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/overview/OverviewScreen.kt`

- New parameter `redesign: RedesignOverviewState? = null`.
- The stacked branch calls `RedesignOverviewOrStock(...) { ... OverviewScreenStacked(...) }`: with
  `redesign` null it draws the stock screen as before. It also gets `showPumpFab`, so the end of
  the graphs page leaves room for the pump activity button while it shows (128 dp, else 72 dp). The call to `OverviewScreenStacked` keeps
  its lines, with the padding from the lambda and the two new parameters.
- Two imports.

### `ui/src/commonMain/kotlin/app/aaps/ui/compose/main/MainScreen.kt`

| Hook | Why |
|---|---|
| `redesignOverview = viewModel { RedesignOverviewState() }` | Page and edit mode. A view model: kept over a rotation, but a new start of the app opens on the home page. |
| `LaunchedEffect(redesignChrome)` also ends the edit mode | Landscape shows the stock screen. |
| `SideEffect { redesignOverview.menuOpen = showRedesignMenu }` | While the Menu is open, Back is for the Menu, not for the graphs page under it. |
| `OverviewScreen(redesign = ...)` with the redesign chrome | The two pages. |
| `RedesignMenuScreen(onEditScreenClick = ...)`, null in simple mode | Menu > "Edit screen": graphs page in edit mode. |
| Imports | `SideEffect`, `viewModel`, `RedesignOverviewState`. |

## Resources

- `ui/src/androidMain/res/values/strings_redesign.xml` and `values-fr-rFR/strings_redesign.xml` are
  new files, not listed in `crowdin.yml`, so Crowdin never overwrites them.
