package com.parking.logger.data.model

enum class ParkingType(val label: String) {
    FRONT_IN("正入"),
    REVERSE_IN("反入"),
    SIDE_PARKING("侧方"),
    MANUAL_PULL_OUT("手动泊入后的泊出")
}
