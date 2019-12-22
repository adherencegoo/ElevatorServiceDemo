package com.xdd.elevatorservicedemo.ui.elevator

import android.app.Application
import androidx.lifecycle.*
import com.xdd.elevatorservicedemo.MyApp
import com.xdd.elevatorservicedemo.model.*
import com.xdd.elevatorservicedemo.utils.createChildJob
import com.xdd.elevatorservicedemo.utils.createScope
import com.xdd.elevatorservicedemo.utils.mutableShadowClone
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
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
        disposableCollectiveFloorEvent.dispose()
        elevators.forEach(Elevator::destroy)
    }

    val floors = List(config.floorCount) { Floor(config.indexToFloorId(it)) }

    val elevators = List(config.elevatorCount) { Elevator(it, this) }

    private val observableCollectiveFloorEvent: Observable<FloorEvent> = Observable.merge(
        floors.map { floor ->
            floor.observablePassengerChange.map {
                FloorEvent.fromPassengerChange(floor, it)
            }
        } + elevators.map { elevator ->
            elevator.observableFloorEvent
        }
    )

    private val disposableCollectiveFloorEvent = observableCollectiveFloorEvent
        .observeOn(Schedulers.newThread())
        .subscribe { event ->
            when (event.type) {
                FloorEvent.Type.REQUEST_SERVICE -> {
                    getPrioritizedElevators(event.floor, event.direction)
                        .firstOrNull { it != event.excludedElevator }
                        ?.triggerMove()
                }
                FloorEvent.Type.NOTIFY_SERVED -> {
                    // For those elevators targeting at the served floor,
                    // cancel the movement for the served floor by recalculating a new movement
                    elevators.filter { elevator ->
                        elevator != event.excludedElevator
                                && elevator.realMovement?.let {
                            it.toFloor == event.floor && it.futureDirection == event.direction
                        } == true
                    }.forEach(Elevator::triggerMove)
                }
            }
        }!!

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

    private fun getPrioritizedElevators(
        requestedFloor: Floor,
        requestedDirection: Direction
    ): List<Elevator> {
        return if (elevators.size == 1) {
            elevators
        } else {
            elevators.mutableShadowClone().apply {
                sortWith(Elevator.PriorityComparator(requestedFloor, requestedDirection))
            }
        }
    }
}