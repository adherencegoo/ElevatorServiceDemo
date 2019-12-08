package com.xdd.elevatorservicedemo.model

import java.io.Serializable

class ElevatorService(val config: Config) {
    data class Config(
        val baseFloor: Int,
        val floorCount: Int,
        val elevatorCount: Int,
        val animationDurationPerFloor: Long,
        val doorAnimationEnabled: Boolean
    ) : Serializable {
        val topFloor = baseFloor + floorCount - 1

        fun floorIdToIndex(floorId: Int) = floorId - baseFloor

        fun indexToFloorId(index: Int) = index + baseFloor
    }

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

    fun getFloor(floorId: Int) = floors[config.floorIdToIndex(floorId)]
}