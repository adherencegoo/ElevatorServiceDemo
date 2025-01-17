package com.xdd.elevatorservicedemo.model

enum class Direction(val offset: Int, val symbol: String) {
    NONE(0, "－"),
    UP(1, "↗"),
    DOWN(-1, "↘");

    companion object {
        fun infer(from: Int, to: Int) = when {
            from > to -> DOWN
            from < to -> UP
            else -> NONE
        }
    }

    fun reverse() = when (this) {
        NONE -> NONE
        UP -> DOWN
        DOWN -> UP
    }
}