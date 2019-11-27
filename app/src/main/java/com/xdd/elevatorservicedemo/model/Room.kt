package com.xdd.elevatorservicedemo.model


/**
 * A space containing people
 * */
abstract class Room<K>(val id: Int) {
    private val passengers = HashMap<K, MutableList<Passenger>>()

    abstract fun getPassengerKey(passenger: Passenger): K

    fun addPassenger(passenger: Passenger) {
        passengers.getOrPut(getPassengerKey(passenger), { mutableListOf() }) += passenger
    }

    fun removePassengers(key: K): List<Passenger> {
        return passengers.remove(key) ?: emptyList()
    }

    fun hasPassengers(key: K): Boolean {
        val list = passengers[key] ?: return false
        return list.isNotEmpty()
    }

    fun hasAnyPassengers() = passengers.isNotEmpty()
}