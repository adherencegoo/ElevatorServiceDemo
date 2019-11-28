package com.xdd.elevatorservicedemo.model

import com.example.xddlib.presentation.Lg


/**
 * A space containing people
 * */
abstract class Room<K>(val id: Int) {
    private val passengers = HashMap<K, MutableList<Passenger>>()

    abstract fun getPassengerKey(passenger: Passenger): K

    fun addPassenger(passenger: Passenger) {
        passengers.getOrPut(getPassengerKey(passenger), { mutableListOf() }) += passenger
        Lg.i(this, passenger)
    }

    fun removePassengers(key: K): List<Passenger> {
        return passengers.remove(key) ?: emptyList()
    }

    fun hasPassengers(key: K): Boolean {
        val list = passengers[key] ?: return false
        return list.isNotEmpty()
    }

    fun hasAnyPassengers() = passengers.isNotEmpty()

    override fun toString(): String {
        return Lg.toNoPackageSimpleString(this, true) + ":{ id:$id }"
    }
}