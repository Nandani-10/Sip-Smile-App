package com.sipsmile.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.glance.appwidget.updateAll
import com.sipsmile.app.R
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.data.ReminderType
import com.sipsmile.app.data.Roster
import com.sipsmile.app.data.UserPrefs
import com.sipsmile.app.reminder.Notifier
import com.sipsmile.app.ui.scenes.BeeScene
import com.sipsmile.app.ui.scenes.BirdScene
import com.sipsmile.app.ui.scenes.ButterflyScene
import com.sipsmile.app.ui.scenes.CloudScene
import com.sipsmile.app.ui.scenes.DropScene
import com.sipsmile.app.ui.scenes.MoonScene
import com.sipsmile.app.ui.scenes.PlaneScene
import com.sipsmile.app.widget.SipSmileWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the full-screen scene should show (comes from the notification or the widget). */
data class SceneRequest(
    val characterId: String,
    val type: ReminderType,
    val message: String,
)

@Composable
fun SceneForCharacter(id: String, modifier: Modifier = Modifier) {
    when (id) {
        Roster.BIRD -> BirdScene(modifier)
        Roster.BUTTERFLY -> ButterflyScene(modifier)
        Roster.CLOUD -> CloudScene(modifier)
        Roster.PLANE -> PlaneScene(modifier)
        Roster.BEE -> BeeScene(modifier)
        Roster.MOON -> MoonScene(modifier)
        else -> DropScene(modifier)
    }
}

@Composable
fun SceneScreen(req: SceneRequest, prefs: UserPrefs, onCapture: (String) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    val repo = remember { PrefsRepository(app) }
    var burst by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var askCapture by remember { mutableStateOf(false) }

    // a soft sound when the scene opens
    LaunchedEffect(Unit) {
        if (prefs.soundOn) {
            Sfx.play(context, if (prefs.soundChoice == "tweet") R.raw.bird_tweet else R.raw.chime_soft, 0.45f)
        }
    }
    BackHandler { onDone() }

    fun celebrate() {
        scope.launch {
            burst = true
            delay(1800)
            burst = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        SceneForCharacter(req.characterId)

        // close button + the buddy's name
        Row(
            Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Pastel.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.8f)) {
                Text(
                    Roster.displayName(req.characterId),
                    color = Pastel.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // message + the two big buttons
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 6.dp
            ) {
                Text(
                    req.message,
                    color = Pastel.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.85f)) {
                Text(
                    status ?: ("💧 ${prefs.progressText} glasses   •   😊 ${prefs.smilesToday} today"),
                    color = Pastel.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CuteButton(
                    text = "I drank 💧",
                    onClick = {
                        scope.launch {
                            val total = repo.logGlass()
                            if (prefs.soundOn) Sfx.play(context, R.raw.water_plop, 0.6f)
                            Notifier.cancel(app)
                            SipSmileWidget().updateAll(app)
                            status = if (total == prefs.goal) "🎉 Goal reached! $total glasses today"
                            else if (total > prefs.goal) "⭐ Extra glass! ${total - prefs.goal} over your goal"
                            else "💧 Glass $total of ${prefs.goal}. Nice!"
                            celebrate()
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
                            val total = repo.logSmile()
                            if (prefs.soundOn) Sfx.play(context, R.raw.sparkle, 0.5f)
                            Notifier.cancel(app)
                            SipSmileWidget().updateAll(app)
                            status = "😊 Smile number $total today!"
                            celebrate()
                            askCapture = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    container = Pastel.PinkDeep,
                    fontSize = 18
                )
            }
            Spacer(Modifier.height(8.dp))
            Pill("Close", onClick = onDone)
        }

        if (burst) CelebrationBurst()
    }

    if (askCapture) {
        AlertDialog(
            onDismissRequest = { askCapture = false },
            title = { Text("Capture this smile? 📸") },
            text = { Text("Take a quick selfie so you can keep this happy moment in your Smile Album.") },
            confirmButton = {
                TextButton(onClick = {
                    askCapture = false
                    onCapture(req.characterId)
                }) { Text("Yes, take a selfie") }
            },
            dismissButton = { TextButton(onClick = { askCapture = false }) { Text("Not now") } }
        )
    }
}
