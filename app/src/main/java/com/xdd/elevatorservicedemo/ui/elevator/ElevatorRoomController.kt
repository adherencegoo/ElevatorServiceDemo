package com.xdd.elevatorservicedemo.ui.elevator

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.xdd.elevatorservicedemo.BR
import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged
import com.xdd.elevatorservicedemo.databinding.ElevatorRoomBinding
import com.xdd.elevatorservicedemo.model.Passenger
import com.xdd.elevatorservicedemo.ui.BindingController

class ElevatorRoomController(roomBinding: ElevatorRoomBinding) :
    BindingController<ElevatorRoomBinding, ConstraintLayout>(roomBinding) {

    init {
        val recycler = binding.passengerRecycler
        recycler.initPassengerRecycler(binding.root.context)

        binding.addOnPropertyChanged { localBinding, propertyId ->
            if (propertyId == BR.elevator) {
                localBinding.elevator?.let { localElevator ->
                    localElevator.livePassengerList.observe(localBinding.lifecycleOwner!!, Observer<List<Passenger>> {
                        (recycler.adapter as PassengerAdapter).postData(it ?: emptyList())
                    })
                }
            }
        }
    }
}