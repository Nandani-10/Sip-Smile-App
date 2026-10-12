package com.sipsmile.app.ui.scenes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sipsmile.app.data.Roster
import com.sipsmile.app.ui.chars.rememberCharPainter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Barnaby zig-zags between flowers; the flowers pop open as he visits them. */
@Composable
fun BeeScene(modifier: Modifier = Modifier) {
    val t = rememberSeconds()
    val hero = rememberCharPainter(Roster.BEE)
    val petals = listOf(Color(0xFFFFB6D9), Color(0xFFFFE066), Color(0xFFC8A2FF), Color(0xFFFF9EB5), Color(0xFFFFB48A))
    Canvas(modifier.fillMaxSize()) {
        val time = t.floatValue
        val w = size.width
        val h = size.height
        drawSky(Color(0xFFCFF0FF), Color(0xFFFFF7D8))
        drawGlow(Offset(w * 0.85f, h * 0.08f), w * 0.6f, Color(0xFFFFE9A0), 0.9f)
        drawDriftingClouds(time, 3, h * 0.05f, h * 0.28f, 0.01f, w * 0.15f, 0.9f, salt = 8f)
        drawHills(time, h * 0.64f, h * 0.012f, w * 0.9f, Color(0xFFD5F2C2), 1f)
        drawHills(time, h * 0.72f, h * 0.014f, w * 0.7f, Color(0xFFB2E6A8), 3f)

        fun beePos(tt: Float): Offset = Offset(
            w * (0.5f + 0.3f * sin(tt * 0.55f)),
            h * 0.33f + h * 0.05f * sin(tt * 1.9f) + h * 0.022f * sin(tt * 4.7f)
        )

        val bee = beePos(time)
        for (i in 0 until 5) {
            val fx = w * (0.12f + 0.19f * i)
            val near = exp(-(((bee.x - fx) / (w * 0.14f)) * ((bee.x - fx) / (w * 0.14f))))
            val r = w * 0.065f
            drawFlower(
                fx, h * (0.86f + 0.015f * hash(i, 2f)), h * (0.13f + 0.05f * hash(i, 1f)), r,
                petals[i % petals.size], Color(0xFFFFF3B0),
                sway = sin(time * 1.5f + i * 1.3f) * r * 0.35f,
                open = 0.75f + 0.5f * near
            )
        }

        // dashed flight path behind the bee
        for (k in 1..26 step 2) {
            val p = beePos(time - k * 0.13f)
            drawCircle(Color(0xFF8B6B3D), w * 0.006f, p, alpha = (1f - k / 27f) * 0.7f)
        }

        val hs = min(w, h * 0.5f) * 0.52f
        // honey sparkles dripping from the bee
        for (i in 0 until 3) {
            val p = frac(time * 0.5f + i / 3f)
            drawDropShape(bee.x + (i - 1) * hs * 0.16f, bee.y + hs * 0.45f + p * h * 0.1f, w * 0.012f, Color(0xFFFFB300), alpha = 1f - p)
        }
        hero.draw(
            this, bee, hs,
            happy = (time % 7f) > 4.5f,
            flap = 0.5f + 0.5f * sin(time * 45f),
            blink = blinkAt(time),
            rotation = 8f * sin(time * 1.9f)
        )
        drawSparkles(time, 18, 9f, Color(0xFFFFE066), w * 0.011f, 0.1f, 0.8f)
    }
}

/** Lumi rocks gently under twinkling stars; a shooting star streaks by and she yawns now and then. */
@Composable
fun MoonScene(modifier: Modifier = Modifier) {
    val t = rememberSeconds()
    val hero = rememberCharPainter(Roster.MOON)
    Canvas(modifier.fillMaxSize()) {
        val time = t.floatValue
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF1B1F4B), Color(0xFF353B8C), Color(0xFF7B5FB0)),
                startY = 0f, endY = h
            )
        )
        // stars
        for (i in 0 until 46) {
            val x = hash(i, 1f) * w
            val y = hash(i, 2f) * h * 0.62f
            val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * (1f + 2f * hash(i, 3f)) + i))
            val r = w * (0.004f + 0.009f * hash(i, 4f))
            if (hash(i, 5f) > 0.6f) drawStar(x, y, r * 2.2f, Color(0xFFFFF3B0), alpha = a)
            else drawCircle(Color.White, r, Offset(x, y), alpha = a)
        }
        // shooting star
        val p = frac(time / 5f)
        if (p < 0.22f) {
            val q = p / 0.22f
            val start = Offset(w * 0.95f, h * 0.05f)
            val end = Offset(w * 0.35f, h * 0.3f)
            val head = Offset(start.x + (end.x - start.x) * q, start.y + (end.y - start.y) * q)
            for (j in 0..10) {
                val back = j * 0.012f
                val pos = Offset(head.x - (end.x - start.x) * back, head.y - (end.y - start.y) * back)
                drawCircle(Color.White, w * 0.008f * (1f - j / 12f), pos, alpha = (1f - j / 11f) * (1f - q * 0.6f))
            }
            drawStar(head.x, head.y, w * 0.02f, Color.White)
        }
        drawDriftingClouds(time, 3, h * 0.5f, h * 0.7f, 0.01f, w * 0.2f, 0.16f, salt = 9f, color = Color(0xFFB8A9FF))

        val hs = min(w, h * 0.5f) * 0.66f
        val cx = w * 0.5f
        val cy = h * 0.34f + sin(time * 1.2f) * h * 0.012f
        drawGlow(Offset(cx, cy), hs * 0.95f, Color(0xFFFFF3B0), 0.3f + 0.1f * sin(time * 1.5f))
        val yawn = frac(time / 8f).let { it in 0.4f..0.55f }
        hero.draw(
            this, Offset(cx, cy), hs,
            happy = (time % 10f) > 7.5f,
            flap = 0.5f + 0.5f * sin(time * 2f),
            blink = blinkAt(time),
            extra = yawn,
            rotation = 6f * sin(time * 0.8f)
        )
        drawSparkles(time, 14, 11f, Color(0xFFFFF3B0), w * 0.011f, 0f, 0.7f)
    }
}

/** Dewy bounces on a pond: squash and stretch, ripples, bubbles, a jumping fish and a popping heart. */
@Composable
fun DropScene(modifier: Modifier = Modifier) {
    val t = rememberSeconds()
    val hero = rememberCharPainter(Roster.DROP)
    Canvas(modifier.fillMaxSize()) {
        val time = t.floatValue
        val w = size.width
        val h = size.height
        drawSky(Color(0xFFBEE9FF), Color(0xFFEAF8FF))
        drawGlow(Offset(w * 0.2f, h * 0.08f), w * 0.55f, Color(0xFFFFF3B0), 0.8f)
        drawDriftingClouds(time, 3, h * 0.05f, h * 0.3f, 0.011f, w * 0.15f, 0.9f, salt = 12f)

        val pondTop = h * 0.5f
        // back water
        drawHills(time, pondTop, h * 0.006f, w * 0.35f, Color(0xFF7DD3FF), 0f, 1.5f)

        // bounce cycle
        val period = 1.7f
        val ph = frac(time / period)
        val air = 4f * ph * (1f - ph)
        val squash = (1f - air * 9f).coerceIn(0f, 1f)
        val sy = 1f - 0.2f * squash + 0.08f * air
        val sx = 1f + 0.18f * squash - 0.04f * air
        val hs = min(w, h * 0.5f) * 0.5f
        val baseY = pondTop + h * 0.035f
        val cx = w * 0.5f
        val cy = baseY - 0.45f * hs * sy - h * 0.17f * air

        // ripples
        for (k in 0..1) {
            val age = (ph + k) * period
            if (age < 1.8f) {
                val rad = age * w * 0.22f
                drawOval(
                    Color.White, Offset(cx - rad, baseY - rad * 0.11f), Size(rad * 2f, rad * 0.22f),
                    alpha = (1f - age / 1.8f) * 0.8f, style = Stroke(width = w * 0.006f)
                )
            }
        }

        hero.draw(
            this, Offset(cx, cy), hs,
            happy = ph < 0.45f,
            blink = blinkAt(time),
            scaleX = sx, scaleY = sy
        )

        // front water covers the drop's feet a little
        drawHills(time, pondTop + h * 0.06f, h * 0.008f, w * 0.5f, Color(0xFF47A8F0), 1f, -1.2f)

        // bubbles
        for (i in 0 until 9) {
            val p = frac(time * 0.12f + hash(i, 2f))
            val x = w * (0.08f + 0.84f * hash(i, 1f)) + sin(time * 2f + i) * w * 0.01f
            val y = h * 0.97f - p * (h * 0.97f - pondTop - h * 0.03f)
            val r = w * (0.012f + 0.016f * hash(i, 3f))
            drawCircle(Color.White, r, Offset(x, y), alpha = 0.22f)
            drawCircle(Color.White, r, Offset(x, y), alpha = 0.6f, style = Stroke(width = r * 0.18f))
            drawCircle(Color.White, r * 0.22f, Offset(x - r * 0.4f, y - r * 0.4f), alpha = 0.8f)
        }

        // a fish jumps out every few seconds
        val q = frac(time / 6f)
        if (q < 0.3f) {
            val s = q / 0.3f
            val fxp = w * (0.12f + 0.76f * s)
            val fyp = pondTop + h * 0.03f - sin(s * PI.toFloat()) * h * 0.16f
            val dxp = w * 0.76f
            val dyp = -kotlin.math.cos(s * PI.toFloat()) * PI.toFloat() * h * 0.16f
            val ang = Math.toDegrees(atan2(dyp.toDouble(), dxp.toDouble())).toFloat()
            withTransform({
                translate(fxp, fyp)
                rotate(ang, Offset.Zero)
            }) {
                val fw = w * 0.07f
                drawOval(Color(0xFFFF9F5A), Offset(-fw, -fw * 0.5f), Size(fw * 2f, fw))
                val tail = Path().apply {
                    moveTo(-fw * 0.8f, 0f)
                    lineTo(-fw * 1.6f, -fw * 0.55f)
                    lineTo(-fw * 1.6f, fw * 0.55f)
                    close()
                }
                drawPath(tail, Color(0xFFFF8A3D))
                drawCircle(Color.White, fw * 0.14f, Offset(fw * 0.5f, -fw * 0.1f))
                drawCircle(Color(0xFF2B2A4C), fw * 0.07f, Offset(fw * 0.54f, -fw * 0.1f))
            }
        }

        // heart pops after each landing
        if (ph in 0.02f..0.5f) {
            val a = 1f - (ph - 0.02f) / 0.48f
            drawHeart(cx + w * 0.2f, baseY - hs * 0.7f - ph * h * 0.12f, w * 0.03f, Color(0xFFFF8FB8), alpha = a)
        }
        drawSparkles(time, 14, 13f, Color.White, w * 0.011f, 0f, 0.5f)
    }
}
