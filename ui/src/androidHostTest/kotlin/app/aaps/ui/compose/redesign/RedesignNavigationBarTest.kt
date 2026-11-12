package app.aaps.ui.compose.redesign

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.plugin.PluginDescription
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.icons.IcPluginObjectives
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.main.MainNavigationBar
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.roundToInt

/**
 * The Menu item the redesign adds to [MainNavigationBar], and the bar with every item shown at once.
 *
 * The worst case writes a picture of the bar to `ui/build/reports/redesign/` and lists every label
 * that is cut or broken inside a word. It does not fail on those: whether the bar is still usable
 * is a design decision, made by looking at the picture.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h891dp-xxhdpi")
class RedesignNavigationBarTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
    }

    private val menuLabel get() = RuntimeEnvironment.getApplication().getString(R.string.redesign_menu)

    @Test
    fun theStockBarHasNoMenuItem() {
        compose.setContent { MaterialTheme { MainNavigationBar(onManageClick = {}, onTreatmentClick = {}) } }
        assertThat(compose.onAllNodes(hasText(menuLabel)).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun theMenuItemIsLastAndShowsWhenTheMenuIsOpen() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { MainNavigationBar(onManageClick = {}, onTreatmentClick = {}, onMenuClick = { clicks++ }, menuSelected = true) }
        }
        compose.onNode(hasText(menuLabel)).assertIsSelected()
        // While open, the screen reader says that the tap closes the menu.
        val closeLabel = RuntimeEnvironment.getApplication().getString(R.string.redesign_close_menu)
        compose.onNode(SemanticsMatcher("click label") { it.config.getOrNull(SemanticsActions.OnClick)?.label == closeLabel }).assertExists()
    }

    @Test
    fun aBadgeOnTheMenuIsSpokenAsActionNeeded() {
        compose.setContent {
            MaterialTheme { MainNavigationBar(onManageClick = {}, onTreatmentClick = {}, onMenuClick = {}, menuBadge = true) }
        }
        val actionNeeded = RuntimeEnvironment.getApplication().getString(R.string.a11y_badge_attention)
        compose.onNode(hasText(menuLabel) and hasStateDescription(actionNeeded)).assertExists()
    }

    @Test
    fun worstCaseEnglish() = worstCase("en")

    @Test
    @Config(qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
    fun worstCaseFrench() = worstCase("fr")

    @Test
    @Config(qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
    fun worstCaseFrenchLargeFont() = worstCase("fr_font130", fontScale = 1.3f)

    private fun worstCase(name: String, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.width(412.dp).testTag(BAR)) {
                    MainNavigationBar(
                        onManageClick = {},
                        onTreatmentClick = {},
                        quickWizardCount = 3,
                        automationTotal = 4,
                        automationCount = 2,
                        // BG source warning, loop accept, pump set-up and permissions are not here: with
                        // the redesign they are in the Menu, which gets a badge instead (see MainScreen).
                        objectivesSetupPlugin = plugin(null, IcPluginObjectives, CoreUiStrings.objectives),
                        objectivesProgressText = "9/10",
                        onMenuClick = {},
                        menuBadge = true
                    )
                }
            }
            }
        }
        compose.waitForIdle()

        val dir = File("build/reports/redesign").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag(BAR).captureToImage().asAndroidBitmap()
        File(dir, "navbar_worst_$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val problems = textProblems()
        File(dir, "navbar_worst_$name.txt").writeText(problems.joinToString("\n").ifEmpty { "no problems" })
        println("navbar worst case $name: ${problems.size} problems\n" + problems.joinToString("\n"))

        // Five items: treatments, scenes, manage, objectives, menu.
        compose.onNode(hasText(menuLabel)).assertExists()
    }

    private fun plugin(name: String?, icon: ImageVector, label: TextRef?): PluginBase =
        mock<PluginBase>().also {
            val description = PluginDescription().icon(icon)
            if (label != null) description.pluginName(label)
            whenever(it.pluginDescription).thenReturn(description)
            whenever(it.name).thenReturn(name ?: "")
        }

    /** Every label that is broken inside a word or cut without an ellipsis (the same rules as `OverviewSizeSweepTest`). */
    private fun textProblems(): List<String> =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
                val shown = node.config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty()
                results.mapNotNull { result -> problemOf(result)?.let { "\"$shown\" $it" } }
            }

    private fun problemOf(result: TextLayoutResult): String? {
        val text = result.layoutInput.text.text
        val lastLine = result.lineCount - 1
        if (result.size.height < result.multiParagraph.height) {
            return "is taller than its place: ${result.multiParagraph.height.roundToInt()} px in ${result.size.height} px"
        }
        if (result.multiParagraph.didExceedMaxLines && !result.isLineEllipsized(lastLine)) return "is cut off without an ellipsis"
        for (line in 0 until lastLine) {
            val end = result.getLineEnd(line)
            if (end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()) {
                return "breaks inside a word, after \"${text.substring(0, end)}\""
            }
        }
        if (result.lineCount > 1) return "takes ${result.lineCount} lines"
        return null
    }

    private companion object {

        const val BAR = "bar"
    }
}
