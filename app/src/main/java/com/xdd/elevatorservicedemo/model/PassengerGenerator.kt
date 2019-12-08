package com.xdd.elevatorservicedemo.model

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class PassengerGenerator {
    private val random = Random()

    suspend fun generate(service: ElevatorService): Passenger {
        val floorCount = service.config.floorCount
        val baseFloor = service.config.baseFloor

        var fromFloor: Int
        var toFloor: Int

        return suspendCancellableCoroutine {
            do {
                fromFloor = random.nextInt(floorCount) + baseFloor
                toFloor = random.nextInt(floorCount) + baseFloor
            } while (fromFloor == toFloor)

            val passenger =
                Passenger(service.getFloor(fromFloor), service.getFloor(toFloor))
            it.resume(passenger)
        }
    }
}