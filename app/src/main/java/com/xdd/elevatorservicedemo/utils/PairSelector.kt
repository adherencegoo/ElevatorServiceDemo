package com.xdd.elevatorservicedemo.utils

import java.lang.UnsupportedOperationException

/**
 * Select a T from the given two Ts
 * */
typealias PairSelector<T> = (T, T) -> T

fun <T> createDefaultSelector(): PairSelector<T> = { a, _ -> a }

class ConditionSelector<T>(
    private val condition: (T) -> Boolean,
    var onBothPassed: PairSelector<T> = createDefaultSelector(),
    var onBothFailed: PairSelector<T> = createDefaultSelector()
) : PairSelector<T> {
    var onBothPassedOrFailed: PairSelector<T> = createDefaultSelector()
        set(value) {
            onBothPassed = value
            onBothFailed = value
            field = value
        }
        get() = throw UnsupportedOperationException("property `onSameConditionResult` is write-only")


    override fun invoke(t1: T, t2: T): T {
        val firstPass = condition(t1)
        val secondPass = condition(t2)

        return if (firstPass && secondPass) {
            onBothPassed(t1, t2)
        } else if (!firstPass && !secondPass) {
            onBothFailed(t1, t2)
        } else if (firstPass) {
            t1
        } else {
            t2
        }
    }
}

class ValueSelector<T, V : Comparable<V>>(
    private val evaluator: (T) -> V,
    private val selectMinValue: Boolean = true,
    var onEqual: PairSelector<T> = createDefaultSelector()
) : PairSelector<T> {
    override fun invoke(t1: T, t2: T): T {
        val v1 = evaluator(t1)
        val v2 = evaluator(t2)

        return when {
            v1 < v2 -> if (selectMinValue) t1 else t2
            v1 > v2 -> if (selectMinValue) t2 else t1
            else -> onEqual(t1, t2)
        }
    }
}