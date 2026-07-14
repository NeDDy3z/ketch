package com.neddy.ketch.ui.watchers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neddy.ketch.di.AppContainer
import com.neddy.ketch.domain.model.Watcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchersViewModel(private val container: AppContainer) : ViewModel() {

    val watchers: StateFlow<List<Watcher>?> = container.watcherRepository
        .observeWatchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEnabled(watcher: Watcher, enabled: Boolean) {
        viewModelScope.launch {
            container.watcherRepository.save(watcher.copy(enabled = enabled))
            container.triggerSyncRequester.requestSync()
        }
    }

    fun delete(watcher: Watcher) {
        viewModelScope.launch {
            container.watcherRepository.delete(watcher)
            container.triggerSyncRequester.requestSync()
        }
    }
}
