package com.xdd.elevatorservicedemo.utils

import android.view.View
import android.view.ViewTreeObserver
import androidx.databinding.Observable
import java.lang.NumberFormatException

inline fun <reified T : Observable> T.addOnPropertyChanged(crossinline callback: (T, Int) -> Unit) =
    object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(observable: Observable?, propertyId: Int) =
            callback(observable as T, propertyId)
    }.also {
        addOnPropertyChangedCallback(it)
    }

fun String?.nullableToInt(): Int = try {
    this?.toInt() ?: 0
} catch (e: NumberFormatException) {
    0
}

fun<T : Any, R : Comparable<R>> Pair<T?, T?>.nonNullMinBy(selector: (T) -> R): T? {
    return if (first != null) {
        if (second != null) {
            val r1 = selector.invoke(first!!)
            val r2 = selector.invoke(second!!)
            if (r1 < r2) first else second
        } else {
            first
        }
    } else {
        second
    }
}

fun View.addDisposableOnGlobalLayoutListener(job: () -> Unit) {
    val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            job.invoke()
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }
    viewTreeObserver.addOnGlobalLayoutListener(listener)
}