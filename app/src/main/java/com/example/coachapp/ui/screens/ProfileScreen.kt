package com.example.coachapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.AssessmentRecord
import com.example.coachapp.data.CdeVivierParser
import com.example.coachapp.data.SeasonConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    history: List<AssessmentRecord> = emptyList(),
    seasonConfig: SeasonConfig = SeasonConfig(),
    userRole: com.example.coachapp.data.UserRole = com.example.coachapp.data.UserRole.USER,
    isCoachCde: Boolean = false,
    cdeCategorie: String? = null,
    cdeRole: String? = null,    // "principal" | "adjoint"
    onUpdateConfig: (SeasonConfig) -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToGlobalAssessment: () -> Unit = {}
) {
    android.util.Log.d("PROFILE_DEBUG", "cdeRole reçu = $cdeRole | isCoachCde = $isCoachCde")
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var expandedSection by remember { mutableStateOf("IDENTITY") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mon Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    text = "Coach ${seasonConfig.coachProfile.nickname.ifEmpty { seasonConfig.coachProfile.firstName }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            RoleBadge(userRole)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 1 : IDENTITY ---
        ProfileExpandableSection(
            title = "Ma Carte d'Identité",
            description = "Gérez votre nom, surnom et club d'appartenance.",
            icon = Icons.Default.Person,
            isExpanded = expandedSection == "IDENTITY",
            onToggle = { expandedSection = if (expandedSection == "IDENTITY") "" else "IDENTITY" }
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Prénom: ${seasonConfig.coachProfile.firstName}", fontWeight = FontWeight.Bold)
                Text("Nom: ${seasonConfig.coachProfile.lastName}")
                Text("Surnom: ${seasonConfig.coachProfile.nickname}")
                Text("Club: ${seasonConfig.coachProfile.clubName}")
                Text("Niveau: ${seasonConfig.coachProfile.formationLevel}")
                Text("Persona: ${seasonConfig.coachProfile.coachPersona}")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- SECTION 2 : HISTORY ---
        ProfileExpandableSection(
            title = "Mon Historique de Bilan",
            description = "Retrouvez vos diagnostics de carrière passés.",
            icon = Icons.Default.History,
            isExpanded = expandedSection == "HISTORY",
            onToggle = { expandedSection = if (expandedSection == "HISTORY") "" else "HISTORY" }
        ) {
            HistoryListInProfile(history, dateFormat)
        }

        // --- SECTION 3 : HUB CDE (visible uniquement si coach CDE) ---
        if (isCoachCde && cdeCategorie != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileExpandableSection(
                title = "Hub CDE — $cdeCategorie",
                description = if (cdeRole == "selection_principal") "Coach principal · Sélection "
                else "Coach adjoint · Vue lecture",
                icon = Icons.Default.Stars,
                isExpanded = expandedSection == "CDE",
                onToggle = { expandedSection = if (expandedSection == "CDE") "" else "CDE" }
            ) {
                HubCdeContent(
                    cdeCategorie = cdeCategorie,
                    cdeRole = cdeRole ?: "adjoint",
                    isPrincipal = cdeRole == "selection_principal"
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = onNavigateToGlobalAssessment,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Assessment, null)
            Spacer(Modifier.width(8.dp))
            Text("Lancer mon Bilan de Carrière")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Déconnexion & Réinitialisation", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------------------
// HUB CDE — contenu de la section
// ----------------------------------------------------------------
@Composable
fun HubCdeContent(
    cdeCategorie: String,
    cdeRole: String,
    isPrincipal: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importStatut by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var nbJoueursImportes by remember { mutableStateOf(0) }
    var showConvocation by remember { mutableStateOf(false) }

    // Picker de fichier CSV
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isImporting = true
                importStatut = null

                val result = CdeVivierParser.parserCSV(context, uri)
                result.fold(
                    onSuccess = { joueurs ->
                        val stats = CdeVivierParser.stats(joueurs)
                        val syncResult = CdeVivierParser.syncVersSupabase(joueurs)
                        syncResult.fold(
                            onSuccess = { nb ->
                                nbJoueursImportes = nb
                                importStatut = "✓ $nb joueurs importés — ${stats.entries.joinToString { "${it.key}: ${it.value}" }}"
                            },
                            onFailure = { e ->
                                importStatut = "Erreur sync Supabase : ${e.localizedMessage}"
                            }
                        )
                    },
                    onFailure = { e ->
                        importStatut = "Erreur lecture CSV : ${e.localizedMessage}"
                    }
                )
                isImporting = false
            }
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {

        // Badge rôle CDE
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Surface(
                color = if (isPrincipal) Color(0xFFE6F1FB) else Color(0xFFF1EFE8),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isPrincipal) "Coach Principal" else "Coach Adjoint",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPrincipal) Color(0xFF0C447C) else Color(0xFF5F5E5A)
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = Color(0xFFE1F5EE),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = cdeCategorie,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF085041)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

        // Import CSV — uniquement pour le coach principal
        if (isPrincipal) {
            Text(
                "Vivier de joueurs",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (nbJoueursImportes > 0) {
                Text(
                    "$nbJoueursImportes joueurs dans le vivier",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF085041),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                Text("Import en cours...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                OutlinedButton(
                    onClick = { csvLauncher.launch("text/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Importer CSV FFVB ($cdeCategorie)", fontSize = 13.sp)
                }
            }

            importStatut?.let { statut ->
                Spacer(Modifier.height(8.dp))
                val isError = statut.startsWith("Erreur")
                Surface(
                    color = if (isError) Color(0xFFFAECE7) else Color(0xFFE1F5EE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        statut,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) Color(0xFF993C1D) else Color(0xFF085041)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }

        // Accès aux convocations
        Text(
            "Convocations",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Placeholder — sera remplacé par la liste des convocations depuis Supabase
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Aucune convocation en cours",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (isPrincipal) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { 
                            android.util.Log.d("DEBUG_CDE", "Click Nouvelle convocation")
                            showConvocation = true 
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nouvelle convocation", fontSize = 13.sp)
                    }
                }
            }
        }

        ConvocationSheet(
            isVisible = showConvocation,
            onDismiss = { showConvocation = false },
            cdeCategorie = cdeCategorie,
            vivierPrincipal = emptyList(),
            vivierInferieur = emptyList()
        )
    }
}

// ----------------------------------------------------------------
// Composants existants — inchangés
// ----------------------------------------------------------------

@Composable
fun RoleBadge(role: com.example.coachapp.data.UserRole) {
    val (label, color, icon) = when (role) {
        com.example.coachapp.data.UserRole.USER     -> Triple("USER",     Color.Gray,                          Icons.Default.Person)
        com.example.coachapp.data.UserRole.ADMIN    -> Triple("ADMIN",    MaterialTheme.colorScheme.primary,   Icons.Default.Shield)
        com.example.coachapp.data.UserRole.MEGADMIN -> Triple("MEGADMIN", Color.Red,                           Icons.Default.AutoAwesome)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, color)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ProfileExpandableSection(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            if (isExpanded) {
                Box(modifier = Modifier.padding(16.dp).padding(top = 0.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun HistoryListInProfile(history: List<AssessmentRecord>, dateFormat: SimpleDateFormat) {
    if (history.isEmpty()) {
        Text("Aucun historique pour le moment.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    } else {
        Column {
            history.reversed().forEach { record ->
                HistoryCard(record, dateFormat)
            }
        }
    }
}

@Composable
fun HistoryCard(record: AssessmentRecord, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Diagnostic du ${dateFormat.format(Date(record.date))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                record.scores.forEach { (id, score) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(id.take(3).uppercase(), style = MaterialTheme.typography.labelSmall)
                        Text("%.1f".format(score), style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            record.coachNote?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f), shape = RoundedCornerShape(4.dp)) {
                    Text(it, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}