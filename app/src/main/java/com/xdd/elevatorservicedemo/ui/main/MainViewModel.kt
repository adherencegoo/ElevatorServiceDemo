package com.xdd.elevatorservicedemo.ui.main

import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.xdd.elevatorservicedemo.R
import com.xdd.elevatorservicedemo.model.ElevatorService
import com.xdd.elevatorservicedemo.model.UserInt
import com.xdd.elevatorservicedemo.ui.elevator.ElevatorFragment

class MainViewModel : ViewModel() {
    val baseFloor = UserInt()
    val floorCount = UserInt(10)
    val elevatorCount = UserInt(1)
    val animationDurationPerFloor = UserInt(1000)

    fun createService(view: View) {
        val activity = view.context as FragmentActivity

        if (floorCount.intValue <= 0) {
            Toast.makeText(activity, "Floor count should be a positive integer", Toast.LENGTH_LONG)
                .show()
        } else {
            val config = ElevatorService.Config(
                baseFloor.intValue,
                floorCount.intValue,
                elevatorCount.intValue,
                animationDurationPerFloor.intValue.toLong()
            )
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.container, ElevatorFragment.newInstance(config))
                .addToBackStack(null)
                .commit()
        }
    }
}
