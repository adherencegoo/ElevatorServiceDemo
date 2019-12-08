package com.xdd.elevatorservicedemo

import android.app.Application
import com.xdd.elevatorservicedemo.utils.CoroutineAsset

class MyApp : Application() {
    val appCoroutine = CoroutineAsset()

    override fun onTerminate() {
        super.onTerminate()
        appCoroutine.cancel()
    }
}