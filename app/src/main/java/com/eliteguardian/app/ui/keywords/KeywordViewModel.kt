package com.eliteguardian.app.ui.keywords

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteguardian.app.data.database.KeywordEntity
import com.eliteguardian.app.data.repository.KeywordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeywordViewModel(application: Application) : AndroidViewModel(application) {

    private val keywordRepo = KeywordRepository.getInstance(application)

    val searchQuery = MutableStateFlow("")
    private val _addError = MutableStateFlow<String?>(null)
    val addError: StateFlow<String?> = _addError.asStateFlow()

    val filteredKeywords: StateFlow<List<KeywordEntity>> = combine(
        keywordRepo.getAllKeywords(),
        searchQuery
    ) { keywords, query ->
        if (query.isBlank()) keywords
        else keywords.filter { it.keyword.contains(query.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addKeyword(word: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (word.trim().length < 2) {
                _addError.value = "Keyword must be at least 2 characters."
                return@launch
            }
            val success = keywordRepo.addKeyword(word)
            if (success) {
                _addError.value = null
                onComplete()
            }
        }
    }

    fun deleteKeyword(entity: KeywordEntity) {
        viewModelScope.launch {
            keywordRepo.deleteKeyword(entity)
        }
    }

    fun clearError() {
        _addError.value = null
    }
}

