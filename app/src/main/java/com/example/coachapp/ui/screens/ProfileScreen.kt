package com.example.coachapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.AssessmentRecord
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
    isStageOpen: Boolean = false,
    cdeAssignments: List<com.example.coachapp.data.CdeAssignment> = emptyList(),
    vivierPrincipal: List<JoueurVivier> = emptyList(),
    vivierInferieur: List<JoueurVivier> = emptyList(),
    slotsPersistes: List<com.example.coachapp.ui.screens.JoueurSlot?> = emptyList(),
    bancPersiste: List<com.example.coachapp.ui.screens.JoueurSlot?> = emptyList(),
    selectionAlerteMessage: String? = null,
    onOuverture: (categorie: String) -> Unit = {},
    onSlotChange: (index: Int, type: String, joueur: com.example.coachapp.ui.screens.JoueurSlot?) -> Unit = { _, _, _ -> },
    onSauvegarder: (principal: List<com.example.coachapp.ui.screens.JoueurSlot?>, banc: List<com.example.coachapp.ui.screens.JoueurSlot?>) -> Unit = { _, _ -> },
    onEnvoyerSelection: (categorie: String) -> Unit = {},
    onUpdateConfig: (SeasonConfig) -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToPresident: () -> Unit = {},
    onNavigateToGlobalAssessment: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var expandedSection by remember { mutableStateOf("IDENTITY") }
    var categorieEnConvocation by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF001529)) // Same Deep Blue as Dashboard
    ) {
        // Decorative Blur Blobs
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = 100.dp)
                .size(250.dp)
                .background(Color(0xFF9C27B0).copy(alpha = 0.15f), CircleShape)
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .size(300.dp)
                .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape)
                .blur(90.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mon Profil", 
                        style = MaterialTheme.typography.headlineLarge, 
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Coach ${seasonConfig.coachProfile.nickname.ifEmpty { seasonConfig.coachProfile.firstName }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00B4D8)
                    )
                }
                RoleBadge(userRole)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 1 : IDENTITY ---
            ProfileExpandableSection(
                title = "Ma Carte d'Identité",
                description = "Gérez votre identité et club.",
                icon = Icons.Default.Person,
                isExpanded = expandedSection == "IDENTITY",
                onToggle = { expandedSection = if (expandedSection == "IDENTITY") "" else "IDENTITY" }
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IdentityRow("Prénom", seasonConfig.coachProfile.firstName)
                    IdentityRow("Nom", seasonConfig.coachProfile.lastName)
                    IdentityRow("Surnom", seasonConfig.coachProfile.nickname)
                    IdentityRow("Club", seasonConfig.coachProfile.clubName)
                    IdentityRow("Niveau", seasonConfig.coachProfile.formationLevel)
                    IdentityRow("Persona", seasonConfig.coachProfile.coachPersona)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- SECTION 2 : HISTORY ---
            ProfileExpandableSection(
                title = "Historique",
                description = "Retrouvez vos bilans passés.",
                icon = Icons.Default.History,
                isExpanded = expandedSection == "HISTORY",
                onToggle = { expandedSection = if (expandedSection == "HISTORY") "" else "HISTORY" }
            ) {
                HistoryListInProfile(history, dateFormat)
            }

            // --- SECTION 3 : HUB CDE ---
            if (isCoachCde) {
                cdeAssignments.forEach { assignment ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val isPrincipalRole = assignment.role == "selection_principal"
                    val displaySexe = if (assignment.sexe == "M") "Masculin" else if (assignment.sexe == "F") "Féminin" else ""
                    
                    ProfileExpandableSection(
                        title = "Hub CDE — ${assignment.categorie} $displaySexe",
                        description = if (isPrincipalRole) "Coach principal · Sélection " else "Coach adjoint · Lecture",
                        icon = Icons.Default.Stars,
                        isExpanded = expandedSection == "CDE_${assignment.categorie}_${assignment.sexe}",
                        onToggle = { 
                            val key = "CDE_${assignment.categorie}_${assignment.sexe}"
                            expandedSection = if (expandedSection == key) "" else key 
                        }
                    ) {
                        HubCdeContent(
                            cdeCategorie = assignment.categorie,
                            cdeRole = assignment.role,
                            isPrincipal = isPrincipalRole,
                            isStageOpen = isStageOpen,
                            slots = slotsPersistes,
                            alerteMessage = selectionAlerteMessage,
                            onShowConvocation = { categorieEnConvocation = assignment.categorie },
                            onEnvoyer = { onEnvoyerSelection(assignment.categorie) }
                        )
                    }
                }
            }

            // --- SECTION 4 : HUB ADMINISTRATION ---
            if (userRole == com.example.coachapp.data.UserRole.PRESIDENT_CLUB || userRole == com.example.coachapp.data.UserRole.REFERENT_TECH) {
                Spacer(modifier = Modifier.height(12.dp))
                ProfileExpandableSection(
                    title = if (userRole == com.example.coachapp.data.UserRole.PRESIDENT_CLUB) "Hub Président" else "Hub Référent Technique",
                    description = "Gérez les collectifs et effectifs.",
                    icon = Icons.Default.Business,
                    isExpanded = expandedSection == "PRESIDENT",
                    onToggle = { expandedSection = if (expandedSection == "PRESIDENT") "" else "PRESIDENT" }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Administration : ${seasonConfig.coachProfile.clubName.ifEmpty { "Mon Club" }}", 
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToPresident,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Dashboard, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Ouvrir le Tableau de Bord Club", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onNavigateToGlobalAssessment,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Assessment, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text("Lancer mon Bilan de Carrière", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Déconnexion", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (categorieEnConvocation != null) {
        val isPrincipal = cdeAssignments.find { it.categorie == categorieEnConvocation }?.role == "selection_principal"
        ConvocationSheet(
            isVisible = true,
            onDismiss = { categorieEnConvocation = null },
            cdeCategorie = categorieEnConvocation!!,
            vivierPrincipal = vivierPrincipal,
            vivierInferieur = vivierInferieur,
            slotsPersistes = slotsPersistes,
            bancPersiste = bancPersiste,
            isEditable = isPrincipal,
            onOuverture = { onOuverture(categorieEnConvocation!!) },
            onSlotChange = { index, type, joueur -> onSlotChange(index, type, joueur) },
            onSauvegarder = { principal, banc -> onSauvegarder(principal, banc) }
        )
    }
}

@Composable
fun IdentityRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        Text(value.ifEmpty { "Non renseigné" }, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

// ----------------------------------------------------------------
// HUB CDE — contenu de la section
// ----------------------------------------------------------------
@Composable
fun HubCdeContent(
    cdeCategorie: String,
    cdeRole: String,
    isPrincipal: Boolean,
    isStageOpen: Boolean = false,
    slots: List<JoueurSlot?>,
    alerteMessage: String? = null,
    onShowConvocation: () -> Unit,
    onEnvoyer: () -> Unit = {}
) {
    val countConvoques = slots.count { it != null }

    Column(modifier = Modifier.padding(8.dp)) {
        if (isStageOpen && isPrincipal) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = Color(0xFF1D9E75).copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1D9E75).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Inscriptions Ouvertes", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text("Le stage $cdeCategorie est ouvert. Finalisez votre liste !", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        if (alerteMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = Color.Red.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(alerteMessage, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Surface(
                color = if (isPrincipal) Color(0xFF0C447C).copy(alpha = 0.2f) else Color(0xFF5F5E5A).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = if (isPrincipal) "Sélectionneur Principal" else "Coach Adjoint",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = cdeCategorie,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (countConvoques > 0) "$countConvoques convocation${if(countConvoques>1) "s" else ""} en attente de validation"
                           else "Aucune convocation en attente",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (countConvoques > 0) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                if (isPrincipal) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onShowConvocation,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (countConvoques > 0) "Modifier la sélection" else "Nouvelle convocation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (countConvoques > 0) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onShowConvocation,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Consulter la sélection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (countConvoques > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Sélection en cours",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    slots.filterNotNull().forEachIndexed { idx, joueur ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(24.dp),
                                color = Color.White.copy(alpha = 0.4f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${joueur.prenom} ${joueur.nom}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    joueur.club,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            if (joueur.estSurclasse) {
                                Surface(
                                    color = Color.Red.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color.Red.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "SURCLASSÉ",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 8.sp,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                        if (idx < slots.filterNotNull().size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }

            if (isPrincipal) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (countConvoques < 14) {
                    Surface(
                        color = Color.Yellow.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = BorderStroke(0.5.dp, Color.Yellow.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sélection incomplète (14 places disponibles)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                Button(
                    onClick = onEnvoyer,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9E75), contentColor = Color.White),
                    enabled = true
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Finaliser et Envoyer ma sélection", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------------------
// Composants existants — inchangés
// ----------------------------------------------------------------

@Composable
fun RoleBadge(role: com.example.coachapp.data.UserRole) {
    val (label, color, icon) = when (role) {
        com.example.coachapp.data.UserRole.USER           -> Triple("USER",      Color.Gray,       Icons.Default.Person)
        com.example.coachapp.data.UserRole.ADMIN          -> Triple("ADMIN",     Color(0xFF2196F3), Icons.Default.Shield)
        com.example.coachapp.data.UserRole.MEGADMIN       -> Triple("MEGADMIN",  Color.Red,        Icons.Default.AutoAwesome)
        com.example.coachapp.data.UserRole.PRESIDENT_CLUB -> Triple("PRÉSIDENT", Color(0xFFFF9800), Icons.Default.Stars)
        com.example.coachapp.data.UserRole.REFERENT_TECH  -> Triple("REF. TECH", Color(0xFF00B4D8), Icons.Default.ManageAccounts)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(56.dp),
            border = BorderStroke(1.5.dp, color.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 0.5.sp
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
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
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(description, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
            if (isExpanded) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 12.dp)) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Diagnostic du ${dateFormat.format(Date(record.date))}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                record.scores.forEach { (id, score) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(id.take(3).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        Text("%.1f".format(score), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black, color = Color(0xFF00B4D8))
                    }
                }
            }
            record.coachNote?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.03f), 
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Text(
                        it, 
                        modifier = Modifier.padding(8.dp), 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
