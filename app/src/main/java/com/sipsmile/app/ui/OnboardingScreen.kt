package com.sipsmile.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sipsmile.app.data.Roster
import kotlinx.coroutines.delay

/**
 * Steps: 0 = "What should I call you?"  1 = hello + confetti  2 = permissions  3 = reminder window.
 * [onFinished] receives the name, start, end and interval.
 */
@Composable
fun OnboardingScreen(onFinished: (name: String, start: Int, end: Int, interval: Int) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var start by remember { mutableIntStateOf(8 * 60) }
    var end by remember { mutableIntStateOf(21 * 60) }
    var interval by remember { mutableIntStateOf(15) }

    SkyBackground {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "onboarding"
        ) { s ->
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (s) {
                    0 -> StepName(name, { name = it.take(20) }) { if (name.isNotBlank()) step = 1 }
                    1 -> StepHello(name.trim()) { step = 2 }
                    2 -> StepPermissions { step = 3 }
                    else -> StepWindow(start, end, interval,
                        onChange = { a, b, c -> start = a; end = b; interval = c }) {
                        onFinished(name.trim(), start, end, interval)
                    }
                }
            }
        }
        if (step == 1) CelebrationBurst()
    }
}

@Composable
private fun StepName(name: String, onName: (String) -> Unit, onNext: () -> Unit) {
    CharacterView(Roster.BIRD, Modifier.size(210.dp))
    Spacer(Modifier.height(8.dp))
    Text("Hi! I'm Pip 👋", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        "What should I call you?",
        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        singleLine = true,
        placeholder = { Text("Type your name") },
        shape = RoundedCornerShape(22.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onNext() }),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(18.dp))
    CuteButton("Nice to meet you!", onNext, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
}

@Composable
private fun StepHello(name: String, onNext: () -> Unit) {
    CharacterView(Roster.BIRD, Modifier.size(210.dp), happy = true)
    Spacer(Modifier.height(8.dp))
    Text(
        "Nice to meet you, $name! 🌈",
        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "I'll remind you to sip some water and to smile, and I'll always say your name.",
        fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(24.dp))
    CuteButton("Let's set it up", onNext, Modifier.fillMaxWidth())
}

private fun notificationsAllowed(context: Context): Boolean {
    val granted = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun exactAlarmsAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 31) return true
    return (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
}

private fun batteryUnrestricted(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@Composable
private fun StepPermissions(onNext: () -> Unit) {
    val context = LocalContext.current
    var notif by remember { mutableStateOf(notificationsAllowed(context)) }
    var exact by remember { mutableStateOf(exactAlarmsAllowed(context)) }
    var battery by remember { mutableStateOf(batteryUnrestricted(context)) }

    // refresh the ticks when the user comes back from a system settings screen
    LaunchedEffect(Unit) {
        while (true) {
            notif = notificationsAllowed(context)
            exact = exactAlarmsAllowed(context)
            battery = batteryUnrestricted(context)
            delay(800)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Text("Let's keep your buddies on time", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(
        "Three quick permissions so reminders arrive exactly when they should. You can skip any of them.",
        fontSize = 15.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(18.dp))

    PermissionCard(
        title = "Notifications 🔔",
        reason = "So a soft chime and a cute message can reach you when your screen is off.",
        done = notif,
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }
    Spacer(Modifier.height(10.dp))
    PermissionCard(
        title = "Exact timing ⏰",
        reason = "Lets Android deliver the reminders right on the minute (8:00, 8:15, 8:30 ...).",
        done = exact,
    ) {
        if (Build.VERSION.SDK_INT >= 31) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(Uri.parse("package:${context.packageName}"))
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    PermissionCard(
        title = "No battery sleeping 🔋",
        reason = "Stops the phone from putting Sip & Smile to sleep, which would delay reminders.",
        done = battery,
    ) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:${context.packageName}"))
            )
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
    Spacer(Modifier.height(22.dp))
    CuteButton("Continue", onNext, Modifier.fillMaxWidth())
    TextButton(onClick = onNext) { Text("Skip for now") }
}

fun openNotificationSettings(context: Context) {
    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(i)
}

@Composable
private fun PermissionCard(title: String, reason: String, done: Boolean, onAllow: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(10.dp))
            if (done) {
                Text("✅", fontSize = 26.sp)
            } else {
                CuteButton("Allow", onAllow, container = Pastel.Sky, fontSize = 14)
            }
        }
    }
}

@Composable
private fun StepWindow(
    start: Int, end: Int, interval: Int,
    onChange: (Int, Int, Int) -> Unit,
    onFinish: () -> Unit,
) {
    Box(Modifier.size(120.dp)) { CharacterView(Roster.DROP, Modifier.fillMaxSize()) }
    Text("When should I remind you?", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(
        "I'll only remind you inside this time window, and stay quiet outside it.",
        fontSize = 15.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            WindowEditor(start, end, interval, onChange)
        }
    }
    Spacer(Modifier.height(20.dp))
    CuteButton("Start sipping & smiling 🌈", onFinish, Modifier.fillMaxWidth())
}
