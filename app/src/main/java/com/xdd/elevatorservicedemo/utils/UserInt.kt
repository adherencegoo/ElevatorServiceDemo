package com.xdd.elevatorservicedemo.utils

import androidx.databinding.ObservableField

class UserInt(initialValue: Int = 0) {
    val observedString = ObservableField<String>(initialValue.toString()).apply {
        addOnPropertyChanged {
            intValue = it.get().nullableToInt()
        }
    }

    var intValue = initialValue
        private set
}