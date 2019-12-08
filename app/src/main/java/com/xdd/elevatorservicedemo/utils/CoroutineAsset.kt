package com.xdd.elevatorservicedemo.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class CoroutineAsset(parentJob: Job? = null) {
    private val currentJob = Job(parentJob)

    val uiScope = CoroutineScope(Dispatchers.Main + currentJob)
    val backgroundScope = CoroutineScope(Dispatchers.IO + currentJob)

    fun newChild() = CoroutineAsset(currentJob)

    fun cancel() = currentJob.cancel()
}