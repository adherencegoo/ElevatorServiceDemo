package com.xdd.elevatorservicedemo.ui.elevator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = ElevatorFragmentBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@ElevatorFragment
        }
        return fragmentBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val factory =
            ElevatorViewModel.Factory(arguments?.getSerializable(KEY_ELEVATOR_CONFIG) as ElevatorService.Config)
        viewModel = ViewModelProviders.of(this, factory).get(ElevatorViewModel::class.java)
        fragmentBinding.viewModel = viewModel
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
