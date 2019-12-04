package com.xdd.elevatorservicedemo.model

import androidx.annotation.DrawableRes
import com.xdd.elevatorservicedemo.R

enum class DoorState(@DrawableRes val resId: Int, val animationDuration: Long) {
    OPENING(R.drawable.door_opening, 2360), // 58 frames, 2.36s
    OPEN(R.drawable.door_open, 0),
    CLOSING(R.drawable.door_closing, 2880), // 58 frames, 2.88s
    CLOSED(R.drawable.door_closed, 0)
}