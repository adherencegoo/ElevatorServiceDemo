package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.xddlib.presentation.Lg
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject


/**
 * A space containing people
 * */
abstract class Room<K>(val id: Int) {
    class PassengerChange<K>(val increase: Boolean, val key: K)

    private val passengers = HashMap<K, MutableList<Passenger>>()

    private val livePassengerMap = MutableLiveData<HashMap<K, MutableList<Passenger>>>()

    // This is UI-related
    val livePassengerList = Transformations.map(livePassengerMap) { passengerMap ->
        passengerMap?.values?.fold(mutableListOf()) { all, part ->
            all += part
            all
        } ?: emptyList<Passenger>()
    }

    // This is UI-unrelated
    private val subjectPassengerChange = ReplaySubject.create<PassengerChange<K>>()
    val observablePassengerChange: Observable<PassengerChange<K>>
        get() = subjectPassengerChange

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
        subjectPassengerChange.onNext(change)
        livePassengerMap.postValue(passengers)
    }
}