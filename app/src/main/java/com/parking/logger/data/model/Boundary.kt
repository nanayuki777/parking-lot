package com.parking.logger.data.model

enum class Boundary(val label: String) {
    LINE("线"),
    STONE("石头"),
    MIXED("混合"),
    CURB("马路牙子"),
    WIRELESS_CAR_ONLY("无线只有车")
}
