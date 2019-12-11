package com.xdd.elevatorservicedemo.ui

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.xdd.elevatorservicedemo.R
import com.xdd.elevatorservicedemo.model.Direction
import com.xdd.elevatorservicedemo.model.DoorState
import com.xdd.elevatorservicedemo.model.Floor
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

@BindingAdapter("app:bindPickedFloor")
fun TextView.bindPickedFloor(floor: Floor?) {
    text = floor?.idToName() ?: "Random"
}

@BindingAdapter("app:bindCurrentPickingType", "app:bindInterestingPickingType")
fun TextView.bindPickingType(currentPickingType: Boolean, interestingPickingType: Boolean) {
    setTextColor(if (currentPickingType == interestingPickingType) Color.BLACK else Color.LTGRAY)
}

@BindingAdapter("app:bindPickedFloorPair", "app:bindSelfFloor")
fun TextView.bindPickedFloorPairToFloorView(pair: Pair<Floor?, Floor?>?, selfFloor: Floor?) {
    val newContent = if (pair == null || selfFloor == null) {
        Pair("", android.R.color.transparent)
    } else {
        assert(!(pair.first == pair.second && pair.second == selfFloor))

        when (selfFloor) {
            pair.first -> {
                Pair("From", R.color.floorMask)
            }
            pair.second -> {
                Pair("To", R.color.floorMask)
            }
            else -> {
                Pair("", android.R.color.transparent)
            }
        }
    }

    text = newContent.first
    background = context.getDrawable(newContent.second)
}