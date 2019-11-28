package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
            return super.toString() + ":{ floor:$floor, futureMovement:$futureMovement }"
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
                field = value
                _liveFloor.postValue(value)
            }
        }

    private val _liveMovement = MutableLiveData(Movement.NONE)
    val liveMovement: LiveData<Movement> = _liveMovement
    private var realMovement = Movement.NONE
        set(value) {
            if (field != value) {
                field = value
                _liveMovement.postValue(value)
            }
        }

    override fun getPassengerKey(passenger: Passenger): Int = passenger.toFloor

    fun move() {
        while (true) {
            getNextGoal()?.let { goal ->
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
        removePassengers(currentFloor)

        // leave floor
        service.getFloor(currentFloor).removePassengers(currentMovement)
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
            Movement.NONE -> getNearestGoal()
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
    private fun getNearestGoal(): Goal? {
        val currentFloor = realFloor

        // find a nearest floor with any waiting passengers
        val targetFloor = service.floors.filter {
            it.hasAnyPassengers()
        }.minBy {
            abs(it.id - currentFloor)
        }

        return targetFloor?.let { floor ->
            // if going from currentFloor to targetFloor,
            // the future movement after arriving at targetFloor will be the same as original one
            val preferredFutureMovement =
                if (floor.id > currentFloor) Movement.UP else Movement.DOWN
            Goal(floor, preferredFutureMovement, preferredFutureMovement.reverse())
        }
    }

    private fun getPrioritizedGoal(vararg candidateRequests: CandidateRequest): Goal? {
        candidateRequests.forEach {
            val goal = it.findGoal()
            if (goal != null) return goal
        }
        return null
    }
}