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

    private val passengersObserver = Observer<List<Passenger>> {
        (binding.passengerRecycler.adapter as PassengerAdapter).postData(it ?: emptyList())
    }

    init {
        // init passenger recycler
        binding.passengerRecycler.initPassengerRecycler()
        binding.addOnPropertyChanged { localBinding, propertyId ->
            if (propertyId == BR.elevator) {
                localBinding.elevator?.livePassengerList?.observe(
                    binding.lifecycleOwner!!,
                    passengersObserver
                )
            }
        }
    }
}