package app.aaps.ui.compose.redesign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.data.model.GlucoseUnit
import app.aaps.core.interfaces.overview.graph.BasalGraphData
import app.aaps.core.interfaces.overview.graph.BgDataPoint
import app.aaps.core.interfaces.overview.graph.BgRange
import app.aaps.core.interfaces.overview.graph.BolusType
import app.aaps.core.interfaces.overview.graph.CobGraphData
import app.aaps.core.interfaces.overview.graph.IobGraphData
import app.aaps.core.interfaces.overview.graph.RunningModeGraphData
import app.aaps.core.interfaces.overview.graph.TreatmentGraphData
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.core.ui.compose.LocalDateUtil
import app.aaps.core.ui.compose.LocalDecimalFormatter
import app.aaps.core.ui.compose.LocalProfileUtil
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.overview.graphs.GraphInspectPill
import app.aaps.ui.compose.overview.graphs.GraphViewModel
import app.aaps.ui.compose.overview.graphs.InspectData
import app.aaps.ui.compose.overview.graphs.beltColor
import app.aaps.ui.compose.overview.graphs.inspectDataAt
import app.aaps.ui.compose.overview.toColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The simplified glucose graph of the home page (SPEC 3.6): range buttons, one dot per reading on a
 * fixed scale, the coloured lines of the ranges, the running-mode band, and the stock inspect pill on
 * a long press, the treatment markers (with their values on 3 h and 6 h). No zoom, no predictions. A
 * tap does nothing (decided 10 Nov: the graphs page opens only from the bottom bar).
 *
 * It fills the height it is given; the plot gets what is left after the buttons and the hour labels.
 */
@Composable
fun RedesignHomeGraph(
    graphViewModel: GraphViewModel,
    range: HomeGraphRange,
    onRangeChange: (HomeGraphRange) -> Unit,
    targetAccent: Color? = null,
    modifier: Modifier = Modifier
) {
    val colors = redesignColors()
    var pill by remember { mutableStateOf<InspectData?>(null) }
    var pillX by remember { mutableStateOf(0f) }
    val sidePx = with(LocalDensity.current) { PLOT_SIDE.toPx() }
    // Full width, radius 0, a border on the top and the bottom only (SPEC 3.6).
    Box(
        modifier
            .background(colors.graphCard)
            .drawBehind {
                val line = 1.dp.toPx()
                drawRect(colors.graphCardBorder, Offset.Zero, Size(size.width, line))
                drawRect(colors.graphCardBorder, Offset(0f, size.height - line), Size(size.width, line))
            }
    ) {
        // 4 dp above and 6 dp under, plus the 1 dp borders.
        Column(Modifier.fillMaxSize().padding(top = 5.dp, bottom = 7.dp)) {
            RangeButtons(range, onRangeChange, colors)
            HomeGraphPlot(
                graphViewModel = graphViewModel,
                range = range,
                colors = colors,
                pill = pill,
                // The plot is 17 dp in from the card edge: the pill is placed in card coordinates.
                onPill = { data, x -> pill = data; pillX = x + sidePx },
                targetAccent = targetAccent,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = PLOT_SIDE)
            )
        }
        // The stock pill, unchanged and with all its lines, over the range buttons, centred on the
        // reading and kept inside the card.
        pill?.let { data ->
            Box(
                Modifier.layout { measurable, constraints ->
                    val edge = PILL_EDGE.roundToPx()
                    val placeable = measurable.measure(Constraints(maxWidth = (constraints.maxWidth - 2 * edge).coerceAtLeast(0)))
                    val x = (pillX - placeable.width / 2f).roundToInt().coerceIn(edge, (constraints.maxWidth - edge - placeable.width).coerceAtLeast(edge))
                    layout(constraints.maxWidth, placeable.height + PILL_TOP.roundToPx()) {
                        placeable.place(x, PILL_TOP.roundToPx())
                    }
                }
            ) { GraphInspectPill(data = data) }
        }
    }
}

/** `3 h | 6 h | 12 h | 18 h | 24 h`, equal widths, no background of their own; the chosen one is filled. */
@Composable
private fun RangeButtons(range: HomeGraphRange, onRangeChange: (HomeGraphRange) -> Unit, colors: RedesignColors) {
    val groupName = stringResource(UiStrings.redesign_graph_ranges)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics { contentDescription = groupName }
            .selectableGroup()
    ) {
        HomeGraphRange.entries.forEach { entry ->
            val selected = entry == range
            val description = stringResource(UiStrings.redesign_graph_range_description, entry.hours)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) colors.selectedRange else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onRangeChange(entry) })
                    .semantics { contentDescription = description }
            ) {
                Text(
                    text = stringResource(UiStrings.redesign_graph_range, entry.hours),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) colors.textPrimary else colors.textSecondary,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/** One coloured line across the plot (250, high mark, low mark, 56), with its label. */
private class ThresholdLine(val mgdl: Double, val label: String, val lineColor: Color, val lineAlpha: Float, val textColor: Color, val dashed: Boolean)

/** Where the plot is inside the canvas, and its time window. */
private class PlotGeometry(val left: Float, val right: Float, val top: Float, val bottom: Float, val start: Long, val end: Long) {

    fun x(time: Long): Float = left + (time - start).toFloat() / (end - start) * (right - left)
    fun time(x: Float): Long = start + ((x.coerceIn(left, right) - left) / (right - left) * (end - start)).toLong()
}

@Composable
private fun HomeGraphPlot(
    graphViewModel: GraphViewModel,
    range: HomeGraphRange,
    colors: RedesignColors,
    pill: InspectData?,
    onPill: (InspectData?, Float) -> Unit,
    targetAccent: Color?,
    modifier: Modifier
) {
    val readings by graphViewModel.bgReadingsFlow.collectAsStateWithLifecycle()
    val chartConfig by graphViewModel.chartConfigFlow.collectAsStateWithLifecycle()
    val runningModes by graphViewModel.runningModeGraphFlow.collectAsStateWithLifecycle()
    val targetLine by graphViewModel.targetLineFlow.collectAsStateWithLifecycle()
    val iobGraph by graphViewModel.iobGraphFlow.collectAsStateWithLifecycle()
    val cobGraph by graphViewModel.cobGraphFlow.collectAsStateWithLifecycle()
    val basalGraph by graphViewModel.basalGraphFlow.collectAsStateWithLifecycle()
    val treatments by graphViewModel.treatmentGraphFlow.collectAsStateWithLifecycle()

    val dateUtil = LocalDateUtil.current
    val units = LocalProfileUtil.current.units
    val decimalFormatter = LocalDecimalFormatter.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val general = AapsTheme.generalColors
    val elementColors = AapsTheme.elementColors

    // The window moves at each full hour; checked every minute and at each new reading.
    val tick by produceState(dateUtil.now(), dateUtil) {
        while (true) {
            delay(NOW_REFRESH_MS)
            value = dateUtil.now()
        }
    }
    val now = remember(tick, readings) { max(tick, dateUtil.now()) }
    val timeZone = TimeZone.currentSystemDefault()
    val nowEnd = homeGraphWindowEnd(now, timeZone)
    // Step 3: a horizontal drag moves the window inside the last 24 hours (not on 24 h). Null = the window
    // follows now. Kept per range, so a change of range always brings it back to now. It is a time, not
    // a distance from now, so a new reading does not move a window the user scrolled back.
    var scrolledEnd by remember(range) { mutableStateOf<Long?>(null) }
    val end = homeGraphScrolledEnd(nowEnd, scrolledEnd, range)
    val start = end - range.hours * HOUR_MS
    val ticks = homeGraphScrolledTicks(nowEnd, end, range)
    val tickLabels = ticks.map { stringResource(UiStrings.redesign_graph_hour, Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).hour) }

    val allSorted = remember(readings) { readings.sortedBy { it.timestamp } }
    val firstShown = remember(allSorted, start) { allSorted.indexOfFirst { it.timestamp >= start }.let { if (it < 0) allSorted.size else it } }
    val shown = remember(allSorted, start, end) { allSorted.filter { it.timestamp in start..end } }
    // The axis is 40 to 280 mg/dL and grows on either side only when a reading is outside it. While the
    // finger drags, it stays as it was; after the release it adapts to the visible window (decided 12 Nov).
    val shownMgdl = shown.map { toMgdl(it.value, units) }
    var frozenAxis by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val maxMgdl = frozenAxis?.second ?: homeGraphMaxMgdl(shownMgdl)
    val minMgdl = frozenAxis?.first ?: homeGraphMinMgdl(shownMgdl)
    val currentAxis by rememberUpdatedState(minMgdl to maxMgdl)
    val currentNowEnd by rememberUpdatedState(nowEnd)
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun label(valueInUnits: Double): String =
        if (units == GlucoseUnit.MMOL) decimalFormatter.to1Decimal(valueInUnits) else decimalFormatter.to0Decimal(valueInUnits)

    val lines = listOf(
        ThresholdLine(
            HOME_GRAPH_VERY_HIGH_MGDL, label(fromMgdl(HOME_GRAPH_VERY_HIGH_MGDL, units)),
            general.bgVeryHigh, 0.75f, colors.thresholdText.veryHigh, dashed = true
        ),
        ThresholdLine(toMgdl(chartConfig.highMark, units), label(chartConfig.highMark), general.bgHigh, 0.6f, colors.thresholdText.high, dashed = false),
        ThresholdLine(toMgdl(chartConfig.lowMark, units), label(chartConfig.lowMark), general.bgLow, 0.7f, colors.thresholdText.low, dashed = false),
        ThresholdLine(
            HOME_GRAPH_VERY_LOW_MGDL, label(fromMgdl(HOME_GRAPH_VERY_LOW_MGDL, units)),
            general.bgVeryLow, 0.9f, colors.thresholdText.veryLow, dashed = true
        )
    )
    // The target now, labelled like the lines. It is one value: the stock target line has one value at a time.
    // The target now: read with the real time, so a new temporary target shows at once, not at the next minute.
    val targetValue = targetLine.targets.lastOrNull { it.timestamp <= dateUtil.now() }?.value
    val targetMgdl = targetValue?.let { toMgdl(it, units) }
    // The grey step follows the height of the plot and the span of the axis, as when the stock graph is zoomed out.
    val plotHeightPx = with(density) { size.height - (HOUR_LABELS + PLOT_BOTTOM + PLOT_TOP).toPx() }
    val stepMgdl = homeGraphStepMgdl(units, minMgdl, maxMgdl, plotHeightPx.coerceAtLeast(1f), with(density) { GRADUATION_SPACING.toPx() })
    val graduations = homeGraphGraduations(minMgdl, maxMgdl, stepMgdl, lines.map { it.mgdl })
    val rangeColors = BgRange.entries.associateWith { it.toColor() }

    // Text layouts once per composition, so the label column can be as wide as its widest label.
    val thresholdStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    val greyStyle = TextStyle(fontSize = 11.sp, color = colors.graphLabel, fontFeatureSettings = "tnum")
    val hourStyle = TextStyle(fontSize = 11.sp, color = colors.textSecondary, fontFeatureSettings = "tnum")
    val treatmentValueStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    val lineLabels = lines.map { textMeasurer.measure(it.label, thresholdStyle.copy(color = it.textColor)) } +
        listOfNotNull(targetValue?.let { textMeasurer.measure(label(it), thresholdStyle.copy(color = targetAccent ?: colors.textSecondary)) })
    val gradLabels = graduations.map { textMeasurer.measure(label(fromMgdl(it, units)), greyStyle) }
    val hourLabels = tickLabels.map { textMeasurer.measure(it, hourStyle) }
    val labelColumn = with(density) { max(LABEL_COLUMN.toPx(), (lineLabels + gradLabels).maxOf { it.size.width }.toFloat()) }

    val geometry = with(density) {
        PlotGeometry(
            left = labelColumn + LABEL_GAP.toPx(),
            right = size.width - PLOT_END.toPx(),
            top = PLOT_TOP.toPx(),
            bottom = size.height - HOUR_LABELS.toPx() - PLOT_BOTTOM.toPx(),
            start = start,
            end = end
        )
    }

    // Long press: the time under the finger, snapped to a reading by the stock inspectDataAt.
    var fingerX by remember { mutableStateOf<Float?>(null) }
    val currentGeometry by rememberUpdatedState(geometry)
    val currentNow by rememberUpdatedState(now)
    val currentPill by rememberUpdatedState(pill)
    val inspectSources by rememberUpdatedState(InspectSources(readings, iobGraph, cobGraph, basalGraph, treatments, runningModes))
    LaunchedEffect(Unit) {
        snapshotFlow { fingerX }.collectLatest { x ->
            if (x == null) {
                // The pill and the line stay 2 s after the finger leaves, as on the stock graph.
                delay(INSPECT_LINGER_MS)
                onPill(null, 0f)
                return@collectLatest
            }
            val g = currentGeometry
            val s = inspectSources
            val time = g.time(x).coerceAtMost(currentNow)
            val data = inspectDataAt(
                timestamp = time,
                bgReadings = s.readings,
                // No predictions on the home graph, so the time never snaps to one.
                predictions = emptyList(),
                iob = s.iob.iob,
                cob = s.cob.cob,
                actualBasal = s.basal.actualBasal,
                profileBasal = s.basal.profileBasal,
                boluses = s.treatments.boluses,
                carbs = s.treatments.carbs,
                calibrations = s.treatments.calibrations,
                therapyEvents = s.treatments.therapyEvents,
                runningModes = s.runningModes.segments
            )
            if (data.timestamp != currentPill?.timestamp) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPill(data, g.x(data.timestamp))
        }
    }

    // "Now" is the newest reading, also when the window is scrolled back.
    val description = allSorted.lastOrNull()?.let { last ->
        stringResource(
            UiStrings.redesign_graph_description,
            stringResource(UiStrings.redesign_graph_range_description, range.hours),
            stringResource(CoreUiStrings.value_with_unit, label(last.value), stringResource(if (units == GlucoseUnit.MMOL) CoreUiStrings.mmol else CoreUiStrings.mgdl))
        )
    } ?: stringResource(UiStrings.redesign_graph_description_no_value, stringResource(UiStrings.redesign_graph_range_description, range.hours))

    Canvas(
        modifier
            .onSizeChanged { size = it }
            .semantics {
                contentDescription = description
            }
            // Step 3: a horizontal drag scrolls the window. It starts after the touch slop and consumes the
            // moves, so the long press below is cancelled; once the long press has started, it consumes the
            // moves and this drag does not start. 24 h shows everything, so it does not scroll.
            .pointerInput(range) {
                if (range.hours >= HOME_GRAPH_SCROLL_HOURS) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { frozenAxis = currentAxis },
                    onDragEnd = { frozenAxis = null },
                    onDragCancel = { frozenAxis = null },
                    onHorizontalDrag = { change, dragPx ->
                        change.consume()
                        val g = currentGeometry
                        scrolledEnd = homeGraphDraggedEnd(currentNowEnd, scrolledEnd, range, dragPx, g.right - g.left)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        fingerX = offset.x
                    },
                    // Consumed, so the drag moves the line and not the page.
                    onDrag = { change, _ ->
                        change.consume()
                        fingerX = change.position.x
                    },
                    onDragEnd = { fingerX = null },
                    onDragCancel = { fingerX = null }
                )
            }
    ) {
        if (size == IntSize.Zero) return@Canvas
        val g = geometry
        val y = { mgdl: Double -> homeGraphY(mgdl, minMgdl, maxMgdl, g.top, g.bottom) }
        val hair = 1.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))

        // Target band between the low and the high mark, and the hour lines.
        val highY = y(lines[1].mgdl)
        val lowY = y(lines[2].mgdl)
        drawRect(general.bgInRange.copy(alpha = TARGET_BAND_ALPHA), Offset(g.left, highY), Size(g.right - g.left, lowY - highY))
        ticks.forEach { t ->
            val x = g.x(t)
            drawLine(colors.textPrimary.copy(alpha = 0.05f), Offset(x, g.top), Offset(x, g.bottom), hair)
        }

        // Running-mode band at the top, where the loop was not closed.
        runningModes.segments.forEach { segment ->
            val color = segment.mode.beltColor(elementColors)
            if (color == Color.Transparent) return@forEach
            val x0 = g.x(max(segment.startTime, start))
            val x1 = g.x(minOf(segment.endTime, end, now))
            if (x1 > x0) drawRoundRect(
                color, Offset(x0, BAND_TOP.toPx()), Size(x1 - x0, BAND_HEIGHT.toPx()),
                CornerRadius(BAND_CORNER.toPx())
            )
        }

        // Grey steps, then the coloured lines, then the target line.
        graduations.forEach { mgdl ->
            drawLine(colors.textPrimary.copy(alpha = 0.06f), Offset(g.left, y(mgdl)), Offset(g.right, y(mgdl)), hair)
        }
        lines.forEach { line ->
            drawLine(
                line.lineColor.copy(alpha = line.lineAlpha), Offset(g.left, y(line.mgdl)), Offset(g.right, y(line.mgdl)), hair,
                pathEffect = if (line.dashed) dash else null
            )
        }
        drawTargetLine(targetLine.targets.map { it.timestamp to toMgdl(it.value, units) }, g, now, y, colors.textPrimary.copy(alpha = 0.16f), targetAccent?.copy(alpha = 0.7f), hair)

        // Labels: the coloured ones moved apart so they never overlap, then the grey ones unless too close to one.
        val labelRight = labelColumn
        val minGap = lineLabels.maxOf { it.size.height }.toFloat()
        // The four lines and, last, the target label (lineLabels has them in that order).
        val lineCentres = spreadLabels(lines.map { y(it.mgdl) } + listOfNotNull(targetMgdl?.let { y(it) }), minGap, g.top, g.bottom)
        lineLabels.forEachIndexed { i, layout -> drawLabel(layout, labelRight, lineCentres[i]) }
        val keepAway = GREY_LABEL_DISTANCE.toPx()
        graduations.forEachIndexed { i, mgdl ->
            val centre = y(mgdl)
            if (lineCentres.none { abs(it - centre) < keepAway }) drawLabel(gradLabels[i], labelRight, centre)
        }

        // A smooth line through all the readings, coloured by range, broken where readings are missing.
        val centres = shown.map { Offset(g.x(it.timestamp), y(toMgdl(it.value, units))) }
        homeGraphRuns(shown.map { it.timestamp }).forEach { run ->
            if (run.last <= run.first) return@forEach
            val xs = FloatArray(run.count()) { centres[run.first + it].x }
            val ys = FloatArray(run.count()) { centres[run.first + it].y }
            val slopes = homeGraphTangents(xs, ys)
            for (k in 0 until xs.size - 1) {
                val third = (xs[k + 1] - xs[k]) / 3f
                val segment = Path().apply {
                    moveTo(xs[k], ys[k])
                    cubicTo(xs[k] + third, ys[k] + slopes[k] * third, xs[k + 1] - third, ys[k + 1] - slopes[k + 1] * third, xs[k + 1], ys[k + 1])
                }
                drawPath(segment, rangeColors.getValue(shown[run.first + k + 1].range).copy(alpha = LINE_ALPHA), style = Stroke(LINE_WIDTH.toPx()))
            }
        }

        // Dots: every reading up to 6 h, fewer on the long ranges, counted back from the newest reading of all
        // (not of the window), so they do not jump while the window scrolls. The newest is always there, and
        // hollow; a window scrolled back has no hollow dot.
        val dotStep = homeGraphDotStep(range)
        val radius = homeGraphDotRadius(g.right - g.left, range, MAX_DOT.toPx())
        shown.forEachIndexed { i, reading ->
            val color = rangeColors.getValue(reading.range)
            val fromNewest = allSorted.lastIndex - (firstShown + i)
            if (fromNewest == 0) {
                drawCircle(colors.graphCard, LAST_DOT.toPx(), centres[i])
                drawCircle(color, LAST_DOT.toPx(), centres[i], style = Stroke(LAST_DOT_STROKE.toPx()))
            } else if (fromNewest % dotStep == 0) drawCircle(color, radius, centres[i])
        }

        // Treatments, as on the stock BG graph: a manual bolus points down at the reading from above,
        // carbs point up at it from below. An SMB is a small triangle pointing up, only up to 12 h.
        val between = y(((lines[1].mgdl + lines[2].mgdl) / 2))
        fun readingY(time: Long): Float =
            shown.minByOrNull { abs(it.timestamp - time) }?.takeIf { abs(it.timestamp - time) <= MARKER_MATCH_MS }
                ?.let { y(toMgdl(it.value, units)) } ?: between
        val gap = MARKER_GAP.toPx()
        // On 3 h and 6 h the dose is written above a bolus triangle and the grams under a carbs triangle
        // (step 2, decided 10 Nov). No value for an SMB. The labels come formatted from the stock data.
        val showValues = range.hours <= TREATMENT_VALUE_MAX_HOURS
        val markerHeight = MARKER_WIDTH.toPx() * 1.1f
        val valueGap = TREATMENT_VALUE_GAP.toPx()
        treatments.boluses.filter { it.isValid && it.timestamp in start..end }.forEach { bolus ->
            val x = g.x(bolus.timestamp)
            if (bolus.bolusType == BolusType.SMB) {
                if (range.hours <= SMB_MAX_HOURS) drawTriangle(Offset(x, readingY(bolus.timestamp) + gap), up = true, width = SMB_MARKER.toPx(), color = elementColors.insulin)
            } else {
                val tipY = readingY(bolus.timestamp) - gap
                drawTriangle(Offset(x, tipY), up = false, width = MARKER_WIDTH.toPx(), color = elementColors.insulin)
                if (showValues) {
                    val layout = textMeasurer.measure(bolus.label, treatmentValueStyle.copy(color = elementColors.insulin))
                    drawTreatmentValue(layout, x, top = tipY - markerHeight - valueGap - layout.size.height)
                }
            }
        }
        treatments.carbs.filter { it.isValid && it.timestamp in start..end }.forEach { carbs ->
            val x = g.x(carbs.timestamp)
            val tipY = readingY(carbs.timestamp) + gap
            drawTriangle(Offset(x, tipY), up = true, width = MARKER_WIDTH.toPx(), color = elementColors.carbs)
            if (showValues) {
                val layout = textMeasurer.measure(carbs.label, treatmentValueStyle.copy(color = elementColors.carbs))
                drawTreatmentValue(layout, x, top = tipY + markerHeight + valueGap)
            }
        }

        // Inspect: dashed line at the shown reading, and a ring on it.
        pill?.let { data ->
            val x = g.x(data.timestamp)
            drawLine(
                colors.textPrimary.copy(alpha = 0.6f), Offset(x, g.top), Offset(x, g.bottom), hair,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
            )
            data.bg?.let { bg ->
                val centre = Offset(x, y(toMgdl(bg.value, units)))
                drawCircle(colors.graphCard, MARKER.toPx(), centre)
                drawCircle(rangeColors.getValue(bg.range), MARKER.toPx(), centre, style = Stroke(MARKER_STROKE.toPx()))
            }
        }

        // Hour labels under the plot, both edges included, kept inside the canvas.
        val labelTop = size.height - HOUR_LABELS.toPx()
        // A label that would touch the one before it is not drawn (narrow screen, large text).
        var lastRight = Float.NEGATIVE_INFINITY
        ticks.forEachIndexed { i, t ->
            val layout = hourLabels[i]
            val x = (g.x(t) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width.toFloat())
            if (x >= lastRight + HOUR_LABEL_GAP.toPx()) {
                drawText(layout, topLeft = Offset(x, labelTop))
                lastRight = x + layout.size.width
            }
        }
    }
}

/** What a long press reads: the same data as the stock pill. */
private class InspectSources(
    val readings: List<BgDataPoint>,
    val iob: IobGraphData,
    val cob: CobGraphData,
    val basal: BasalGraphData,
    val treatments: TreatmentGraphData,
    val runningModes: RunningModeGraphData
)

/**
 * The target as a step line: each value holds until the next change, the last one until now. A vertical
 * segment joins two values at each change, as on the stock graph.
 */
private fun DrawScope.drawTargetLine(points: List<Pair<Long, Double>>, g: PlotGeometry, now: Long, y: (Double) -> Float, color: Color, currentColor: Color?, width: Float) {
    targetLineSegments(points, g.start, g.end, now).forEach { segment ->
        // The step in force now, and the vertical segment into it, have the accent colour (a temporary or
        // adjusted target); the past ones stay grey.
        val segmentColor = if (segment.current && currentColor != null) currentColor else color
        drawLine(segmentColor, Offset(g.x(segment.startTime), y(segment.startMgdl)), Offset(g.x(segment.endTime), y(segment.endMgdl)), width)
    }
}

/** A triangle whose tip is [tip], pointing [up] or down, with a base [width] wide and 1.1 Ã— as high. */
private fun DrawScope.drawTriangle(tip: Offset, up: Boolean, width: Float, color: Color) {
    val height = width * 1.1f
    val baseY = if (up) tip.y + height else tip.y - height
    drawPath(
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x + width / 2f, baseY)
            lineTo(tip.x - width / 2f, baseY)
            close()
        },
        color
    )
}

/** The value of a treatment, centred on [centreX] with its top at [top], kept inside the canvas. */
private fun DrawScope.drawTreatmentValue(layout: TextLayoutResult, centreX: Float, top: Float) {
    val x = (centreX - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
    val y = top.coerceIn(0f, (size.height - layout.size.height).coerceAtLeast(0f))
    drawText(layout, topLeft = Offset(x, y))
}

/** A label right-aligned on [right], centred on [centreY]. */
private fun DrawScope.drawLabel(layout: TextLayoutResult, right: Float, centreY: Float) =
    drawText(layout, topLeft = Offset(right - layout.size.width, centreY - layout.size.height / 2f))

private const val HOUR_MS = 3_600_000L
private const val NOW_REFRESH_MS = 60_000L

/** As on the stock graph (`INSPECT_LINGER_MS` in `GraphInspect.kt`, private there). */
private const val INSPECT_LINGER_MS = 2_000L

/** SPEC 7: target band at 7 % on the home graph. */
private const val TARGET_BAND_ALPHA = 0.07f

private val LABEL_COLUMN = 28.dp
private val LABEL_GAP = 6.dp
private val PLOT_END = 12.dp
private val PLOT_TOP = 8.dp
private val PLOT_BOTTOM = 8.dp
private val HOUR_LABELS = 16.dp
private val BAND_TOP = 1.dp
private val BAND_HEIGHT = 5.dp
private val BAND_CORNER = 2.dp
private val MAX_DOT = 2.8.dp
private val LAST_DOT = 4.5.dp
private val LAST_DOT_STROKE = 2.dp
private val MARKER = 6.dp
private val MARKER_STROKE = 2.5.dp

/** The smooth line of the readings. */
private val LINE_WIDTH = 1.5.dp
private const val LINE_ALPHA = 0.55f

/** Treatment markers: base width, distance of the tip from the reading, and the SMB size. */
private val MARKER_WIDTH = 8.dp
private val SMB_MARKER = 5.dp
private val MARKER_GAP = 5.dp

/** A treatment belongs to the nearest reading within this time; else it sits in the middle of the target range. */
private const val MARKER_MATCH_MS = 15 * 60_000L

/** SMBs are too many beyond this range, so they are left out (manual boluses and carbs stay). */
private const val SMB_MAX_HOURS = 12

/** Treatment values are written on the 3 h and 6 h ranges only. */
private const val TREATMENT_VALUE_MAX_HOURS = 6

/** Space between a treatment triangle and its value. */
private val TREATMENT_VALUE_GAP = 2.dp

/** Least space between two grey graduations. */
private val GRADUATION_SPACING = 32.dp

/** A grey label is hidden closer than this to a coloured one (SPEC 3.6). */
private val GREY_LABEL_DISTANCE = 14.dp
private val PILL_TOP = 8.dp
private val PILL_EDGE = 4.dp
private val PLOT_SIDE = 17.dp
private val HOUR_LABEL_GAP = 4.dp
