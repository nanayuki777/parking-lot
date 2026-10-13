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
    data object Boundary : Screen("boundary/{sessionId}/{parkingType}/{condition}") {
        fun createRoute(sessionId: Long, parkingType: String, condition: String) =
            "boundary/$sessionId/$parkingType/$condition"
    }
    data object Count : Screen("count/{sessionId}/{parkingType}/{condition}/{boundary}") {
        fun createRoute(sessionId: Long, parkingType: String, condition: String, boundary: String) =
            "count/$sessionId/$parkingType/$condition/$boundary"
    }
    data object Evaluation : Screen("evaluation/{sessionId}/{parkingType}/{condition}/{boundary}/{totalCount}") {
        fun createRoute(
            sessionId: Long, parkingType: String, condition: String, boundary: String, totalCount: Int
        ) = "evaluation/$sessionId/$parkingType/$condition/$boundary/$totalCount"
    }
    data object PullOut : Screen("pull_out/{recordId}/{sessionId}") {
        fun createRoute(recordId: Long, sessionId: Long) = "pull_out/$recordId/$sessionId"
    }
}
