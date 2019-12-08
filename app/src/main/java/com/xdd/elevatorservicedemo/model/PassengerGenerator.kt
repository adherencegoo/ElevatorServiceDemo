package com.xdd.elevatorservicedemo.model

import com.xdd.elevatorservicedemo.ui.elevator.ElevatorViewModel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class PassengerGenerator {
    private val random = Random()

    suspend fun generate(viewModel: ElevatorViewModel): Passenger {
        val floorCount = viewModel.config.floorCount
        val baseFloor = viewModel.config.baseFloor

        var fromFloor: Int
        var toFloor: Int

        return suspendCancellableCoroutine {
            do {
                fromFloor = random.nextInt(floorCount) + baseFloor
                toFloor = random.nextInt(floorCount) + baseFloor
            } while (fromFloor == toFloor)

            val passenger =
                Passenger(viewModel.getFloor(fromFloor), viewModel.getFloor(toFloor))
            it.resume(passenger)
        }
    }
}