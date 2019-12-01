package com.xdd.elevatorservicedemo.model

import java.io.Serializable
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

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

    private val lock = ReentrantLock()

    private val noPassengerCondition: Condition = lock.newCondition()

    val floors = List(config.floorCount) { Floor(config.indexToFloorId(it)) }

    val elevators = List(config.elevatorCount) { Elevator(it, this) }

    fun getFloor(floorId: Int) = floors[config.floorIdToIndex(floorId)]

    fun getFloor(floor: Floor, offset: Int) = floors[config.floorIdToIndex(floor.id + offset)]

    fun newPassenger(passenger: Passenger) {
        getFloor(passenger.fromFloor).addPassenger(passenger)
        conditionSignal()
    }

    fun start() {
        elevators.forEach {
            // xdd: use coroutine
            Thread {
                it.move()
            }.start()
        }
    }

    fun conditionWait() {
        lock.lock()
        try {
            noPassengerCondition.await()
        } finally {
            lock.unlock()
        }
    }

    fun conditionSignal() {
        lock.lock()
        try {
            noPassengerCondition.signalAll()
        } finally {
            lock.unlock()
        }
    }
}