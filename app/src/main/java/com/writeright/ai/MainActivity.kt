package com.writeright.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.writeright.ai.core.design.WriteRightTheme
import com.writeright.ai.core.navigation.AppNavHost
import com.writeright.ai.data.AppDatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val repository = withContext(Dispatchers.IO) {
                AppDatabaseProvider.repository(applicationContext).also { it.initialize() }
            }
            setContent {
                WriteRightTheme {
                    Surface { WriteRightApp(repository) }
                }
            }
        }
    }
}

@Composable
private fun WriteRightApp(repository: com.writeright.ai.data.repository.CourseRepository) {
    val navController = rememberNavController()
    AppNavHost(navController = navController, repository = repository)
}
