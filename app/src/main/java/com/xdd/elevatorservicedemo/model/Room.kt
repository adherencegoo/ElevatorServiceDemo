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

    val livePassengerList = Transformations.map(livePassengerMap) { passengerMap ->
        passengerMap?.values?.fold(mutableListOf()) { all, part ->
            all += part
            all
        } ?: emptyList<Passenger>()
    }

    /** must be called in sync block*/
    private fun notifyPassengerMapUpdated() = livePassengerMap.postValue(passengers)

    abstract fun getPassengerKey(passenger: Passenger): K

    open fun idToName() = id.toString()

    open fun addPassengers(newPassengers: List<Passenger>) {
        synchronized(passengers) {
            newPassengers.forEach {
                passengers.getOrPut(getPassengerKey(it), { mutableListOf() }) += it
            }
            notifyPassengerMapUpdated()
            Lg.i(this, newPassengers)
        }
    }

    fun removePassengers(key: K): List<Passenger> {
        return synchronized(passengers) {
            passengers.remove(key)?.also { notifyPassengerMapUpdated() } ?: emptyList()
        }
    }

    fun hasPassengers(key: K): Boolean {
        return synchronized(passengers) {
            passengers[key]?.isNotEmpty() == true
        }
    }

    fun hasAnyPassengers() = synchronized(passengers) {
        passengers.isNotEmpty()
    }

    override fun toString(): String {
        return Lg.toNoPackageSimpleString(this, true) + ":{ id:${idToName()} }"
    }
}