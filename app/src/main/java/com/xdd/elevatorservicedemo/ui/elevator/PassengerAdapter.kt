package com.xdd.elevatorservicedemo.ui.elevator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.xdd.elevatorservicedemo.databinding.PassengerViewBinding
import com.xdd.elevatorservicedemo.model.Passenger
import com.xdd.elevatorservicedemo.ui.RecyclerBindingAdapter

class PassengerAdapter : RecyclerBindingAdapter<Passenger, PassengerViewBinding>() {

    class ViewHolder(binding: PassengerViewBinding) :
        RecyclerBindingAdapter.ViewHolder<Passenger, PassengerViewBinding>(binding) {
        override fun bindData(data: Passenger) {
            binding.passenger = data
        }
    }

    class DiffCallback(oldList: List<Passenger>, newList: List<Passenger>) :
        RecyclerBindingAdapter.DiffCallback<Passenger>(oldList, newList) {
        override fun areItemsTheSame(old: Passenger, new: Passenger): Boolean = old.id == new.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            PassengerViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .apply {
                    lifecycleOwner = parent.context as LifecycleOwner
                })

    override fun newDiffCallback(
        oldList: List<Passenger>,
        newList: List<Passenger>
    ): RecyclerBindingAdapter.DiffCallback<Passenger> = DiffCallback(oldList, newList)
}