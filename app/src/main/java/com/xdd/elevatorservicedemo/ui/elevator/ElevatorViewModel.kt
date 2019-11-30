package com.xdd.elevatorservicedemo.ui.elevator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xdd.elevatorservicedemo.model.ElevatorService
import com.xdd.elevatorservicedemo.model.Passenger
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class ElevatorViewModel(config: ElevatorService.Config) : ViewModel() {
    class Factory(private val config: ElevatorService.Config) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ElevatorViewModel(config) as T
    }

    private val elevatorService = ElevatorService(config)

    val floors = elevatorService.floors

//    init { // xdd
//        elevatorService.elevators.forEach {  elevator ->
//            elevator.liveFloor.observeForever {
//                Lg.v("elevator(${elevator.id}): floor:${it}")
//            }
//
//            elevator.liveMovement.observeForever {
//                Lg.d("elevator(${elevator.id}): movement:${it}")
//            }
//        }
//    }


    private val random = Random()
    private var coordinateGenerator: TimerTask? = null

    fun startRandomPassenger(start: Boolean) {
        coordinateGenerator = if (start) {
            elevatorService.start()

            val config = elevatorService.config
            Timer().scheduleAtFixedRate(0, 5000) {

                var fromFloor: Int
                var toFloor: Int
                do {
                    fromFloor = random.nextInt(config.floorCount) + config.baseFloor
                    toFloor = random.nextInt(config.floorCount) + config.baseFloor
                } while (fromFloor == toFloor)

                elevatorService.newPassenger(Passenger(fromFloor, toFloor))
            }
        } else {
            coordinateGenerator?.cancel()
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        coordinateGenerator?.cancel()
    }
}