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
        fromFloor.addPassenger(this)
    }

    val id = generateId()

    val color = Color.rgb(random.nextFloat(), random.nextFloat(), random.nextFloat())

    fun name() = "p$id"
}