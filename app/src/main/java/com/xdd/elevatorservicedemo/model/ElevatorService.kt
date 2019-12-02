package com.xdd.elevatorservicedemo.model

import java.io.Serializable

class ElevatorService(val config: Config) {
    data class Config(
        val baseFloor: Int,
        val floorCount: Int,
        val elevatorCount: Int,
        val animationDurationPerFloor: Long = 500L
    ) : Serializable {
        val topFloor = baseFloor + floorCount - 1

        fun floorIdToIndex(floorId: Int) = floorId - baseFloor

        fun indexToFloorId(index: Int) = index + baseFloor
    }

    val floors = List(config.floorCount) { Floor(config.indexToFloorId(it)) }

    val elevators = List(config.elevatorCount) { Elevator(it, this) }

    fun getFloor(floorId: Int) = floors[config.floorIdToIndex(floorId)]

    fun newPassenger(passenger: Passenger) {
        getFloor(passenger.fromFloor).addPassenger(passenger)
        elevators.forEach(Elevator::move)
    }

    private val directionSymbolMap = mapOf(
        Direction.UP to "↗",
        Direction.DOWN to "↘",
        Direction.NONE to "－"
    )

    fun getPassengerContent(passenger: Passenger) = "(${passenger.name()}) " +
            getFloor(passenger.fromFloor).idToName() +
            directionSymbolMap[Direction.infer(passenger.fromFloor, passenger.toFloor)] +
            getFloor(passenger.toFloor).idToName()
}