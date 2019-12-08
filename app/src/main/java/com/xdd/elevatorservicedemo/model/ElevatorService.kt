package com.xdd.elevatorservicedemo.model

import com.xdd.elevatorservicedemo.utils.CoroutineAsset

class ElevatorService(val config: ElevatorServiceConfig, val coroutineAsset: CoroutineAsset) {

    val floors = List(config.floorCount) { Floor(config.indexToFloorId(it)) }

    val elevators = List(config.elevatorCount) {  index ->
        val elevator = Elevator(index, this)

        // An elevator must observe the event: passenger arrival to all floors
        floors.forEach {  floor ->
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