package com.xdd.elevatorservicedemo.utils

import android.view.View
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume

fun Job.createChildJob() = Job(this)

fun Job.createScope(dispatcher: CoroutineDispatcher) = CoroutineScope(dispatcher + this)

fun Job.createSingleThreadScope() =
    CoroutineScope(this + Executors.newSingleThreadExecutor().asCoroutineDispatcher())

suspend fun <V : View> V.suspendGlobalLayout(job: V.() -> Unit) {
    suspendCancellableCoroutine<Unit> {
        addDisposableOnGlobalLayoutListener {
            it.resume(Unit)
        }
        job.invoke(this)
    }
}