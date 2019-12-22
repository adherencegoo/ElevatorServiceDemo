package com.xdd.elevatorservicedemo.model

data class FloorEvent(
    val floor: Floor,
    val direction: Direction,
    val excludedElevator: Elevator?,
    val type: Type
) {
    enum class Type {
        REQUEST_SERVICE,
        NOTIFY_SERVED
    }

    companion object {
        fun fromPassengerChange(floor: Floor, passengerChange: Room.PassengerChange<Direction>) =
            FloorEvent(
                floor,
                passengerChange.key,
                null,
                if (passengerChange.increase) Type.REQUEST_SERVICE else Type.NOTIFY_SERVED
            )

        fun fromElevatorMovementChange(
            elevator: Elevator,
            movement: Elevator.Movement,
            type: Type
        ) = FloorEvent(movement.toFloor, movement.futureDirection, elevator, type)
    }
}