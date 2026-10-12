package com.parking.logger.data.model

enum class ParkingCondition(val label: String, val parentTypes: List<ParkingType>) {
    ONE_SIDE_CAR("一侧有车", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN)),
    BOTH_SIDES_CAR("两侧有车", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN)),
    NO_CAR("两侧都没有", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN)),
    CAR_AND_PILLAR("车和柱子", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN)),
    PILLAR_AND_LINE("柱子和线", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN)),
    FISHBONE("鱼骨车位", listOf(ParkingType.FRONT_IN, ParkingType.REVERSE_IN)),
    FRONT_CAR("前有车", listOf(ParkingType.SIDE_PARKING)),
    REAR_CAR("后有车", listOf(ParkingType.SIDE_PARKING)),
    BOTH_CAR("前后都有车", listOf(ParkingType.SIDE_PARKING)),
    NO_CAR_AROUND("前后都没车", listOf(ParkingType.SIDE_PARKING)),
    SIDE_PULL_OUT("侧方泊出", listOf(ParkingType.MANUAL_PULL_OUT)),
    STRAIGHT_PULL_OUT("直出", listOf(ParkingType.MANUAL_PULL_OUT))
}
