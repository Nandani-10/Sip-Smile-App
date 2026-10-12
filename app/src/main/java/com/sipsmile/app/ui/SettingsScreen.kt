package com.sipsmile.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import com.sipsmile.app.data.ReminderType
import com.sipsmile.app.data.UserPrefs
import com.sipsmile.app.reminder.ReminderEngine
import com.sipsmile.app.reminder.Scheduler
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
                Pill("← Back", onClick = onBack)
                Spacer(Modifier.padding(start = 12.dp))
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

            Section("Testing") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CuteButton(
                        "Test water", onClick = { scope.launch { ReminderEngine.deliver(app, ReminderType.WATER, true) } },
                        container = Pastel.Sky, fontSize = 14
                    )
                    CuteButton(
                        "Test smile", onClick = { scope.launch { ReminderEngine.deliver(app, ReminderType.SMILE, true) } },
                        container = Pastel.PinkDeep, fontSize = 14
                    )
                }
                SwitchRow(
                    "1-minute interval", "For testing only: reminders every minute inside your time window",
                    prefs.debugFast
                ) { save { repo.setDebugFast(it) } }
            }

            Text(
                "Sip & Smile 1.0 • made with love for ${prefs.name.ifBlank { "you" }} 💙",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
