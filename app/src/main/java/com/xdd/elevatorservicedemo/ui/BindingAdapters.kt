package com.xdd.elevatorservicedemo.ui

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.xdd.elevatorservicedemo.R
import com.xdd.elevatorservicedemo.model.Direction
import com.xdd.elevatorservicedemo.model.DoorState
import pl.droidsonroids.gif.GifImageView

@BindingAdapter("app:bindDirection")
fun ImageView.bindDirection(direction: Direction?) {
    setImageResource(
        when (direction) {
            Direction.UP -> R.drawable.direction_up
            Direction.DOWN -> R.drawable.direction_down
            else -> android.R.color.transparent
        }
    )
}

@BindingAdapter("app:bindDoorState")
fun GifImageView.bindDoorState(doorState: DoorState?) {
    setImageResource((doorState ?: DoorState.CLOSED).resId)
}