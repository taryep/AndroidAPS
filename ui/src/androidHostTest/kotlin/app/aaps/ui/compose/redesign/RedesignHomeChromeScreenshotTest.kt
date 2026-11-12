package app.aaps.ui.compose.redesign

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.aaps.core.data.model.RM
import app.aaps.core.interfaces.notifications.AapsNotification
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationLevel
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.main.MainNavigationBar
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.OverviewScreen
import app.aaps.ui.compose.testing.AapsScreenFixture
import app.aaps.ui.compose.testing.OverviewViewModelFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import app.aaps.core.ui.R as CoreUiR

/**
 * A picture of the phone home screen with the redesign chrome of phase 1: no top bar, no QuickLaunch
 * toolbar, the Menu item in the bottom bar, and a notification, to check where the notification
 * button lands now that the toolbar is gone.
 *
 * The overview and the bar are placed the way `MainScreen` places them: the content gets the bar's
 * height as bottom padding, and `fabBottomOffset` is 0 because there is no toolbar. It writes
 * `ui/build/reports/redesign/home_chrome.png`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
class RedesignHomeChromeScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var screen: AapsScreenFixture
    private lateinit var viewModels: OverviewViewModelFixture

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
        screen = AapsScreenFixture()
        viewModels = OverviewViewModelFixture(screen)
        viewModels.withBg()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun homeWithANotification() {
        val status = viewModels.awaitStatusItems()
        val notification = AapsNotification(
            id = NotificationId.PROFILE_SET_OK,
            instanceKey = 1,
            text = "Profile set",
            level = NotificationLevel.URGENT
        )
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                screen.Content {
                    var barHeightPx by remember { mutableIntStateOf(0) }
                    val barHeight = with(LocalDensity.current) { barHeightPx.toDp() }
                    Box(Modifier.requiredSize(412.dp, 891.dp).testTag(SCREEN)) {
                        OverviewScreen(
                            profileName = "Normal", isProfileModified = false, profileProgress = 0f,
                            tempTargetText = "120–130", tempTargetState = TempTargetChipState.None,
                            tempTargetProgress = 0f, tempTargetReason = null,
                            runningMode = RM.Mode.CLOSED_LOOP, runningModeText = "Boucle fermée",
                            runningModeRemaining = "", runningModeProgress = 0f,
                            tbrState = TbrState.NONE, smbEnabled = true, isSimpleMode = false, calcProgress = 100,
                            graphViewModel = viewModels.graphViewModel, chipsViewModel = viewModels.chipsViewModel,
                            manageViewModel = viewModels.manageViewModel, statusViewModel = status,
                            statusLightsDef = PreferenceSubScreenDef(key = "statuslights", titleResId = CoreUiR.string.statuslights),
                            onNavigate = {}, onTbrChipClick = {}, onIobChipClick = {},
                            notifications = listOf(notification), onDismissNotification = {}, onNotificationActionClick = {},
                            autoShowNotificationSheet = false, onAutoShowConsumed = {},
                            paddingValues = PaddingValues(top = STATUS_BAR, bottom = GESTURE_BAR + barHeight),
                            fabBottomOffset = 0.dp
                        )
                        MainNavigationBar(
                            onManageClick = {},
                            onTreatmentClick = {},
                            onMenuClick = {},
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = GESTURE_BAR)
                                .onSizeChanged { barHeightPx = it.height }
                        )
                    }
                }
            }
        }
        compose.waitForIdle()

        val dir = File("build/reports/redesign").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag(SCREEN).captureToImage().asAndroidBitmap()
        File(dir, "home_chrome.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {

        const val SCREEN = "screen"

        /** Typical insets of the reference phone; the real app reads them from the window. */
        val STATUS_BAR = 32.dp
        val GESTURE_BAR = 24.dp
    }
}
