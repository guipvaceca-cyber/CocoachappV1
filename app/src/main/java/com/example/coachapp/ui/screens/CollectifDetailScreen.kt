package com.example.coachapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.model.CollectifStatut
import com.example.coachapp.data.model.FormatLimite
import com.example.coachapp.data.model.JoueurCollectif
import com.example.coachapp.data.model.JoueurVivier
import com.example.coachapp.ui.president.PresidentViewModel
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001529))
    ) {
        // Background Blobs
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = 150.dp)
                .size(200.dp)
                .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape)
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .size(250.dp)
                .background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape)
                .blur(80.dp)
        )

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
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
                            onSuccess = { snackbarMessage = "Composition soumise au président" },
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
                item { ProgressionBar(joueurs = joueurs, formatLimite = formatLimite) }
                item { StatutBar(joueurs = joueurs, formatLimite = formatLimite) }
                item {
                    SearchBar(
                        query = query,
                        onQueryChange = { q ->
                            query = q
                            viewModel.rechercherVivier(q, collectifId, categorieCoach, collectifSexe)
                        }
                    )
                }

                if (vivierRecherche.isNotEmpty() && query.length >= 2) {
                    item {
                        SuggestionsBox(
                            joueurs = vivierRecherche,
                            onSelect = { joueur -> joueurForPoste = joueur }
                        )
                    }
                }

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
                                viewModel.retirerJoueur(joueur.id, collectifId, onError = { snackbarMessage = it })
                            }
                        )
                    }
                }

                val surclasses = joueurs.filter { it.estSurclasse }.groupBy { it.categorie }
                surclasses.forEach { (categorie, liste) ->
                    item { SectionLabel("Surclassés $categorie — ${liste.size} joueur${if (liste.size > 1) "s" else ""}") }
                    items(liste) { joueur ->
                        JoueurRow(
                            joueur = joueur,
                            collectifCategorie = categorieCoach,
                            onRetirer = {
                                viewModel.retirerJoueur(joueur.id, collectifId, onError = { snackbarMessage = it })
                            }
                        )
                    }
                }

                val manuels = joueurs.filter { it.estManuel }
                if (manuels.isNotEmpty()) {
                    item { SectionLabel("Ajoutés manuellement — ${manuels.size}") }
                    items(manuels) { joueur ->
                        JoueurRow(
                            joueur = joueur,
                            collectifCategorie = categorieCoach,
                            onRetirer = {
                                viewModel.retirerJoueur(joueur.id, collectifId, onError = { snackbarMessage = it })
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }

        if (joueurForPoste != null) {
            JoueurPosteDialog(
                joueurNom = joueurForPoste!!.nomComplet,
                onDismiss = { joueurForPoste = null },
                onConfirm = { poste ->
                    viewModel.ajouterJoueur(collectifId, joueurForPoste!!.id, poste,
                        onSuccess = { query = ""; joueurForPoste = null },
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
                    snackbarMessage = "Joueur ajouté — signalement CT"
                }
            )
        }

        if (showQrCode) {
            val payload = TeamQrPayload(
                team = collectifNom,
                players = joueurs.map { j ->
                    PlayerQrData(n = j.nom, p = j.prenom, l = j.id, c = j.categorie, a = AgeUtils.calculateAge(j.dateNaissance))
                }
            )
            AlertDialog(
                onDismissRequest = { showQrCode = false },
                containerColor = Color(0xFF002147),
                title = { Text("QR Code de match", fontWeight = FontWeight.Black, color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Présentez ce code à la table de marque.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        QrCodeDisplay(payload = payload)
                        Spacer(Modifier.height(8.dp))
                        Text(collectifNom, fontWeight = FontWeight.Bold, color = Color(0xFF00B4D8))
                    }
                },
                confirmButton = {
                    Button(onClick = { showQrCode = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("Fermer") }
                }
            )
        }
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
        count >= max -> Color.Red.copy(alpha = 0.2f)
        count >= min -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        else -> Color.White.copy(alpha = 0.1f)
    }
    val badgeTextColor = when {
        count >= max -> Color.Red
        count >= min -> Color(0xFF4CAF50)
        else -> Color.White
    }

    TopAppBar(
        title = {
            Column {
                Text(nom, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Montelimar VB · 2026–2027", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Retour") }
        },
        actions = {
            if (isValidated) {
                IconButton(onClick = onQrClick) { Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = Color(0xFF2196F3)) }
            }
            Surface(modifier = Modifier.padding(end = 12.dp), shape = RoundedCornerShape(12.dp), color = badgeColor, border = BorderStroke(0.5.dp, badgeTextColor.copy(alpha = 0.4f))) {
                Text("$count / $max", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = badgeTextColor)
            }
        }
    )
}

@Composable
private fun ProgressionBar(joueurs: List<JoueurCollectif>, formatLimite: FormatLimite?) {
    val count = joueurs.size
    val max = formatLimite?.maxJoueurs ?: 14
    val min = formatLimite?.minJoueurs ?: 6
    val progress by animateFloatAsState(targetValue = if (max > 0) count.toFloat() / max else 0f, label = "progress")
    val barColor by animateColorAsState(targetValue = when { count >= max -> Color.Red; count >= min -> Color(0xFF4CAF50); else -> Color(0xFF00B4D8) }, label = "barColor")
    val minPct = if (max > 0) min.toFloat() / max else 0f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
            Text("seuil min : $min", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f))
            Text("$max", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f))) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(barColor))
            Box(modifier = Modifier.fillMaxWidth(minPct).fillMaxHeight().wrapContentWidth(Alignment.End)) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.4f)))
            }
        }
    }
}

@Composable
private fun StatutBar(joueurs: List<JoueurCollectif>, formatLimite: FormatLimite?) {
    val count = joueurs.size
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), color = Color(0xFFFF9800).copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.PersonAdd, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
            Text("Composition en cours · $count joueur${if (count > 1) "s" else ""}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onQueryChange, placeholder = { Text("Rechercher un joueur...", fontSize = 14.sp, color = Color.White.copy(alpha = 0.2f)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), singleLine = true, shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00B4D8), unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.White.copy(alpha = 0.05f), unfocusedContainerColor = Color.White.copy(alpha = 0.05f))
    )
}

@Composable
private fun SuggestionsBox(joueurs: List<JoueurVivier>, onSelect: (JoueurVivier) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF001529).copy(alpha = 0.95f)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))) {
        Column {
            joueurs.forEach { joueur ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(joueur) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarCircle(joueur.initiales, Color.White.copy(alpha = 0.1f), Color.White)
                    Column {
                        Text(joueur.nomComplet, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${joueur.categorie} · né ${joueur.dateNaissance?.take(4) ?: ""}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
                if (joueur != joueurs.last()) HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f), letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
private fun JoueurRow(joueur: JoueurCollectif, collectifCategorie: String, onRetirer: () -> Unit) {
    val isSurclasse = joueur.categorie != collectifCategorie
    val accentColor = if (joueur.estManuel) Color.Red else if (isSurclasse) Color(0xFFFF9800) else Color(0xFF00B4D8)
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp), border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarCircle(joueur.initiales, accentColor.copy(alpha = 0.15f), accentColor)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(joueur.nomComplet, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (joueur.poste != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.2f), border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f))) {
                            Text(joueur.poste.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = accentColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("${joueur.categorie} · né ${joueur.dateNaissance?.take(4) ?: ""}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
            }
            if (joueur.estManuel || isSurclasse) {
                Surface(shape = RoundedCornerShape(20.dp), color = accentColor.copy(alpha = 0.15f), border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))) {
                    Text(if (joueur.estManuel) "manuel" else joueur.niveauSurclassement ?: "S", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            IconButton(onClick = onRetirer, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun JoueurPosteDialog(joueurNom: String, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    val postes = listOf("Passeur" to "passeur", "Central" to "central", "R/A" to "receptionneur_attaquant", "Pointu" to "pointu", "Libero" to "libero", "Polyvalent" to "polyvalent")
    var selected by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF002147), title = { Text("Poste de $joueurNom", color = Color.White, fontWeight = FontWeight.Black) }, text = {
        Column {
            Text("Définir un poste ?", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(20.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                postes.forEach { (label, slug) -> FilterChip(selected = selected == slug, onClick = { selected = if (selected == slug) null else slug }, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00B4D8), selectedLabelColor = Color.White, labelColor = Color.White.copy(alpha = 0.6f))) }
            }
        }
    }, confirmButton = { Button(onClick = { onConfirm(selected) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) { Text("Confirmer", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Ignorer", color = Color.White.copy(alpha = 0.5f)) } })
}

@Composable
private fun CollectifFooter(joueurs: List<JoueurCollectif>, formatLimite: FormatLimite?, onAjoutManuel: () -> Unit, onSoumettre: () -> Unit) {
    val count = joueurs.size
    val min = formatLimite?.minJoueurs ?: 6
    val max = formatLimite?.maxJoueurs ?: 14
    val peutSoumettre = count in min..max
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF001529).copy(alpha = 0.95f), shadowElevation = 8.dp, border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onAjoutManuel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Ajout", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Button(onClick = onSoumettre, enabled = peutSoumettre, modifier = Modifier.weight(1.5f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8), contentColor = Color.White, disabledContainerColor = Color.White.copy(alpha = 0.1f))) { Text("Envoyer au président", fontSize = 14.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun AjoutManuelDialog(collectifId: String, categorieCoach: String, viewModel: PresidentViewModel, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var nom by remember { mutableStateOf("") }; var prenom by remember { mutableStateOf("") }; var dateNaissance by remember { mutableStateOf("") }; var localError by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF002147), title = { Text("Ajout manuel", fontWeight = FontWeight.Black, color = Color.White) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (localError != null) Surface(color = Color.Red.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text(localError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold) }
            val tfColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00B4D8), unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF00B4D8), unfocusedLabelColor = Color.White.copy(alpha = 0.4f))
            OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = dateNaissance, onValueChange = { dateNaissance = it }, label = { Text("Date naissance (AAAA-MM-JJ)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors, shape = RoundedCornerShape(12.dp))
        }
    }, confirmButton = { Button(onClick = {
        if (nom.isNotBlank() && prenom.isNotBlank()) {
            val catJoueur = AgeUtils.getCategoryFromBirthDate(dateNaissance)
            viewModel.ajouterJoueurManuel(collectifId, nom, prenom, null, dateNaissance.ifBlank { null }, catJoueur ?: categorieCoach, onSuccess = onSuccess, onError = { localError = it })
        }
    }, enabled = nom.isNotBlank() && prenom.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) { Text("Ajouter", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Color.White.copy(alpha = 0.5f)) } })
}

@Composable
private fun AvatarCircle(initiales: String, couleur: Color, textColor: Color) {
    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(couleur), contentAlignment = Alignment.Center) {
        Text(initiales.take(2).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = textColor)
    }
}
