package com.xdd.elevatorservicedemo.utils

import kotlinx.coroutines.*
import java.util.concurrent.Executors

fun Job.createChildJob() = Job(this)

fun Job.createScope(dispatcher: CoroutineDispatcher) = CoroutineScope(dispatcher + this)

fun Job.createSingleThreadScope() =
    CoroutineScope(this + Executors.newSingleThreadExecutor().asCoroutineDispatcher())