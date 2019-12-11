package com.xdd.elevatorservicedemo.ui.elevator

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdd.elevatorservicedemo.BR
import com.xdd.elevatorservicedemo.databinding.ElevatorFragmentBinding
import com.xdd.elevatorservicedemo.ui.BindingController
import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged
import com.xdd.elevatorservicedemo.utils.suspendGlobalLayout
import kotlinx.coroutines.launch
import kotlin.math.max

class ElevatorFragmentController(fragmentBinding: ElevatorFragmentBinding) :
    BindingController<ElevatorFragmentBinding, ConstraintLayout>(fragmentBinding) {

    private val elevatorShaftController = ElevatorShaftController(binding.elevatorShaftBinding)
    private var canNotifyScrollableFloors = true
    private var canNotifyScrollableShaft = true

    init {
        val scrollableFloors = binding.floorsView
        val scrollableShaft = binding.scrollableElevatorShaft

        binding.addOnPropertyChanged { localBinding, propertyId ->
            if (propertyId == BR.viewModel) {
                val localViewModel = localBinding.viewModel!!

                elevatorShaftController.setConfig(localViewModel.config)

                localViewModel.uiScope.launch {
                    localBinding.floorsView.suspendGlobalLayout {
                        adapter = FloorAdapter(localViewModel)

                        val layoutOrientation = RecyclerView.VERTICAL
                        layoutManager = LinearLayoutManager(context, layoutOrientation, true)
                        addItemDecoration(DividerItemDecoration(context, layoutOrientation))
                    }

                    // when floorsView is fully loaded, update height of elevatorShaft
                    // --> let floorsView and elevatorShaft have the same height
                    val buildingHeight = localBinding.floorsView.computeVerticalScrollRange()
                    elevatorShaftController.setTotalHeight(
                        buildingHeight,
                        max(scrollableShaft.height - buildingHeight, 0)
                    )

                    // When height of the shaft is updated, scroll to the bottom
                    scrollableShaft.scrollTo(0, buildingHeight - scrollableShaft.height)
                }
            }
        }

        scrollableFloors.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                canNotifyScrollableFloors = false
                if (canNotifyScrollableShaft) {
                    scrollableShaft.scrollBy(dx, dy)
                }
                canNotifyScrollableFloors = true
            }
        })

        scrollableShaft.setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
            canNotifyScrollableShaft = false
            if (canNotifyScrollableFloors) {
                scrollableFloors.scrollBy(scrollX - oldScrollX, scrollY - oldScrollY)
            }
            canNotifyScrollableShaft = true
        }
    }
}