package com.xdd.elevatorservicedemo.model

import android.graphics.Color
import java.util.*

data class Passenger(val fromFloor: Floor, val toFloor: Floor) {
    companion object {
        private var currentId = 0

        @Synchronized
        private fun generateId(): Int = currentId++

        private val random = Random()
    }

    val direction = Direction.infer(fromFloor.id, toFloor.id)

    init {
        fromFloor.addPassengers(listOf(this))
    }

    val id = generateId()

    val color = Color.rgb(random.nextFloat(), random.nextFloat(), random.nextFloat())

    fun name() = "p$id"

    fun getContentString() =
        "(${name()}) " + fromFloor.idToName() + direction.symbol + toFloor.idToName()
}