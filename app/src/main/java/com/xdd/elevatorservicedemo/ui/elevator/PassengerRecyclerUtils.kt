package com.xdd.elevatorservicedemo.ui.elevator

import android.content.Context
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.initPassengerRecycler(context: Context) {
    adapter = PassengerAdapter()

    val layoutOrientation = RecyclerView.HORIZONTAL
    layoutManager = LinearLayoutManager(context, layoutOrientation, false)
    addItemDecoration(DividerItemDecoration(context, layoutOrientation))

    setRecycledViewPool(PassengerAdapter.viewPool)
}