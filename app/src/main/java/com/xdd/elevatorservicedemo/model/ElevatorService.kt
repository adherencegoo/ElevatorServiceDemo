package com.xdd.elevatorservicedemo.model

import java.io.Serializable
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class ElevatorService(val config: Config) {
    data class Config(
        val baseFloor: Int,
        val floorCount: Int,
        val elevatorCount: Int
    ) : Serializable

    val topFloor = config.baseFloor + config.floorCount - 1

    private fun floorToIndex(floor: Int) = floor - config.baseFloor

    private fun indexToFloor(index: Int) = index + config.baseFloor

    private val lock = ReentrantLock()

    val noPassengerCondition: Condition = lock.newCondition()

    val floors = List(config.floorCount) { Floor(indexToFloor(it)) }

    private val elevators = List(config.elevatorCount) {
        Elevator(it, this)
    }

    fun getFloor(floor: Int) = floors[floorToIndex(floor)]

    fun newPassenger(passenger: Passenger) {
        getFloor(passenger.fromFloor).addPassenger(passenger)
        noPassengerCondition.signalAll()
    }

    fun start() {
        elevators.forEach(Elevator::move)
    }
}