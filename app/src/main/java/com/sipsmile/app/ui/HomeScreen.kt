package com.sipsmile.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.sipsmile.app.R
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.data.ReminderType
import com.sipsmile.app.data.Roster
import com.sipsmile.app.data.UserPrefs
import com.sipsmile.app.data.formatMl
import com.sipsmile.app.reminder.ReminderEngine
import com.sipsmile.app.reminder.Scheduler
import com.sipsmile.app.widget.SipSmileWidget
import com.sipsmile.app.widget.SipSmileWidgetReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

private fun nextReminderText(prefs: UserPrefs): String {
    if (prefs.paused) return "Reminders are paused"
    val next = Scheduler.nextSlot(ZonedDateTime.now(), prefs.startMinutes, prefs.endMinutes, prefs.effectiveInterval)
    val label = Scheduler.format12(next.hour * 60 + next.minute)
    return if (next.toLocalDate() == LocalDate.now()) "Next reminder at $label" else "Next reminder tomorrow at $label"
}

/** Asks the launcher to pin the widget. Returns false when the launcher can't do it (then we explain manually). */
private fun requestPinWidget(context: Context): Boolean {
    val mgr = AppWidgetManager.getInstance(context)
    if (!mgr.isRequestPinAppWidgetSupported) return false
    return mgr.requestPinAppWidget(ComponentName(context, SipSmileWidgetReceiver::class.java), null, null)
}

private fun moodLine(prefs: UserPrefs, buddy: String): String {
    val first = buddy.substringBefore(" the")
    return when {
        prefs.goalReached -> "$first is so proud of you! 🎉"
        prefs.glassesToday == 0 -> "$first is thirsty. Sip something? 🥺"
        prefs.progress < 0.5f -> "$first says: keep going, you're doing great 💪"
        else -> "$first is happy! Almost there 🌟"
    }
}

@Composable
fun HomeScreen(
    prefs: UserPrefs,
    onSettings: () -> Unit,
    onOpenScene: (SceneRequest) -> Unit,
    onAlbum: () -> Unit,
    onStats: () -> Unit,
    onCamera: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    val repo = remember { PrefsRepository(app) }
    var cheer by remember { mutableStateOf(false) }
    var burst by remember { mutableStateOf(false) }
    var notifOk by remember { mutableStateOf(true) }
    var goalDialog by remember { mutableStateOf(false) }
    var askCapture by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            notifOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
            delay(1500)
        }
    }

    val charId = prefs.lastCharacter.ifBlank { Roster.DROP }
    val type = runCatching { ReminderType.valueOf(prefs.lastType) }.getOrDefault(ReminderType.COMBO)
    val message = prefs.lastMessage.ifBlank { "Hi ${prefs.name}! Time to sip & smile 🌈" }
    val happy = cheer || prefs.goalReached || prefs.progress >= 0.5f

    SkyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Hello, ${prefs.name} 💧", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(nextReminderText(prefs), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Pill("⚙ Settings", onClick = onSettings)
            }

            if (!notifOk) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Pastel.Peach,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openNotificationSettings(context) }
                ) {
                    Text(
                        "Notifications are turned off, so reminders can't reach you. Tap to fix 🔔",
                        color = Pastel.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // the buddy of the moment; tap to replay its scene
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(220.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .clickable { onOpenScene(SceneRequest(charId, type, message)) }
            ) {
                CharacterView(charId, Modifier.fillMaxSize(), happy = happy)
            }
            Text(
                moodLine(prefs, Roster.displayName(charId)),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                Text(
                    message,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // progress card (tap to change the goal)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { goalDialog = true }
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(prefs.progress, Modifier.size(120.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                prefs.progressText,
                                fontSize = if (prefs.glassesToday > prefs.goal) 20.sp else 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text("glasses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(
                            when {
                                prefs.glassesToday > prefs.goal -> "Goal smashed! ⭐ +${prefs.extraGlasses}"
                                prefs.goalReached -> "Goal reached! 🎉"
                                else -> "Today's water"
                            },
                            fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "💧 ${formatMl(prefs.drankMlToday)} of ${formatMl(prefs.goalMl)}",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "😊 ${prefs.smilesToday} ${if (prefs.smilesToday == 1) "smile" else "smiles"} today",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (prefs.streak > 0) "🔥 ${prefs.streak} day streak" else "Reach ${prefs.goal} glasses to start a streak",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Tap to change your goal", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CuteButton(
                    text = "I drank 💧",
                    onClick = {
                        scope.launch {
                            val total = repo.logGlass()
                            SipSmileWidget().updateAll(app)
                            if (prefs.soundOn) Sfx.play(context, R.raw.water_plop, 0.6f)
                            cheer = true
                            if (total == prefs.goal) {
                                burst = true
                                Toast.makeText(context, "Goal reached! 🎉 Well done, ${prefs.name}!", Toast.LENGTH_LONG).show()
                            }
                            delay(2000)
                            cheer = false
                            burst = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    container = Pastel.Sky,
                    fontSize = 18
                )
                CuteButton(
                    text = "😊 I smiled",
                    onClick = {
                        scope.launch {
                            repo.logSmile()
                            SipSmileWidget().updateAll(app)
                            if (prefs.soundOn) Sfx.play(context, R.raw.sparkle, 0.5f)
                            askCapture = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    container = Pastel.PinkDeep,
                    fontSize = 18
                )
            }

            if (prefs.glassesToday > 0) {
                Spacer(Modifier.height(8.dp))
                Pill("↩ Undo last glass") {
                    scope.launch {
                        repo.undoGlass()
                        SipSmileWidget().updateAll(app)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("📸 Album", Modifier.weight(1f), onClick = onAlbum)
                Pill("📊 Stats & badges", Modifier.weight(1f), onClick = onStats)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(if (prefs.paused) "▶ Resume" else "⏸ Pause", Modifier.weight(1f)) {
                    scope.launch {
                        repo.setPaused(!prefs.paused)
                        Scheduler.scheduleNext(app, repo.read())
                    }
                }
                Pill("🧩 Add widget", Modifier.weight(1f)) {
                    if (!requestPinWidget(context)) {
                        Toast.makeText(
                            context,
                            "Long-press your home screen, tap Widgets, then pick Sip & Smile",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                Pill("🔔 Test", Modifier.weight(1f)) {
                    scope.launch { ReminderEngine.deliver(app, isTest = true) }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (burst) CelebrationBurst()
    }

    if (goalDialog) {
        AlertDialog(
            onDismissRequest = { goalDialog = false },
            title = { Text("Your daily water goal") },
            text = {
                GoalEditor(
                    prefs = prefs,
                    onGoal = { g -> scope.launch { repo.setGoal(g); SipSmileWidget().updateAll(app) } },
                    onGlassMl = { ml -> scope.launch { repo.setGlassMl(ml); SipSmileWidget().updateAll(app) } }
                )
            },
            confirmButton = { TextButton(onClick = { goalDialog = false }) { Text("Done") } }
        )
    }

    if (askCapture) {
        AlertDialog(
            onDismissRequest = { askCapture = false },
            title = { Text("Capture this smile? 📸") },
            text = { Text("Take a quick selfie so you can keep this happy moment in your Smile Album.") },
            confirmButton = {
                TextButton(onClick = {
                    askCapture = false
                    onCamera(charId)
                }) { Text("Yes, take a selfie") }
            },
            dismissButton = { TextButton(onClick = { askCapture = false }) { Text("Not now") } }
        )
    }
}

/** Small rounded chip button. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, enabledLook: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (enabledLook) 0.9f else 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    }
}
