package com.xdd.elevatorservicedemo.ui.elevator

import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged
import com.xdd.elevatorservicedemo.databinding.ElevatorRoomBinding

class ElevatorRoom(roomBinding: ElevatorRoomBinding) {
    val roomLayout = roomBinding.elevatorRoom!!

    init {
        val recycler = roomBinding.passengerRecycler
        recycler.initPassengerRecycler(roomBinding.root.context)
        roomBinding.addOnPropertyChanged {
            recycler.bindPassengerRoom(it.elevator!!)
        }
    }
}