package com.sipsmile.app.ui.chars

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

/** One shape of a character. Colours are ARGB with the alpha baked in; 0 means "none". */
class ArtLayer(
    val group: String,
    val mood: Int,          // 0 always, 1 normal face only, 2 happy face only, 3 "extra" (yawn)
    val fill: Long,
    val stroke: Long,
    val strokeWidth: Float,
    val d: String,
)

/** Animated part of a character: wings flap, eyes blink, pom-poms bob. */
class ArtGroup(
    val px: Float,          // pivot
    val py: Float,
    val mode: Int,          // 0 none, 1 rotate, 2 scale X, 3 scale Y (blink)
    val amp: Float,
)

class CharArt(
    val id: String,
    val name: String,
    val layers: List<ArtLayer>,
    val groups: Map<String, ArtGroup>,
)

/** Parses the SVG-style path data of a character once, then draws it as often as needed. */
class CharPainter(val art: CharArt) {
    private val paths: List<Path> = art.layers.map { PathParser().parsePathString(it.d).toPath() }
    private val fills: List<Color?> = art.layers.map { if (it.fill != 0L) Color(it.fill) else null }
    private val strokes: List<Color?> = art.layers.map { if (it.stroke != 0L) Color(it.stroke) else null }
    private val groupOf: List<ArtGroup?> = art.layers.map { art.groups[it.group] }

    fun draw(
        scope: DrawScope,
        center: Offset,
        size: Float,
        happy: Boolean = false,
        flap: Float = 0.5f,      // 0..1  (wing down .. wing up / closed .. open)
        blink: Float = 0f,       // 0 open .. 1 closed
        extra: Boolean = false,  // e.g. the moon's yawn
        rotation: Float = 0f,    // degrees, clockwise
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        alpha: Float = 1f,
    ) {
        val k = size / 200f
        with(scope) {
            withTransform({
                translate(center.x, center.y)
                rotate(rotation, Offset.Zero)
                scale(scaleX, scaleY, Offset.Zero)
                scale(k, k, Offset.Zero)
                translate(-100f, -100f)
            }) {
                for (i in art.layers.indices) {
                    val layer = art.layers[i]
                    when (layer.mood) {
                        1 -> if (happy) continue
                        2 -> if (!happy) continue
                        3 -> if (!extra) continue
                        else -> Unit
                    }
                    val g = groupOf[i]
                    if (g == null || g.mode == 0) {
                        drawLayer(i, alpha)
                    } else {
                        val pivot = Offset(g.px, g.py)
                        withTransform({
                            when (g.mode) {
                                1 -> rotate(g.amp * (flap * 2f - 1f), pivot)
                                2 -> scale(1f - g.amp * (1f - flap), 1f, pivot)
                                3 -> scale(1f, 1f - g.amp * blink, pivot)
                                else -> Unit
                            }
                        }) {
                            drawLayer(i, alpha)
                        }
                    }
                }
            }
        }
    }

    private fun DrawScope.drawLayer(i: Int, alpha: Float) {
        val path = paths[i]
        fills[i]?.let { drawPath(path, it, alpha = alpha) }
        strokes[i]?.let {
            drawPath(
                path, it, alpha = alpha,
                style = Stroke(width = art.layers[i].strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

private val painterCache = HashMap<String, CharPainter>()

/** Looks up (and caches) the painter for a character id such as "bird". */
@Composable
fun rememberCharPainter(id: String): CharPainter = remember(id) {
    painterCache.getOrPut(id) {
        CharPainter(ArtData.all.firstOrNull { it.id == id } ?: ArtData.all.first())
    }
}
