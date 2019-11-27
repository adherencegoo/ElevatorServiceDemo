package com.xdd.elevatorservicedemo.model

import androidx.databinding.ObservableField
import com.xdd.elevatorservicedemo.addOnPropertyChanged
import com.xdd.elevatorservicedemo.nullableToInt

class UserInt(initialValue: Int = 0) {
    val observedString = ObservableField<String>(initialValue.toString()).apply {
        addOnPropertyChanged {
            intValue = it.get().nullableToInt()
        }
    }

    var intValue = initialValue
        private set
}