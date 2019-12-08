package com.xdd.elevatorservicedemo.ui.elevator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xdd.elevatorservicedemo.MyApp
import com.xdd.elevatorservicedemo.model.ElevatorService

class ElevatorViewModel(application: Application, config: ElevatorService.Config) :
    AndroidViewModel(application) {

    class Factory(
        private val application: Application,
        private val config: ElevatorService.Config
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            ElevatorViewModel(application, config) as T
    }

    val coroutineAsset = getApplication<MyApp>().appCoroutine.newChild()

    val elevatorService = ElevatorService(config, coroutineAsset)

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


    override fun onCleared() {
        super.onCleared()
        coroutineAsset.cancel()
    }
}