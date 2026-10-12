package com.sipsmile.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipsmile.app.data.Badges
import com.sipsmile.app.data.PhotoStore
import com.sipsmile.app.data.UserPrefs
import com.sipsmile.app.data.formatMl
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Weekly chart, totals and badges. */
@Composable
fun StatsScreen(prefs: UserPrefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val photoCount = remember { PhotoStore.count(context) }
    BackHandler { onBack() }

    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val byDate = prefs.history.associateBy { it.date }
    val glasses = days.map { d -> if (d == today) prefs.glassesToday else byDate[d]?.glasses ?: 0 }
    val smiles = days.map { d -> if (d == today) prefs.smilesToday else byDate[d]?.smiles ?: 0 }
    val weekGlasses = glasses.sum()
    val weekSmiles = smiles.sum()
    val bestIdx = glasses.indices.maxByOrNull { glasses[it] } ?: 0

    SkyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Stats & badges", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("🔥", "${prefs.streak}", "day streak", Modifier.weight(1f))
                StatTile("🏆", "${prefs.bestStreak}", "best streak", Modifier.weight(1f))
                StatTile("💧", "${prefs.totalGlasses}", "glasses total", Modifier.weight(1f))
                StatTile("😊", "${prefs.totalSmiles}", "smiles total", Modifier.weight(1f))
            }

            Card("This week") {
                WeekChart(days, glasses, smiles, prefs.goal)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(MaterialTheme.colorScheme.primary, "Glasses")
                    Legend(Pastel.PinkDeep, "Smiles")
                    Legend(MaterialTheme.colorScheme.onSurfaceVariant, "Goal line")
                }
                val avg = weekGlasses / 7f
                Text(
                    "You drank $weekGlasses glasses (${formatMl(weekGlasses * prefs.glassMl)}) and smiled $weekSmiles times in the last 7 days. " +
                        "That's about ${"%.1f".format(Locale.US, avg)} glasses a day.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (glasses[bestIdx] > 0) {
                    val d = days[bestIdx]
                    Text(
                        "Best day: ${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} with ${glasses[bestIdx]} glasses 🌟",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            val badges = Badges.evaluate(prefs, photoCount)
            Card("Badges  ${badges.count { it.unlocked }}/${badges.size}") {
                for (row in badges.chunked(3)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (b in row) {
                            Column(
                                Modifier
                                    .weight(1f)
                                    .background(
                                        if (b.unlocked) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(if (b.unlocked) b.emoji else "🔒", fontSize = 26.sp)
                                Text(
                                    b.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                                    color = if (b.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    b.hint, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 6.dp).background(color, RoundedCornerShape(50)).padding(5.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeekChart(days: List<LocalDate>, glasses: List<Int>, smiles: List<Int>, goal: Int) {
    val water = MaterialTheme.colorScheme.primary
    val smile = Pastel.PinkDeep
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val maxV = maxOf(goal, glasses.maxOrNull() ?: 0, smiles.maxOrNull() ?: 0, 1).toFloat()

    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val slot = size.width / days.size
        val barW = slot * 0.26f
        val h = size.height
        // goal line
        val goalY = h - (goal / maxV) * h
        drawLine(
            muted.copy(alpha = 0.6f), Offset(0f, goalY), Offset(size.width, goalY),
            strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
        )
        for (i in days.indices) {
            val cx = slot * i + slot / 2f
            val gh = (glasses[i] / maxV) * h
            val sh = (smiles[i] / maxV) * h
            val corner = CornerRadius(barW / 2f, barW / 2f)
            if (gh > 0f) drawRoundRect(water, Offset(cx - barW - 2f, h - gh), Size(barW, gh), corner)
            else drawRoundRect(water.copy(alpha = 0.2f), Offset(cx - barW - 2f, h - 6f), Size(barW, 6f), corner)
            if (sh > 0f) drawRoundRect(smile, Offset(cx + 2f, h - sh), Size(barW, sh), corner)
            else drawRoundRect(smile.copy(alpha = 0.2f), Offset(cx + 2f, h - 6f), Size(barW, 6f), corner)
        }
    }
    Row(Modifier.fillMaxWidth()) {
        for (d in days) {
            Text(
                d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = muted
            )
        }
    }
}
