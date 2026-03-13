package edu.nd.pmcburne.hwapp.one

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GameUiState(
    val games: List<GameEntity> = emptyList(),
    val selectedDate: String = todayDateString(),
    val selectedGender: String = "men",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
) {
    companion object {
        fun todayDateString(): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return formatter.format(Date())
        }
    }
}

class GameViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        refreshGames(_uiState.value.selectedDate, _uiState.value.selectedGender)
    }

    fun onGenderSelected(gender: String) {
        val date = _uiState.value.selectedDate
        _uiState.value = _uiState.value.copy(selectedGender = gender)
        refreshGames(date, gender)
    }

    fun onDateSelected(date: String) {
        val gender = _uiState.value.selectedGender
        _uiState.value = _uiState.value.copy(selectedDate = date)
        refreshGames(date, gender)
    }

    fun refreshGames(date: String, gender: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val fetchedGames = repository.loadGames(date, gender)
                _uiState.value = _uiState.value.copy(
                    games = fetchedGames.games,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}