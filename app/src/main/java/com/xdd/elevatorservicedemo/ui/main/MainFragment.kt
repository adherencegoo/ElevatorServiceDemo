package com.xdd.elevatorservicedemo.ui.main

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.xdd.elevatorservicedemo.R
import com.xdd.elevatorservicedemo.databinding.MainFragmentBinding
import com.xdd.elevatorservicedemo.ui.elevator.ElevatorFragment

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var fragmentBinding: MainFragmentBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = MainFragmentBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@MainFragment
        }
        return fragmentBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        fragmentBinding.viewModel = viewModel
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_create_service -> {
                createService()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createService() {
        val activity = requireActivity()
        val config = viewModel.createConfig()

        if (config.floorCount < 2) {
            Toast.makeText(activity, "Invalid floor count, must >= 2", Toast.LENGTH_LONG)
                .show()
        } else {
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.container, ElevatorFragment.newInstance(config))
                .addToBackStack(null)
                .commit()
        }
    }
}
