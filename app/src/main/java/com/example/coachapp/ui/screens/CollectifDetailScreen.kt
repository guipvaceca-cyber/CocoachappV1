package com.example.coachapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.model.FormatLimite
import com.example.coachapp.data.model.JoueurCollectif
import com.example.coachapp.data.model.JoueurVivier
import com.example.coachapp.ui.president.PresidentViewModel

import androidx.compose.material.icons.filled.QrCode
import com.example.coachapp.data.model.CollectifStatut
import com.example.coachapp.ui.util.AgeUtils
import com.example.coachapp.ui.util.PlayerQrData
import com.example.coachapp.ui.util.QrCodeDisplay
import com.example.coachapp.ui.util.TeamQrPayload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectifDetailScreen(
    collectifId: String,
    collectifNom: String,
    collectifFormat: String,
    collectifSexe : String,
    categorieCoach: String,
    clubId: String,
    clubCode: String,
    viewModel: PresidentViewModel,
    onBack: () -> Unit
) {
    val joueurs by viewModel.joueurs.collectAsState()
    val vivierRecherche by viewModel.vivierRecherche.collectAsState()
    val formatLimite by viewModel.formatLimite.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // Récupérer l'état actuel du collectif depuis uiState pour savoir s'il est validé
    val currentCollectif = (uiState as? com.example.coachapp.ui.president.PresidentUiState.Success)
        ?.collectifs?.find { it.collectif.id == collectifId }?.collectif
    
    var query by remember { mutableStateOf("") }
    var showAjoutManuel by remember { mutableStateOf(false) }
    var joueurForPoste by remember { mutableStateOf<JoueurVivier?>(null) }
    var showQrCode by remember { mutableStateOf(false) }
    
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(collectifId) {
        viewModel.chargerCollectifDetail(collectifId, collectifFormat, clubCode)
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CollectifTopBar(
                nom = collectifNom,
                joueurs = joueurs,
                formatLimite = formatLimite,
                isValidated = currentCollectif?.statut == CollectifStatut.EN_ATTENTE_CT,
                onQrClick = { showQrCode = true },
                onBack = onBack
            )
        },
        bottomBar = {
            CollectifFooter(
                joueurs = joueurs,
                formatLimite = formatLimite,
                onAjoutManuel = { showAjoutManuel = true },
                onSoumettre = {
                    viewModel.soumettreAuPresident(
                        collectifId = collectifId,
                        onSuccess = {
                            snackbarMessage = "Composition soumise au président"
                        },
                        onError = { snackbarMessage = it }
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barre de progression
            item {
                ProgressionBar(joueurs = joueurs, formatLimite = formatLimite)
            }

            // Statut
            item {
                StatutBar(joueurs = joueurs, formatLimite = formatLimite)
            }

            // Recherche
            item {
                SearchBar(
                    query = query,
                    onQueryChange = { q ->
                        query = q
                        viewModel.rechercherVivier(q, collectifId, categorieCoach, collectifSexe)
                    }
                )
            }

            // Suggestions
            if (vivierRecherche.isNotEmpty() && query.length >= 2) {
                item {
                    SuggestionsBox(
                        joueurs = vivierRecherche,
                        onSelect = { joueur ->
                            joueurForPoste = joueur
                        }
                    )
                }
            }

            // Section joueurs catégorie propre
            val joueursPropres = joueurs.filter { !it.estSurclasse && !it.estManuel }
            if (joueursPropres.isNotEmpty()) {
                item {
                    SectionLabel("${joueursPropres.first().categorie} — ${joueursPropres.size} joueur${if (joueursPropres.size > 1) "s" else ""}")
                }
                items(joueursPropres) { joueur ->
                    JoueurRow(
                        joueur = joueur,
                        collectifCategorie = categorieCoach,
                        onRetirer = {
                            viewModel.retirerJoueur(
                                collectifJoueurId = joueur.id,
                                collectifId = collectifId,
                                onError = { snackbarMessage = it }
                            )
                        }
                    )
                }
            }

            // Sections surclassés groupées
            val surclasses = joueurs.filter { it.estSurclasse }
                .groupBy { it.categorie }
            surclasses.forEach { (categorie, liste) ->
                item {
                    val niveau = liste.first().niveauSurclassement ?: ""
                    SectionLabel("Surclassés $categorie — ${liste.size} joueur${if (liste.size > 1) "s" else ""}")
                }
                items(liste) { joueur ->
                    JoueurRow(
                        joueur = joueur,
                        collectifCategorie = categorieCoach,
                        onRetirer = {
                            viewModel.retirerJoueur(
                                collectifJoueurId = joueur.id,
                                collectifId = collectifId,
                                onError = { snackbarMessage = it }
                            )
                        }
                    )
                }
            }

            // Section joueurs manuels
            val manuels = joueurs.filter { it.estManuel }
            if (manuels.isNotEmpty()) {
                item { SectionLabel("Ajoutés manuellement — ${manuels.size}") }
                items(manuels) { joueur ->
                    JoueurRow(
                        joueur = joueur,
                        collectifCategorie = categorieCoach,
                        onRetirer = {
                            viewModel.retirerJoueur(
                                collectifJoueurId = joueur.id,
                                collectifId = collectifId,
                                onError = { snackbarMessage = it }
                            )
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (joueurForPoste != null) {
        JoueurPosteDialog(
            joueurNom = joueurForPoste!!.nomComplet,
            onDismiss = { joueurForPoste = null },
            onConfirm = { poste ->
                viewModel.ajouterJoueur(
                    collectifId = collectifId,
                    joueurId = joueurForPoste!!.id,
                    poste = poste,
                    onSuccess = { 
                        query = ""
                        joueurForPoste = null 
                    },
                    onError = { snackbarMessage = it }
                )
            }
        )
    }

    if (showAjoutManuel) {
        AjoutManuelDialog(
            collectifId = collectifId,
            categorieCoach = categorieCoach,
            viewModel = viewModel,
            onDismiss = { showAjoutManuel = false },
            onSuccess = {
                showAjoutManuel = false
                snackbarMessage = "Joueur ajouté — signalement envoyé au CT"
            }
        )
    }

    if (showQrCode) {
        val payload = TeamQrPayload(
            team = collectifNom,
            players = joueurs.map { j ->
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
            onDismissRequest = { showQrCode = false },
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
                    Text(collectifNom, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                }
            },
            confirmButton = {
                Button(onClick = { showQrCode = false }) { Text("Fermer") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectifTopBar(
    nom: String,
    joueurs: List<JoueurCollectif>,
    formatLimite: FormatLimite?,
    isValidated: Boolean = false,
    onQrClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val count = joueurs.size
    val max = formatLimite?.maxJoueurs ?: 14
    val min = formatLimite?.minJoueurs ?: 6

    val badgeColor = when {
        count >= max -> MaterialTheme.colorScheme.errorContainer
        count >= min -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val badgeTextColor = when {
        count >= max -> MaterialTheme.colorScheme.onErrorContainer
        count >= min -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    TopAppBar(
        title = {
            Column {
                Text(nom, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Montelimar VB · 2026–2027",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }
        },
        actions = {
            if (isValidated) {
                IconButton(onClick = onQrClick) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code de match", tint = Color(0xFF2196F3))
                }
            }

            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "$count / $max",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = badgeTextColor
                )
            }
        }
    )
}

@Composable
private fun ProgressionBar(
    joueurs: List<JoueurCollectif>,
    formatLimite: FormatLimite?
) {
    val count = joueurs.size
    val max = formatLimite?.maxJoueurs ?: 14
    val min = formatLimite?.minJoueurs ?: 6
    val progress by animateFloatAsState(
        targetValue = if (max > 0) count.toFloat() / max else 0f,
        label = "progress"
    )
    val barColor by animateColorAsState(
        targetValue = when {
            count >= max -> MaterialTheme.colorScheme.error
            count >= min -> Color(0xFF4CAF50)
            else -> MaterialTheme.colorScheme.primary
        },
        label = "barColor"
    )
    val minPct = if (max > 0) min.toFloat() / max else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "seuil min : $min",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("$max", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
            // Marqueur seuil min
            Box(
                modifier = Modifier
                    .fillMaxWidth(minPct)
                    .fillMaxHeight()
                    .wrapContentWidth(Alignment.End)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun StatutBar(
    joueurs: List<JoueurCollectif>,
    formatLimite: FormatLimite?
) {
    val count = joueurs.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.PersonAdd,
            contentDescription = null,
            tint = Color(0xFFF57F17),
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Composition en cours · $count joueur${if (count > 1) "s" else ""}",
            fontSize = 12.sp,
            color = Color(0xFFF57F17)
        )
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Rechercher un joueur...", fontSize = 14.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        singleLine = true,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun SuggestionsBox(
    joueurs: List<JoueurVivier>,
    onSelect: (JoueurVivier) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column {
            joueurs.forEach { joueur ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(joueur) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AvatarCircle(
                        initiales = joueur.initiales,
                        couleur = when (joueur.groupeAffichage) {
                            0 -> Color(0xFFE3F2FD)
                            1 -> Color(0xFFFFF8E1)
                            else -> Color(0xFFE8F5E9)
                        },
                        textColor = when (joueur.groupeAffichage) {
                            0 -> Color(0xFF1565C0)
                            1 -> Color(0xFFF57F17)
                            else -> Color(0xFF2E7D32)
                        }
                    )
                    Column {
                        Text(joueur.nomComplet, fontSize = 13.sp)
                        val meta = buildString {
                            append(joueur.categorie)
                            joueur.niveauSurclassement?.let { append(" → surclassé $it") }
                            joueur.dateNaissance?.let { append(" · né ${it.take(4)}") }
                        }
                        Text(meta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (joueur != joueurs.last()) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.06.sp,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun JoueurRow(
    joueur: JoueurCollectif,
    collectifCategorie: String,
    onRetirer: () -> Unit
) {
    val isSurclasse = joueur.categorie != collectifCategorie
    val bgColor = if (joueur.estManuel) Color(0xFFFFEBEE) 
                  else if (isSurclasse) Color(0xFFFFF9C4) 
                  else Color(0xFFE3F2FD)
    val contentColor = if (joueur.estManuel) Color(0xFFC62828)
                       else if (isSurclasse) Color(0xFFF57F17)
                       else Color(0xFF1565C0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarCircle(
                initiales = joueur.initiales,
                couleur = contentColor.copy(alpha = 0.2f),
                textColor = contentColor
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        joueur.nomComplet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (joueur.poste != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = contentColor.copy(alpha = 0.1f)
                        ) {
                            val displayPoste = when(joueur.poste) {
                                "receptionneur_attaquant" -> "R/A"
                                "passeur" -> "Passeur"
                                "central" -> "Central"
                                "pointu" -> "Pointu"
                                "libero" -> "Libéro"
                                "polyvalent" -> "Poly."
                                else -> joueur.poste
                            }
                            Text(
                                displayPoste.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    buildString {
                        append(joueur.categorie)
                        joueur.dateNaissance?.let { append(" · né ${it.take(4)}") }
                    },
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            if (joueur.estManuel) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.5f)
                ) {
                    Text(
                        "manuel",
                        fontSize = 10.sp,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            } else if (isSurclasse) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.5f)
                ) {
                    Text(
                        joueur.niveauSurclassement ?: "S",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(onClick = onRetirer, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Retirer",
                    tint = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun JoueurPosteDialog(
    joueurNom: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    // Mapping: UI Label -> DB Slug
    val postesMap = listOf(
        "Passeur" to "passeur",
        "Central" to "central",
        "R/A" to "receptionneur_attaquant",
        "Pointu" to "pointu",
        "Libero" to "libero",
        "Polyvalent" to "polyvalent"
    )
    var selectedPosteSlug by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Poste de $joueurNom") },
        text = {
            Column {
                Text("Voulez-vous définir un poste pour ce joueur ? (Optionnel)", fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    postesMap.forEach { (label, slug) ->
                        FilterChip(
                            selected = selectedPosteSlug == slug,
                            onClick = { selectedPosteSlug = if (selectedPosteSlug == slug) null else slug },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPosteSlug) }) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun CollectifFooter(
    joueurs: List<JoueurCollectif>,
    formatLimite: FormatLimite?,
    onAjoutManuel: () -> Unit,
    onSoumettre: () -> Unit
) {
    val count = joueurs.size
    val min = formatLimite?.minJoueurs ?: 6
    val max = formatLimite?.maxJoueurs ?: 14
    val peutSoumettre = count in min..max

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onAjoutManuel,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Ajout manuel", fontSize = 13.sp)
            }
            Button(
                onClick = onSoumettre,
                enabled = peutSoumettre,
                modifier = Modifier.weight(2f)
            ) {
                Text("Envoyer au président", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AjoutManuelDialog(
    collectifId: String,
    categorieCoach: String,
    viewModel: PresidentViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var numLicence by remember { mutableStateOf("") }
    var dateNaissance by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un joueur manuellement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    "Ce joueur sera signalé au CT pour mise à jour du vivier.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = prenom,
                    onValueChange = { prenom = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = numLicence,
                    onValueChange = { numLicence = it },
                    label = { Text("N° licence (si connu)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateNaissance,
                    onValueChange = { dateNaissance = it },
                    label = { Text("Date de naissance (AAAA-MM-JJ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nom.isNotBlank() && prenom.isNotBlank()) {
                        // 1. Calcul de la catégorie théorique
                        val catJoueur = AgeUtils.getCategoryFromBirthDate(dateNaissance)
                        
                        if (catJoueur != null) {
                            val levelJoueur = AgeUtils.getCategoryLevel(catJoueur)
                            val levelEquipe = AgeUtils.getCategoryLevel(categorieCoach)
                            
                            if (levelJoueur > levelEquipe) {
                                localError = "Impossible d'ajouter un joueur $catJoueur dans une équipe $categorieCoach "
                                return@Button
                            }
                            
                            if (levelEquipe - levelJoueur > 3) {
                                localError = "Le joueur ($catJoueur) est trop jeune pour ce collectif, sur-classement nécessaire ($categorieCoach)."
                                return@Button
                            }
                        }

                        viewModel.ajouterJoueurManuel(
                            collectifId = collectifId,
                            nom = nom,
                            prenom = prenom,
                            numLicence = numLicence.ifBlank { null },
                            dateNaissance = dateNaissance.ifBlank { null },
                            categorie = catJoueur ?: categorieCoach,
                            onSuccess = onSuccess,
                            onError = { localError = it }
                        )
                    }
                },
                enabled = nom.isNotBlank() && prenom.isNotBlank() && (dateNaissance.isBlank() || dateNaissance.length >= 10)
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun AvatarCircle(
    initiales: String,
    couleur: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(couleur),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initiales.take(2).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}