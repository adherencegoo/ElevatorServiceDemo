package com.xdd.elevatorservicedemo.model

import java.io.Serializable

data class ElevatorServiceConfig(
    val baseFloor: Int,
    val floorCount: Int,
    val elevatorCount: Int,
    val animationDurationPerFloor: Long,
    val doorAnimationEnabled: Boolean
) : Serializable {
    val topFloor = baseFloor + floorCount - 1

    fun floorIdToIndex(floorId: Int) = floorId - baseFloor

    fun indexToFloorId(index: Int) = index + baseFloor

    fun getInvalidDescription() = when {
        floorCount < 2 -> "Invalid floor count, must >= 2"
        elevatorCount < 1 -> "Invalid elevator count, must >= 1"
        animationDurationPerFloor < 0 -> "Invalid animation duration, must >= 0"
        else -> null
    }
}