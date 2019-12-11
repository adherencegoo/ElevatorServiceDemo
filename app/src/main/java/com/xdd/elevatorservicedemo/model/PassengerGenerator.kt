package com.xdd.elevatorservicedemo.model

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.xdd.elevatorservicedemo.ui.elevator.ElevatorViewModel
import com.xdd.elevatorservicedemo.utils.CombinedLiveData
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class PassengerGenerator {
    companion object {
        const val FLOOR_PICKING_TYPE_START = false
//       const val FLOOR_PICKING_TYPE_END = true
    }

    private val random = Random()

    val observablePickingType = ObservableBoolean(FLOOR_PICKING_TYPE_START)

    val livePickedStartFloor = MutableLiveData<Floor?>()
    private var pickedStartFloor: Floor? = null
        private set(value) {
            field = value
            livePickedStartFloor.postValue(value)
        }

    val livePickedEndFloor = MutableLiveData<Floor?>()
    private var pickedEndFloor: Floor? = null
        private set(value) {
            field = value
            livePickedEndFloor.postValue(value)
        }

    val livePickedPair = CombinedLiveData(livePickedStartFloor, livePickedEndFloor) { start, end ->
        Pair(start, end)
    }

    suspend fun generate(viewModel: ElevatorViewModel): Passenger {
        val floorCount = viewModel.config.floorCount
        val baseFloor = viewModel.config.baseFloor

        var fromFloor: Int
        var toFloor: Int

        return suspendCancellableCoroutine {
            do {
                fromFloor = pickedStartFloor?.id ?: random.nextInt(floorCount) + baseFloor
                toFloor = pickedEndFloor?.id ?: random.nextInt(floorCount) + baseFloor
            } while (fromFloor == toFloor)

            val passenger =
                Passenger(viewModel.getFloor(fromFloor), viewModel.getFloor(toFloor))
            it.resume(passenger)
        }
    }

    fun pickFloor(picked: Floor) {
        if (observablePickingType.get() == FLOOR_PICKING_TYPE_START) {
            // if picking the same floor --> reset to null
            pickedStartFloor = if (pickedStartFloor == picked) null else picked
            if (invalidPickPair) {
                pickedEndFloor = null
            }
        } else {
            pickedEndFloor = if (pickedEndFloor == picked) null else picked
            if (invalidPickPair) {
                pickedStartFloor = null
            }
        }
    }

    private val invalidPickPair: Boolean
        get() = pickedStartFloor != null && pickedStartFloor == pickedEndFloor

    fun setPickingType(type: Boolean) {
        observablePickingType.set(type)
    }

    fun exchangeStartEndFloor() {
        val tmp = pickedStartFloor
        pickedStartFloor = pickedEndFloor
        pickedEndFloor = tmp
    }
}