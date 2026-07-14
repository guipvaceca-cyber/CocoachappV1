package com.example.coachapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coachapp.data.TrainingSchedule
import com.example.coachapp.data.model.Collectif
import com.example.coachapp.data.repository.PresidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: PresidentRepository
) : ViewModel() {

    private val _myCollectifs = MutableStateFlow<List<Collectif>>(emptyList())
    val myCollectifs: StateFlow<List<Collectif>> = _myCollectifs.asStateFlow()

    private val _selectedCollectifId = MutableStateFlow<String?>(null)
    val selectedCollectifId: StateFlow<String?> = _selectedCollectifId.asStateFlow()

    private val _currentPlanning = MutableStateFlow<List<TrainingSchedule>>(emptyList())
    val currentPlanning: StateFlow<List<TrainingSchedule>> = _currentPlanning.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPlayerInfo()
    }

    fun loadPlayerInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val collectifs = repository.getPlayerCollectifs()
                _myCollectifs.value = collectifs
                if (collectifs.isNotEmpty() && _selectedCollectifId.value == null) {
                    selectCollectif(collectifs.first().id)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCollectif(collectifId: String) {
        _selectedCollectifId.value = collectifId
        viewModelScope.launch {
            try {
                _currentPlanning.value = repository.getCollectifPlanning(collectifId)
            } catch (e: Exception) {
                _currentPlanning.value = emptyList()
            }
        }
    }
}

class PlayerViewModelFactory(
    private val repository: PresidentRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlayerViewModel(repository) as T
    }
}
