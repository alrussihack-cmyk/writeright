package com.writeright.ai.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.writeright.ai.data.repository.CourseRepository
import com.writeright.ai.feature.dashboard.DashboardScreen
import com.writeright.ai.feature.lessons.LessonPreviewScreen

@Composable
fun AppNavHost(navController: NavHostController, repository: CourseRepository) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(repository = repository, onStartLesson = { lessonId ->
                navController.navigate("lesson/$lessonId")
            })
        }
        composable(
            route = "lesson/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { entry ->
            val lessonId = entry.arguments?.getString("lessonId").orEmpty()
            LessonPreviewScreen(lessonId, repository) { navController.popBackStack() }
        }
    }
}
