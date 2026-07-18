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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

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

    private val _clubEvents = MutableStateFlow<List<com.example.coachapp.data.ClubEvent>>(emptyList())
    val clubEvents: StateFlow<List<com.example.coachapp.data.ClubEvent>> = _clubEvents.asStateFlow()

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
        // Le chargement automatique est supprimé. 
        // Il doit être déclenché manuellement lors de l'accès à l'espace Admin.
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
                if (id == null) {
                    _uiState.value = PresidentUiState.Error("Aucun club associé à votre compte")
                    return@launch
                }
                clubId = id
                clubCode = repository.getClubCode(id)

                val collectifs = repository.getCollectifsAvecDetail(id, selectedSeason)
                _uiState.value = PresidentUiState.Success(
                    collectifs = collectifs,
                    collectifsEnAttente = collectifs.filter {
                        it.collectif.statut == CollectifStatut.EN_ATTENTE_CT
                    }
                )
                chargerClubEvents()
            } catch (e: Exception) {
                _uiState.value = PresidentUiState.Error(e.message ?: "Erreur")
            }
        }
    }

    fun chargerClubEvents() {
        val id = clubId ?: return
        viewModelScope.launch {
            _clubEvents.value = repository.getClubEvents(id)
        }
    }

    fun pushClubEvent(
        event: com.example.coachapp.data.ClubEvent,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val id = clubId ?: return onError("ID Club introuvable. Veuillez rafraîchir.")
        viewModelScope.launch {
            repository.pushClubEvent(event.copy(clubId = id))
                .onSuccess { 
                    chargerClubEvents()
                    onSuccess() 
                }
                .onFailure { onError(it.message ?: "Erreur push événement") }
        }
    }

    fun chargerCollectifDetail(collectifId: String, format: String, codeFourni: String) {
        collectifCourantId = collectifId
        viewModelScope.launch {
            try {
                _formatLimite.value = repository.getFormatLimite(format)
                chargerJoueurs(collectifId)
                if (vivierClubCache.isEmpty()) {
                    // Utilisation directe du code club fourni et de la saison active
                    vivierClubCache = repository.chargerVivierClub(codeFourni, selectedSeason)
                    ordresCategorie = repository.getTousLesOrdresCategorie()
                }
            } catch (e: Exception) {
                android.util.Log.e("PRESIDENT_VM", "Erreur chargerCollectifDetail", e)
            }
        }
    }

    fun chargerJoueurs(collectifId: String) {
        viewModelScope.launch {
            _joueurs.value = repository.getJoueursCollectif(collectifId)
        }
    }

    fun rechercherDansVivier(query: String) {
        if (query.length < 2) {
            _vivierRecherche.value = emptyList()
            return
        }
        val resultats = vivierClubCache.filter {
            val nom = it["nom"]?.jsonPrimitive?.contentOrNull ?: ""
            val prenom = it["prenom"]?.jsonPrimitive?.contentOrNull ?: ""
            nom.contains(query, ignoreCase = true) || prenom.contains(query, ignoreCase = true)
        }.map { row ->
            val cat = row["categorie"]?.jsonPrimitive?.contentOrNull ?: "M15"
            val ordre = ordresCategorie[cat] ?: 99
            JoueurVivier(
                id = row["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                nom = row["nom"]?.jsonPrimitive?.contentOrNull ?: "",
                prenom = row["prenom"]?.jsonPrimitive?.contentOrNull ?: "",
                categorie = cat,
                dateNaissance = row["date_naissance"]?.jsonPrimitive?.contentOrNull ?: "",
                niveauSurclassement = row["niveau_surclassement"]?.jsonPrimitive?.contentOrNull,
                groupeAffichage = 0 // Par défaut catégorie propre
            )
        }
        _vivierRecherche.value = resultats
    }

    fun rechercherVivier(query: String, collectifId: String, categorie: String, sexe: String) {
        rechercherDansVivier(query)
    }

    fun ajouterJoueur(
        collectifId: String,
        joueurId: Long,
        poste: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = clubCode ?: return@launch onError("Code club manquant")
            repository.ajouterJoueurCollectif(collectifId, joueurId, poste, id)
                .onSuccess {
                    chargerJoueurs(collectifId)
                    onSuccess()
                }
                .onFailure { onError(it.message ?: "Erreur ajout") }
        }
    }

    fun retirerJoueur(
        collectifId: String,
        collectifJoueurId: String,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.retirerJoueurCollectif(collectifJoueurId)
                .onSuccess { chargerJoueurs(collectifId) }
                .onFailure { onError(it.message ?: "Erreur suppression") }
        }
    }

    fun ajouterJoueurManuel(
        collectifId: String,
        nom: String,
        prenom: String,
        dateNaissance: String?,
        numLicence: String?,
        categorie: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.ajouterJoueurManuel(
                collectifId = collectifId,
                nom = nom,
                prenom = prenom,
                numLicence = numLicence,
                dateNaissance = dateNaissance,
                categorie = categorie,
                saison = selectedSeason
            )
                .onSuccess {
                    chargerJoueurs(collectifId)
                    onSuccess()
                }
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

    fun creerCollectif(
        nom: String,
        cat: String,
        sexe: String,
        format: String,
        comp: String,
        saison: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = clubId ?: repository.getClubId() ?: return@launch onError("ID Club introuvable")
            repository.creerCollectif(id, nom, cat, sexe, format, comp, saison)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur création") }
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

    fun envoyerInvitation(
        collectifId: String,
        email: String?,
        tel: String?,
        poste: Poste,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.envoyerInvitation(collectifId, email, tel, poste)
                .onSuccess { chargerCollectifs(); onSuccess() }
                .onFailure { onError(it.message ?: "Erreur invitation") }
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
                .onFailure { onError(it.message ?: "Erreur rattachement") }
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
