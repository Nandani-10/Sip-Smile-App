package com.sipsmile.app.ui.scenes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

const val PI_F = PI.toFloat()

/** Seconds since this composable appeared. Reading it inside a Canvas only redraws, it does not recompose. */
@Composable
fun rememberSeconds(): FloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                seconds.floatValue = (now - start) / 1_000_000_000f
            }
        }
    }
    return seconds
}

fun frac(x: Float): Float = x - floor(x)

/** Deterministic pseudo random number in 0..1 for (index, salt). */
fun hash(i: Int, salt: Float = 0f): Float = frac(sin(i * 127.1f + salt * 311.7f + 17.3f) * 43758.547f)

fun smooth(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** 0 -> 1 -> 0 pulse. */
fun pulse(x: Float): Float = 0.5f - 0.5f * cos(x * 2f * PI_F)

/** Blink amount (0 open .. 1 closed) with a quick blink every [period] seconds. */
fun blinkAt(t: Float, period: Float = 3.7f, offset: Float = 0f): Float {
    val p = frac((t + offset) / period)
    return if (p > 0.94f) 1f - abs(p - 0.97f) / 0.03f else 0f
}

fun DrawScope.drawSky(top: Color, bottom: Color) {
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = size.height))
}

fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color, alpha: Float = 0.6f) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/** A fluffy cloud made of circles. [s] is roughly the cloud's half width. */
fun DrawScope.drawCloudShape(cx: Float, cy: Float, s: Float, alpha: Float = 1f, color: Color = Color.White) {
    val c = color.copy(alpha = alpha)
    drawCircle(c, s * 0.42f, Offset(cx - s * 0.5f, cy + s * 0.05f))
    drawCircle(c, s * 0.55f, Offset(cx - s * 0.05f, cy - s * 0.18f))
    drawCircle(c, s * 0.40f, Offset(cx + s * 0.5f, cy + s * 0.04f))
    drawRoundRectSimple(cx - s * 0.9f, cy - s * 0.02f, s * 1.8f, s * 0.45f, c)
}

private fun DrawScope.drawRoundRectSimple(x: Float, y: Float, w: Float, h: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
    )
}

/** Clouds drifting to the right at different speeds. */
fun DrawScope.drawDriftingClouds(
    t: Float,
    count: Int,
    yTop: Float,
    yBottom: Float,
    speed: Float,
    sizeBase: Float,
    alpha: Float,
    salt: Float = 0f,
    color: Color = Color.White,
) {
    val w = size.width
    for (i in 0 until count) {
        val p = frac(hash(i, salt + 1f) + t * speed * (0.6f + 0.8f * hash(i, salt + 2f)))
        val x = (p * 1.6f - 0.3f) * w
        val y = yTop + (yBottom - yTop) * hash(i, salt + 3f)
        val s = sizeBase * (0.7f + 0.6f * hash(i, salt + 4f))
        drawCloudShape(x, y, s, alpha, color)
    }
}

/** Four-point twinkle star (unit size, scaled when drawn). */
val unitStar: Path = Path().apply {
    moveTo(0f, -1f)
    lineTo(0.22f, -0.22f)
    lineTo(1f, 0f)
    lineTo(0.22f, 0.22f)
    lineTo(0f, 1f)
    lineTo(-0.22f, 0.22f)
    lineTo(-1f, 0f)
    lineTo(-0.22f, -0.22f)
    close()
}

fun DrawScope.drawStar(cx: Float, cy: Float, r: Float, color: Color, alpha: Float = 1f, rotation: Float = 0f) {
    withTransform({
        translate(cx, cy)
        rotate(rotation, Offset.Zero)
        scale(r, r, Offset.Zero)
    }) {
        drawPath(unitStar, color, alpha = alpha)
    }
}

/** Soft twinkling sparkles scattered over the screen. Shared by every scene. */
fun DrawScope.drawSparkles(
    t: Float,
    count: Int = 16,
    salt: Float = 0f,
    color: Color = Color.White,
    maxRadius: Float = 14f,
    yMin: Float = 0f,
    yMax: Float = 1f,
) {
    val w = size.width
    val h = size.height
    for (i in 0 until count) {
        val x = hash(i, salt + 5f) * w
        val y = (yMin + (yMax - yMin) * hash(i, salt + 6f)) * h
        val speed = 0.8f + 1.6f * hash(i, salt + 7f)
        val a = (0.5f + 0.5f * sin(t * speed + i * 1.7f)).coerceIn(0f, 1f)
        val r = maxRadius * (0.35f + 0.65f * hash(i, salt + 8f)) * (0.4f + 0.6f * a)
        drawStar(x, y, r, color, alpha = a * 0.9f, rotation = 0f)
    }
}

fun heartPath(cx: Float, cy: Float, s: Float): Path = Path().apply {
    moveTo(cx, cy + s)
    cubicTo(cx - s * 1.3f, cy + s * 0.2f, cx - s * 1.2f, cy - s * 1.0f, cx, cy - s * 0.35f)
    cubicTo(cx + s * 1.2f, cy - s * 1.0f, cx + s * 1.3f, cy + s * 0.2f, cx, cy + s)
    close()
}

fun DrawScope.drawHeart(cx: Float, cy: Float, s: Float, color: Color, alpha: Float = 1f) {
    drawPath(heartPath(cx, cy, s), color, alpha = alpha)
}

/** A simple flower with 5 petals on a stem. [open] 0..1 scales the petals ("pops open"). */
fun DrawScope.drawFlower(
    x: Float,
    groundY: Float,
    stemHeight: Float,
    r: Float,
    petal: Color,
    center: Color,
    sway: Float,
    open: Float = 1f,
    leaf: Color = Color(0xFF7CCB8B),
    stem: Color = Color(0xFF68BC7C),
) {
    val hx = x + sway
    val hy = groundY - stemHeight
    // stem (bent a little towards the head)
    val p = Path().apply {
        moveTo(x, groundY)
        cubicTo(x, groundY - stemHeight * 0.4f, hx, groundY - stemHeight * 0.6f, hx, hy)
    }
    drawPath(p, stem, style = Stroke(width = r * 0.28f, cap = StrokeCap.Round))
    drawOval(
        leaf,
        topLeft = Offset(x + r * 0.1f, groundY - stemHeight * 0.45f),
        size = Size(r * 0.9f, r * 0.42f)
    )
    val pr = r * 0.55f * (0.3f + 0.7f * open)
    for (k in 0 until 5) {
        val a = k * 2f * PI_F / 5f - PI_F / 2f
        drawCircle(petal, pr, Offset(hx + cos(a) * r * 0.62f * open, hy + sin(a) * r * 0.62f * open))
    }
    drawCircle(center, r * 0.38f, Offset(hx, hy))
}

/** Little background bird: two curved wings. */
fun DrawScope.drawMiniBird(cx: Float, cy: Float, s: Float, flap: Float, color: Color, alpha: Float = 1f) {
    val lift = (flap * 2f - 1f) * s * 0.55f
    val left = Path().apply {
        moveTo(cx - s, cy - lift)
        cubicTo(cx - s * 0.6f, cy + s * 0.15f - lift * 0.4f, cx - s * 0.3f, cy + s * 0.15f, cx, cy)
    }
    val right = Path().apply {
        moveTo(cx + s, cy - lift)
        cubicTo(cx + s * 0.6f, cy + s * 0.15f - lift * 0.4f, cx + s * 0.3f, cy + s * 0.15f, cx, cy)
    }
    val st = Stroke(width = s * 0.28f, cap = StrokeCap.Round)
    drawPath(left, color, alpha = alpha, style = st)
    drawPath(right, color, alpha = alpha, style = st)
    drawCircle(color, s * 0.22f, Offset(cx, cy + s * 0.05f), alpha = alpha)
}

/** Raindrop / water-drop shape. */
fun DrawScope.drawDropShape(cx: Float, cy: Float, s: Float, color: Color, alpha: Float = 1f) {
    val p = Path().apply {
        moveTo(cx, cy - s)
        cubicTo(cx + s * 0.2f, cy - s * 0.5f, cx + s * 0.8f, cy, cx + s * 0.8f, cy + s * 0.35f)
        cubicTo(cx + s * 0.8f, cy + s * 0.85f, cx - s * 0.8f, cy + s * 0.85f, cx - s * 0.8f, cy + s * 0.35f)
        cubicTo(cx - s * 0.8f, cy, cx - s * 0.2f, cy - s * 0.5f, cx, cy - s)
        close()
    }
    drawPath(p, color, alpha = alpha)
}

/** Rolling hills along the bottom of the screen. */
fun DrawScope.drawHills(t: Float, baseY: Float, amp: Float, wavelength: Float, color: Color, phase: Float = 0f, drift: Float = 0f) {
    val w = size.width
    val h = size.height
    val p = Path()
    p.moveTo(0f, h)
    p.lineTo(0f, baseY)
    var x = 0f
    while (x <= w + 8f) {
        val y = baseY + sin((x / wavelength) * 2f * PI_F + phase + t * drift) * amp
        p.lineTo(x, y)
        x += 8f
    }
    p.lineTo(w, h)
    p.close()
    drawPath(p, color)
}
