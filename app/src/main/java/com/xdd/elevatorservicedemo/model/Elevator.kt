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
    internal class Goal(
        internal val floor: Floor,
        preferredFutureDirection: Direction,
        elseFutureDirection: Direction
    ) {
        internal val futureMovement = if (floor.hasPassengers(preferredFutureDirection)) {
            preferredFutureDirection
        } else {
            elseFutureDirection
        }

        override fun toString(): String {
            return Lg.toNoPackageSimpleString(this, true) + ":{ floor:$floor, futureMovement:$futureMovement }"
        }
    }

    internal inner class CandidateRequest(
        private val wantedDirection: Direction,
        private val candidateFloors: Iterable<Int>
    ) {
        internal fun findGoal(): Goal? {
            // from candidate floors, find the first floor
            // that has (internal requests) or (external requests with the same movement)
            val targetFloor = candidateFloors.map {
                service.getFloor(it)
            }.firstOrNull { floor ->
                hasPassengers(floor) || floor.hasPassengers(wantedDirection)
            }

            return targetFloor?.let { floor ->
                Goal(floor, wantedDirection, Direction.NONE)
            }
        }
    }

    private val _liveFloor = MutableLiveData(service.floors.first())
    val liveFloor: LiveData<Floor> = _liveFloor
    var realFloor = service.floors.first()
        private set(value) {
            if (field != value) {
                Lg.become("realFloor", field, value).printLog(Lg.Type.I)
                field = value
                _liveFloor.postValue(value)
            }
        }

    fun setRealFloorId(floorId: Int) {
        realFloor = service.getFloor(floorId)
    }

    private val _liveMovement = MutableLiveData(Direction.NONE)
    val liveDirection: LiveData<Direction> = _liveMovement
    private var realMovement = Direction.NONE
        set(value) {
            if (field != value) {
                Lg.become("realMovement", field, value).printLog(Lg.Type.I)
                field = value
                _liveMovement.postValue(value)
            }
        }

    override fun getPassengerKey(passenger: Passenger): Floor = service.getFloor(passenger.toFloor)

    fun move() {
        while (true) {
            getNextGoal().also { Lg.v("nextGoal:$it, realFloor:$realFloor, realMovement:$realMovement") }?.let { goal ->
                val goalFloor = goal.floor

                realMovement = Direction.infer(realFloor.id, goalFloor.id)
                realFloor = service.getFloor(realFloor, realMovement.offset)

                if (realFloor == goalFloor) {
                    arriveFloor(goal)
                }

                Thread.sleep(500) // xdd
            } ?: kotlin.run {
                realMovement = Direction.NONE
                service.conditionWait()
            }
        }
    }

    private fun arriveFloor(goal: Goal) {
        if (goal.futureMovement != Direction.NONE) {
            realMovement = goal.futureMovement
        }

        val currentFloor = realFloor
        val currentMovement = realMovement

        // leave elevator
        removePassengers(currentFloor).also { Lg.d("leave elevator($this):$it") }

        // leave floor
        currentFloor.removePassengers(currentMovement)
            .also { Lg.d("($currentMovement) leave $currentFloor, enter $this: $it") }
            // enter elevator
            .forEach {
                addPassenger(it)
            }
    }

    /**
     * getNextDestFloor
     * */
    private fun getNextGoal(): Goal? {
        val currentFloorId = realFloor.id
        val currentMovement = realMovement
        val top = service.config.topFloor
        val base = service.config.baseFloor

        return when (currentMovement) {
            Direction.NONE -> getPrioritizedGoalWithNoneMovement()
            Direction.UP -> getPrioritizedGoal(
                CandidateRequest(currentMovement, currentFloorId + 1..top),
                CandidateRequest(currentMovement.reverse(), top downTo base),
                CandidateRequest(currentMovement, base until currentFloorId)
            )
            Direction.DOWN -> getPrioritizedGoal(
                CandidateRequest(currentMovement, currentFloorId - 1 downTo base),
                CandidateRequest(currentMovement.reverse(), base..top),
                CandidateRequest(currentMovement, top downTo currentFloorId + 1)
            )
        }
    }

    /**
     * doesn't take Movement into account
     * */
    private fun getPrioritizedGoalWithNoneMovement(): Goal? {
        val currentFloor = realFloor
        val top = service.config.topFloor
        val base = service.config.baseFloor

        // serve current floor
        currentFloor.takeIf { it.hasAnyPassengers() }?.let {
            return Goal(it, Direction.UP, Direction.DOWN)
        }

        val currentFloorId = currentFloor.id
        // serve outward passengers
        Pair(
            CandidateRequest(Direction.UP, currentFloorId + 1..top).findGoal(),
            CandidateRequest(Direction.DOWN, currentFloorId - 1 downTo base).findGoal())
            .nonNullMinBy {
                // find the one which is closer to currentFloor
                abs(it.floor - currentFloor)
            }?.let {
                return it
            }

        // serve inward passengers
        Pair(
            CandidateRequest(Direction.DOWN, top downTo currentFloorId + 1).findGoal(),
            CandidateRequest(Direction.UP, base until currentFloorId).findGoal())
            .nonNullMinBy {
                // find the one which is farther to currentFloor
                -abs(it.floor - currentFloor)
            }?.let {
                return it
            }

        return null
    }

    private fun getPrioritizedGoal(vararg candidateRequests: CandidateRequest): Goal? {
        candidateRequests.forEach {
            val goal = it.findGoal()
            if (goal != null) return goal
        }
        return null
    }

    override fun idToName(): String {
        return "Elevator:" + super.idToName()
    }
}