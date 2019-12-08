package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData


class Floor(id: Int) : Room<Direction>(id) {
    companion object {
        fun floorName(id: Int): String = if (id >= 0) {
            (id + 1).toString() + "F"
        } else {
            "B" + (id * -1)
        }
    }

    private val _livePassengerArrived = MediatorLiveData<Unit>()
    val livePassengerArrived: LiveData<Unit> = _livePassengerArrived

    override fun getPassengerKey(passenger: Passenger): Direction = passenger.direction.also {
        assert(it != Direction.NONE)
    }

    override fun idToName(): String = floorName(id)

    operator fun minus(floor: Floor): Int = this.id - floor.id

    override fun addPassengers(newPassengers: List<Passenger>) {
        super.addPassengers(newPassengers)
        _livePassengerArrived.postValue(Unit)
    }
}