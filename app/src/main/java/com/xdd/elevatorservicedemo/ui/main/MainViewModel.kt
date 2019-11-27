package com.xdd.elevatorservicedemo.ui.main

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.xdd.elevatorservicedemo.R
import com.xdd.elevatorservicedemo.model.ElevatorService
import com.xdd.elevatorservicedemo.model.UserInt
import com.xdd.elevatorservicedemo.ui.elevator.ElevatorFragment

class MainViewModel : ViewModel() {
    val baseFloor = UserInt()
    val floorCount = UserInt()
    val elevatorCount = UserInt(1)

    fun createService(view: View) {
        val activity = view.context as FragmentActivity

        val config = ElevatorService.Config(
            baseFloor.intValue,
            floorCount.intValue,
            elevatorCount.intValue
        )
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.container, ElevatorFragment.newInstance(config))
            .addToBackStack(null)
            .commit()
    }
}
