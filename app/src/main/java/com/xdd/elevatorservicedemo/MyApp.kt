package com.xdd.elevatorservicedemo

import android.app.Application
import kotlinx.coroutines.Job

class MyApp : Application() {
    val appCoroutineJob = Job()

    override fun onTerminate() {
        super.onTerminate()
        appCoroutineJob.cancel()
    }
}