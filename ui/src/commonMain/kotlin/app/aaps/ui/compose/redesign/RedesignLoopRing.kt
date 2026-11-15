package app.aaps.ui.compose.redesign

import app.aaps.core.data.model.RM
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * How the ring of the glucose circle is drawn for a running mode (step 12): the pattern of the loop
 * icon. The home circle and the 4x2 widget use it.
 */
enum class RedesignLoopRing { SOLID, DASHES, LONG_DASHES, DOTS }

/** The tint of a running mode. The Loop slot, the circle ring and the widget use the same one. */
fun RM.Mode?.redesignLoopTint(colors: RedesignColors): RedesignTint = when (this) {
    RM.Mode.CLOSED_LOOP, RM.Mode.RESUME -> colors.closedLoop
    RM.Mode.OPEN_LOOP                   -> colors.openLoop
    RM.Mode.CLOSED_LOOP_LGS             -> colors.lgs
    RM.Mode.SUSPENDED_BY_USER,
    RM.Mode.SUSPENDED_BY_PUMP,
    RM.Mode.SUSPENDED_BY_DST            -> colors.suspended
    RM.Mode.DISCONNECTED_PUMP           -> colors.disconnected
    RM.Mode.DISABLED_LOOP               -> colors.disabled
    RM.Mode.SUPER_BOLUS                 -> colors.superBolus
    null                                -> colors.neutral
}

fun RM.Mode?.redesignLoopRing(): RedesignLoopRing = when (this) {
    RM.Mode.CLOSED_LOOP, RM.Mode.RESUME, RM.Mode.SUPER_BOLUS, null -> RedesignLoopRing.SOLID
    RM.Mode.OPEN_LOOP, RM.Mode.CLOSED_LOOP_LGS                     -> RedesignLoopRing.DASHES
    RM.Mode.SUSPENDED_BY_USER,
    RM.Mode.SUSPENDED_BY_PUMP,
    RM.Mode.SUSPENDED_BY_DST                                       -> RedesignLoopRing.LONG_DASHES
    RM.Mode.DISABLED_LOOP, RM.Mode.DISCONNECTED_PUMP               -> RedesignLoopRing.DOTS
}

/**
 * Dash and gap lengths (in px) of the ring on a circle of [radius], or null for a solid ring. The gap is
 * stretched so a whole number of dashes fits the circle: no short dash where the ring closes. A dot is
 * a near-zero dash drawn with a round cap ([roundCap]), so it is as wide as the stroke.
 */
fun RedesignLoopRing.intervals(radius: Float, stroke: Float): FloatArray? {
    val (on, off) = when (this) {
        RedesignLoopRing.SOLID       -> return null
        RedesignLoopRing.DASHES      -> stroke * 2f to stroke * 1.5f
        RedesignLoopRing.LONG_DASHES -> stroke * 5f to stroke * 1.5f
        RedesignLoopRing.DOTS        -> 0.01f to stroke * 2f
    }
    val circumference = 2f * PI.toFloat() * radius
    val count = (circumference / (on + off)).roundToInt().coerceAtLeast(1)
    return floatArrayOf(on, circumference / count - on)
}

val RedesignLoopRing.roundCap: Boolean get() = this == RedesignLoopRing.DOTS

/** The loop ring is dimmed, so the value and the trend arc stay in front. */
const val REDESIGN_LOOP_RING_ALPHA = 0.5f
