package com.xdd.elevatorservicedemo.ui.elevator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.xdd.elevatorservicedemo.databinding.FloorRoomBinding
import com.xdd.elevatorservicedemo.model.Floor
import com.xdd.elevatorservicedemo.model.Passenger
import com.xdd.elevatorservicedemo.ui.RecyclerBindingAdapter
import java.lang.UnsupportedOperationException
import java.lang.ref.WeakReference

class FloorAdapter(viewModel: ElevatorViewModel) :
    RecyclerBindingAdapter<Floor, FloorRoomBinding>() {
    private val passengerGenerator = viewModel.passengerGenerator

    init {
        super.postData(viewModel.floors)
    }

    class ViewHolder(binding: FloorRoomBinding) :
        RecyclerBindingAdapter.ViewHolder<Floor, FloorRoomBinding>(binding) {
        private val passengerObserver = Observer<List<Passenger>> {
            (binding.passengerRecycler.adapter as PassengerAdapter).postData(it ?: emptyList())
        }

        private var lastLivePassengers: WeakReference<LiveData<List<Passenger>>>? = null

        override fun bindData(data: Floor) {
            binding.floor = data

            // Remove observation on previous passenger source
            lastLivePassengers?.get()?.removeObserver(passengerObserver)
            data.livePassengerList.let { livePassengers ->
                livePassengers.observe(binding.lifecycleOwner!!, passengerObserver)
                // For unknown reason, observer won't be triggered after right executing the above function
                // Manually fetch the latest passengers from this floor
                passengerObserver.onChanged(livePassengers.value)

                lastLivePassengers = WeakReference(livePassengers)
            }
        }
    }

    class DiffCallback(oldList: List<Floor>, newList: List<Floor>) :
        RecyclerBindingAdapter.DiffCallback<Floor>(oldList, newList) {
        override fun areItemsTheSame(old: Floor, new: Floor): Boolean = old.id == new.id
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerBindingAdapter.ViewHolder<Floor, FloorRoomBinding> = ViewHolder(
        FloorRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .apply {
                generator = passengerGenerator
                lifecycleOwner = parent.context as LifecycleOwner
                passengerRecycler.initPassengerRecycler()
            })

    override fun postData(data: List<Floor>) {
        throw UnsupportedOperationException("Floors can't be changed at runtime")
    }

    override fun newDiffCallback(
        oldList: List<Floor>,
        newList: List<Floor>
    ): RecyclerBindingAdapter.DiffCallback<Floor> = DiffCallback(oldList, newList)
}