package com.xdd.elevatorservicedemo.ui.elevator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdd.elevatorservicedemo.BR
import com.xdd.elevatorservicedemo.databinding.ElevatorFragmentBinding
import com.xdd.elevatorservicedemo.databinding.ElevatorShaftBinding
import com.xdd.elevatorservicedemo.ui.BindingController
import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged
import com.xdd.elevatorservicedemo.utils.suspendGlobalLayout
import kotlinx.coroutines.launch
import kotlin.math.max

class ElevatorFragmentController(fragmentBinding: ElevatorFragmentBinding) :
    BindingController<ElevatorFragmentBinding, ConstraintLayout>(fragmentBinding) {

    companion object {
        private fun initShaftControllers(
            layoutInflater: LayoutInflater,
            parent: ViewGroup,
            lifecycleOwner: LifecycleOwner,
            viewModel: ElevatorViewModel
        ) {
            viewModel.elevators.map { elevator ->
                val shaftBinding =
                    ElevatorShaftBinding.inflate(layoutInflater, parent, true).also {
                        it.root.id = View.generateViewId()
                        it.lifecycleOwner = lifecycleOwner
                        it.elevator = elevator
                    }

                ElevatorShaftController(shaftBinding).also {
                    it.setConfig(viewModel.config)
                }
            }
        }
    }

    private var canNotifyScrollableFloors = true
    private var canNotifyScrollableShaft = true

    init {
        val scrollableFloors = binding.floorsView
        val scrollableShaft = binding.verticalScrollableShaftContainer

        binding.addOnPropertyChanged { localBinding, propertyId ->
            if (propertyId == BR.viewModel) {
                val localViewModel = localBinding.viewModel!!

                localViewModel.uiScope.launch {
                    // init floorsView
                    localBinding.floorsView.suspendGlobalLayout {
                        adapter = FloorAdapter(localViewModel)

                        val layoutOrientation = RecyclerView.VERTICAL
                        layoutManager = LinearLayoutManager(context, layoutOrientation, true)
                        addItemDecoration(DividerItemDecoration(context, layoutOrientation))
                    }


                    // init layoutParams of shaftContainer
                    val shaftHeight = localBinding.floorsView.computeVerticalScrollRange()
                    // Used to let shaftContainer align bottom
                    val shaftTopMargin = max(scrollableShaft.height - shaftHeight, 0)
                    binding.shaftContainer.suspendGlobalLayout {
                        val params = layoutParams as ViewGroup.MarginLayoutParams
                        params.height = shaftHeight
                        params.topMargin = shaftTopMargin
                        layoutParams = params
                    }

                    initShaftControllers(
                        LayoutInflater.from(binding.root.context),
                        binding.shaftContainer,
                        binding.lifecycleOwner!!,
                        localViewModel
                    )

                    // When height of the shaft is updated, scroll to the bottom
                    scrollableShaft.scrollTo(0, shaftHeight - scrollableShaft.height)
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