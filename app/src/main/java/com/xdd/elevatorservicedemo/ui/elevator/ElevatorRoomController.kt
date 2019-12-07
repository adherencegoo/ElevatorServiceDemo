package com.xdd.elevatorservicedemo.ui.elevator

import androidx.constraintlayout.widget.ConstraintLayout
import com.xdd.elevatorservicedemo.BR
import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged
import com.xdd.elevatorservicedemo.databinding.ElevatorRoomBinding
import com.xdd.elevatorservicedemo.ui.BindingController

class ElevatorRoomController(roomBinding: ElevatorRoomBinding) :
    BindingController<ElevatorRoomBinding, ConstraintLayout>(roomBinding) {

    init {
        val recycler = binding.passengerRecycler
        recycler.initPassengerRecycler(binding.root.context)

        binding.addOnPropertyChanged { localBinding, propertyId ->
            if (propertyId == BR.elevator) {
                localBinding.elevator?.let {
                    recycler.bindPassengerRoom(it)
                }
            }
        }
    }
}