package com.xdd.elevatorservicedemo.ui.elevator

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdd.elevatorservicedemo.BR
import com.xdd.elevatorservicedemo.databinding.ElevatorFragmentBinding
import com.xdd.elevatorservicedemo.ui.BindingController
import com.xdd.elevatorservicedemo.utils.addDisposableOnGlobalLayoutListener
import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged

class ElevatorFragmentController(fragmentBinding: ElevatorFragmentBinding) :
    BindingController<ElevatorFragmentBinding, ConstraintLayout>(fragmentBinding) {

    private val elevatorShaftController = ElevatorShaftController(binding.elevatorShaftBinding)

    init {
        binding.addOnPropertyChanged { localBinding, propertyId ->
            if (propertyId == BR.viewModel) {
                val localViewModel = localBinding.viewModel!!

                elevatorShaftController.setConfig(localViewModel.elevatorService.config)

                localBinding.floorsView.apply {
                    // when floorsView is fully loaded, update height of elevatorShaft
                    addDisposableOnGlobalLayoutListener {
                        // let floorsView and elevatorShaft have the same height
                        elevatorShaftController.setTotalHeight(computeVerticalScrollRange())
                    }

                    adapter = FloorAdapter(localViewModel.elevatorService.floors)

                    val layoutOrientation = RecyclerView.VERTICAL
                    layoutManager = LinearLayoutManager(context, layoutOrientation, true)
                    addItemDecoration(DividerItemDecoration(context, layoutOrientation))
                }
            }
        }
    }
}