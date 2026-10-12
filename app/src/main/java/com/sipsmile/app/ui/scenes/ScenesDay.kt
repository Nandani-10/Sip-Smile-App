package com.sipsmile.app.ui.scenes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sipsmile.app.data.Roster
import com.sipsmile.app.ui.chars.rememberCharPainter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Pip and a V-shaped flock fly across a pink-blue sky. */
@Composable
fun BirdScene(modifier: Modifier = Modifier) {
    val t = rememberSeconds()
    val hero = rememberCharPainter(Roster.BIRD)
    Canvas(modifier.fillMaxSize()) {
        val time = t.floatValue
        val w = size.width
        val h = size.height
        drawSky(Color(0xFF9AD8FA), Color(0xFFFFE6F2))
        drawGlow(Offset(w * 0.82f, h * 0.1f), w * 0.6f, Color(0xFFFFF3B0), 0.9f)
        drawDriftingClouds(time, 4, h * 0.08f, h * 0.5f, 0.012f, w * 0.16f, 0.9f, salt = 1f)

        // the flock (V formation) crossing the sky
        val fx = (frac(time * 0.055f) * 1.5f - 0.25f) * w
        val fy = h * 0.17f + sin(time * 1.1f) * h * 0.012f
        for (i in 0 until 7) {
            val row = (i + 1) / 2
            val side = if (i % 2 == 0) -1f else 1f
            val x = fx - row * w * 0.075f
            val y = fy + side * row * h * 0.02f + row * h * 0.014f
            val flap = 0.5f + 0.5f * sin(time * 9f + i * 0.7f)
            drawMiniBird(x, y, w * 0.045f, flap, Color(0xFF5FB3D3))
        }

        // Pip, the star of the show
        val hs = min(w, h * 0.5f) * 0.62f
        val cx = w * 0.5f + sin(time * 0.8f) * w * 0.1f
        val cy = h * 0.37f + sin(time * 1.6f) * h * 0.018f
        hero.draw(
            this, Offset(cx, cy), hs,
            happy = (time % 9f) > 6f,
            flap = 0.5f + 0.5f * sin(time * 6f),
            blink = blinkAt(time),
            rotation = 5f * sin(time * 0.8f)
        )

        // feathers floating down
        for (i in 0 until 5) {
            val p = frac(time * 0.05f + hash(i, 1f))
            val x = hash(i, 2f) * w + sin(time * 1.1f + i * 2f) * w * 0.04f
            val y = h * 0.05f + p * h * 0.75f
            val r = w * 0.022f
            withTransform({
                translate(x, y)
                rotate(sin(time + i) * 40f + i * 30f, Offset.Zero)
            }) {
                drawOval(Color.White, Offset(-r * 0.25f, -r), Size(r * 0.5f, r * 2f), alpha = 0.85f * (1f - p * 0.5f))
            }
        }
        drawSparkles(time, 14, 1f, Color.White, w * 0.012f)
    }
}

/** Flutter and friends flutter above a flower meadow. */
@Composable
fun ButterflyScene(modifier: Modifier = Modifier) {
    val t = rememberSeconds()
    val hero = rememberCharPainter(Roster.BUTTERFLY)
    val petals = listOf(Color(0xFFFFB6D9), Color(0xFFFFE066), Color(0xFFC8A2FF), Color(0xFFFFB48A))
    Canvas(modifier.fillMaxSize()) {
        val time = t.floatValue
        val w = size.width
        val h = size.height
        drawSky(Color(0xFFBFEBFF), Color(0xFFFFF1D6))
        drawGlow(Offset(w * 0.2f, h * 0.1f), w * 0.55f, Color(0xFFFFF3B0), 0.8f)
        drawDriftingClouds(time, 3, h * 0.06f, h * 0.35f, 0.01f, w * 0.15f, 0.9f, salt = 2f)
        drawHills(time, h * 0.62f, h * 0.012f, w * 0.9f, Color(0xFFCFF0C8), 0.5f)
        drawHills(time, h * 0.68f, h * 0.014f, w * 0.7f, Color(0xFFA9E3B1), 2f)
        drawHills(time, h * 0.76f, h * 0.012f, w * 0.8f, Color(0xFF8ED79C), 4f)

        for (i in 0 until 8) {
            val x = (i + 0.5f) / 8f * w
            val ground = h * (0.8f + 0.04f * hash(i, 1f))
            val stem = h * (0.07f + 0.06f * hash(i, 2f))
            val r = w * (0.04f + 0.016f * hash(i, 3f))
            drawFlower(
                x, ground, stem, r,
                petals[i % petals.size],
                if (i % 2 == 0) Color(0xFFFFF3B0) else Color(0xFFFF8F6B),
                sway = sin(time * 1.4f + i) * r * 0.4f
            )
        }

        // little butterflies
        for (i in 0 until 4) {
            val bx = w * (0.18f + 0.21f * i) + w * 0.1f * sin(time * 0.9f + i * 1.7f)
            val by = h * (0.2f + 0.06f * (i % 2)) + h * 0.05f * sin(time * 1.7f + i * 2.3f)
            hero.draw(
                this, Offset(bx, by), w * 0.17f,
                flap = 0.5f + 0.5f * sin(time * 10f + i * 1.3f),
                blink = blinkAt(time, 4.3f, i.toFloat()),
                rotation = 12f * cos(time * 0.9f + i * 1.7f),
                alpha = 0.95f
            )
        }

        // Flutter herself, drifting in a lazy figure eight
        val hs = min(w, h * 0.5f) * 0.6f
        hero.draw(
            this,
            Offset(w * 0.5f + w * 0.17f * sin(time * 0.7f), h * 0.37f + h * 0.045f * sin(time * 1.4f)),
            hs,
            happy = (time % 8f) > 5.5f,
            flap = 0.5f + 0.5f * sin(time * 8f),
            blink = blinkAt(time),
            rotation = 8f * cos(time * 0.7f)
        )
        drawSparkles(time, 20, 2f, Color(0xFFFFF3B0), w * 0.011f, 0.15f, 0.8f)
    }
}

/** Nimbus under a smiling sun, with a rainbow that fades in and out and a few happy raindrops. */
@Composable
fun CloudScene(modifier: Modifier = Modifier) {
    val t = rememberSeconds()
    val hero = rememberCharPainter(Roster.CLOUD)
    val rainbow = listOf(
        Color(0xFFFF8FA3), Color(0xFFFFB48A), Color(0xFFFFE066), Color(0xFF9BE29B),
        Color(0xFF7CCBFF), Color(0xFF8E9AF7), Color(0xFFC8A2FF)
    )
    Canvas(modifier.fillMaxSize()) {
        val time = t.floatValue
        val w = size.width
        val h = size.height
        drawSky(Color(0xFF8FD0FF), Color(0xFFE4F6FF))

        // sun with a face
        val sun = Offset(w * 0.8f, h * 0.12f)
        val sr = w * 0.085f
        drawGlow(sun, w * 0.5f, Color(0xFFFFF3B0), 0.85f)
        for (k in 0 until 12) {
            val a = (k * 30f + time * 14f) * PI.toFloat() / 180f
            drawLine(
                Color(0xFFFFD166),
                Offset(sun.x + cos(a) * sr * 1.35f, sun.y + sin(a) * sr * 1.35f),
                Offset(sun.x + cos(a) * sr * 1.85f, sun.y + sin(a) * sr * 1.85f),
                strokeWidth = sr * 0.18f, cap = StrokeCap.Round
            )
        }
        drawCircle(Color(0xFFFFD166), sr, sun)
        drawCircle(Color(0xFF2B2A4C), sr * 0.09f, Offset(sun.x - sr * 0.32f, sun.y - sr * 0.1f))
        drawCircle(Color(0xFF2B2A4C), sr * 0.09f, Offset(sun.x + sr * 0.32f, sun.y - sr * 0.1f))
        drawArc(
            Color(0xFF2B2A4C), 20f, 140f, false,
            Offset(sun.x - sr * 0.3f, sun.y - sr * 0.1f), Size(sr * 0.6f, sr * 0.45f),
            style = Stroke(width = sr * 0.07f, cap = StrokeCap.Round)
        )
        drawCircle(Color(0xFFFF9EB5), sr * 0.12f, Offset(sun.x - sr * 0.55f, sun.y + sr * 0.22f), alpha = 0.7f)
        drawCircle(Color(0xFFFF9EB5), sr * 0.12f, Offset(sun.x + sr * 0.55f, sun.y + sr * 0.22f), alpha = 0.7f)

        // rainbow
        val rbAlpha = 0.2f + 0.6f * pulse(time / 11f)
        val band = w * 0.028f
        val rc = Offset(w * 0.5f, h * 0.55f)
        for (i in rainbow.indices) {
            val radius = w * 0.56f - i * band
            drawArc(
                rainbow[i], 180f, 180f, false,
                Offset(rc.x - radius, rc.y - radius), Size(radius * 2f, radius * 2f),
                alpha = rbAlpha, style = Stroke(width = band * 1.02f)
            )
        }

        drawDriftingClouds(time, 4, h * 0.05f, h * 0.3f, 0.012f, w * 0.14f, 0.85f, salt = 3f)

        // Nimbus
        val hs = min(w, h * 0.5f) * 0.62f
        val cx = w * 0.5f + sin(time * 0.6f) * w * 0.06f
        val cy = h * 0.34f + sin(time * 1.3f) * h * 0.015f
        val rainOn = smooth(pulse(time / 9f) * 2.2f - 0.5f)
        for (i in 0 until 7) {
            val p = frac(time * 0.55f + i / 7f)
            val x = cx + (i - 3) * hs * 0.11f
            val y = cy + hs * 0.32f + p * h * 0.2f
            drawDropShape(x, y, hs * 0.03f, Color(0xFF6EC6FF), alpha = (1f - p) * 0.9f * rainOn)
        }
        hero.draw(
            this, Offset(cx, cy), hs,
            happy = (time % 8f) > 5f,
            flap = 0.5f + 0.5f * sin(time * 3f),
            blink = blinkAt(time),
            rotation = 3f * sin(time * 0.6f)
        )
        drawDriftingClouds(time, 3, h * 0.55f, h * 0.7f, 0.02f, w * 0.26f, 0.95f, salt = 4f)
        drawSparkles(time, 14, 3f, Color.White, w * 0.011f)
    }
}

/** Zoom loops and glides, leaving a dotted heart trail, with clouds sliding past at different speeds. */
@Composable
fun PlaneScene(modifier: Modifier = Modifier) {
    val t = rememberSeconds()
    val hero = rememberCharPainter(Roster.PLANE)
    Canvas(modifier.fillMaxSize()) {
        val time = t.floatValue
        val w = size.width
        val h = size.height
        drawSky(Color(0xFF86CFFF), Color(0xFFDDF3FF))
        drawGlow(Offset(w * 0.15f, h * 0.08f), w * 0.5f, Color(0xFFFFF3B0), 0.8f)
        drawDriftingClouds(time, 3, h * 0.04f, h * 0.22f, 0.01f, w * 0.13f, 0.6f, salt = 5f)
        drawDriftingClouds(time, 3, h * 0.3f, h * 0.55f, 0.022f, w * 0.18f, 0.8f, salt = 6f)

        fun pos(u: Float): Offset = Offset(w * 0.5f + w * 0.27f * sin(u), h * 0.31f + h * 0.065f * sin(2f * u))

        val u = time * 0.55f
        // dotted heart trail
        for (k in 1..11) {
            val p = pos(u - k * 0.075f)
            val a = 1f - k / 12f
            drawHeart(p.x, p.y + w * 0.01f, w * 0.022f * (0.5f + a * 0.5f), Color(0xFFFF8FB8), alpha = a * 0.85f)
        }
        val p = pos(u)
        val dx = w * 0.27f * cos(u)
        val dy = h * 0.065f * 2f * cos(2f * u)
        val movingRight = dx >= 0f
        val ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        hero.draw(
            this, p, w * 0.4f,
            happy = (time % 7f) > 4.5f,
            blink = blinkAt(time),
            rotation = if (movingRight) ang * 0.8f else (ang - 180f) * 0.8f,
            scaleX = if (movingRight) 1f else -1f
        )
        drawDriftingClouds(time, 2, h * 0.58f, h * 0.7f, 0.036f, w * 0.27f, 0.95f, salt = 7f)
        drawSparkles(time, 12, 4f, Color.White, w * 0.011f, 0f, 0.6f)
    }
}
