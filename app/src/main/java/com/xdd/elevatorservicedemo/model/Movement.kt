package com.xdd.elevatorservicedemo.model

enum class Movement(val offset: Int) {
    NONE(0),
    UP(1),
    DOWN(-1)
}