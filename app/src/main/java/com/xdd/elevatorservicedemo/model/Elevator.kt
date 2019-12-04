package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.xddlib.presentation.Lg
import com.xdd.elevatorservicedemo.nonNullMinBy
import kotlin.math.abs

class Elevator(id: Int, private val service: ElevatorService) : Room<Floor>(id) {
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
        realFloor = service.getFloor(floorId)
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

    fun move() {
        realMovement = getNextMovement()
    }

    private fun arriveFloor(movement: Movement) {
        if (movement.futureDirection != Direction.NONE) {
            realDirection = movement.futureDirection
        }

        val currentFloor = realFloor
        val currentDirection = realDirection

        // leave elevator
        removePassengers(currentFloor).also { Lg.d("leave elevator($this):$it") }

        // leave floor
        currentFloor.removePassengers(currentDirection)
            .also { Lg.d("($currentDirection) leave $currentFloor, enter $this: $it") }
            // enter elevator
            .forEach {
                addPassenger(it)
            }

        move()
    }

    /**
     * getNextDestFloor
     * */
    private fun getNextMovement(): Movement? {
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