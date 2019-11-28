package com.xdd.elevatorservicedemo.model

enum class Movement(val offset: Int) {
    NONE(0),
    UP(1),
    DOWN(-1);

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