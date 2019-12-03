package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.xddlib.presentation.Lg


/**
 * A space containing people
 * */
abstract class Room<K>(val id: Int) {
    private val passengers = HashMap<K, MutableList<Passenger>>()

    private val livePassengerMap = MutableLiveData<HashMap<K, MutableList<Passenger>>>()

    val livePassengerList = Transformations.switchMap(livePassengerMap) { passengerMap ->

        val passengerList: List<Passenger> =
            passengerMap?.values?.fold(mutableListOf()) { all, part ->
                all += part
                all
            } ?: emptyList()
        MutableLiveData(passengerList)
    }

    private fun notifyPassengerMapUpdated() = livePassengerMap.postValue(passengers)

    abstract fun getPassengerKey(passenger: Passenger): K

    open fun idToName() = id.toString()

    fun addPassenger(passenger: Passenger) {
        passengers.getOrPut(getPassengerKey(passenger), { mutableListOf() }) += passenger
        notifyPassengerMapUpdated()
        Lg.i(this, passenger)
    }

    fun removePassengers(key: K): List<Passenger> {
        return passengers.remove(key)?.also { notifyPassengerMapUpdated() } ?: emptyList()
    }

    fun hasPassengers(key: K): Boolean {
        val list = passengers[key] ?: return false
        return list.isNotEmpty()
    }

    fun hasAnyPassengers() = passengers.isNotEmpty()

    override fun toString(): String {
        return Lg.toNoPackageSimpleString(this, true) + ":{ id:${idToName()} }"
    }
}