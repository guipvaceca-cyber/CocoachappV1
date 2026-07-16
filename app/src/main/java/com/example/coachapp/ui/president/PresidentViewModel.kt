package com.example.coachapp.ui.president

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.coachapp.data.model.CollectifAvecDetail
import com.example.coachapp.data.model.CollectifStatut
import com.example.coachapp.data.model.FormatLimite
import com.example.coachapp.data.model.JoueurCollectif
import com.example.coachapp.data.model.JoueurVivier
import com.example.coachapp.data.model.Poste
import com.example.coachapp.data.repository.PresidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class PresidentViewModel(
    private val repository: PresidentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PresidentUiState>(PresidentUiState.Loading)
    val uiState: StateFlow<PresidentUiState> = _uiState.asStateFlow()

    private val _joueurs = MutableStateFlow<List<JoueurCollectif>>(emptyList())
    val joueurs: StateFlow<List<JoueurCollectif>> = _joueurs.asStateFlow()

    private val _vivierRecherche = MutableStateFlow<List<JoueurVivier>>(emptyList())
    val vivierRecherche: StateFlow<List<JoueurVivier>> = _vivierRecherche.asStateFlow()

    private val _formatLimite = MutableStateFlow<FormatLimite?>(null)
    val formatLimite: StateFlow<FormatLimite?> = _formatLimite.asStateFlow()

    private val _clubPlanning = MutableStateFlow<List<com.example.coachapp.data.TrainingSchedule>>(emptyList())
    val clubPlanning: StateFlow<List<com.example.coachapp.data.TrainingSchedule>> = _clubPlanning.asStateFlow()

    var selectedSeason by mutableStateOf("2026-2027")
        private set

    private var clubId: String? = null
    var clubCode: String? = null
        private set
    var collectifCourantId: String? = null

    // Cache vivier en mémoire
    private var vivierClubCache: List<JsonObject> = emptyList()
    private var ordresCategorie: Map<String, Int> = emptyMap()

    init {
        chargerCollectifs()
    }

    fun updateSeason(newSeason: String) {
        selectedSeason = newSeason
        vivierClubCache = emptyList() // invalider le cache
        chargerCollectifs()
    }

    fun chargerCollectifs() {
        viewModelScope.launch {
            _uiState.value = PresidentUiState.Loading
            try {
                val id = repository.getClubId()
                android.util.Log.d("DIAG_PRESIDENT", "Club ID récupéré: $id")
                if (id == null) {
                    _uiState.value = PresidentUiState.Error("Aucun club associé à votre compte")
                    return@launch
                }
                clubId = id
                clubCode = repository.getClubCode(id)
                android.util.Log.d("DIAG_PRESIDENT", "Club Code récupéré: $clubCode")

                // Charger le planning global du club
                _clubPlanning.value = repository.getClubPlanning(id, selectedSeason)

                val collectifs = repository.getCollectifsAvecDetail(id, selectedSeason)
                android.util.Log.d("DIAG_PRESIDENT", "Nombre de collectifs: ${collectifs.size}")

                _uiState.value = PresidentUiState.Success(
                    collectifs = collectifs,
                    collectifsEnAttente = collectifs.filter {
                        it.collectif.statut == CollectifStatut.EN_ATTENTE_CT
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("DIAG_PRESIDENT", "Erreur chargerCollectifs", e)
                _uiState.value = PresidentUiState.Error(e.message ?: "Erreur")
            }
        }
    }

    fun chargerCollectifDetail(collectifId: String, format: String, codeFourni: String) {
        android.util.Log.d("DIAG_PRESIDENT", "chargerCollectifDetail appelé: collectifId=$collectifId clubCode=$clubCode")
        collectifCourantId = collectifId
        viewModelScope.launch {
            try {
                _formatLimite.value = repository.getFormatLimite(format)
                chargerJoueurs(collectifId)

                // S'assurer que le clubCode local est synchronisé
                if (clubCode == null) clubCode = codeFourni

                if (vivierClubCache.isEmpty()) {
                    vivierClubCache = repository.chargerVivierClub(codeFourni, selectedSeason)
                    android.util.Log.d("DIAG_PRESIDENT", "Cache vivier: ${vivierClubCache.size} joueurs")
                }
                if (ordresCategorie.isEmpty()) {
                    ordresCategorie = repository.getTousLesOrdresCategorie()
                    android.util.Log.d("DIAG_PRESIDENT", "Ordres catégorie: $ordresCategorie")
                }
            } catch (e: Exception) {
                android.util.Log.e("PRESIDENT_VM", "Erreur chargerCollectifDetail", e)
            }
        }
    }

    private fun chargerJoueurs(collectifId: String) {
        viewModelScope.launch {
            _joueurs.value = repository.getJoueursCollectif(collectifId)
        }
    }

    fun rechercherVivier(query: String, collectifId: String, categorieCollectif: String, sexeCollectif :String) {
        android.util.Log.d("DIAG_PRESIDENT", "Recherche locale: '$query' | Cat: $categorieCollectif | Cache: ${vivierClubCache.size}")
        if (query.length < 2) {
            _vivierRecherche.value = emptyList()
            return
        }

        viewModelScope.launch {
            android.util.Log.d("DIAG_PRESIDENT", "Dans la coroutine, cache=${vivierClubCache.size}")
            try {
                val ordreCoach = ordresCategorie[categorieCollectif] ?: repository.getCategorieOrdre(categorieCollectif)
                val dejaDans = _joueurs.value.mapNotNull { it.vivierJoueurId?.toString() }
                val queryLower = query.lowercase()

                val results = vivierClubCache
                    .filter { row ->
                        val id = row["id"]?.jsonPrimitive?.content ?: ""
                        if (id in dejaDans) return@filter false

                        val nom = row["nom"]?.jsonPrimitive?.content?.lowercase() ?: ""
                        val prenom = row["prenom"]?.jsonPrimitive?.content?.lowercase() ?: ""
                        if (!nom.contains(queryLower) && !prenom.contains(queryLower)) return@filter false

                        // Normalisation du sexe (Base: Masc/Fem vs App: M/F)
                        val sexeJoueurRaw = row["sexe"]?.jsonPrimitive?.content ?: "INCONNU"
                        val sexeJoueur = when(sexeJoueurRaw) {
                            "Masc" -> "M"
                            "Fem" -> "F"
                            else -> sexeJoueurRaw
                        }

                        if (sexeJoueur != "INCONNU" && sexeJoueur != sexeCollectif) return@filter false

                        val categorieJoueur = row["categorie"]?.jsonPrimitive?.content ?: ""
                        val ordreJoueur = ordresCategorie[categorieJoueur] ?: 99
                        val surclass = row["niveau_surclassement"]?.jsonPrimitive?.content

                        when {
                            ordreJoueur == ordreCoach -> true
                            ordreJoueur == ordreCoach - 1 -> true
                            ordreJoueur == ordreCoach - 2 -> surclass in listOf("DS", "TS")
                            ordreJoueur == ordreCoach - 3 -> surclass == "TS"
                            else -> false
                        }
                    }
                    .map { row ->
                        val categorieJoueur = row["categorie"]?.jsonPrimitive?.content ?: ""
                        val ordreJoueur = ordresCategorie[categorieJoueur] ?: ordreCoach
                        JoueurVivier(
                            id = row["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                            nom = row["nom"]?.jsonPrimitive?.content ?: "",
                            prenom = row["prenom"]?.jsonPrimitive?.content ?: "",
                            categorie = categorieJoueur,
                            dateNaissance = row["date_naissance"]?.jsonPrimitive?.content,
                            niveauSurclassement = row["niveau_surclassement"]?.jsonPrimitive?.content,
                            groupeAffichage = ordreCoach - ordreJoueur
                        )
                    }
                    .sortedWith(compareBy({ it.groupeAffichage }, { it.nom }, { it.prenom }))

                android.util.Log.d("DIAG_PRESIDENT", "Résultats filtrés: ${results.size}")
                _vivierRecherche.value = results
            } catch (e: Exception) {
                android.util.Log.e("PRESIDENT_VM", "Erreur recherche vivier", e)
                android.util.Log.e("DIAG_PRESIDENT", "Erreur filtre: ${e.message}", e)
            }
        }
    }

    fun creerCollectif(
        nom: String,
        categorie: String,
        sexe: String,
        format: String,
        competition: String,
        saison: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = clubId ?: return@launch onError("Club non identifié")
            repository.creerCollectif(id, nom, categorie, sexe, format, competition, saison)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur création") }
        }
    }

    fun envoyerInvitation(
        collectifId: String,
        email: String?,
        telephone: String?,
        poste: Poste,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.envoyerInvitation(collectifId, email, telephone, poste)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur invitation") }
        }
    }

    fun supprimerCollectif(
        collectifId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.supprimerCollectif(collectifId)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur suppression") }
        }
    }

    fun rattacherSoiMeme(
        collectifId: String,
        poste: Poste,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.rattacherSoiMeme(collectifId, poste, selectedSeason)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur auto-rattachement") }
        }
    }

    fun ajouterJoueur(
        collectifId: String,
        joueurId: Long,
        poste: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.ajouterJoueurCollectif(collectifId, joueurId, poste, selectedSeason)
                .onSuccess {
                    chargerJoueurs(collectifId)
                    _vivierRecherche.value = emptyList()
                    onSuccess()
                }
                .onFailure { onError(it.message ?: "Erreur ajout joueur") }
        }
    }

    fun retirerJoueur(
        collectifJoueurId: String,
        collectifId: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.retirerJoueurCollectif(collectifJoueurId)
                .onSuccess { chargerJoueurs(collectifId) }
                .onFailure { onError(it.message ?: "Erreur retrait joueur") }
        }
    }

    fun ajouterJoueurManuel(
        collectifId: String,
        nom: String,
        prenom: String,
        numLicence: String?,
        dateNaissance: String?,
        categorie: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.ajouterJoueurManuel(
                collectifId, nom, prenom, numLicence,
                dateNaissance, categorie, selectedSeason
            )
                .onSuccess { chargerJoueurs(collectifId); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur ajout manuel") }
        }
    }

    fun soumettreAuPresident(
        collectifId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.soumettreComposition(collectifId)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur soumission") }
        }
    }

    fun validerCollectif(
        collectifId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.validerCollectif(collectifId)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur validation") }
        }
    }

    fun enregistrerPlanning(
        schedule: com.example.coachapp.data.TrainingSchedule,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = clubId ?: return@launch onError("Club non identifié")
            repository.upsertPlanning(schedule.copy(clubId = id))
                .onSuccess { 
                    chargerCollectifs() // Rafraîchit le planning global
                    onSuccess() 
                }
                .onFailure { onError(it.message ?: "Erreur planning") }
        }
    }

    fun supprimerPlanning(
        scheduleId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.supprimerPlanning(scheduleId)
                .onSuccess { 
                    chargerCollectifs()
                    onSuccess() 
                }
                .onFailure { onError(it.message ?: "Erreur suppression") }
        }
    }

    fun pushSession(
        session: com.example.coachapp.data.TrainingSession,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = clubId ?: return@launch onError("Club non identifié")
            repository.pushSession(session, id, selectedSeason)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Erreur push séance") }
        }
    }

    fun pushMatch(
        event: com.example.coachapp.data.CompetitionEvent,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = clubId ?: return@launch onError("Club non identifié")
            repository.pushMatch(event, id, selectedSeason)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Erreur push match") }
        }
    }

    fun refuserEffectif(
        collectifId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.refuserEffectif(collectifId)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur refus") }
        }
    }

    fun syncPresencesForSession(sessionId: String, onResult: (Map<Long, String>) -> Unit) {
        viewModelScope.launch {
            val presences = repository.fetchSessionPresences(sessionId)
            onResult(presences)
        }
    }
}

sealed class PresidentUiState {
    object Loading : PresidentUiState()
    data class Success(
        val collectifs: List<CollectifAvecDetail>,
        val collectifsEnAttente: List<CollectifAvecDetail>
    ) : PresidentUiState()
    data class Error(val message: String) : PresidentUiState()
}

class PresidentViewModelFactory(
    private val repository: PresidentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PresidentViewModel(repository) as T
    }
}
