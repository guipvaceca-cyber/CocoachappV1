package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ----------------------------------------------------------------
// Modèle local
// ----------------------------------------------------------------
data class JoueurSlot(
    val id: String,
    val nom: String,
    val prenom: String,
    val club: String,
    val categorie: String,
    val estSurclasse: Boolean = false,
    val statut: StatutSlot = StatutSlot.CONVOQUE
)

enum class StatutSlot {
    CONVOQUE, CONFIRME, INDISPONIBLE, PROMU, INSCRIT, ERREUR
}

data class JoueurVivier(
    val id: String,
    val nom: String,
    val prenom: String,
    val club: String,
    val categorie: String,
    val estSurclasse: Boolean = false,
    val taille: Int = 0
)

fun quotaParCategorie(categorie: String): Int = when {
    categorie.contains("M12") -> 6
    categorie.contains("M13") -> 8
    else -> 14
}

fun initiales(prenom: String, nom: String): String =
    "${prenom.firstOrNull() ?: ""}${nom.firstOrNull() ?: ""}"

// ----------------------------------------------------------------
// Bottom Sheet principale
// ----------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvocationSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    cdeCategorie: String,
    vivierPrincipal: List<JoueurVivier>,    // M15 (ou catégorie coach)
    vivierInferieur: List<JoueurVivier>,    // M13 surclassés
    slotsPersistes: List<JoueurSlot?> = emptyList(),
    bancPersiste: List<JoueurSlot?> = emptyList(),
    isEditable: Boolean = true,
    onOuverture: () -> Unit = {},
    onSlotChange: (index: Int, type: String, joueur: JoueurSlot?) -> Unit = { _, _, _ -> },
    onSauvegarder: (principal: List<JoueurSlot?>, banc: List<JoueurSlot?>) -> Unit = { _, _ -> }
) {
    android.util.Log.d("DEBUG_CDE", "ConvocationSheet isVisible = $isVisible")
    if (!isVisible) return

    val quota = quotaParCategorie(cdeCategorie)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // État des tableaux
    val slotsPrincipal = remember(slotsPersistes) {
        mutableStateListOf<JoueurSlot?>().apply {
            repeat(quota) { i -> add(slotsPersistes.getOrNull(i)) }
        }
    }
    val slotsBanc = remember(bancPersiste) {
        mutableStateListOf<JoueurSlot?>().apply {
            repeat(quota) { i -> add(bancPersiste.getOrNull(i)) }
        }
    }
    LaunchedEffect(isVisible) {
        if (isVisible) onOuverture()
    }

    // Slot actif pour l'autocomplete
    var slotActifIndex by remember { mutableStateOf<Int?>(null) }
    var slotActifType by remember { mutableStateOf<String?>(null) } // "PRINCIPAL" | "BANC"
    var queryRecherche by remember { mutableStateOf("") }

    // Joueurs déjà sélectionnés (pour éviter les doublons)
    val idsSelectionnes = remember(slotsPrincipal.toList(), slotsBanc.toList()) {
        (slotsPrincipal.filterNotNull() + slotsBanc.filterNotNull()).map { it.id }.toSet()
    }

    // Résultats filtrés
    val resultats = remember(queryRecherche, idsSelectionnes) {
        if (queryRecherche.isEmpty()) emptyList()
        else {
            val q = queryRecherche.lowercase()
            val principal = vivierPrincipal
                .filter { it.id !in idsSelectionnes }
                .filter { "${it.prenom} ${it.nom}".lowercase().contains(q) }
                .map { it to false }   // false = pas surclassé dans ce groupe
            val inferieur = vivierInferieur
                .filter { it.id !in idsSelectionnes && it.estSurclasse }
                .filter { "${it.prenom} ${it.nom}".lowercase().contains(q) }
                .map { it to true }    // true = surclassé
            principal + inferieur
        }
    }

    fun selectionnerJoueur(joueur: JoueurVivier) {
        val slot = JoueurSlot(
            id = joueur.id,
            nom = joueur.nom,
            prenom = joueur.prenom,
            club = joueur.club,
            categorie = joueur.categorie,
            estSurclasse = joueur.estSurclasse
        )
        when (slotActifType) {
            "PRINCIPAL" -> slotActifIndex?.let {
                slotsPrincipal[it] = slot
                onSlotChange(it, "PRINCIPAL", slot)
            }
            "BANC" -> slotActifIndex?.let {
                slotsBanc[it] = slot
                onSlotChange(it, "BANC", slot)
            }
        }
        slotActifIndex = null
        slotActifType = null
        queryRecherche = ""
    }

    fun retirerJoueur(index: Int, type: String) {
        if (type == "PRINCIPAL") {
            val premierBanc = slotsBanc.indexOfFirst { it != null }
            if (premierBanc >= 0) {
                val remplacant = slotsBanc[premierBanc]!!.copy(statut = StatutSlot.PROMU)
                slotsPrincipal[index] = remplacant
                slotsBanc[premierBanc] = null
                onSlotChange(index, "PRINCIPAL", remplacant)
                onSlotChange(premierBanc, "BANC", null)
                return
            }
        }
        val slots = if (type == "PRINCIPAL") slotsPrincipal else slotsBanc
        slots[index] = null
        onSlotChange(index, type, null)
        slotActifIndex = index
        slotActifType = type
        queryRecherche = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isEditable) "Convocation $cdeCategorie" else "Sélection $cdeCategorie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isEditable) "Sélectionne $quota joueurs" else "Liste des joueurs convoqués",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val nbPrincipal = slotsPrincipal.count { it != null }
                    
                    if (isEditable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Bouton Enregistrer (toujours visible)
                            TextButton(
                                onClick = {
                                    onSauvegarder(slotsPrincipal.toList(), slotsBanc.toList())
                                    scope.launch { sheetState.hide(); onDismiss() }
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Enregistrer", fontSize = 13.sp)
                            }

                            // Bouton Envoyer (activé seulement si quota atteint)
                            Button(
                                onClick = {
                                    // Logique d'envoi final (passer en statut ENVOYÉ par ex)
                                    onSauvegarder(slotsPrincipal.toList(), slotsBanc.toList())
                                    scope.launch { sheetState.hide(); onDismiss() }
                                },
                                enabled = nbPrincipal == quota,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1D9E75)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Envoyer", fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Close button for read-only mode
                        IconButton(onClick = { scope.launch { sheetState.hide(); onDismiss() } }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
        ) {
            // ---- TABLEAU PRINCIPAL ----
            item {
                SectionHeader(
                    label = "Tableau principal",
                    remplis = slotsPrincipal.count { it != null },
                    total = quota
                )
            }

            itemsIndexed(slotsPrincipal.toList()) { index, slot ->
                val isLocked = !isEditable || slot?.statut == StatutSlot.INSCRIT || slot?.statut == StatutSlot.CONFIRME
                SlotRow(
                    numero = index + 1,
                    slot = slot,
                    isActive = isEditable && slotActifIndex == index && slotActifType == "PRINCIPAL",
                    query = if (slotActifIndex == index && slotActifType == "PRINCIPAL") queryRecherche else "",
                    onQueryChange = { queryRecherche = it },
                    onTap = {
                        if (isEditable && slot == null) {
                            slotActifIndex = index
                            slotActifType = "PRINCIPAL"
                            queryRecherche = ""
                        }
                    },
                    onRetirer = { retirerJoueur(index, "PRINCIPAL") },
                    isLocked = isLocked
                )

                // Dropdown sous le slot actif
                if (slotActifIndex == index && slotActifType == "PRINCIPAL" && resultats.isNotEmpty()) {
                    DropdownResultats(
                        resultats = resultats,
                        query = queryRecherche,
                        onSelect = { selectionnerJoueur(it) }
                    )
                }
            }

            // ---- BANC DES REMPLAÇANTS ----
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(
                    label = "Banc des remplaçants",
                    remplis = slotsBanc.count { it != null },
                    total = quota,
                    optionnel = true
                )
            }

            itemsIndexed(slotsBanc.toList()) { index, slot ->
                val isLocked = !isEditable || slot?.statut == StatutSlot.INSCRIT || slot?.statut == StatutSlot.CONFIRME
                SlotRow(
                    numero = index + 1,
                    slot = slot,
                    isActive = isEditable && slotActifIndex == index && slotActifType == "BANC",
                    query = if (slotActifIndex == index && slotActifType == "BANC") queryRecherche else "",
                    onQueryChange = { queryRecherche = it },
                    onTap = {
                        if (isEditable && slot == null) {
                            slotActifIndex = index
                            slotActifType = "BANC"
                            queryRecherche = ""
                        }
                    },
                    onRetirer = { retirerJoueur(index, "BANC") },
                    isBanc = true,
                    isLocked = isLocked
                )

                if (slotActifIndex == index && slotActifType == "BANC" && resultats.isNotEmpty()) {
                    DropdownResultats(
                        resultats = resultats,
                        query = queryRecherche,
                        onSelect = { selectionnerJoueur(it) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------
// En-tête de section
// ----------------------------------------------------------------
@Composable
fun SectionHeader(label: String, remplis: Int, total: Int, optionnel: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (optionnel) {
            Spacer(Modifier.width(6.dp))
            Text("(optionnel)", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        val complet = remplis == total
        Text(
            "$remplis / $total",
            style = MaterialTheme.typography.labelSmall,
            color = if (complet) Color(0xFF1D9E75) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (complet) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ----------------------------------------------------------------
// Ligne de slot
// ----------------------------------------------------------------
@Composable
fun SlotRow(
    numero: Int,
    slot: JoueurSlot?,
    isActive: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onTap: () -> Unit,
    onRetirer: () -> Unit,
    isBanc: Boolean = false,
    isLocked: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isActive) {
        if (isActive) runCatching { focusRequester.requestFocus() }
    }

    val bgColor = when {
        isActive -> Color(0xFFE6F1FB)
        slot?.statut == StatutSlot.INSCRIT || slot?.statut == StatutSlot.CONFIRME -> Color.LightGray.copy(alpha = 0.2f)
        slot?.statut == StatutSlot.PROMU -> Color(0xFFE1F5EE)
        slot?.statut == StatutSlot.INDISPONIBLE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        slot != null -> MaterialTheme.colorScheme.surface
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(if (slot == null && !isActive && !isLocked) Modifier.clickable { onTap() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Numéro
        Text(
            "$numero",
            modifier = Modifier.width(28.dp).padding(start = 8.dp),
            fontSize = 11.sp,
            color = if (isActive) Color(0xFF185FA5) else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Contenu
        when {
            slot != null -> {
                // Joueur sélectionné
                val alpha = if (slot.statut == StatutSlot.INDISPONIBLE) 0.4f else 1f

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (slot.estSurclasse) Color(0xFFFAEEDA)
                            else Color(0xFFE6F1FB)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initiales(slot.prenom, slot.nom),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (slot.estSurclasse) Color(0xFF633806) else Color(0xFF0C447C)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${slot.prenom} ${slot.nom}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (slot.estSurclasse) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Surcl.",
                                fontSize = 9.sp,
                                color = Color(0xFF633806),
                                modifier = Modifier
                                    .background(Color(0xFFFAEEDA), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        if (slot.statut == StatutSlot.PROMU) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "↑ promu",
                                fontSize = 9.sp,
                                color = Color(0xFF085041),
                                modifier = Modifier
                                    .background(Color(0xFFE1F5EE), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        "${slot.club} · ${slot.categorie}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Statut dot
                val dotColor = when (slot.statut) {
                    StatutSlot.CONFIRME, StatutSlot.INSCRIT -> Color(0xFF1D9E75)
                    StatutSlot.INDISPONIBLE -> Color(0xFF888780)
                    StatutSlot.PROMU -> Color(0xFF1D9E75)
                    StatutSlot.ERREUR -> Color(0xFF993C1D)
                    else -> Color(0xFF378ADD)
                }
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))

                Spacer(Modifier.width(8.dp))

                // Bouton retirer (caché si verrouillé)
                if (!isLocked) {
                    IconButton(
                        onClick = onRetirer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Retirer",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Verrouillé",
                        modifier = Modifier.size(14.dp).padding(horizontal = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            isActive -> {
                // Champ de recherche actif
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF185FA5)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                    ),
                    cursorBrush = SolidColor(Color(0xFF185FA5)),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    "Rechercher un joueur…",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
            }

            else -> {
                // Slot vide — invitation à taper
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Rechercher un joueur…",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.width(if (slot == null && !isActive) 8.dp else 0.dp))
    }
}

// ----------------------------------------------------------------
// Liste déroulante des résultats
// ----------------------------------------------------------------
@Composable
fun DropdownResultats(
    resultats: List<Pair<JoueurVivier, Boolean>>,
    query: String,
    onSelect: (JoueurVivier) -> Unit
) {
    val m15 = resultats.filter { !it.second }
    val m13s = resultats.filter { it.second }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column {
            if (m15.isNotEmpty()) {
                Text(
                    "Catégorie principale",
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                m15.forEach { (joueur, _) ->
                    ResultatItem(joueur = joueur, isSurclasse = false, onSelect = onSelect)
                }
            }
            if (m13s.isNotEmpty()) {
                Text(
                    "Surclassés (${m13s.first().first.categorie})",
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFAEEDA).copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 10.sp,
                    color = Color(0xFF633806)
                )
                m13s.forEach { (joueur, _) ->
                    ResultatItem(joueur = joueur, isSurclasse = true, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
fun ResultatItem(
    joueur: JoueurVivier,
    isSurclasse: Boolean,
    onSelect: (JoueurVivier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(joueur) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSurclasse) Color(0xFFFAEEDA) else Color(0xFFE6F1FB)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initiales(joueur.prenom, joueur.nom),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSurclasse) Color(0xFF633806) else Color(0xFF0C447C)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${joueur.prenom} ${joueur.nom}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSurclasse) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Surcl.",
                        fontSize = 9.sp,
                        color = Color(0xFF633806),
                        modifier = Modifier
                            .background(Color(0xFFFAEEDA), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                "${joueur.club} · ${joueur.categorie}${if (joueur.taille > 0) " · ${joueur.taille}cm" else ""}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            Icons.Default.Add,
            contentDescription = "Ajouter",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}