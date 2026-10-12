package com.sipsmile.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.sipsmile.app.R
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.data.UserPrefs
import com.sipsmile.app.reminder.ReminderEngine
import com.sipsmile.app.reminder.Scheduler
import com.sipsmile.app.update.UpdateChecker
import com.sipsmile.app.widget.SipSmileWidget
import kotlinx.coroutines.launch

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
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
private fun SwitchRow(label: String, hint: String? = null, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (hint != null) Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun SettingsScreen(prefs: UserPrefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    val repo = remember { PrefsRepository(app) }
    var nameDraft by remember(prefs.name) { mutableStateOf(prefs.name) }

    BackHandler { onBack() }

    /** Saves a change and then re-plans the alarms + refreshes the widget. */
    fun save(block: suspend () -> Unit) {
        scope.launch {
            block()
            Scheduler.scheduleNext(app, repo.read())
            SipSmileWidget().updateAll(app)
        }
    }

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
                Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }

            Section("Your name") {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(20) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                CuteButton(
                    "Save name",
                    onClick = { if (nameDraft.isNotBlank()) save { repo.setName(nameDraft) } },
                    enabled = nameDraft.isNotBlank() && nameDraft.trim() != prefs.name,
                    fontSize = 15
                )
            }

            Section("Daily water goal") {
                GoalEditor(
                    prefs = prefs,
                    onGoal = { g -> save { repo.setGoal(g) } },
                    onGlassMl = { ml -> save { repo.setGlassMl(ml) } }
                )
            }

            Section("Appearance") {
                Text("Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((key, label) in listOf("light" to "☀️ Light", "dark" to "🌙 Dark", "system" to "📱 Phone")) {
                        FilterChip(
                            selected = prefs.themeMode == key,
                            onClick = { save { repo.setThemeMode(key) } },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    for ((key, label, color) in AccentChoices) {
                        Box(
                            Modifier
                                .size(if (prefs.accent == key) 42.dp else 34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { save { repo.setAccent(key) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (prefs.accent == key) Text("✓", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Section("Reminder times") {
                WindowEditor(prefs.startMinutes, prefs.endMinutes, prefs.intervalMinutes) { s, e, i ->
                    save {
                        repo.setWindow(s, e)
                        repo.setInterval(i)
                    }
                }
                SwitchRow(
                    "Pause reminders", "No reminders until you switch this off", prefs.paused
                ) { save { repo.setPaused(it) } }
            }

            Section("Sound & delivery") {
                SwitchRow("Sound", null, prefs.soundOn) { save { repo.setSoundOn(it) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Notification sound", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.soundChoice == "chime",
                        onClick = {
                            save { repo.setSoundChoice("chime") }
                            Sfx.play(context, R.raw.chime_soft, 0.5f)
                        },
                        label = { Text("🔔 Soft chime") }
                    )
                    FilterChip(
                        selected = prefs.soundChoice == "tweet",
                        onClick = {
                            save { repo.setSoundChoice("tweet") }
                            Sfx.play(context, R.raw.bird_tweet, 0.5f)
                        },
                        label = { Text("🐦 Bird tweet") }
                    )
                }
                SwitchRow("Vibration", null, prefs.vibrationOn) { save { repo.setVibration(it) } }
                SwitchRow(
                    "Evening summary", "One quiet notification after your last reminder: glasses and smiles of the day",
                    prefs.summaryOn
                ) { save { repo.setSummaryOn(it) } }
                SwitchRow(
                    "Also notify when screen is on",
                    "Normally you only get a notification when the screen is off; with the widget on your home screen it updates silently.",
                    prefs.notifyWhenScreenOn
                ) { save { repo.setNotifyWhenScreenOn(it) } }
                Text(
                    "Tip: you can fine-tune the sound in your phone's notification settings for Sip & Smile.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CuteButton(
                    "Open notification settings", onClick = { openNotificationSettings(context) },
                    container = Pastel.Mint, contentColor = Pastel.Ink, fontSize = 14
                )
            }

            Section("Today") {
                Text("${prefs.glassesToday} of ${prefs.goal} glasses so far", fontSize = 15.sp)
                CuteButton(
                    "Reset today's count",
                    onClick = { save { repo.resetToday() } },
                    container = Pastel.Peach, contentColor = Pastel.Ink, fontSize = 14
                )
            }

            Section("Home-screen widget") {
                Text(
                    "Put Sip & Smile on your home screen to see your buddy and tap I drank / I smiled without opening the app.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CuteButton(
                    "🧩 Add widget", fontSize = 14, container = Pastel.Mint, contentColor = Pastel.Ink,
                    onClick = {
                        if (!requestPinWidget(context)) {
                            Toast.makeText(
                                context,
                                "Long-press your home screen, tap Widgets, then pick Sip & Smile",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }

            Section("App updates") {
                var checking by remember { mutableStateOf(false) }
                Text("You have version ${UpdateChecker.currentVersion(context)}", fontSize = 14.sp)
                CuteButton(
                    if (checking) "Checking..." else "Check for updates",
                    onClick = {
                        if (!checking) {
                            checking = true
                            scope.launch {
                                val latest = UpdateChecker.fetchLatest()
                                checking = false
                                when {
                                    latest == null ->
                                        Toast.makeText(context, "Couldn't check right now. Are you online?", Toast.LENGTH_LONG).show()
                                    UpdateChecker.isNewer(latest.version, UpdateChecker.currentVersion(context)) -> {
                                        Toast.makeText(context, "Version ${latest.version} found. Opening the download...", Toast.LENGTH_LONG).show()
                                        UpdateChecker.openDownload(context, latest)
                                    }
                                    else -> Toast.makeText(context, "You're on the latest version 🎉", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    container = Pastel.Mint, contentColor = Pastel.Ink, fontSize = 14
                )
            }

            Section("Testing") {
                CuteButton(
                    "Send a test reminder", onClick = { scope.launch { ReminderEngine.deliver(app, isTest = true) } },
                    fontSize = 14
                )
                SwitchRow(
                    "1-minute interval", "For testing only: reminders every minute inside your time window",
                    prefs.debugFast
                ) { save { repo.setDebugFast(it) } }
            }

            Text(
                "Sip & Smile • made with love for ${prefs.name.ifBlank { "you" }} 💙",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
