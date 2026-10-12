@file:OptIn(ExperimentalMaterial3Api::class)

package com.sipsmile.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipsmile.app.reminder.Scheduler

@Composable
fun TimePickDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) }
    )
}

/** A tappable row "Start reminding at   8:00 AM" that opens a time picker. */
@Composable
fun TimeRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(Scheduler.format12(minutes), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
    }
    if (open) {
        TimePickDialog(
            initialMinutes = minutes,
            onDismiss = { open = false },
            onConfirm = {
                open = false
                onChange(it)
            }
        )
    }
}

/**
 * Start / end time + interval chips + a live preview of the reminder times.
 * [onChange] is only called with a valid combination (end after start).
 */
@Composable
fun WindowEditor(
    startMinutes: Int,
    endMinutes: Int,
    intervalMinutes: Int,
    onChange: (start: Int, end: Int, interval: Int) -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TimeRow("Start reminding at", startMinutes) { s ->
            if (s < endMinutes) {
                error = null
                onChange(s, endMinutes, intervalMinutes)
            } else error = "Start must be before the end time"
        }
        TimeRow("Stop reminding at", endMinutes) { e ->
            if (e > startMinutes) {
                error = null
                onChange(startMinutes, e, intervalMinutes)
            } else error = "End must be after the start time"
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Every", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            for (m in listOf(15, 30, 60)) {
                FilterChip(
                    selected = intervalMinutes == m,
                    onClick = { onChange(startMinutes, endMinutes, m) },
                    label = { Text(if (m == 60) "1 hour" else "$m min") }
                )
            }
        }
        val message = error
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Text(
            "Reminders at " + Scheduler.previewText(startMinutes, endMinutes, intervalMinutes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(2.dp))
    }
}
