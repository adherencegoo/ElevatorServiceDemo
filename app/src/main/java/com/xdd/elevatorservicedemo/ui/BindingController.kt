package com.xdd.elevatorservicedemo.ui

import android.view.View
import androidx.databinding.ViewDataBinding

/**
 * A wrapper to subclass of [ViewDataBinding]
 *
 * @param V: type for root element
 * */
abstract class BindingController<B : ViewDataBinding, V : View>(val binding: B) {
    @Suppress("UNCHECKED_CAST")
    val typedRoot = binding.root as V
}