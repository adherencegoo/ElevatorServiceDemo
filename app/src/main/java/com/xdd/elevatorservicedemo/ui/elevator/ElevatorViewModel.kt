package com.xdd.elevatorservicedemo.ui.elevator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xdd.elevatorservicedemo.model.ElevatorService

class ElevatorViewModel(config: ElevatorService.Config) : ViewModel() {
    class Factory(private val config: ElevatorService.Config) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ElevatorViewModel(config) as T
    }

    private val elevatorService = ElevatorService(config)
}