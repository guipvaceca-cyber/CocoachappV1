package com.example.coachapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun QuestionItem(
    text: String,
    currentScore: Int,
    onScoreSelected: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Slider(
                    value = if (currentScore == 0) 3f else currentScore.toFloat(),
                    onValueChange = { 
                        val newValue = it.toInt()
                        if (newValue != currentScore) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onScoreSelected(newValue)
                        }
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..5).forEach { score ->
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (currentScore == score) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (currentScore == score) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
