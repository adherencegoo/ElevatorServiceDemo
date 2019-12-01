package com.xdd.elevatorservicedemo.ui.elevator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdd.elevatorservicedemo.addDisposableOnGlobalLayoutListener
import com.xdd.elevatorservicedemo.databinding.ElevatorFragmentBinding
import com.xdd.elevatorservicedemo.model.ElevatorService

class ElevatorFragment : Fragment() {

    companion object {
        private const val KEY_ELEVATOR_CONFIG = "KEY_ELEVATOR_CONFIG"

        fun newInstance(config: ElevatorService.Config) = ElevatorFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_ELEVATOR_CONFIG, config)
            }
        }
    }

    private lateinit var fragmentBinding: ElevatorFragmentBinding
    private lateinit var viewModel: ElevatorViewModel
    private lateinit var elevatorShaft: ElevatorShaft

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = ElevatorFragmentBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@ElevatorFragment
        }

        elevatorShaft = ElevatorShaft(fragmentBinding.elevatorShaftBinding)
        return fragmentBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val config = arguments?.getSerializable(KEY_ELEVATOR_CONFIG) as ElevatorService.Config
        val factory = ElevatorViewModel.Factory(config)
        viewModel = ViewModelProviders.of(this, factory).get(ElevatorViewModel::class.java)

        fragmentBinding.viewModel = viewModel

        initFloorsView()
    }

    private fun initFloorsView() {
        fragmentBinding.floorsView.apply {
            // when floorsView is fully loaded, update height of elevatorShaft
            addDisposableOnGlobalLayoutListener {
                // let floorsView and elevatorShaft have the same height
                elevatorShaft.init(viewModel, 0, computeVerticalScrollRange())
            }

            adapter = FloorAdapter(viewModel.elevatorService.floors)

            val layoutOrientation = RecyclerView.VERTICAL
            layoutManager = LinearLayoutManager(context, layoutOrientation, true)
            addItemDecoration(DividerItemDecoration(context, layoutOrientation))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startRandomPassenger(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.startRandomPassenger(false)
    }
}
