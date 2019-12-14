package com.xdd.elevatorservicedemo.ui.elevator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xdd.elevatorservicedemo.MyApp
import com.xdd.elevatorservicedemo.model.*
import com.xdd.elevatorservicedemo.utils.createChildJob
import com.xdd.elevatorservicedemo.utils.createScope
import com.xdd.elevatorservicedemo.utils.includeSource
import kotlinx.coroutines.Dispatchers

class ElevatorViewModel(application: Application, val config: ElevatorServiceConfig) :
    AndroidViewModel(application) {

    class Factory(
        private val application: Application,
        private val config: ElevatorServiceConfig
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ElevatorViewModel(application, config) as T
    }

    val coroutineJob = getApplication<MyApp>().appCoroutineJob.createChildJob()
    val uiScope = coroutineJob.createScope(Dispatchers.Main)
    val backgroundScope = coroutineJob.createScope(Dispatchers.IO)

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    /**
     * Observe passenger arrival event from all floors
     * */
    private val livePassengerArrived = MediatorLiveData<Unit>()

    val floors = List(config.floorCount) { index ->
        Floor(config.indexToFloorId(index)).also {
            livePassengerArrived.includeSource(it.livePassengerArrived)
        }
    }

    val elevators = List(config.elevatorCount) { index ->
        val elevator = Elevator(index, this)

        // An elevator only need to observes the collective event
        livePassengerArrived.observeForever {
            elevator.triggerMove()
        }

        elevator
    }

    val passengerGenerator = PassengerGenerator()

    fun getFloor(floorId: Int) = floors[config.floorIdToIndex(floorId)]

    suspend fun generatePassenger() = passengerGenerator.generate(this)
}