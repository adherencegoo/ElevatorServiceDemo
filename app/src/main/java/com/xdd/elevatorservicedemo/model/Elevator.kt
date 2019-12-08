package com.xdd.elevatorservicedemo.model

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.xddlib.presentation.Lg
import com.xdd.elevatorservicedemo.utils.nonNullMinBy
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs

class Elevator(id: Int, service: ElevatorService) : Room<Floor>(id) {
    companion object {
        private const val DELAY_FOR_PASSENGER_ANIMATION = 1000L
    }

    /**
     * a target floor and a future movement after arriving at that floor
     * */
    class Movement(
        val fromFloor: Floor,
        val toFloor: Floor,
        preferredFutureDirection: Direction,
        elseFutureDirection: Direction
    ) {
        internal val futureDirection = if (toFloor.hasPassengers(preferredFutureDirection)) {
            preferredFutureDirection
        } else {
            elseFutureDirection
        }

        override fun toString(): String {
            return Lg.toNoPackageSimpleString(this, true) + ":{ $fromFloor --> $toFloor, futureDirection:$futureDirection }"
        }
    }

    internal inner class CandidateRequest(
        private val wantedDirection: Direction,
        private val candidateFloors: Iterable<Int>
    ) {
        internal fun findMovement(): Movement? {
            val service = weakService.get() ?: return null
            // from candidate floors, find the first floor
            // that has (internal requests) or (external requests with the same movement)
            val targetFloor = candidateFloors.map {
                service.getFloor(it)
            }.firstOrNull { floor ->
                hasPassengers(floor) || floor.hasPassengers(wantedDirection)
            }

            return targetFloor?.let { floor ->
                Movement(realFloor, floor, wantedDirection, Direction.NONE)
            }
        }
    }

    private val weakService = WeakReference(service)

    private val uiHandler = Handler(Looper.getMainLooper())

    private val _liveDoorState = MutableLiveData(DoorState.CLOSED)
    val liveDoorState: LiveData<DoorState> = _liveDoorState
    private var realDoorState = DoorState.CLOSED
        private set(value) {
            field = value
            _liveDoorState.postValue(value)
        }

    private val _liveFloor = MutableLiveData(service.floors.first())
    val liveFloor: LiveData<Floor> = _liveFloor
    private var realFloor = service.floors.first()
        private set(value) {
            if (field != value) {
                Lg.become("realFloor", field, value).printLog(Lg.Type.I)
                field = value
                _liveFloor.postValue(value)

                // When floor changed, if currentFloor == targetFloor, then arrive
                realMovement?.let {
                    if (field == it.toFloor) {
                        arriveFloor(it)
                    }
                }
            }
        }

    fun setRealFloorId(floorId: Int) {
        weakService.get()?.let {
            realFloor = it.getFloor(floorId)
        }
    }

    private val _liveDirection = MutableLiveData(Direction.NONE)
    val liveDirection: LiveData<Direction> = _liveDirection
    private var realDirection = Direction.NONE
        set(value) {
            if (field != value) {
                Lg.become("realDirection", field, value).printLog(Lg.Type.I)
                field = value
                _liveDirection.postValue(value)
            }
        }

    private val _liveMovement = MutableLiveData<Movement>()
    val liveMovement: LiveData<Movement> = _liveMovement
    private var realMovement: Movement? = null
        private set(value) {
            val differentDestFloor = field?.toFloor != value?.toFloor
            // When movement changed, if currentFloor == targetFloor, then arrive
            val alreadyAtDestFloor = realFloor == value?.toFloor

            if (differentDestFloor || alreadyAtDestFloor) {
                Lg.become("realMovement", field, value).printLog(Lg.Type.I)
                field = value
                _liveMovement.postValue(value)

                if (alreadyAtDestFloor) {
                    arriveFloor(value!!)
                }
            }
        }

    override fun getPassengerKey(passenger: Passenger): Floor = passenger.toFloor

    /**
     * Long: delayed time for animation of this action
     * */
    private val pendingOnArriveActions = ArrayDeque<() -> Long>()

    private fun supplyOnArriveAction(action: () -> Long, allowDuplicate: Boolean = true) {
        synchronized(this) {
            if (allowDuplicate || !pendingOnArriveActions.contains(action)) {
                pendingOnArriveActions.offer(action)
            }
        }
    }

    private fun consumeOnArriveAction() {
        // remove all events
        uiHandler.removeCallbacksAndMessages(null)
        while (true) {
            val delay = synchronized(this) { pendingOnArriveActions.poll() }?.invoke() ?: break
            if (delay > 0) {
                //xdd: how to prevent postDelayed, and listen to end of PassengerAdapter animation
                uiHandler.postDelayed(this::consumeOnArriveAction, delay)
                break
            }
        }
    }

    private val actionMove = {
        realMovement = getNextMovement()
        0L
    }

    fun triggerMove() {
        supplyOnArriveAction(actionMove, false)
        consumeOnArriveAction()
    }

    private fun arriveFloor(movement: Movement) {
        val service = weakService.get() ?: return

        if (movement.futureDirection != Direction.NONE) {
            realDirection = movement.futureDirection
        }

        val currentFloor = realFloor
        val currentDirection = realDirection

        if (service.config.doorAnimationEnabled) {
            supplyOnArriveAction({
                realDoorState = DoorState.OPENING
                realDoorState.animationDuration
            })
        }

        supplyOnArriveAction({
            realDoorState = DoorState.OPEN
            realDoorState.animationDuration
        })

        // leave elevator
        supplyOnArriveAction({
            val passengers = removePassengers(currentFloor).also { Lg.d("leave elevator($this):$it") }
            if (passengers.isEmpty()) 0 else DELAY_FOR_PASSENGER_ANIMATION
        })

        supplyOnArriveAction({
            // leave floor
            val fromFloorToElevator = currentFloor.removePassengers(currentDirection)
                .also { Lg.d("($currentDirection) leave $currentFloor, enter $this: $it") }
            // enter elevator
            addPassengers(fromFloorToElevator)

            if (fromFloorToElevator.isEmpty()) 0 else DELAY_FOR_PASSENGER_ANIMATION
        })

        if (service.config.doorAnimationEnabled) {
            supplyOnArriveAction({
                realDoorState = DoorState.CLOSING
                realDoorState.animationDuration
            })
        }

        supplyOnArriveAction({
            realDoorState = DoorState.CLOSED
            realDoorState.animationDuration
        })

        triggerMove()
    }

    /**
     * getNextDestFloor
     * */
    private fun getNextMovement(): Movement? {
        val service = weakService.get() ?: return null

        val currentFloorId = realFloor.id
        val currentMovement = realDirection
        val top = service.config.topFloor
        val base = service.config.baseFloor

        val movement = when (currentMovement) {
            Direction.NONE -> getPrioritizedMovementWithNoneDirection()
            Direction.UP -> getPrioritizedMovement(
                CandidateRequest(currentMovement, currentFloorId + 1..top),
                CandidateRequest(currentMovement.reverse(), top downTo base),
                CandidateRequest(currentMovement, base until currentFloorId)
            )
            Direction.DOWN -> getPrioritizedMovement(
                CandidateRequest(currentMovement, currentFloorId - 1 downTo base),
                CandidateRequest(currentMovement.reverse(), base..top),
                CandidateRequest(currentMovement, top downTo currentFloorId + 1)
            )
        }

        realDirection = movement?.let {
            Direction.infer(realFloor.id, it.toFloor.id)
        } ?: Direction.NONE

        return movement
    }

    /**
     * doesn't take Movement into account
     * */
    private fun getPrioritizedMovementWithNoneDirection(): Movement? {
        val service = weakService.get() ?: return null

        val currentFloor = realFloor
        val top = service.config.topFloor
        val base = service.config.baseFloor

        // serve current floor
        currentFloor.takeIf { it.hasAnyPassengers() }?.let {
            return Movement(currentFloor, it, Direction.UP, Direction.DOWN)
        }

        val currentFloorId = currentFloor.id
        // serve outward passengers
        Pair(
            CandidateRequest(Direction.UP, currentFloorId + 1..top).findMovement(),
            CandidateRequest(Direction.DOWN, currentFloorId - 1 downTo base).findMovement())
            .nonNullMinBy {
                // find the one which is closer to currentFloor
                abs(it.toFloor - currentFloor)
            }?.let {
                return it
            }

        // serve inward passengers
        Pair(
            CandidateRequest(Direction.DOWN, top downTo currentFloorId + 1).findMovement(),
            CandidateRequest(Direction.UP, base until currentFloorId).findMovement())
            .nonNullMinBy {
                // find the one which is farther to currentFloor
                -abs(it.toFloor - currentFloor)
            }?.let {
                return it
            }

        return null
    }

    private fun getPrioritizedMovement(vararg candidateRequests: CandidateRequest): Movement? {
        candidateRequests.forEach {
            val movement = it.findMovement()
            if (movement != null) return movement
        }
        return null
    }

    override fun idToName(): String {
        return "Elevator:" + super.idToName()
    }
}