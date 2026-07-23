package com.example.coachapp.ui.training

import com.example.coachapp.ui.components.AnalyseSeanceUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// ============================================================
// DTO Supabase
// ============================================================
@Serializable
data class SeanceAnalyseDto(
    val id: String,
    @SerialName("seance_id")    val seanceId: String,
    @SerialName("coach_id")     val coachId: String,
    @SerialName("score_global") val scoreGlobal: Int,
    @SerialName("score_charge") val scoreCharge: Int,
    @SerialName("score_progression") val scoreProgression: Int,
    @SerialName("score_contenu") val scoreContenu: Int,
    @SerialName("niveau_alerte") val niveauAlerte: String,
    @SerialName("motif_score")  val motifScore: String?,
    @SerialName("resume_seance") val resumeSeance: String?,
    @SerialName("module_charge") val moduleCharge: JsonObject?,
    @SerialName("module_contenu") val moduleContenu: JsonObject?,
)

@Serializable
data class SeanceAnalyseWindowDto(
    @SerialName("coach_id")          val coachId: String,
    @SerialName("score_global_moy")  val scoreGlobalMoy: Float,
    @SerialName("delta_score")       val deltaScore: Float?,
    @SerialName("tendance")          val tendance: String?,
)

@Serializable
data class SeanceDto(
    val id: String,
    val titre: String,
    @SerialName("date_heure")     val dateHeure: String,
    @SerialName("duree_minutes")  val dureeMinutes: Int?,
)

// ============================================================
// ViewModel
// ============================================================
class AnalyseSeanceViewModel(
    private val supabase: SupabaseClient,
    private val coachId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyseSeanceUiState(isLoading = true))
    val uiState: StateFlow<AnalyseSeanceUiState> = _uiState.asStateFlow()

    private val _showDrawer = MutableStateFlow(false)
    val showDrawer: StateFlow<Boolean> = _showDrawer.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null

    // --------------------------------------------------------
    // Déclenché depuis l'écran de clôture
    // --------------------------------------------------------
    fun onSeanceClôturée(seanceId: String) {
        _showDrawer.value = true
        _uiState.value = AnalyseSeanceUiState(isLoading = true)
        loadSeanceInfo(seanceId)
        subscribeToAnalyse(seanceId)
    }

    // --------------------------------------------------------
    // Chargement des infos séance pour l'en-tête
    // --------------------------------------------------------
    private fun loadSeanceInfo(seanceId: String) {
        viewModelScope.launch {
            runCatching {
                supabase.from("seance")
                    .select { filter { eq("id", seanceId) } }
                    .decodeSingle<SeanceDto>()
            }.onSuccess { seance ->
                val date = seance.dateHeure.take(10) // "2025-07-22"
                val label = formatDateLabel(date)
                _uiState.update { it.copy(
                    titreSeance = seance.titre,
                    dureeMinutes = seance.dureeMinutes ?: 0,
                    dateLabel = label,
                )}
            }
        }
    }

    // --------------------------------------------------------
    // Realtime : écoute l'INSERT dans seance_analyse
    // --------------------------------------------------------
    private fun subscribeToAnalyse(seanceId: String) {
        viewModelScope.launch {
            runCatching {
                val channel = supabase.realtime.channel("analyse-$seanceId")
                realtimeChannel = channel

                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "seance_analyse"
                    filter("seance_id", FilterOperator.EQ, seanceId)
                }.collect { action ->
                    val dto = Json.decodeFromJsonElement<SeanceAnalyseDto>(action.record)
                    onAnalyseReçue(dto)
                }

                channel.subscribe()
            }.onFailure { e ->
                // Fallback : polling 5s si Realtime échoue
                kotlinx.coroutines.delay(5_000)
                fetchAnalyseDirecte(seanceId)
            }
        }
    }

    // --------------------------------------------------------
    // Fallback polling direct
    // --------------------------------------------------------
    private suspend fun fetchAnalyseDirecte(seanceId: String) {
        runCatching {
            supabase.from("seance_analyse")
                .select { filter { eq("seance_id", seanceId) } }
                .decodeSingle<SeanceAnalyseDto>()
        }.onSuccess { dto ->
            onAnalyseReçue(dto)
        }
    }

    // --------------------------------------------------------
    // Traitement de l'analyse reçue
    // --------------------------------------------------------
    private fun onAnalyseReçue(dto: SeanceAnalyseDto) {
        viewModelScope.launch {
            // Blocs depuis module_contenu
            val blocsTypes = dto.moduleContenu
                ?.get("types_presents")
                ?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?: emptyList()

            // Présences depuis module_charge
            val nbPresents = dto.moduleCharge
                ?.get("nb_joueurs")
                ?.jsonPrimitive?.int ?: 0

            // Fenêtre glissante
            val window = fetchFenetreGlissante()

            _uiState.update { current -> current.copy(
                isLoading = false,
                resumeSeance = dto.resumeSeance ?: "",
                motifScore = dto.motifScore ?: "",
                scoreGlobal = dto.scoreGlobal,
                scoreCharge = dto.scoreCharge,
                scoreProgression = dto.scoreProgression,
                scoreContenu = dto.scoreContenu,
                niveauAlerte = dto.niveauAlerte,
                blocsTypes = blocsTypes,
                nbPresents = nbPresents,
                nbAbsents = 0,
                scoreMoyFenetre = window?.scoreGlobalMoy ?: 0f,
                deltaFenetre = window?.deltaScore ?: 0f,
            )}

            // Désabonnement Realtime — on a ce qu'on voulait
            realtimeChannel?.let {
                supabase.realtime.removeChannel(it)
                realtimeChannel = null
            }
        }
    }

    // --------------------------------------------------------
    // Fenêtre glissante
    // --------------------------------------------------------
    private suspend fun fetchFenetreGlissante(): SeanceAnalyseWindowDto? {
        return runCatching {
            supabase.from("seance_analyse_window")
                .select {
                    filter { eq("coach_id", coachId) }
                    order("computed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }
                .decodeSingle<SeanceAnalyseWindowDto>()
        }.getOrNull()
    }

    // --------------------------------------------------------
    // Fermeture du drawer
    // --------------------------------------------------------
    fun dismissDrawer() {
        _showDrawer.value = false
        realtimeChannel?.let {
            viewModelScope.launch {
                supabase.realtime.removeChannel(it)
                realtimeChannel = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        dismissDrawer()
    }

    // --------------------------------------------------------
    // Utilitaire date
    // --------------------------------------------------------
    private fun formatDateLabel(iso: String): String {
        return runCatching {
            val parts = iso.split("-")
            val months = listOf("","jan.","fév.","mar.","avr.","mai","juin","juil.","août","sep.","oct.","nov.","déc.")
            "${parts[2].trimStart('0')} ${months[parts[1].toInt()]}"
        }.getOrDefault(iso)
    }
}
