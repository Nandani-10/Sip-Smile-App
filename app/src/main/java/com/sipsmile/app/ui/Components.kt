package com.sipsmile.app.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipsmile.app.ui.chars.rememberCharPainter
import com.sipsmile.app.ui.scenes.PI_F
import com.sipsmile.app.ui.scenes.blinkAt
import com.sipsmile.app.ui.scenes.drawDriftingClouds
import com.sipsmile.app.ui.scenes.drawHeart
import com.sipsmile.app.ui.scenes.drawSparkles
import com.sipsmile.app.ui.scenes.drawStar
import com.sipsmile.app.ui.scenes.hash
import com.sipsmile.app.ui.scenes.rememberSeconds
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Plays the short sounds from res/raw. */
object Sfx {
    fun play(context: Context, resId: Int, volume: Float = 0.5f) {
        try {
            val mp = MediaPlayer.create(context.applicationContext, resId) ?: return
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Throwable) {
            // sound is a nice-to-have, never crash for it
        }
    }
}

/** Soft sky with drifting clouds and twinkles, used behind Home, Settings and Onboarding. */
@Composable
fun SkyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = LocalDarkTheme.current
    val t = rememberSeconds()
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val time = t.floatValue
            if (dark) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF121B26), Color(0xFF1F2B4A)), 0f, size.height))
                drawDriftingClouds(time, 4, size.height * 0.05f, size.height * 0.6f, 0.01f, size.width * 0.14f, 0.07f, salt = 21f)
                drawSparkles(time, 14, 22f, Color(0xFFFFF3B0), size.width * 0.01f)
            } else {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFCDEBFF), Color(0xFFFFF1F7)), 0f, size.height))
                drawDriftingClouds(time, 4, size.height * 0.04f, size.height * 0.6f, 0.01f, size.width * 0.14f, 0.8f, salt = 21f)
                drawSparkles(time, 14, 22f, Color.White, size.width * 0.011f)
            }
        }
        content()
    }
}

/** A character that floats, blinks and flaps by itself. */
@Composable
fun CharacterView(id: String, modifier: Modifier = Modifier, happy: Boolean = false) {
    val painter = rememberCharPainter(id)
    val t = rememberSeconds()
    Canvas(modifier) {
        val time = t.floatValue
        val s = min(size.width, size.height)
        painter.draw(
            this,
            Offset(size.width / 2f, size.height / 2f + sin(time * 2f) * s * 0.025f),
            s * 0.92f,
            happy = happy,
            flap = 0.5f + 0.5f * sin(time * 5f),
            blink = blinkAt(time),
            rotation = 3f * sin(time * 1.1f)
        )
    }
}

/** Big rounded button that squishes when pressed. */
@Composable
fun CuteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.Unspecified,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    fontSize: Int = 17,
) {
    val base = if (container == Color.Unspecified) MaterialTheme.colorScheme.primary else container
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "press"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(if (enabled) base else base.copy(alpha = 0.4f))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = contentColor, fontWeight = FontWeight.Bold, fontSize = fontSize.sp)
    }
}

/** Circular progress ring with content in the middle. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "ring"
    )
    val ringColor = MaterialTheme.colorScheme.primary
    val ringColor2 = MaterialTheme.colorScheme.secondary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = size.minDimension * 0.13f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                ringColor.copy(alpha = 0.18f), 0f, 360f, false,
                Offset(inset, inset), arcSize, style = Stroke(width = stroke)
            )
            if (animated > 0.001f) {
                drawArc(
                    Brush.linearGradient(
                        listOf(ringColor2, ringColor),
                        start = Offset(0f, 0f), end = Offset(size.width, size.height)
                    ),
                    -90f, 360f * animated, false,
                    Offset(inset, inset), arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Hearts, stars and confetti bursting upwards from the bottom of the screen. */
@Composable
fun CelebrationBurst(modifier: Modifier = Modifier, durationMs: Int = 1600) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMs, easing = LinearOutSlowInEasing))
    }
    val colors = listOf(Color(0xFFFF8FB8), Color(0xFFFFD166), Color(0xFF7CCBFF), Color(0xFF9BE29B), Color(0xFFC8A2FF))
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        val ox = size.width * 0.5f
        val oy = size.height * 0.78f
        for (i in 0 until 34) {
            val a = -PI_F / 2f + (hash(i, 1f) - 0.5f) * PI_F * 1.5f
            val sp = 0.35f + 0.65f * hash(i, 2f)
            val x = ox + cos(a) * sp * size.width * 0.9f * p
            val y = oy + sin(a) * sp * size.height * 0.75f * p + p * p * size.height * 0.25f
            val s = size.width * (0.014f + 0.02f * hash(i, 3f)) * (1f - p * 0.4f)
            val alpha = (1f - p * p).coerceIn(0f, 1f)
            val c = colors[i % colors.size]
            when (i % 3) {
                0 -> drawHeart(x, y, s * 1.4f, c, alpha)
                1 -> drawStar(x, y, s * 1.7f, c, alpha, p * 180f)
                else -> drawCircle(c, s * 0.7f, Offset(x, y), alpha = alpha)
            }
        }
    }
}
