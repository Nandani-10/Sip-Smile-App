package com.sipsmile.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sipsmile.app.R
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.data.ReminderType
import com.sipsmile.app.data.Roster
import com.sipsmile.app.data.UserPrefs
import com.sipsmile.app.reminder.Notifier
import java.text.DateFormat
import java.util.Date

class SipSmileWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SipSmileWidget()
}

private val SMALL = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(250.dp, 110.dp)

private val INK = Color(0xFF2D3A4A)
private val MUTED = Color(0xFF6B7C93)
private val MINT_TEXT = Color(0xFF0F6B4F)

class SipSmileWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = PrefsRepository(context).read()
        provideContent { WidgetContent(prefs) }
    }
}

@Composable
private fun WidgetContent(prefs: UserPrefs) {
    val context = LocalContext.current
    val widgetSize = LocalSize.current
    val wide = widgetSize.width >= 200.dp

    val hasName = prefs.name.isNotBlank()
    val characterId = prefs.lastCharacter.ifBlank { Roster.DROP }
    // the buddy is happy once you drank something after the latest reminder
    val happy = prefs.lastDrinkEpoch > prefs.lastReminderEpoch && prefs.lastDrinkEpoch > 0L
    val message = when {
        !hasName -> "Open Sip & Smile to say hi!"
        prefs.lastMessage.isNotBlank() -> prefs.lastMessage
        else -> "Hi ${prefs.name}! Time to sip & smile"
    }
    val type = if (prefs.lastType == ReminderType.WATER.name) ReminderType.WATER else ReminderType.SMILE
    val openIntent = Notifier.sceneIntent(context, characterId, type, message)

    val timeText = if (prefs.lastReminderEpoch > 0L) {
        "Last reminder " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prefs.lastReminderEpoch))
    } else {
        "Waiting for your first reminder"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(10.dp)
            .clickable(actionStartActivity(openIntent)),
        contentAlignment = Alignment.Center
    ) {
        if (wide) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(Roster.widgetImage(characterId, happy)),
                    contentDescription = Roster.displayName(characterId),
                    modifier = GlanceModifier.size(88.dp)
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        maxLines = 3,
                        style = TextStyle(color = ColorProvider(INK), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = timeText,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(MUTED), fontSize = 11.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    DrinkButton(prefs)
                }
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(Roster.widgetImage(characterId, happy)),
                    contentDescription = Roster.displayName(characterId),
                    modifier = GlanceModifier.size(56.dp)
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = message,
                    maxLines = 2,
                    style = TextStyle(
                        color = ColorProvider(INK),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                DrinkButton(prefs)
            }
        }
    }
}

@Composable
private fun DrinkButton(prefs: UserPrefs) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_btn_bg))
            .padding(vertical = 6.dp, horizontal = 6.dp)
            .clickable(actionRunCallback<DrinkAction>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "I drank 💧  ${prefs.glassesToday}/${prefs.goal}",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(MINT_TEXT),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

/** The "I drank" button on the widget. */
class DrinkAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PrefsRepository(context).logGlass()
        Notifier.cancel(context)
        SipSmileWidget().updateAll(context)
    }
}
