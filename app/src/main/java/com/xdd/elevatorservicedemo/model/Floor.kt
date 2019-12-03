package com.xdd.elevatorservicedemo.model


class Floor(id: Int) : Room<Direction>(id) {
    companion object {
        fun floorName(id: Int): String = if (id >= 0) {
            (id + 1).toString() + "F"
        } else {
            "B" + (id * -1)
        }
    }

    override fun getPassengerKey(passenger: Passenger): Direction = passenger.direction.also {
        assert(it != Direction.NONE)
    }

    override fun idToName(): String = floorName(id)

    operator fun minus(floor: Floor): Int = this.id - floor.id
}