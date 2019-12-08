package com.xdd.elevatorservicedemo.ui.elevator

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.xdd.elevatorservicedemo.R
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

    private lateinit var viewModel: ElevatorViewModel
    private lateinit var fragmentController: ElevatorFragmentController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentController = ElevatorFragmentController(
            ElevatorFragmentBinding.inflate(inflater, container, false).apply {
                lifecycleOwner = this@ElevatorFragment
            })

        return fragmentController.binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val config = arguments?.getSerializable(KEY_ELEVATOR_CONFIG) as ElevatorService.Config
        val factory = ElevatorViewModel.Factory(config)
        viewModel = ViewModelProviders.of(this, factory).get(ElevatorViewModel::class.java)
        fragmentController.binding.viewModel = viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        viewModel.startRandomPassenger(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.startRandomPassenger(false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.elevator_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_new_passenger -> {
                val passenger = viewModel.newPassenger()
                Toast.makeText(
                    context,
                    passenger.getContentString(),
                    Toast.LENGTH_LONG
                ).show()
                true
            }
            else -> false
        }
    }
}
