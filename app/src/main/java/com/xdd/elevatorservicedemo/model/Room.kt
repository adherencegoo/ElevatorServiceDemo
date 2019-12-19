package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.xddlib.presentation.Lg


/**
 * A space containing people
 * */
abstract class Room<K>(val id: Int) {
    class PassengerChange<K>(val increase: Boolean, val key: K)

    private val passengers = HashMap<K, MutableList<Passenger>>()

    private val livePassengerMap = MutableLiveData<HashMap<K, MutableList<Passenger>>>()

    val livePassengerList = Transformations.map(livePassengerMap) { passengerMap ->
        passengerMap?.values?.fold(mutableListOf()) { all, part ->
            all += part
            all
        } ?: emptyList<Passenger>()
    }

    private val _livePassengerChange = MutableLiveData<PassengerChange<K>>()
    val livePassengerChange: LiveData<PassengerChange<K>>
        get() = _livePassengerChange

    abstract fun getPassengerKey(passenger: Passenger): K

    open fun idToName() = id.toString()

    open fun addPassengers(newPassengers: List<Passenger>) {
        newPassengers.groupBy { getPassengerKey(it) }.forEach { (key, list) ->
            addPassengersWithSameKey(key, list)
        }
    }

    private fun addPassengersWithSameKey(key: K, newPassengers: List<Passenger>) {
        synchronized(passengers) {
            passengers.getOrPut(key, { mutableListOf() }) += newPassengers
            notifyPassengerChanged(PassengerChange(true, key))
            Lg.i(this, newPassengers)
        }
    }

    fun removePassengers(key: K): List<Passenger> {
        return synchronized(passengers) {
            passengers.remove(key)?.also {
                notifyPassengerChanged(PassengerChange(false, key))
            } ?: emptyList()
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

    private fun notifyPassengerChanged(change: PassengerChange<K>) {
        _livePassengerChange.postValue(change)
        livePassengerMap.postValue(passengers)
    }
}