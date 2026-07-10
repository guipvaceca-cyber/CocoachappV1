package com.example.coachapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.LaboResource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LaboDetailScreen(
    resource: LaboResource,
    onBack: () -> Unit,
    onTestNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFocalPoint by remember { mutableStateOf(resource.focalPoints.firstOrNull()) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // --- TOP BAR ---
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(resource.title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text("Par ${resource.authorNickname} • v${resource.versionsCount}", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            // --- DESCRIPTION ---
            item {
                Text("LA SITUATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(resource.description, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))
            }

            // --- FOCAL POINTS (THE CORE PHILOSOPHY) ---
            item {
                Text("VOTRE FOCALISATION (INTENTION)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Une situation, plusieurs angles d'analyse.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    resource.focalPoints.forEach { fp ->
                        FilterChip(
                            selected = selectedFocalPoint == fp,
                            onClick = { selectedFocalPoint = fp },
                            label = { Text(fp.title) }
                        )
                    }
                }
                
                AnimatedVisibility(visible = selectedFocalPoint != null) {
                    Card(
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Focus ${selectedFocalPoint?.title} :", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(4.dp))
                            Text(selectedFocalPoint?.instruction ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // --- CRASH TESTS ---
            item {
                Text("CRASH-TESTS TERRAIN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }

            if (resource.crashTests.isEmpty()) {
                item { Text("Soyez le premier à tester cette situation !", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
            } else {
                items(resource.crashTests) { test ->
                    Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (test.result == "Succès") Color(0xFFE8F5E9) else Color(0xFFFFF1F0),
                                    shape = CircleShape
                                ) {
                                    Text(test.result, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (test.result == "Succès") Color(0xFF2E7D32) else Color.Red)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(test.coachNickname, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(test.feedback, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onTestNow,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("TESTER CETTE SITUATION (PLANIFIER)")
                }
                Spacer(Modifier.height(100.dp)) 
            }
        }
    }
}
