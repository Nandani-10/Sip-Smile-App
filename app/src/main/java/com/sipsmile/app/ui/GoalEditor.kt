package com.sipsmile.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipsmile.app.data.UserPrefs
import com.sipsmile.app.data.formatMl
import com.sipsmile.app.data.suggestedGlasses

@Composable
private fun RoundStep(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                CircleShape
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

/** Daily goal (glasses) + glass size + an optional "suggest from my weight" helper. */
@Composable
fun GoalEditor(prefs: UserPrefs, onGoal: (Int) -> Unit, onGlassMl: (Int) -> Unit) {
    var weight by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundStep("−", prefs.goal > UserPrefs.MIN_GOAL) { onGoal(prefs.goal - 1) }
            Spacer(Modifier.width(18.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${prefs.goal}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (prefs.goal == 1) "glass a day" else "glasses a day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(18.dp))
            RoundStep("+", prefs.goal < UserPrefs.MAX_GOAL) { onGoal(prefs.goal + 1) }
        }
        Text("Size of one glass", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (ml in UserPrefs.GLASS_SIZES) {
                FilterChip(
                    selected = prefs.glassMl == ml,
                    onClick = { onGlassMl(ml) },
                    label = { Text("$ml ml") }
                )
            }
        }
        Text(
            "Your goal: ${formatMl(prefs.goalMl)} a day",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Weight (kg), optional") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            Pill("Suggest", enabledLook = weight.isNotBlank()) {
                val kg = weight.toIntOrNull() ?: 0
                if (kg > 0) onGoal(suggestedGlasses(kg, prefs.glassMl))
            }
        }
        Text(
            "A common guide is about 33 ml per kg. It's only a suggestion, so pick what feels right for you.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
    }
}
