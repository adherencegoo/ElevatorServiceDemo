package com.xdd.elevatorservicedemo.model

import android.graphics.Color
import java.util.*

class Passenger(val fromFloor: Int, val toFloor: Int) {
    companion object {
        private var currentId = 0

        @Synchronized
        private fun generateId(): Int = currentId++

        private val random = Random()
    }

    val id = generateId()

    val color = Color.rgb(random.nextFloat(), random.nextFloat(), random.nextFloat())
}