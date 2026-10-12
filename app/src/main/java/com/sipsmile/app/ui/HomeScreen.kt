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
import com.sipsmile.app.update.UpdateChecker
import com.sipsmile.app.update.UpdateInfo
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
fun requestPinWidget(context: Context): Boolean {
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
    onOpenScene: (SceneRequest) -> Unit,
    onCamera: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    val repo = remember { PrefsRepository(app) }
    var cheer by remember { mutableStateOf(false) }
    var burst by remember { mutableStateOf(false) }
    var notifOk by remember { mutableStateOf(true) }
    var widgetPlaced by remember { mutableStateOf(true) }
    var goalDialog by remember { mutableStateOf(false) }
    var askCapture by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    // quietly look for a newer version once each time the app opens
    LaunchedEffect(Unit) {
        val latest = UpdateChecker.fetchLatest()
        if (latest != null && UpdateChecker.isNewer(latest.version, UpdateChecker.currentVersion(context))) update = latest
    }

    LaunchedEffect(Unit) {
        while (true) {
            notifOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
            widgetPlaced = ReminderEngine.hasWidget(context)
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
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hello, ${prefs.name} 💧", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    Text(nextReminderText(prefs), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Pill(if (prefs.paused) "▶ Resume" else "⏸ Pause") {
                    scope.launch {
                        repo.setPaused(!prefs.paused)
                        Scheduler.scheduleNext(app, repo.read())
                    }
                }
            }

            // only one gentle tip at a time, so the page never gets crowded
            val newVersion = update
            val tip: Triple<String, androidx.compose.ui.graphics.Color, () -> Unit>? = when {
                !notifOk -> Triple("🔔 Notifications are off, so reminders can't reach you. Tap to fix", Pastel.Peach) { openNotificationSettings(context) }
                !widgetPlaced -> Triple("🧩 Add the Sip & Smile widget to your home screen. Tap here", Pastel.Mint) {
                    if (!requestPinWidget(context)) {
                        Toast.makeText(context, "Long-press your home screen, tap Widgets, then pick Sip & Smile", Toast.LENGTH_LONG).show()
                    }
                }
                newVersion != null -> Triple("✨ New version ${newVersion.version} is ready. Tap to download", Pastel.Mint) {
                    UpdateChecker.openDownload(context, newVersion)
                }
                else -> null
            }
            if (tip != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tip.second,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { tip.third() }
                ) {
                    Text(
                        tip.first, color = Pastel.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }

            // the buddy takes whatever space is left; tap to replay its scene
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable { onOpenScene(SceneRequest(charId, type, message)) },
                contentAlignment = Alignment.Center
            ) {
                CharacterView(charId, Modifier.fillMaxSize(), happy = happy)
            }
            Text(
                moodLine(prefs, Roster.displayName(charId)),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                Text(
                    message,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // compact progress card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(prefs.progress, Modifier.size(92.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                prefs.progressText,
                                fontSize = if (prefs.glassesToday > prefs.goal) 15.sp else 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text("glasses", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                prefs.glassesToday > prefs.goal -> "Goal smashed! ⭐ +${prefs.extraGlasses}"
                                prefs.goalReached -> "Goal reached! 🎉"
                                else -> "Today's water"
                            },
                            fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "💧 ${formatMl(prefs.drankMlToday)} of ${formatMl(prefs.goalMl)}  •  😊 ${prefs.smilesToday}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (prefs.streak > 0) "🔥 ${prefs.streak} day streak" else "Reach your goal to start a streak",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "✏️ Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { goalDialog = true }
                            )
                            if (prefs.glassesToday > 0) {
                                Text(
                                    "↩ Undo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            repo.undoGlass()
                                            SipSmileWidget().updateAll(app)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
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
                    fontSize = 17
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
                    fontSize = 17
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (burst) CelebrationBurst()
    }

    // first time: invite the user to put the widget on the home screen
    if (!prefs.widgetPromptDone && !widgetPlaced) {
        AlertDialog(
            onDismissRequest = { scope.launch { repo.setWidgetPromptDone(true) } },
            title = { Text("Add the widget? 🧩") },
            text = {
                Text("Put Sip & Smile on your home screen. You'll see your buddy and can tap I drank or I smiled without opening the app.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.setWidgetPromptDone(true) }
                    if (!requestPinWidget(context)) {
                        Toast.makeText(context, "Long-press your home screen, tap Widgets, then pick Sip & Smile", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Add widget") }
            },
            dismissButton = {
                TextButton(onClick = { scope.launch { repo.setWidgetPromptDone(true) } }) { Text("Maybe later") }
            }
        )
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
