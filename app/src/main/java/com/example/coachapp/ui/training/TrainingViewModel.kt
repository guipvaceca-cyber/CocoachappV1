package com.example.coachapp.ui.training

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.coachapp.data.TrainingSchedule
import com.example.coachapp.data.TrainingSession
import com.example.coachapp.data.CompetitionEvent
import com.example.coachapp.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TrainingViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _clubPlanning = MutableStateFlow<List<TrainingSchedule>>(emptyList())
    val clubPlanning: StateFlow<List<TrainingSchedule>> = _clubPlanning.asStateFlow()

    private var clubId: String? = null
    var clubCode: String? = null
        private set

    var selectedSeason by mutableStateOf("2026-2027")
        private set

    init {
        com.example.coachapp.data.SupabaseManager.auth.sessionStatus
            .onEach { status ->
                if (status is io.github.jan.supabase.gotrue.SessionStatus.Authenticated) {
                    chargerInfosClub()
                }
            }
            .launchIn(viewModelScope)
    }

    fun chargerInfosClub() {
        viewModelScope.launch {
            val id = repository.getClubId()
            if (id != null) {
                clubId = id
                clubCode = repository.getClubCode(id)
                chargerPlanning()
            }
        }
    }

    fun chargerPlanning() {
        val id = clubId ?: return
        viewModelScope.launch {
            _clubPlanning.value = repository.getClubPlanning(id, selectedSeason)
        }
    }

    fun pushSession(
        session: TrainingSession,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = clubId ?: return@launch onError("Club non identifié")
            repository.pushSession(session, id, selectedSeason)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Erreur push séance") }
        }
    }

    fun syncPresencesForSession(sessionId: String, onResult: (Map<Long, String>) -> Unit) {
        viewModelScope.launch {
            val presences = repository.fetchSessionPresences(sessionId)
            onResult(presences)
        }
    }

    fun pushMatch(
        event: CompetitionEvent,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = clubId ?: return@launch onError("Club non identifié")
            repository.pushMatch(event, id, selectedSeason)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Erreur push match") }
        }
    }

    fun upsertPlanning(schedule: TrainingSchedule, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.upsertPlanning(schedule.copy(clubId = clubId))
                .onSuccess { 
                    chargerPlanning()
                    onSuccess() 
                }
                .onFailure { onError(it.message ?: "Erreur sauvegarde planning") }
        }
    }

    fun supprimerPlanning(scheduleId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.supprimerPlanning(scheduleId)
                .onSuccess { 
                    chargerPlanning()
                    onSuccess() 
                }
                .onFailure { onError(it.message ?: "Erreur suppression") }
        }
    }
}

class TrainingViewModelFactory(private val repository: TrainingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrainingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
