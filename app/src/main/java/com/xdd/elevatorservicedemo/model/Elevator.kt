package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.xddlib.presentation.Lg
import com.xdd.elevatorservicedemo.nonNullMinBy
import kotlin.math.abs

class Elevator(id: Int, private val service: ElevatorService) : Room<Int>(id) {
    /**
     * a target floor and a future movement after arriving at that floor
     * */
    internal class Goal(
        internal val floor: Floor,
        preferredFutureMovement: Movement,
        elseFutureMovement: Movement
    ) {
        internal val futureMovement = if (floor.hasPassengers(preferredFutureMovement)) {
            preferredFutureMovement
        } else {
            elseFutureMovement
        }

        override fun toString(): String {
            return Lg.toNoPackageSimpleString(this, true) + ":{ floor:$floor, futureMovement:$futureMovement }"
        }
    }

    internal inner class CandidateRequest(
        private val wantedMovement: Movement,
        private val candidateFloors: Iterable<Int>
    ) {
        internal fun findGoal(): Goal? {
            // from candidate floors, find the first floor
            // that has (internal requests) or (external requests with the same movement)
            val targetFloor = candidateFloors.map {
                service.getFloor(it)
            }.firstOrNull { floor ->
                hasPassengers(floor.id) || floor.hasPassengers(wantedMovement)
            }

            return targetFloor?.let { floor ->
                Goal(floor, wantedMovement, Movement.NONE)
            }
        }
    }

    private val _liveFloor = MutableLiveData(0)
    val liveFloor: LiveData<Int> = _liveFloor
    private var realFloor = service.config.baseFloor
        set(value) {
            if (field != value) {
                Lg.become("realFloor", field, value).printLog(Lg.Type.I)
                field = value
                _liveFloor.postValue(value)
            }
        }

    private val _liveMovement = MutableLiveData(Movement.NONE)
    val liveMovement: LiveData<Movement> = _liveMovement
    private var realMovement = Movement.NONE
        set(value) {
            if (field != value) {
                Lg.become("realMovement", field, value).printLog(Lg.Type.I)
                field = value
                _liveMovement.postValue(value)
            }
        }

    override fun getPassengerKey(passenger: Passenger): Int = passenger.toFloor

    fun move() {
        while (true) {
            getNextGoal().also { Lg.v("nextGoal:$it, realFloor:${service.getFloor(realFloor)}, realMovement:$realMovement") }?.let { goal ->
                val goalFloorId = goal.floor.id

                realMovement = Movement.infer(realFloor, goalFloorId)
                realFloor += realMovement.offset

                if (realFloor == goalFloorId) {
                    arriveFloor(goal)
                }

                Thread.sleep(500) // xdd
            } ?: kotlin.run {
                realMovement = Movement.NONE
                service.conditionWait()
            }
        }
    }

    private fun arriveFloor(goal: Goal) {
        if (goal.futureMovement != Movement.NONE) {
            realMovement = goal.futureMovement
        }

        val currentFloor = realFloor
        val currentMovement = realMovement

        // leave elevator
        removePassengers(currentFloor).also { Lg.d("leave elevator($this):$it") }

        // leave floor
        service.getFloor(currentFloor).removePassengers(currentMovement)
            .also { Lg.d("($currentMovement) leave ${service.getFloor(currentFloor)}, enter $this: $it") }
            // enter elevator
            .forEach {
                addPassenger(it)
            }

        var nextMovement: Movement? = null
        if (hasAnyPassengers()) {
            if (currentFloor == service.topFloor && currentMovement == Movement.UP) {
                nextMovement = Movement.DOWN
            } else if (currentFloor == service.config.baseFloor && currentMovement == Movement.DOWN) {
                nextMovement = Movement.UP
            }
        } else {
            nextMovement = Movement.NONE
        }

        nextMovement?.let {
            realMovement = it
        }
    }

    /**
     * getNextDestFloor
     * */
    private fun getNextGoal(): Goal? {
        val currentFloor = realFloor
        val currentMovement = realMovement
        val top = service.topFloor
        val base = service.config.baseFloor

        return when (currentMovement) {
            Movement.NONE -> getPrioritizedGoalWithNoneMovement()
            Movement.UP -> getPrioritizedGoal(
                CandidateRequest(currentMovement, currentFloor + 1..top),
                CandidateRequest(currentMovement.reverse(), top downTo base),
                CandidateRequest(currentMovement, base until currentFloor)
            )
            Movement.DOWN -> getPrioritizedGoal(
                CandidateRequest(currentMovement, currentFloor - 1 downTo base),
                CandidateRequest(currentMovement.reverse(), base..top),
                CandidateRequest(currentMovement, top downTo currentFloor + 1)
            )
        }
    }

    /**
     * doesn't take Movement into account
     * */
    private fun getPrioritizedGoalWithNoneMovement(): Goal? {
        val currentFloor = realFloor
        val top = service.topFloor
        val base = service.config.baseFloor

        // serve current floor
        service.getFloor(currentFloor).takeIf { it.hasAnyPassengers() }?.let {
            return Goal(it, Movement.UP, Movement.DOWN)
        }

        // serve outward passengers
        Pair(
            CandidateRequest(Movement.UP, currentFloor + 1..top).findGoal(),
            CandidateRequest(Movement.DOWN, currentFloor - 1 downTo base).findGoal())
            .nonNullMinBy {
                // find the one which is closer to currentFloor
                abs(it.floor.id - currentFloor)
            }?.let {
                return it
            }

        // serve inward passengers
        Pair(
            CandidateRequest(Movement.DOWN, top downTo currentFloor + 1).findGoal(),
            CandidateRequest(Movement.UP, base until currentFloor).findGoal())
            .nonNullMinBy {
                // find the one which is farther to currentFloor
                -abs(it.floor.id - currentFloor)
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
}