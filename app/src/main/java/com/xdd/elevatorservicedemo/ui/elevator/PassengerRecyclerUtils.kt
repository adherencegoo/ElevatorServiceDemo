package com.xdd.elevatorservicedemo.ui.elevator

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdd.elevatorservicedemo.model.Passenger
import com.xdd.elevatorservicedemo.model.Room

fun RecyclerView.initPassengerRecycler(context: Context) {
    adapter = PassengerAdapter()

    val layoutOrientation = RecyclerView.HORIZONTAL
    layoutManager = LinearLayoutManager(context, layoutOrientation, false)
    addItemDecoration(DividerItemDecoration(context, layoutOrientation))

    setRecycledViewPool(PassengerAdapter.viewPool)
}

fun RecyclerView.bindPassengerRoom(room: Room<*>) {
    room.livePassengerList.observe(context as LifecycleOwner, Observer<List<Passenger>> {
        (adapter as PassengerAdapter).postData(it ?: emptyList())
    })
}