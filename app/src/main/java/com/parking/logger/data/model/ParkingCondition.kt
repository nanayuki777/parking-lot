package com.parking.logger.data.model

enum class ParkingCondition(val label: String, val parentTypes: List<ParkingType>) {
    BETWEEN_TWO_OBJECT("Between two object", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN, ParkingType.SIDE_PARKING)),
    FREE_ONE_SIDE("Free one side", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN, ParkingType.SIDE_PARKING)),
    FREE_BOTH_SIDE("Free both side", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN, ParkingType.SIDE_PARKING)),
    SIDE_PULL_OUT("侧方泊出", listOf(ParkingType.FISHBONE)),
    STRAIGHT_PULL_OUT("直出", listOf(ParkingType.FISHBONE))
}
