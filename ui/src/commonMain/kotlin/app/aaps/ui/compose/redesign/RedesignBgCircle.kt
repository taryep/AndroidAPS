package app.aaps.ui.compose.redesign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.data.model.TrendArrow
import app.aaps.core.interfaces.overview.graph.BgInfoData
import app.aaps.core.ui.compose.stringResource
import app.aaps.core.ui.extensions.directionToIcon
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.components.bgSpokenDescription
import app.aaps.ui.compose.overview.ArcIndicator
import app.aaps.ui.compose.overview.toArcIndicator
import app.aaps.ui.compose.overview.toColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Test tag of the glucose circle. */
const val REDESIGN_BG_CIRCLE_TAG = "redesign_bg_circle"

/**
 * The glucose circle of the home page (SPEC 3.3): the same data and trend arc as the stock
 * `BgInfoSection`, drawn larger. A tap opens the graphs page.
 */
@Composable
fun RedesignBgCircle(
    bgInfo: BgInfoData?,
    timeAgoText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = BG_CIRCLE_SIZE
) {
    val colors = redesignColors()
    val openGraphs = stringResource(UiStrings.redesign_open_graphs)
    val clickable = Modifier
        .clip(CircleShape)
        .clickable(onClickLabel = openGraphs, onClick = onClick)
    if (bgInfo == null) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.padding(vertical = TRIANGLE_OVERFLOW).size(size).then(clickable)) {
            Text("---", fontSize = 48.sp, color = colors.textSecondary)
        }
        return
    }
    val bgColor = bgInfo.bgRange.toColor()
    val description = bgSpokenDescription(
        bgText = bgInfo.bgText,
        range = bgInfo.bgRange,
        trend = bgInfo.trendDescription,
        deltaText = bgInfo.deltaText,
        timeAgo = timeAgoText,
        isOutdated = bgInfo.isOutdated
    )
    // Not clipped: the trend triangles stand out of the ring. Only the touch layer at the end is round.
    // The padding keeps room for them above and below, so the gaps around the row are measured from
    // the tip of a triangle, and the row has the same height whatever the trend.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(vertical = TRIANGLE_OVERFLOW)
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                testTag = REDESIGN_BG_CIRCLE_TAG
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = RING_STROKE.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = bgColor.copy(alpha = RING_ALPHA),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            bgInfo.trendArrow?.toArcIndicator()?.let { arc ->
                drawArc(
                    color = bgColor,
                    startAngle = arc.centerAngle - arc.sweepAngle / 2,
                    sweepAngle = arc.sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawTrendTriangles(arc, bgColor, topLeft, arcSize, stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            bgInfo.deltaText?.let { delta ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val arrow = bgInfo.trendArrow?.takeIf { it != TrendArrow.NONE }
                    if (arrow != null) {
                        Icon(arrow.directionToIcon(), contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                    }
                    Text(delta, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, fontFeatureSettings = "tnum"))
                }
            }
            // The ring does not grow with the system font, so the value keeps its size, as in the stock circle.
            RedesignFontScaleCap(1f) {
                Text(
                    text = bgInfo.bgText,
                    style = TextStyle(
                        fontSize = 72.sp,
                        lineHeight = 74.sp,
                        letterSpacing = (-2).sp,
                        fontWeight = FontWeight.Bold,
                        color = bgColor,
                        fontFeatureSettings = "tnum",
                        textDecoration = if (bgInfo.isOutdated) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
            }
            Text(timeAgoText, style = TextStyle(fontSize = 13.sp, color = colors.textSecondary), maxLines = 1)
        }
        Box(Modifier.matchParentSize().then(clickable))
    }
}

/**
 * The 1 to 3 triangles that point out of the ring at the trend arc.
 *
 * Copied from the stock circle: `BgInfoSection.kt`, block "Triangles pointing outward" (upstream,
 * not changed here). Keep the two the same: same sizes, same spacing, same maths.
 */
private fun DrawScope.drawTrendTriangles(indicator: ArcIndicator, color: Color, topLeft: Offset, arcSize: Size, strokeWidth: Float) {
    val ringCenterX = topLeft.x + arcSize.width / 2
    val ringCenterY = topLeft.y + arcSize.height / 2
    val ringRadius = arcSize.width / 2
    val triHeight = strokeWidth * 1.2f
    val triHalfBase = strokeWidth * 1.6f
    val n = indicator.triangleCount
    val baseDist = ringRadius + strokeWidth * 0.2f
    // Same expressions the JDK's Math.toDegrees / Math.toRadians use, written out
    // so they work on every platform. Note the order: toRadians divides first.
    val angularSpacing = ((triHalfBase * 1.6 / ringRadius) * 180.0 / PI).toFloat()

    for (i in 0 until n) {
        val triAngle = indicator.centerAngle + (i - (n - 1) / 2f) * angularSpacing
        val triRad = triAngle.toDouble() / 180.0 * PI
        val dirX = cos(triRad).toFloat()
        val dirY = sin(triRad).toFloat()
        val perpX = -dirY
        val perpY = dirX
        val baseX = ringCenterX + baseDist * dirX
        val baseY = ringCenterY + baseDist * dirY
        val tipX = baseX + triHeight * dirX
        val tipY = baseY + triHeight * dirY
        drawPath(
            path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseX + triHalfBase * perpX, baseY + triHalfBase * perpY)
                lineTo(baseX - triHalfBase * perpX, baseY - triHalfBase * perpY)
                close()
            },
            color = color
        )
    }
}

val BG_CIRCLE_SIZE = 184.dp
private val RING_STROKE = 7.4.dp
private const val RING_ALPHA = 0.22f

/**
 * How far a trend triangle stands out of the circle. From [drawTrendTriangles]: the ring radius is
 * (size - stroke) / 2 and the tip is 0.2 + 1.2 strokes further out, so size / 2 + 0.9 stroke from
 * the centre (7.2 dp).
 */
private val TRIANGLE_OVERFLOW = RING_STROKE * 0.9f
