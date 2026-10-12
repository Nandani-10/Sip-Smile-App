package com.sipsmile.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.data.ReminderType
import com.sipsmile.app.reminder.Notifier
import com.sipsmile.app.reminder.Scheduler
import com.sipsmile.app.ui.HomeScreen
import com.sipsmile.app.ui.OnboardingScreen
import com.sipsmile.app.ui.SceneRequest
import com.sipsmile.app.ui.SceneScreen
import com.sipsmile.app.ui.SettingsScreen
import com.sipsmile.app.ui.SipSmileTheme
import com.sipsmile.app.widget.SipSmileWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sceneRequest by mutableStateOf<SceneRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sceneRequest = readScene(intent)
        setContent {
            SipSmileTheme {
                AppRoot(scene = sceneRequest, onSceneDone = { sceneRequest = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readScene(intent)?.let { sceneRequest = it }
    }

    override fun onStart() {
        super.onStart()
        AppState.foreground = true
    }

    override fun onStop() {
        AppState.foreground = false
        super.onStop()
    }

    /** The notification and the widget open the app with the scene to show. Consumed once. */
    private fun readScene(intent: Intent?): SceneRequest? {
        if (intent == null || !intent.getBooleanExtra(EXTRA_SCENE, false)) return null
        val character = intent.getStringExtra(EXTRA_CHARACTER) ?: return null
        val type = if (intent.getStringExtra(EXTRA_TYPE) == ReminderType.WATER.name) ReminderType.WATER else ReminderType.SMILE
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        intent.removeExtra(EXTRA_SCENE)
        return SceneRequest(character, type, message)
    }

    companion object {
        const val EXTRA_SCENE = "sipsmile.scene"
        const val EXTRA_CHARACTER = "sipsmile.character"
        const val EXTRA_TYPE = "sipsmile.type"
        const val EXTRA_MESSAGE = "sipsmile.message"
    }
}

@Composable
private fun AppRoot(scene: SceneRequest?, onSceneDone: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext
    val scope = rememberCoroutineScope()
    val repo = remember { PrefsRepository(app) }
    val prefs by repo.flow.collectAsState(initial = null)
    var screen by rememberSaveable { mutableStateOf("home") }
    var replay by remember { mutableStateOf<SceneRequest?>(null) }

    // every time the app opens, make sure the next reminder alarm is in place
    LaunchedEffect(Unit) {
        Notifier.createChannels(app)
        Scheduler.scheduleNext(app, repo.read())
    }

    val p = prefs
    if (p == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    val activeScene = scene ?: replay
    when {
        !p.onboardingDone -> OnboardingScreen { name, start, end, interval ->
            scope.launch {
                repo.completeOnboarding(name, start, end, interval)
                Scheduler.scheduleNext(app, repo.read())
                SipSmileWidget().updateAll(app)
            }
        }

        activeScene != null -> SceneScreen(activeScene, p) {
            replay = null
            onSceneDone()
        }

        screen == "settings" -> SettingsScreen(p) { screen = "home" }

        else -> HomeScreen(
            prefs = p,
            onSettings = { screen = "settings" },
            onOpenScene = { replay = it }
        )
    }
}
