package com.xdd.elevatorservicedemo.model


class Floor(id: Int) : Room<Movement>(id) {
    companion object {
        fun floorName(id: Int): String = if (id >= 0) {
            (id + 1).toString() + "F"
        } else {
            "B" + (id * -1)
        }
    }

    override fun getPassengerKey(passenger: Passenger): Movement =
        if (passenger.toFloor > id) Movement.UP else Movement.DOWN

    override fun idToName(): String = floorName(id)
}