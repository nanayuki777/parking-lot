package com.parking.logger.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object TesterMgmt : Screen("tester_mgmt")
    data object History : Screen("history")
    data object Session : Screen("session/{sessionId}") {
        fun createRoute(sessionId: Long) = "session/$sessionId"
    }
    data object ParkingType : Screen("parking_type/{sessionId}") {
        fun createRoute(sessionId: Long) = "parking_type/$sessionId"
    }
    data object Condition : Screen("condition/{sessionId}/{parkingType}") {
        fun createRoute(sessionId: Long, parkingType: String) = "condition/$sessionId/$parkingType"
    }
    data object Count : Screen("count/{sessionId}/{parkingType}/{condition}") {
        fun createRoute(sessionId: Long, parkingType: String, condition: String) =
            "count/$sessionId/$parkingType/$condition"
    }
    data object Evaluation : Screen("evaluation/{sessionId}/{parkingType}/{condition}/{totalCount}") {
        fun createRoute(
            sessionId: Long, parkingType: String, condition: String, totalCount: Int
        ) = "evaluation/$sessionId/$parkingType/$condition/$totalCount"
    }
    data object PullOut : Screen("pull_out/{recordId}/{sessionId}") {
        fun createRoute(recordId: Long, sessionId: Long) = "pull_out/$recordId/$sessionId"
    }
}
