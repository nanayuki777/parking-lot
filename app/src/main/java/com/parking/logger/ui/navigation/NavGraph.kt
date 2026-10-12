package com.parking.logger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.parking.logger.ParkingApp
import com.parking.logger.ui.screens.boundary.BoundaryScreen
import com.parking.logger.ui.screens.condition.ConditionScreen
import com.parking.logger.ui.screens.count.CountScreen
import com.parking.logger.ui.screens.evaluation.EvaluationScreen
import com.parking.logger.ui.screens.home.HomeScreen
import com.parking.logger.ui.screens.parking_type.ParkingTypeScreen
import com.parking.logger.ui.screens.pullout.PullOutScreen
import com.parking.logger.ui.screens.session.SessionScreen
import com.parking.logger.ui.screens.tester_mgmt.TesterMgmtScreen

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as ParkingApp }
    val repository = remember { app.repository }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onStartSession = { sessionId ->
                    navController.navigate(Screen.Session.createRoute(sessionId))
                },
                onManageTesters = {
                    navController.navigate(Screen.TesterMgmt.route)
                }
            )
        }

        composable(Screen.TesterMgmt.route) {
            TesterMgmtScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Session.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            SessionScreen(
                sessionId = sessionId,
                repository = repository,
                onStartParking = {
                    navController.navigate(Screen.ParkingType.createRoute(sessionId))
                },
                onSessionDeleted = {
                    navController.popBackStack(Screen.Home.route, false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ParkingType.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val records by repository.getRecordsBySession(sessionId).collectAsState(initial = emptyList())
            ParkingTypeScreen(
                records = records,
                onTypeSelected = { type ->
                    navController.navigate(Screen.Condition.createRoute(sessionId, type.name))
                },
                onEndTest = {
                    navController.popBackStack(Screen.Session.createRoute(sessionId), false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Condition.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("parkingType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val parkingType = backStackEntry.arguments?.getString("parkingType") ?: return@composable
            ConditionScreen(
                parkingTypeName = parkingType,
                onConditionSelected = { condition ->
                    navController.navigate(Screen.Boundary.createRoute(sessionId, parkingType, condition.name))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Boundary.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("parkingType") { type = NavType.StringType },
                navArgument("condition") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val parkingType = backStackEntry.arguments?.getString("parkingType") ?: return@composable
            val condition = backStackEntry.arguments?.getString("condition") ?: return@composable
            BoundaryScreen(
                onBoundarySelected = { boundary ->
                    navController.navigate(Screen.Count.createRoute(sessionId, parkingType, condition, boundary.name))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Count.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("parkingType") { type = NavType.StringType },
                navArgument("condition") { type = NavType.StringType },
                navArgument("boundary") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val parkingType = backStackEntry.arguments?.getString("parkingType") ?: return@composable
            val condition = backStackEntry.arguments?.getString("condition") ?: return@composable
            val boundary = backStackEntry.arguments?.getString("boundary") ?: return@composable
            val parkingTypeLabel = runCatching {
                com.parking.logger.data.model.ParkingType.valueOf(parkingType).label
            }.getOrDefault(parkingType)
            CountScreen(
                parkingTypeLabel = parkingTypeLabel,
                onConfirm = { totalCount ->
                    navController.navigate(
                        Screen.Evaluation.createRoute(sessionId, parkingType, condition, boundary, totalCount)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Evaluation.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("parkingType") { type = NavType.StringType },
                navArgument("condition") { type = NavType.StringType },
                navArgument("boundary") { type = NavType.StringType },
                navArgument("totalCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val parkingType = backStackEntry.arguments?.getString("parkingType") ?: return@composable
            val condition = backStackEntry.arguments?.getString("condition") ?: return@composable
            val boundary = backStackEntry.arguments?.getString("boundary") ?: return@composable
            val totalCount = backStackEntry.arguments?.getInt("totalCount") ?: 0
            EvaluationScreen(
                sessionId = sessionId,
                parkingTypeName = parkingType,
                conditionName = condition,
                boundaryName = boundary,
                totalCount = totalCount,
                repository = repository,
                onRecordSaved = { recordId, needsPullOut ->
                    if (needsPullOut) {
                        navController.navigate(Screen.PullOut.createRoute(recordId, sessionId))
                    } else {
                        navController.popBackStack(Screen.ParkingType.createRoute(sessionId), false)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PullOut.route,
            arguments = listOf(
                navArgument("recordId") { type = NavType.LongType },
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: return@composable
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            PullOutScreen(
                recordId = recordId,
                repository = repository,
                onSaved = {
                    navController.popBackStack(Screen.ParkingType.createRoute(sessionId), false)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
