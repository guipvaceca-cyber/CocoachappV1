package com.example.coachapp.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.coachapp.data.ClubEvent
import com.example.coachapp.data.ClubEventScope
import com.example.coachapp.data.ClubEventType
import com.example.coachapp.data.Team
import com.example.coachapp.data.model.Collectif
import com.example.coachapp.data.model.CollectifAvecDetail
import com.example.coachapp.data.model.CollectifStatut
import com.example.coachapp.data.model.Poste
import com.example.coachapp.ui.president.PresidentUiState
import com.example.coachapp.ui.president.PresidentViewModel
import androidx.compose.material.icons.filled.QrCode
import com.example.coachapp.ui.util.AgeUtils
import com.example.coachapp.ui.util.PlayerQrData
import com.example.coachapp.ui.util.QrCodeDisplay
import com.example.coachapp.ui.util.TeamQrPayload
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresidentDashboardScreen(
    viewModel: PresidentViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.chargerCollectifs()
    }
    var showAddCollectif by remember { mutableStateOf(false) }
    var showAddClubEvent by remember { mutableStateOf(false) }
    var collectifIdForInvite by remember { mutableStateOf<String?>(null) }
    var collectifIdForSelfAssign by remember { mutableStateOf<String?>(null) }
    var collectifForDelete by remember { mutableStateOf<CollectifAvecDetail?>(null) }
    var collectifForQr by remember { mutableStateOf<CollectifAvecDetail?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestion du Club", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.chargerCollectifs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCollectif = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau Collectif")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is PresidentUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PresidentUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text(state.message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Button(onClick = { viewModel.chargerCollectifs() }) {
                            Text("Réessayer")
                        }
                    }
                }
                is PresidentUiState.Success -> {
                    val clubEvents by viewModel.clubEvents.collectAsState()
                    PresidentContent(
                        collectifs = state.collectifs,
                        collectifsEnAttente = state.collectifsEnAttente,
                        clubEvents = clubEvents,
                        selectedSeason = viewModel.selectedSeason,
                        onSeasonChange = { viewModel.updateSeason(it) },
                        onInviteClick = { collectifIdForInvite = it },
                        onDeleteClick = { collectifForDelete = it },
                        onSelfAssignClick = { collectifIdForSelfAssign = it },
                        onQrClick = { collectifForQr = it },
                        onAddClubEventClick = { showAddClubEvent = true },
                        onValidateClick = { id ->
                            viewModel.validerCollectif(id, 
                                onSuccess = { scope.launch { snackbarHostState.showSnackbar("Collectif validé !") } },
                                onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                            )
                        },
                        onRejectClick = { id ->
                            viewModel.refuserEffectif(id,
                                onSuccess = { scope.launch { snackbarHostState.showSnackbar("Effectif renvoyé pour modification") } },
                                onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                            )
                        }
                    )
                }
            }
        }

        if (showAddCollectif) {
            AddCollectifDialog(
                initialSeason = viewModel.selectedSeason,
                onDismiss = { showAddCollectif = false },
                onConfirm = { nom, cat, sexe, format, comp, sai ->
                    viewModel.creerCollectif(nom, cat, sexe, format, comp, sai,
                        onSuccess = { 
                            showAddCollectif = false 
                            scope.launch { snackbarHostState.showSnackbar("Collectif créé !") }
                        },
                        onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                    )
                }
            )
        }

        if (showAddClubEvent) {
            val presidentSuccess = uiState as? PresidentUiState.Success
            AddClubEventDialog(
                collectifs = presidentSuccess?.collectifs ?: emptyList(),
                onDismiss = { showAddClubEvent = false },
                onConfirm = { event ->
                    viewModel.pushClubEvent(event,
                        onSuccess = { 
                            showAddClubEvent = false
                            scope.launch { snackbarHostState.showSnackbar("Événement club créé !") }
                        },
                        onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                    )
                }
            )
        }

        if (collectifIdForInvite != null) {
            InviteCoachDialog(
                onDismiss = { collectifIdForInvite = null },
                onConfirm = { email, tel, poste ->
                    viewModel.envoyerInvitation(collectifIdForInvite!!, email, tel, poste,
                        onSuccess = { collectifIdForInvite = null },
                        onError = { scope.launch { snackbarHostState.showSnackbar(it) } }
                    )
                }
            )
        }

        if (collectifForDelete != null) {
            DeleteCollectifDialog(
                collectifNom = collectifForDelete!!.collectif.nom,
                onDismiss = { collectifForDelete = null },
                onConfirm = {
                    viewModel.supprimerCollectif(
                        collectifId = collectifForDelete!!.collectif.id,
                        onSuccess = {
                            collectifForDelete = null
                            scope.launch { snackbarHostState.showSnackbar("Collectif supprimé") }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                }
            )
        }

        if (collectifIdForSelfAssign != null) {
            SelfAssignDialog(
                onDismiss = { collectifIdForSelfAssign = null },
                onConfirm = { poste ->
                    viewModel.rattacherSoiMeme(collectifIdForSelfAssign!!, poste,
                        onSuccess = {
                            collectifIdForSelfAssign = null
                            scope.launch { snackbarHostState.showSnackbar("Vous êtes maintenant rattaché !") }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                }
            )
        }

        if (collectifForQr != null) {
            val payload = TeamQrPayload(
                team = collectifForQr!!.collectif.nom,
                players = collectifForQr!!.joueurs.map { j ->
                    PlayerQrData(
                        n = j.nom,
                        p = j.prenom,
                        l = j.id,
                        c = j.categorie,
                        a = AgeUtils.calculateAge(j.dateNaissance)
                    )
                }
            )

            AlertDialog(
                onDismissRequest = { collectifForQr = null },
                title = { Text("QR Code de match", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Présentez ce code à la table de marque pour importer l'effectif.", 
                             fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        QrCodeDisplay(payload = payload)
                        Spacer(Modifier.height(8.dp))
                        Text(collectifForQr!!.collectif.nom, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                    }
                },
                confirmButton = {
                    Button(onClick = { collectifForQr = null }) { Text("Fermer") }
                }
            )
        }
    }
}

@Composable
fun PresidentContent(
    collectifs: List<CollectifAvecDetail>,
    collectifsEnAttente: List<CollectifAvecDetail>,
    clubEvents: List<com.example.coachapp.data.ClubEvent>,
    selectedSeason: String,
    onSeasonChange: (String) -> Unit,
    onInviteClick: (String) -> Unit,
    onDeleteClick: (CollectifAvecDetail) -> Unit,
    onSelfAssignClick: (String) -> Unit,
    onValidateClick: (String) -> Unit,
    onRejectClick: (String) -> Unit,
    onAddClubEventClick: () -> Unit,
    onQrClick: (CollectifAvecDetail) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SeasonSelector(selectedSeason, onSeasonChange)
        }

        // --- SECTION VIE DU CLUB ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF001529)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("VIE DU CLUB", style = MaterialTheme.typography.labelLarge, color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                        TextButton(onClick = onAddClubEventClick) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ajouter", fontSize = 12.sp)
                        }
                    }
                    
                    if (clubEvents.isEmpty()) {
                        Text("Aucun événement club prévu.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                    } else {
                        clubEvents.take(3).forEach { event ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF00B4D8), CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(event.title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("${event.date} à ${event.startTime} - ${event.location}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Les compositions soumises (Prêtes pour validation président) -> VERT
        val soumises = collectifs.filter { it.collectif.compoStatut == "soumise_president" && it.collectif.statut != CollectifStatut.EN_ATTENTE_CT }
        if (soumises.isNotEmpty()) {
            item {
                Text("COMPOSITIONS À VALIDER (${soumises.size})", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
            items(soumises) { detail ->
                CollectifCard(
                    detail = detail, 
                    onInviteClick = onInviteClick, 
                    onDeleteClick = onDeleteClick, 
                    onSelfAssignClick = onSelfAssignClick, 
                    onValidateClick = onValidateClick,
                    onRejectClick = onRejectClick,
                    onQrClick = onQrClick
                )
            }
        }

        // 2. Les collectifs validés par président (En attente CT) -> BLEU
        if (collectifsEnAttente.isNotEmpty()) {
            item {
                Text("VALIDÉS (EN ATTENTE CT) (${collectifsEnAttente.size})", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
            }
            items(collectifsEnAttente) { detail ->
                CollectifCard(
                    detail = detail, 
                    onInviteClick = onInviteClick, 
                    onDeleteClick = onDeleteClick, 
                    onSelfAssignClick = onSelfAssignClick, 
                    onValidateClick = {}, 
                    onRejectClick = {},
                    onQrClick = onQrClick
                )
            }
        }

        // 3. Le reste du vivier (En construction / Nouveaux) -> ORANGE / ROUGE
        item {
            Text("MES COLLECTIFS (${collectifs.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        items(collectifs.filter { it.collectif.statut != CollectifStatut.EN_ATTENTE_CT && it.collectif.compoStatut != "soumise_president" }) { detail ->
            CollectifCard(
                detail = detail, 
                onInviteClick = onInviteClick, 
                onDeleteClick = onDeleteClick, 
                onSelfAssignClick = onSelfAssignClick, 
                onValidateClick = {},
                onRejectClick = {}
            )
        }
        
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun CollectifCard(
    detail: CollectifAvecDetail,
    onInviteClick: (String) -> Unit,
    onDeleteClick: (CollectifAvecDetail) -> Unit,
    onSelfAssignClick: (String) -> Unit,
    onValidateClick: (String) -> Unit,
    onRejectClick: (String) -> Unit,
    onQrClick: (CollectifAvecDetail) -> Unit = {}
) {
    val collectif = detail.collectif
    var isExpanded by remember { mutableStateOf(false) }
    
    // Logique de coloration du bandeau
    val statusColor = when {
        collectif.statut == CollectifStatut.EN_ATTENTE_CT -> Color(0xFF2196F3) // Bleu - Validé
        collectif.compoStatut == "soumise_president" || collectif.compoStatut == "refusee" -> Color(0xFF4CAF50) // Vert - Soumis / Refusé
        detail.rattachements.isNotEmpty() || detail.invitationsEnAttente.isNotEmpty() -> Color(0xFFFF9800) // Orange - Encadrement
        else -> Color(0xFFF44336) // Rouge - Nouveau / Vide
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Bandeau de titre colorisé - CLIQUABLE pour plier/déplier
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${collectif.categorie} ${if(collectif.sexe == "M") "Masculin" else "Féminin"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                collectif.nom, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            if (!isExpanded) {
                                val mainCoach = detail.coachPrincipal
                                if (mainCoach != null) {
                                    Text(" • ", color = Color.White.copy(alpha = 0.8f))
                                    Text(
                                        mainCoach,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (collectif.statut == CollectifStatut.EN_ATTENTE_CT) {
                        IconButton(onClick = { onQrClick(detail) }) {
                            Icon(Icons.Default.QrCode, null, tint = Color.White)
                        }
                    }

                    if (!isExpanded) {
                        IconButton(onClick = { onDeleteClick(detail) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint = Color.White
                        )
                    } else {
                        IconButton(onClick = { isExpanded = false }) {
                            Icon(Icons.Default.ExpandLess, null, tint = Color.White)
                        }
                    }
                }
            }

            // Corps de la carte avec animation de dépliage
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Encadrement :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        StatusBadge(collectif.statut, collectif.compoStatut)
                        IconButton(onClick = { onDeleteClick(detail) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    detail.rattachements.forEach { ratt ->
                        CoachRow(
                            name = "${ratt.coachPrenom ?: ""} ${ratt.coachNom ?: ""}".trim().ifBlank { "Coach sans nom" },
                            poste = ratt.poste.label(),
                            statut = ratt.statut.name
                        )
                    }
                    
                    detail.invitationsEnAttente.forEach { inv ->
                        CoachRow(
                            name = inv.email ?: inv.telephone ?: "Inconnu",
                            poste = inv.poste.label(),
                            statut = "INVITÉ",
                            isInvitation = true
                        )
                    }

                    // Boutons d'invitation coach
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        val userId = com.example.coachapp.data.SupabaseManager.auth.currentUserOrNull()?.id
                        val isAlreadyIn = detail.rattachements.any { it.coachId == userId }

                        if (!isAlreadyIn && !detail.estComplet) {
                            TextButton(onClick = { onSelfAssignClick(collectif.id) }) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("M'ajouter", fontSize = 12.sp)
                            }
                        }

                        if (!detail.estComplet) {
                            TextButton(onClick = { onInviteClick(collectif.id) }) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("+ Inviter un coach", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // SECTION EFFECTIF (JOUEURS)
                    Text("Effectif (${detail.joueurs.size}) :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    if (detail.joueurs.isEmpty()) {
                        Text("Aucun joueur pour le moment", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        detail.joueurs.forEach { joueur ->
                            PlayerAuditRow(joueur, collectif.categorie)
                        }
                    }

                    // Actions de validation (Si soumis)
                    if (collectif.compoStatut == "soumise_president" && collectif.statut != CollectifStatut.EN_ATTENTE_CT) {
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { onRejectClick(collectif.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refuser", fontSize = 12.sp)
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { onValidateClick(collectif.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Valider", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerAuditRow(
    joueur: com.example.coachapp.data.model.JoueurCollectif,
    collectifCategorie: String
) {
    val isSurclasse = joueur.categorie != collectifCategorie
    val bgColor = if (isSurclasse) Color(0xFFFFF9C4) else Color(0xFFE3F2FD)
    val contentColor = if (isSurclasse) Color(0xFFF57F17) else Color(0xFF1565C0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${joueur.prenom} ${joueur.nom}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    "Licence : ${joueur.id.take(8)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            // Badge de catégorie réelle
            Surface(
                color = contentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    joueur.categorie,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun CoachRow(name: String, poste: String, statut: String, isInvitation: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = if (isInvitation) Color(0xFFFF9800) else Color(0xFF4CAF50)
        ) {}
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(poste, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Text(statut, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SeasonSelector(selectedSeason: String, onSeasonChange: (String) -> Unit) {
    val seasons = listOf("2024-2025", "2025-2026", "2026-2027")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Gestion de saison", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasons.forEach { season ->
                    FilterChip(
                        selected = selectedSeason == season,
                        onClick = { onSeasonChange(season) },
                        label = { Text(season) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(statut: CollectifStatut, compoStatut: String? = null) {
    val (label, color) = when {
        statut == CollectifStatut.EN_ATTENTE_CT -> "VALIDÉ (CT)" to Color(0xFF2196F3)
        compoStatut == "soumise_president" -> "À VALIDER" to Color(0xFF4CAF50)
        compoStatut == "refusee" -> "REFUSÉ" to Color.Red
        statut == CollectifStatut.ACTIF -> "ACTIF" to Color(0xFF4CAF50)
        statut == CollectifStatut.ARCHIVE -> "ARCHIVÉ" to Color.Gray
        statut == CollectifStatut.REFUSE -> "REFUSÉ" to Color.Red
        else -> "BROUILLON" to Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SelfAssignDialog(
    onDismiss: () -> Unit,
    onConfirm: (Poste) -> Unit
) {
    var selectedPoste by remember { mutableStateOf(Poste.PRINCIPAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prendre en charge ce collectif", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Quel sera votre rôle dans cette équipe ?")
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Poste.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPoste == p,
                            onClick = { selectedPoste = p },
                            label = { Text(p.label()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPoste) }) { Text("Confirmer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun DeleteCollectifDialog(
    collectifNom: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer le collectif", fontWeight = FontWeight.Bold) },
        text = { Text("Êtes-vous sûr de vouloir supprimer définitivement le collectif '$collectifNom' ? Cette action est irréversible.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Supprimer", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun AddCollectifDialog(
    initialSeason: String = "2026-2027",
    onDismiss: () -> Unit,
    onConfirm: (nom: String, cat: String, sexe: String, format: String, comp: String, saison: String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("M15") }
    var sexe by remember { mutableStateOf("M") }
    var format by remember { mutableStateOf("6x6") }
    var comp by remember { mutableStateOf("departemental") }
    var saison by remember { mutableStateOf(initialSeason) }

    val categories = listOf("M9", "M11", "M13", "M15", "M18", "M21", "Seniors")
    val formats = listOf("2x2", "3x3", "4x4", "6x6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Collectif", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom de l'équipe (ex: Équipe 1)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = saison,
                    onValueChange = { saison = it },
                    label = { Text("Saison") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = comp,
                    onValueChange = { comp = it },
                    label = { Text("Compétition") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Catégorie", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        categories.chunked(4).forEach { chunk ->
                            Row {
                                chunk.forEach { c ->
                                    FilterChip(
                                        selected = cat == c,
                                        onClick = { cat = c },
                                        label = { Text(c, fontSize = 10.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Sexe", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row {
                    FilterChip(selected = sexe == "M", onClick = { sexe = "M" }, label = { Text("Masculin") }, modifier = Modifier.padding(end = 8.dp))
                    FilterChip(selected = sexe == "F", onClick = { sexe = "F" }, label = { Text("Féminin") })
                }
                Spacer(Modifier.height(16.dp))
                Text("Format", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row {
                    formats.forEach { f ->
                        FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nom, cat, sexe, format, comp, saison) },
                enabled = nom.isNotBlank() && comp.isNotBlank() && saison.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun InviteCoachDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String?, tel: String?, poste: Poste) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var tel by remember { mutableStateOf("") }
    var selectedPoste by remember { mutableStateOf(Poste.PRINCIPAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inviter un coach", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email de l'invité") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Email, null) }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = tel,
                    onValueChange = { tel = it },
                    label = { Text("Téléphone (Optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
                Spacer(Modifier.height(20.dp))
                Text("Poste au sein du collectif", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Poste.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPoste == p,
                            onClick = { selectedPoste = p },
                            label = { Text(p.label(), fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email.takeIf { it.isNotBlank() }, tel.takeIf { it.isNotBlank() }, selectedPoste) },
                enabled = email.isNotBlank() || tel.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Envoyer l'invitation") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddClubEventDialog(
    collectifs: List<CollectifAvecDetail>,
    onDismiss: () -> Unit,
    onConfirm: (ClubEvent) -> Unit
) {
    var type by remember { mutableStateOf(ClubEventType.TOURNOI) }
    var scope by remember { mutableStateOf(ClubEventScope.CLUB_ENTIER) }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.of(10, 0)) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val selectedTeamIds = remember { mutableStateListOf<String>() }
    val selectedCoachIds = remember { mutableStateListOf<String>() }

    // Reset scope and selections if type changes
    LaunchedEffect(type) {
        scope = ClubEventScope.CLUB_ENTIER
        selectedTeamIds.clear()
        selectedCoachIds.clear()
    }

    val coaches = remember(collectifs) {
        collectifs.flatMap { it.rattachements }
            .distinctBy { it.coachId }
            .filter { it.coachId.isNotEmpty() }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF002147))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SÉLECTIONNER L'HEURE", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(24.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("ANNULER", color = Color.White.copy(alpha = 0.6f)) }
                        TextButton(onClick = {
                            time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF002147)),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("NOUVEL ÉVÉNEMENT CLUB", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.height(24.dp))
                
                // TYPE SWITCH
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    ClubEventType.entries.forEach { t ->
                        val isSel = type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.1f))
                                .clickable { type = t },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'événement") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(time.toString())
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lieu") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(Modifier.height(16.dp))

                // SCOPE SELECTION
                Text("Visibilité :", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                val filteredScopes = when(type) {
                    ClubEventType.TOURNOI -> listOf(ClubEventScope.CLUB_ENTIER, ClubEventScope.ÉQUIPES_CIBLÉES, ClubEventScope.EXTERNE_DA)
                    ClubEventType.SOIRÉE -> listOf(ClubEventScope.CLUB_ENTIER, ClubEventScope.ÉQUIPES_CIBLÉES)
                    ClubEventType.RÉUNION -> listOf(ClubEventScope.CLUB_ENTIER, ClubEventScope.COACHS_CIBLÉS)
                }

                filteredScopes.forEach { s ->
                    val label = when(s) {
                        ClubEventScope.CLUB_ENTIER -> "CLUB"
                        ClubEventScope.ÉQUIPES_CIBLÉES -> "CATEGORIES"
                        ClubEventScope.COACHS_CIBLÉS -> "COACHS"
                        ClubEventScope.EXTERNE_DA -> "COMITE DA"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { scope = s }) {
                        RadioButton(selected = scope == s, onClick = { scope = s }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00B4D8)))
                        Text(label, color = Color.White)
                    }
                }

                if (scope == ClubEventScope.ÉQUIPES_CIBLÉES) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        collectifs.forEach { detail ->
                            val isSelected = selectedTeamIds.contains(detail.collectif.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { if (isSelected) selectedTeamIds.remove(detail.collectif.id) else selectedTeamIds.add(detail.collectif.id) },
                                label = { Text(detail.collectif.nom, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(labelColor = Color.White.copy(alpha = 0.7f), selectedLabelColor = Color.White)
                            )
                        }
                    }
                }

                if (scope == ClubEventScope.COACHS_CIBLÉS) {
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        coaches.forEach { coach ->
                            val isSelected = selectedCoachIds.contains(coach.coachId)
                            val name = "${coach.coachPrenom} ${coach.coachNom}"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (isSelected) selectedCoachIds.remove(coach.coachId) else selectedCoachIds.add(coach.coachId)
                                }
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { if (it) selectedCoachIds.add(coach.coachId) else selectedCoachIds.remove(coach.coachId) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00B4D8))
                                )
                                Text(name, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Annuler", color = Color.White.copy(alpha = 0.6f)) }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val event = ClubEvent(
                                id = UUID.randomUUID().toString(),
                                clubId = "", // Filled by VM
                                title = title,
                                type = type,
                                scope = scope,
                                date = date,
                                startTime = time,
                                location = location,
                                description = description,
                                targetTeamIds = selectedTeamIds.toList(),
                                targetCoachIds = selectedCoachIds.toList()
                            )
                            Log.d("CLUB_EVENT", "Bouton Valider & Push appuyé : $title")
                            onConfirm(event)
                        },
                        enabled = title.isNotBlank() && location.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Valider & Push", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
