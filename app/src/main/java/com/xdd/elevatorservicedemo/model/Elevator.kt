package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.MutableLiveData
import kotlin.math.abs

class Elevator(id: Int, private val service: ElevatorService) : Room<Int>(id) {
    private val liveFloor = MutableLiveData(service.config.baseFloor)
    private val liveMovement = MutableLiveData(Movement.NONE)

    override fun getPassengerKey(passenger: Passenger): Int = passenger.toFloor

    fun move() {
        while (true) {
            getNextDestFloor()?.let {
                val origFloor = liveFloor.value!!
                val newMovement = if (it.id > origFloor) Movement.UP else Movement.DOWN
                val newFloor = origFloor + newMovement.offset

                liveFloor.postValue(newFloor)
                liveMovement.postValue(newMovement)

                if (newFloor == it.id) {
                    arriveFloor()
                }

                Thread.sleep(500) // xdd
            } ?: kotlin.run {
                liveMovement.postValue(Movement.NONE)
                service.noPassengerCondition.await()
            }
        }
    }

    private fun arriveFloor() {
        val currentFloor = liveFloor.value!!
        val currentMovement = liveMovement.value!!

        // leave elevator
        removePassengers(currentFloor)

        // leave floor
        service.getFloor(currentFloor).removePassengers(currentMovement)
            // enter elevator
            .forEach {
                addPassenger(it)
            }

        // movement can only be updated when arriving floor
        if (hasAnyPassengers()) {
            if (currentFloor == service.topFloor && currentMovement == Movement.UP) {
                liveMovement.postValue(Movement.DOWN)
            } else if (currentFloor == service.config.baseFloor && currentMovement == Movement.DOWN) {
                liveMovement.postValue(Movement.UP)
            }
        } else {
            liveMovement.postValue(Movement.NONE)
        }
    }

    private fun getNextDestFloor(): Floor? {
        val currentFloor = liveFloor.value!!

        return when (liveMovement.value!!) {
            Movement.NONE -> getNearestFloor()
            Movement.UP -> getNearestSameMovementFloor(
                Movement.UP,
                currentFloor + 1..service.topFloor
            )
            Movement.DOWN -> getNearestSameMovementFloor(
                Movement.DOWN,
                currentFloor - 1 downTo service.config.baseFloor
            )
        }
    }

    private fun getNearestFloor(): Floor? {
        val currentFloor = liveFloor.value!!

        return service.floors.filter {
            it.hasAnyPassengers()
        }.minBy {
            abs(it.id - currentFloor)
        }
    }

    private fun getNearestSameMovementFloor(movement: Movement, floorRange: Iterable<Int>): Floor? {
        return floorRange.map {
            service.getFloor(it)
        }.first {
            // has passengers who want to leave at `it` floor
            // has passengers who want to enter the elevator and have the same movement at `it` floor
            hasPassengers(it.id) || it.hasPassengers(movement)
        }
    }
}