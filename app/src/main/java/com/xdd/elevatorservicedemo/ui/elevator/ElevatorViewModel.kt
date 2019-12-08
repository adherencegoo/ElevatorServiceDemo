package com.xdd.elevatorservicedemo.ui.elevator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xdd.elevatorservicedemo.MyApp
import com.xdd.elevatorservicedemo.model.*

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

    val coroutineAsset = getApplication<MyApp>().appCoroutine.newChild()

    override fun onCleared() {
        super.onCleared()
        coroutineAsset.cancel()
    }

    val floors = List(config.floorCount) { Floor(config.indexToFloorId(it)) }

    val elevators = List(config.elevatorCount) { index ->
        val elevator = Elevator(index, this)

        // An elevator must observe the event: passenger arrival to all floors
        floors.forEach { floor ->
            floor.livePassengerArrived.observeForever {
                elevator.triggerMove()
            }
        }

        elevator
    }

    private val passengerGenerator = PassengerGenerator()

    fun getFloor(floorId: Int) = floors[config.floorIdToIndex(floorId)]

    suspend fun generatePassenger() = passengerGenerator.generate(this)
}