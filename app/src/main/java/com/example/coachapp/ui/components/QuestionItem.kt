package com.example.coachapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
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
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF00B4D8),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..5).forEach { score ->
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentScore == score) Color(0xFF00B4D8) 
                                    else Color.White.copy(alpha = 0.4f),
                            fontWeight = if (currentScore == score) FontWeight.Black else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
