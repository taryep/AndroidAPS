package app.aaps.ui.compose.redesign

import app.aaps.core.data.configuration.Constants
import app.aaps.core.data.model.GlucoseUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Instant

/**
 * The maths of the home graph (SPEC 3.6), without any drawing, so it can be tested on every target.
 * Glucose values here are in mg/dL; the graph converts mmol/L values before it uses them.
 */

/** The time ranges of the home graph and the step of their hour labels. */
enum class HomeGraphRange(val hours: Int, val labelStepHours: Int) {
    H3(3, 1),
    H6(6, 1),
    H12(12, 2),
    H18(18, 3),
    H24(24, 4);

    companion object {

        val DEFAULT = H6
    }
}

/** Bottom of the Y axis while no reading is lower. */
const val HOME_GRAPH_MIN_MGDL = 40.0

/** Top of the Y axis while no reading is higher. */
const val HOME_GRAPH_MAX_MGDL = 280.0

/** The axis grows by this step when a reading is above [HOME_GRAPH_MAX_MGDL]. */
private const val MAX_STEP_MGDL = 50.0

/** The bottom of the axis goes down by this step when a reading is below [HOME_GRAPH_MIN_MGDL]. */
private const val MIN_STEP_MGDL = 10.0

/** The fixed very low and very high lines: the same limits as `BgRange` and the TIR statistics. */
const val HOME_GRAPH_VERY_LOW_MGDL = Constants.STATS_RANGE_VERY_LOW_MMOL * Constants.MMOLL_TO_MGDL
const val HOME_GRAPH_VERY_HIGH_MGDL = Constants.STATS_RANGE_VERY_HIGH_MMOL * Constants.MMOLL_TO_MGDL

private const val HOUR_MS = 3_600_000L

/** A value in the user's units, in mg/dL. */
fun toMgdl(value: Double, units: GlucoseUnit): Double = if (units == GlucoseUnit.MMOL) value * Constants.MMOLL_TO_MGDL else value

/** A value in mg/dL, in the user's units. */
fun fromMgdl(mgdl: Double, units: GlucoseUnit): Double = if (units == GlucoseUnit.MMOL) mgdl / Constants.MMOLL_TO_MGDL else mgdl

/**
 * End of the window: the next full hour in local time (now 00:55 → 01:00). At a full hour exactly,
 * that hour.
 */
fun homeGraphWindowEnd(now: Long, timeZone: TimeZone): Long {
    val local = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone)
    val hourStart = LocalDateTime(local.year, local.month, local.day, local.hour, 0).toInstant(timeZone).toEpochMilliseconds()
    return if (hourStart == now) now else hourStart + HOUR_MS
}

/** Times of the hour labels, both edges included, from the start of the window to [end]. */
fun homeGraphHourTicks(end: Long, range: HomeGraphRange): List<Long> =
    (0..range.hours step range.labelStepHours).map { end - (range.hours - it) * HOUR_MS }

/** Top of the Y axis: [HOME_GRAPH_MAX_MGDL], or the next multiple of 50 above the highest reading. */
fun homeGraphMaxMgdl(readingsMgdl: List<Double>): Double {
    val highest = readingsMgdl.maxOrNull() ?: return HOME_GRAPH_MAX_MGDL
    return if (highest <= HOME_GRAPH_MAX_MGDL) HOME_GRAPH_MAX_MGDL else ceil(highest / MAX_STEP_MGDL) * MAX_STEP_MGDL
}

/** Bottom of the Y axis: [HOME_GRAPH_MIN_MGDL], or the multiple of 10 under the lowest reading (never below 0). */
fun homeGraphMinMgdl(readingsMgdl: List<Double>): Double {
    val lowest = readingsMgdl.minOrNull() ?: return HOME_GRAPH_MIN_MGDL
    return if (lowest >= HOME_GRAPH_MIN_MGDL) HOME_GRAPH_MIN_MGDL else (floor(lowest / MIN_STEP_MGDL) * MIN_STEP_MGDL).coerceAtLeast(0.0)
}

/**
 * The step of the grey graduations, in mg/dL: the smallest round step that leaves at least
 * [minSpacingPx] between two of them on a plot [plotHeightPx] high. With the default axis (40 to 280)
 * and a normal plot it is 50 mg/dL (2 mmol/L); a taller axis or a shorter plot gives a coarser step,
 * as when the stock graph is zoomed out.
 */
fun homeGraphStepMgdl(units: GlucoseUnit, minMgdl: Double, maxMgdl: Double, plotHeightPx: Float, minSpacingPx: Float): Double {
    val candidates =
        if (units == GlucoseUnit.MMOL) listOf(1.0, 2.0, 5.0, 10.0).map { it * Constants.MMOLL_TO_MGDL }
        else listOf(10.0, 20.0, 25.0, 50.0, 100.0, 200.0, 500.0)
    val span = maxMgdl - minMgdl
    return candidates.firstOrNull { it / span * plotHeightPx >= minSpacingPx } ?: candidates.last()
}

/**
 * The faint grey graduations, in mg/dL: the multiples of [stepMgdl] inside the axis. One on a
 * coloured line (less than 1 mg/dL away) is left out: the line is already there.
 */
fun homeGraphGraduations(minMgdl: Double, maxMgdl: Double, stepMgdl: Double, linesMgdl: List<Double>): List<Double> =
    generateSequence(floor(minMgdl / stepMgdl) * stepMgdl + stepMgdl) { it + stepMgdl }
        .takeWhile { it <= maxMgdl + 0.001 }
        .filter { it > minMgdl }
        .filter { step -> linesMgdl.none { abs(it - step) < 1.0 } }
        .toList()

/** Vertical position of [mgdl] between [top] and [bottom], in pixels. Values outside the axis stay on its edge. */
fun homeGraphY(mgdl: Double, minMgdl: Double, maxMgdl: Double, top: Float, bottom: Float): Float {
    val share = (mgdl.coerceIn(minMgdl, maxMgdl) - minMgdl) / (maxMgdl - minMgdl)
    return bottom - (share * (bottom - top)).toFloat()
}

/**
 * Every how many readings a dot is drawn, counted back from the newest (which is always drawn):
 * all of them up to 6 h, then fewer, so a long range stays light. The smoothed line has them all.
 */
fun homeGraphDotStep(range: HomeGraphRange): Int = when (range) {
    HomeGraphRange.H3, HomeGraphRange.H6   -> 1
    HomeGraphRange.H12                     -> 3
    HomeGraphRange.H18, HomeGraphRange.H24 -> 6
}

/** A gap longer than this between two readings breaks the line. */
const val HOME_GRAPH_MAX_GAP_MS = 16 * 60_000L

/** The runs of readings with no gap longer than [maxGapMs] between them, as index ranges. [times] must be sorted. */
fun homeGraphRuns(times: List<Long>, maxGapMs: Long = HOME_GRAPH_MAX_GAP_MS): List<IntRange> {
    if (times.isEmpty()) return emptyList()
    val runs = mutableListOf<IntRange>()
    var start = 0
    for (i in 1 until times.size) {
        if (times[i] - times[i - 1] > maxGapMs) {
            runs += start until i
            start = i
        }
    }
    runs += start until times.size
    return runs
}

/**
 * Slopes for a smooth curve through the points ([xs] rising), so it never goes beyond the points
 * at a peak or a dip: the slope there is 0, elsewhere the slope between the two neighbours.
 */
fun homeGraphTangents(xs: FloatArray, ys: FloatArray): FloatArray {
    val n = xs.size
    val slopes = FloatArray(n)
    for (i in 1 until n - 1) {
        val before = ys[i] - ys[i - 1]
        val after = ys[i + 1] - ys[i]
        // A peak or a dip stays flat; elsewhere the slope across the two neighbours.
        slopes[i] = if (before * after <= 0f) 0f else (ys[i + 1] - ys[i - 1]) / (xs[i + 1] - xs[i - 1])
    }
    return slopes
}

/**
 * Centres of the line labels, moved apart so two labels never overlap: at least [minGap] between
 * two centres, all of them kept between [top] and [bottom]. The order stays the same. Input and
 * output are in the same order as [centres].
 */
fun spreadLabels(centres: List<Float>, minGap: Float, top: Float, bottom: Float): List<Float> {
    if (centres.isEmpty()) return centres
    val order = centres.indices.sortedBy { centres[it] }
    val placed = FloatArray(order.size) { centres[order[it]] }
    // Downwards: each label at least minGap under the one above it.
    for (i in 1 until placed.size) placed[i] = max(placed[i], placed[i - 1] + minGap)
    // Then upwards from the bottom edge, if the last one went too low.
    placed[placed.size - 1] = min(placed[placed.size - 1], bottom)
    for (i in placed.size - 2 downTo 0) placed[i] = min(placed[i], placed[i + 1] - minGap)
    // And never above the top edge (only when they cannot all fit).
    for (i in placed.indices) placed[i] = max(placed[i], top)
    val result = FloatArray(centres.size)
    order.forEachIndexed { sortedIndex, original -> result[original] = placed[sortedIndex] }
    return result.toList()
}

/** Dot radius: [maxRadius], or less when the readings are closer (42 % of the space between two readings). */
fun homeGraphDotRadius(plotWidth: Float, range: HomeGraphRange, maxRadius: Float): Float {
    // One reading every 5 minutes; on long ranges only some of them get a dot.
    val readingsInWindow = range.hours * 12 / homeGraphDotStep(range)
    return min(maxRadius, 0.42f * plotWidth / readingsInWindow)
}

/**
 * One straight part of the target line, in time and mg/dL: a step (same value at both ends) or the
 * vertical segment of a change (same time at both ends). [current] is true for the step in force now
 * and for the vertical segment that leads into it.
 */
data class TargetLineSegment(val startTime: Long, val startMgdl: Double, val endTime: Long, val endMgdl: Double, val current: Boolean)

/**
 * The target line of the home graph between [start] and [end] (SPEC 3.6). Each value holds until the next
 * change, the last one until [now]. At each change a vertical segment joins the old and the new value, as
 * the stock graph does.
 *
 * @param points time and target in mg/dL, in time order, as `buildTargetLine` gives them (it repeats the
 *   last value at the end of the graph)
 */
fun targetLineSegments(points: List<Pair<Long, Double>>, start: Long, end: Long, now: Long): List<TargetLineSegment> {
    // The step in force: the points from the last change at or before now, with the same value. The
    // repeated last value is not a change, so it belongs to that step.
    var firstCurrent = points.indexOfLast { it.first <= now }
    while (firstCurrent > 0 && points[firstCurrent - 1].second == points[firstCurrent].second) firstCurrent--
    var lastCurrent = firstCurrent
    while (firstCurrent >= 0 && lastCurrent + 1 < points.size && points[lastCurrent + 1].second == points[firstCurrent].second) lastCurrent++
    val segments = mutableListOf<TargetLineSegment>()
    points.forEachIndexed { i, (time, mgdl) ->
        val current = firstCurrent >= 0 && i in firstCurrent..lastCurrent
        val previous = points.getOrNull(i - 1)
        if (previous != null && previous.second != mgdl && time in start..end)
            segments += TargetLineSegment(time, previous.second, time, mgdl, current)
        val from = max(time, start)
        val to = min(points.getOrNull(i + 1)?.first ?: now, end)
        if (to > from) segments += TargetLineSegment(from, mgdl, to, mgdl, current)
    }
    return segments
}
