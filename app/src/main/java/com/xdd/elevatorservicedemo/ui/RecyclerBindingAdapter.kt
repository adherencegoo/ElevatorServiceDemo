package com.xdd.elevatorservicedemo.ui

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerBindingAdapter<T : Any, VDB : ViewDataBinding> :
    RecyclerView.Adapter<RecyclerBindingAdapter.ViewHolder<T, VDB>>() {

    abstract class ViewHolder<T : Any, VDB : ViewDataBinding>(protected val binding: VDB) :
        RecyclerView.ViewHolder(binding.root) {

        abstract fun bindData(data: T)
    }

    abstract class DiffCallback<T>(private val oldList: List<T>, private val newList: List<T>) :
        DiffUtil.Callback() {

        abstract fun areItemsTheSame(old: T, new: T): Boolean

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    private val data = mutableListOf<T>()

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder<T, VDB>, position: Int) =
        holder.bindData(data[position])

    abstract fun newDiffCallback(oldList: List<T>, newList: List<T>): DiffCallback<T>

    open fun postData(data: List<T>) {
        val result = DiffUtil.calculateDiff(newDiffCallback(this.data, data))
        this.data.clear()
        this.data += data
        result.dispatchUpdatesTo(this)
    }
}