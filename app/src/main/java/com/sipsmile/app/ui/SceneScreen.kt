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
fun SceneScreen(req: SceneRequest, prefs: UserPrefs, onDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    val repo = remember { PrefsRepository(app) }
    var celebrating by remember { mutableStateOf(false) }
    val water = req.type == ReminderType.WATER

    // a soft sound when the scene opens
    LaunchedEffect(Unit) {
        if (prefs.soundOn) {
            Sfx.play(context, if (prefs.soundChoice == "tweet") R.raw.bird_tweet else R.raw.chime_soft, 0.45f)
        }
    }
    BackHandler { onDone() }

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

        // message + big action button
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
            Spacer(Modifier.height(16.dp))
            CuteButton(
                text = if (water) "I drank 💧" else "😊 I smiled",
                onClick = {
                    if (!celebrating) {
                        celebrating = true
                        scope.launch {
                            if (water) {
                                repo.logGlass()
                                if (prefs.soundOn) Sfx.play(context, R.raw.water_plop, 0.6f)
                            } else if (prefs.soundOn) {
                                Sfx.play(context, R.raw.sparkle, 0.5f)
                            }
                            Notifier.cancel(app)
                            SipSmileWidget().updateAll(app)
                            delay(1700)
                            onDone()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                container = if (water) Pastel.Sky else Pastel.PinkDeep,
                fontSize = 20
            )
        }

        if (celebrating) CelebrationBurst()
    }
}
