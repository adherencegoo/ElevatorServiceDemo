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
}