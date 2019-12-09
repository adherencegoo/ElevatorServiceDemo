package com.xdd.elevatorservicedemo.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

fun Job.createChildJob() = Job(this)

fun Job.createScope(dispatcher: CoroutineDispatcher) = CoroutineScope(dispatcher + this)