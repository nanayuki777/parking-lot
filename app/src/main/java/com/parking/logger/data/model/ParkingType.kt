package com.parking.logger.data.model

enum class ParkingType(val label: String, val issueLabel: String) {
    FRONT_IN("Prependicular Forward", "Prependicular Forward"),
    REVERSE_IN("Prependicular Backward", "Prependicular Backward"),
    SIDE_PARKING("Parallel", "Parallel"),
    FISHBONE("Fishbone", "Fishbone")
}
