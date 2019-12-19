package com.xdd.elevatorservicedemo.ui.elevator

import android.app.Application
import androidx.lifecycle.*
import com.xdd.elevatorservicedemo.MyApp
import com.xdd.elevatorservicedemo.model.*
import com.xdd.elevatorservicedemo.utils.createChildJob
import com.xdd.elevatorservicedemo.utils.createScope
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

    sealed class FloorEvent(
        val floor: Floor,
        val change: Room.PassengerChange<Direction>
    ) {
        class RawChange(
            floor: Floor,
            passengerChange: Room.PassengerChange<Direction>
        ) : FloorEvent(floor, passengerChange)

        class Claimed(
            floor: Floor,
            passengerChange: Room.PassengerChange<Direction>,
            val claimer: Elevator
        ) : FloorEvent(floor, passengerChange)
    }

    val coroutineJob = getApplication<MyApp>().appCoroutineJob.createChildJob()
    val uiScope = coroutineJob.createScope(Dispatchers.Main)
    val backgroundScope = coroutineJob.createScope(Dispatchers.IO)

    override fun onCleared() {
        super.onCleared()
        coroutineJob.cancel()
    }

    private val _liveFloorEvent = MediatorLiveData<FloorEvent>().apply {
        observeForever { event ->
            when (event) {
                is FloorEvent.RawChange -> {
                    //xdd
                    if (event.change.increase) {
                        elevators.forEach {
                            it.triggerMove()
                        }
                    } else {
                        //xdd
                    }
                }
                is FloorEvent.Claimed -> {
                    //xdd
                }
            }
        }
    }

    val floors = List(config.floorCount) { index ->
        val floor = Floor(config.indexToFloorId(index))
        _liveFloorEvent.addSource(floor.livePassengerChange) {
            _liveFloorEvent.value = FloorEvent.RawChange(floor, it)
        }
        floor
    }

    val elevators = List(config.elevatorCount) { Elevator(it, this) }

    val passengerGenerator = PassengerGenerator()

    fun getFloor(floorId: Int) = floors[config.floorIdToIndex(floorId)]

    /**
     * @return There is a [requestedDirection] request in [requestedFloor], should [elevator] handle it?
     *
     * Used to prevent that a floor request is handled by multiple elevators
     * */
    fun elevatorShouldHandleFloorRequest(
        elevator: Elevator,
        requestedFloor: Floor,
        requestedDirection: Direction
    ): Boolean {
        if (!requestedFloor.hasPassengers(requestedDirection)) {
            return false
        }

        // Find elevators which have handled the request: going to [requestedFloor] for [requestedDirection] passengers
        val otherHandlers = elevators.filter {
            // exclude self
            it != elevator &&
                    it.realMovement?.let { movement ->
                        movement.toFloor == requestedFloor && movement.futureDirection == requestedDirection
                    } == true
        }
        // This request hasn't been handled by other elevators
        return otherHandlers.isEmpty()
    }


    suspend fun generatePassenger() = passengerGenerator.generate(this)
}