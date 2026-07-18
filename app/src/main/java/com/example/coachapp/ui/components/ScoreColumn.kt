package com.example.coachapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.Team

@Composable
fun ScoreColumn(
    teamName: String,
    score: Int,
    sets: Int,
    teams: List<Team> = emptyList(),
    onTeamChange: (String) -> Unit = {},
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isLeft: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // --- TEAM SELECTOR DROPDOWN ---
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teamName.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(alpha = 0.6f))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF001529)).border(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                DropdownMenuItem(
                    text = { Text("ADVERSAIRE", color = Color.White, fontWeight = FontWeight.Bold) },
                    onClick = { onTeamChange("ADVERSAIRE"); expanded = false }
                )
                teams.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(team.name, color = Color.White) },
                        onClick = { onTeamChange(team.name); expanded = false }
                    )
                }
            }
        }

        Text(
            text = "SETS : $sets",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00B4D8),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // --- LED SCORE WITH SIDE CORRECTION ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLeft) {
                CorrectionButton(onDecrement)
                Spacer(Modifier.width(8.dp))
            }

            Surface(
                modifier = Modifier.size(110.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.1f)),
                onClick = onIncrement
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(80.dp).background(Color.Red.copy(alpha = 0.05f), CircleShape))
                    Text(
                        text = "%02d".format(score),
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            if (!isLeft) {
                Spacer(Modifier.width(8.dp))
                CorrectionButton(onDecrement)
            }
        }
    }
}

@Composable
fun CorrectionButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Remove, null, tint = Color.Red, modifier = Modifier.size(20.dp))
        }
    }
}
