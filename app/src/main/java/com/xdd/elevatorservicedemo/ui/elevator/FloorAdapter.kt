package com.xdd.elevatorservicedemo.ui.elevator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.xdd.elevatorservicedemo.databinding.FloorRoomBinding
import com.xdd.elevatorservicedemo.model.Floor
import com.xdd.elevatorservicedemo.ui.RecyclerBindingAdapter
import java.lang.UnsupportedOperationException

class FloorAdapter(viewModel: ElevatorViewModel) : RecyclerBindingAdapter<Floor, FloorRoomBinding>() {
    private val passengerGenerator = viewModel.passengerGenerator

    init {
        super.postData(viewModel.floors)
    }

    class ViewHolder(binding: FloorRoomBinding) :
        RecyclerBindingAdapter.ViewHolder<Floor, FloorRoomBinding>(binding) {
        override fun bindData(data: Floor) {
            binding.floor = data
            binding.passengerRecycler.bindPassengerRoom(data)
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
                passengerRecycler.initPassengerRecycler(parent.context)
            })

    override fun postData(data: List<Floor>) {
        throw UnsupportedOperationException("Floors can't be changed at runtime")
    }

    override fun newDiffCallback(
        oldList: List<Floor>,
        newList: List<Floor>
    ): RecyclerBindingAdapter.DiffCallback<Floor> = DiffCallback(oldList, newList)
}