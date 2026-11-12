package app.aaps.ui.compose.overview.statusLights

import app.aaps.core.data.model.TDD
import app.aaps.core.data.model.TE
import app.aaps.core.data.pump.defs.PumpDescription
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.nsclient.ProcessedDeviceStatusData
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.pump.Pump
import app.aaps.core.interfaces.pump.PumpInsulin
import app.aaps.core.interfaces.pump.PumpWithConcentration
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventConfigBuilderChange
import app.aaps.core.interfaces.rx.events.EventInitializationChanged
import app.aaps.core.interfaces.rx.events.EventNsClientStatusUpdated
import app.aaps.core.interfaces.source.BgSource
import app.aaps.core.interfaces.stats.TddCalculator
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.interfaces.utils.TimeDiff
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.TextRef
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * `pumpHasMenu` (redesign, SPEC 3.5.1) must ask the pump PLUGIN. `activePump` is a concentration
 * wrapper and never a [PluginBase], so asking it made the patch row never clickable on the phone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class StatusViewModelPumpMenuTest {

    @Mock private lateinit var rh: ResourceHelper
    @Mock private lateinit var activePlugin: ActivePlugin
    @Mock private lateinit var profileFunction: ProfileFunction
    @Mock private lateinit var config: Config
    @Mock private lateinit var persistenceLayer: PersistenceLayer
    @Mock private lateinit var dateUtil: DateUtil
    @Mock private lateinit var rxBus: RxBus
    @Mock private lateinit var preferences: Preferences
    @Mock private lateinit var tddCalculator: TddCalculator
    @Mock private lateinit var decimalFormatter: DecimalFormatter
    @Mock private lateinit var processedDeviceStatusData: ProcessedDeviceStatusData

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(rxBus.toFlow(EventInitializationChanged::class)).thenReturn(emptyFlow())
        whenever(rxBus.toFlow(EventConfigBuilderChange::class)).thenReturn(emptyFlow())
        whenever(rxBus.toFlow(EventNsClientStatusUpdated::class)).thenReturn(emptyFlow())
        whenever(persistenceLayer.observeChanges(TE::class)).thenReturn(emptyFlow())
        whenever(persistenceLayer.databaseClearedFlow).thenReturn(emptyFlow())
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a pump plugin with its own screen gives pumpHasMenu`() {
        assertThat(pumpHasMenu(pluginHasScreen = true)).isTrue()
    }

    @Test
    fun `a pump plugin without its own screen does not give pumpHasMenu`() {
        assertThat(pumpHasMenu(pluginHasScreen = false)).isFalse()
    }

    private fun pumpHasMenu(pluginHasScreen: Boolean): Boolean = runBlocking {
        // The wrapper the rest of the view model reads.
        val wrapper = mock<PumpWithConcentration>()
        whenever(activePlugin.activePump).thenReturn(wrapper)
        whenever(wrapper.pumpDescription).thenReturn(PumpDescription())
        whenever(wrapper.batteryLevel).thenReturn(MutableStateFlow<Int?>(null))
        whenever(wrapper.reservoirLevel).thenReturn(MutableStateFlow(PumpInsulin(10.0)))
        // The plugin behind it.
        val plugin = mock<PluginBase>(extraInterfaces = arrayOf(Pump::class))
        whenever(plugin.hasComposeContent()).thenReturn(pluginHasScreen)
        whenever(activePlugin.activePumpInternal).thenReturn(plugin as Pump)

        val bgSource = mock<BgSource>()
        whenever(activePlugin.activeBgSource).thenReturn(bgSource)
        whenever(bgSource.sensorBatteryLevel).thenReturn(-1)
        whenever(rh.gs(any<TextRef>())).thenReturn("text")
        whenever(dateUtil.computeDiff(any(), any())).thenReturn(TimeDiff(1, 2, 0, 0, 0, 0, 0))
        whenever(persistenceLayer.getLastTherapyRecordUpToNow(any())).thenReturn(null)
        whenever(tddCalculator.calculateIntervalWithCachedDays(any(), any(), any())).thenReturn(TDD(timestamp = 0, totalAmount = 0.0))

        val viewModel = StatusViewModel(
            rh, activePlugin, profileFunction, config, persistenceLayer, dateUtil, rxBus, preferences,
            tddCalculator, decimalFormatter, processedDeviceStatusData
        )
        // Collect the state the way a screen does, so a refresh runs.
        val screen = launch(Dispatchers.Unconfined) { viewModel.uiState.collect { } }
        withTimeoutOrNull(20_000) { while (viewModel.uiState.value.cannulaStatus == null) delay(10) }
        val result = viewModel.uiState.value.pumpHasMenu
        screen.cancelAndJoin()
        result
    }
}
