package com.writeright.ai.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.writeright.ai.feature.dashboard.DashboardScreen
import com.writeright.ai.feature.lessons.LessonPreviewScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(onStartLesson = { navController.navigate(Routes.LESSON) })
        }
        composable(Routes.LESSON) {
            LessonPreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
