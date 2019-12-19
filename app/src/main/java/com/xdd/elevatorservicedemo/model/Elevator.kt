package com.xdd.elevatorservicedemo.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.xddlib.presentation.Lg
import com.xdd.elevatorservicedemo.ui.elevator.ElevatorViewModel
import com.xdd.elevatorservicedemo.utils.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

class Elevator(id: Int, private val viewModel: ElevatorViewModel) : Room<Floor>(id) {
    companion object {
        private const val DELAY_FOR_PASSENGER_ANIMATION = 1000L
        private val ELEVATORS_COWORK_LOCK = object : Any() {}
    }

    class PriorityComparator(requestedFloor: Floor, requestedDirection: Direction) :
        Comparator<Elevator> {
        private val rootSelector: PairSelector<Elevator>

        init {
            val matchFloorRequestSelector = ConditionSelector<Elevator>({ elevator ->
                elevator.realMovement?.let {
                    it.toFloor == requestedFloor && it.futureDirection == requestedDirection
                } == true
            })

            val hasInteriorRequestSelector = ConditionSelector<Elevator>({
                it.hasPassengers(requestedFloor)
            })

            val minCurrentDistanceSelector = ValueSelector<Elevator, Int>({
                abs(it.realFloor.id - requestedFloor.id)
            })

            // distance after finishing current movement
            val minFutureDistanceSelector = ValueSelector<Elevator, Int>({
                abs((it.realMovement?.toFloor ?: it.realFloor).id - requestedFloor.id)
            })

            val idleSelector = ConditionSelector<Elevator>({
                it.realMovement == null
            })

            // the elevator is moving toward the requestedFloor
            val moveTowardRequestSelector = ConditionSelector<Elevator>({
                it.realDirection == Direction.infer(it.realFloor.id, requestedFloor.id)
            })

            // requestedFloor is on the way from current floor to original destination floor
            val withinRealMovementSelector = ConditionSelector<Elevator>({ elevator ->
                elevator.realMovement?.let {
                    requestedFloor.id in elevator.realFloor.id..it.toFloor.id
                } == true
            })

            // assemble the above selectors into a tree structure selector
            moveTowardRequestSelector.also { moveToward ->
                moveToward.onBothPassed = withinRealMovementSelector.also { withinMovement ->
                    withinMovement.onBothPassed = matchFloorRequestSelector.also { matchRequest ->
                        matchRequest.onBothPassedOrFailed =
                            hasInteriorRequestSelector.also { hasInteriorRequest ->
                                hasInteriorRequest.onBothPassed = minCurrentDistanceSelector
                            }
                    }

                    withinMovement.onBothFailed =
                        minFutureDistanceSelector.also { minFutureDistance ->
                            minFutureDistance.onEqual = hasInteriorRequestSelector
                        }
                }

                moveToward.onBothFailed = idleSelector.also { idle ->
                    idle.onBothPassed = minCurrentDistanceSelector
                }
            }

            rootSelector = moveTowardRequestSelector
        }

        override fun compare(e0: Elevator, e1: Elevator): Int {
            return if (rootSelector(e0, e1) == e0) -1 else 1
        }
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
            return Lg.toNoPackageSimpleString(
                this,
                true
            ) + ":{ $fromFloor --> $toFloor, futureDirection:$futureDirection }"
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
                viewModel.getFloor(it)
            }.firstOrNull { floor ->
                hasPassengers(floor) || viewModel.elevatorShouldHandleFloorRequest(
                    this@Elevator,
                    floor,
                    wantedDirection
                )
            }

            return targetFloor?.let { floor ->
                Movement(realFloor, floor, wantedDirection, Direction.NONE)
            }
        }
    }

    private val actionConsumerScope = viewModel.coroutineJob.createSingleThreadScope()

    private abstract class NamedAction(private val name: String) : () -> Long {
        override fun toString(): String {
            return "$name-NamedAction@${Integer.toHexString(hashCode())}"
        }
    }

    private val subjectDoArriveFloor = PublishSubject.create<Unit>()

    private val disposableOnArriveFloor = subjectDoArriveFloor
        .subscribeOn(Schedulers.single())
        // the following action is supposed to be very light-weighted
        // --> use a single shared background scheduler
        .subscribe {
            realMovement?.let(this@Elevator::arriveFloor)
        }

    private val _liveDoorState = MutableLiveData(DoorState.CLOSED)
    val liveDoorState: LiveData<DoorState> = _liveDoorState
    private var realDoorState = DoorState.CLOSED
        private set(value) {
            field = value
            _liveDoorState.postValue(value)
        }

    private val _liveFloor = MutableLiveData(viewModel.floors.first())
    val liveFloor: LiveData<Floor> = _liveFloor
    private var realFloor = viewModel.floors.first()
        private set(value) {
            if (field != value) {
                Lg.i(this, Lg.become("realFloor", field, value))
                field = value
                _liveFloor.postValue(value)

                // When floor changed, if currentFloor == targetFloor, then arrive
                if (field == realMovement?.toFloor) {
                    subjectDoArriveFloor.onNext(Unit)
                }
            }
        }

    fun setRealFloorId(floorId: Int) {
        realFloor = viewModel.getFloor(floorId)
    }

    private val _liveDirection = MutableLiveData(Direction.NONE)
    val liveDirection: LiveData<Direction> = _liveDirection
    private var realDirection = Direction.NONE
        set(value) {
            if (field != value) {
                Lg.i(this, Lg.become("realDirection", field, value))
                field = value
                _liveDirection.postValue(value)
            }
        }

    private val _liveMovement = MutableLiveData<Movement>()
    val liveMovement: LiveData<Movement> = _liveMovement
    var realMovement: Movement? = null
        private set(value) {
            val differentDestFloor = field?.toFloor != value?.toFloor
            // When movement changed, if currentFloor == targetFloor, then arrive
            val alreadyAtDestFloor = realFloor == value?.toFloor

            if (differentDestFloor || alreadyAtDestFloor) {
                Lg.i(this, Lg.become("realMovement", field, value))
                field = value
                _liveMovement.postValue(value)

                if (alreadyAtDestFloor) {
                    /* The setter of realMovement is encapsulated in a coroutine Job
                     * And, `arriveFloor` will start a new coroutine Job
                     *
                     * Use non-overlapping Scheduler to prevent nested Job
                    */
                    subjectDoArriveFloor.onNext(Unit)
                }
            }
        }

    override fun getPassengerKey(passenger: Passenger): Floor = passenger.toFloor

    /**
     * Synced by `this`
     *
     * Long: delayed time for animation of this action
     * */
    private val pendingActions = LinkedList<() -> Long>()

    /**
     * @return the `action` is enqueued
     * */
    private fun supplyAction(action: () -> Long) =
        synchronized(this) {
            if (!pendingActions.contains(action)) {
                pendingActions.offer(action)
                true
            } else {
                false
            }
        }

    private fun consumeAction() {
        actionConsumerScope.launch {
            while (isActive) {
                val action = synchronized(this) { pendingActions.poll() } ?: break
                val delayTime = action.invoke()

                // Use sleep to block the latter actions
                @Suppress("BlockingMethodInNonBlockingContext")
                Thread.sleep(delayTime)
            }
        }
    }

    private val actionMove = object : NamedAction("Move") {
        override fun invoke(): Long {
            // `getNextMovement` will use `realMovement`s from other elevators
            // Use synchronized to make sure that: `realMovement` is already assigned when reading it
            synchronized(ELEVATORS_COWORK_LOCK) {
                realMovement = getNextMovement()
            }
            return 0L
        }
    }

    private val actionMapDoorState = DoorState.values().map {
        it to object : NamedAction(it.toString()) {
            override fun invoke(): Long {
                realDoorState = it
                return realDoorState.animationDuration
            }
        }
    }.toMap()

    private val actionLeaveElevator = object : NamedAction("LeaveElevator") {
        override fun invoke(): Long {
            val passengers =
                removePassengers(realFloor).also { Lg.d("leave elevator(${this@Elevator}):$it") }
            return if (passengers.isEmpty()) 0 else DELAY_FOR_PASSENGER_ANIMATION
        }
    }

    private val actionEnterElevator = object : NamedAction("EnterElevator") {
        override fun invoke(): Long {
            // leave floor
            val fromFloorToElevator = realFloor.removePassengers(realDirection)
                .also { Lg.d("($realDirection) leave $realFloor, enter ${this@Elevator}: $it") }
            // enter elevator
            addPassengers(fromFloorToElevator)

            return if (fromFloorToElevator.isEmpty()) 0 else DELAY_FOR_PASSENGER_ANIMATION
        }
    }

    fun triggerMove() {
        if (supplyAction(actionMove)) {
            consumeAction()
        }
    }

    /**
     * Should be invoked only by [subjectDoArriveFloor]
     * */
    private fun arriveFloor(movement: Movement) {
        if (movement.futureDirection != Direction.NONE) {
            realDirection = movement.futureDirection
        }

        // Clear pending actions
        synchronized(this) {
            pendingActions.clear()
        }

        if (viewModel.config.doorAnimationEnabled) {
            supplyAction(actionMapDoorState.getValue(DoorState.OPENING))
        }
        supplyAction(actionMapDoorState.getValue(DoorState.OPEN))

        supplyAction(actionLeaveElevator)
        supplyAction(actionEnterElevator)

        if (viewModel.config.doorAnimationEnabled) {
            supplyAction(actionMapDoorState.getValue(DoorState.CLOSING))
        }
        supplyAction(actionMapDoorState.getValue(DoorState.CLOSED))

        triggerMove()
    }

    /**
     * getNextDestFloor
     * */
    private fun getNextMovement(): Movement? {
        val currentFloorId = realFloor.id
        val currentMovement = realDirection
        val top = viewModel.config.topFloor
        val base = viewModel.config.baseFloor

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
        val top = viewModel.config.topFloor
        val base = viewModel.config.baseFloor

        // serve current floor
        currentFloor.takeIf { it.hasAnyPassengers() }?.let {
            return Movement(currentFloor, it, Direction.UP, Direction.DOWN)
        }

        val currentFloorId = currentFloor.id
        // serve outward passengers
        Pair(
            CandidateRequest(Direction.UP, currentFloorId + 1..top).findMovement(),
            CandidateRequest(Direction.DOWN, currentFloorId - 1 downTo base).findMovement()
        ).nonNullMinBy {
            // find the one which is closer to currentFloor
            abs(it.toFloor - currentFloor)
        }?.let {
            return it
        }

        // serve inward passengers
        Pair(
            CandidateRequest(Direction.DOWN, top downTo currentFloorId + 1).findMovement(),
            CandidateRequest(Direction.UP, base until currentFloorId).findMovement()
        ).nonNullMinBy {
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

    fun destroy() {
        disposableOnArriveFloor.dispose()
    }
}