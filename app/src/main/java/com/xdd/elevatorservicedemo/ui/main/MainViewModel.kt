package com.xdd.elevatorservicedemo.ui.main

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import com.xdd.elevatorservicedemo.model.ElevatorServiceConfig
import com.xdd.elevatorservicedemo.utils.UserInt

class MainViewModel : ViewModel() {
    val baseFloor = UserInt()
    val floorCount = UserInt(10)
    val elevatorCount = UserInt(1)
    val animationDurationPerFloor = UserInt(1000)
    val doorAnimationEnabled = ObservableBoolean(true)

    fun createConfig() = ElevatorServiceConfig(
        baseFloor.intValue,
        floorCount.intValue,
        elevatorCount.intValue,
        animationDurationPerFloor.intValue.toLong(),
        doorAnimationEnabled.get()
    )
}
