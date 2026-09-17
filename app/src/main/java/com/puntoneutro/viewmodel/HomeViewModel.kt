package com.puntoneutro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.puntoneutro.model.network.NetworkStatusTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tracker = NetworkStatusTracker(application.applicationContext)

    val isConnected: StateFlow<Boolean> = tracker.isConnected.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )
}