package com.writeright.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.writeright.ai.core.design.WriteRightTheme
import com.writeright.ai.core.navigation.AppNavHost
import com.writeright.ai.data.AppDatabaseProvider
import com.writeright.ai.data.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var repository by mutableStateOf<CourseRepository?>(null)
    private var startupError by mutableStateOf<String?>(null)
    private var isInitializing by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Render immediately so a database/seed failure cannot leave the Activity without UI.
        setContent {
            WriteRightTheme {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    val readyRepository = repository
                    when {
                        readyRepository != null -> WriteRightApp(readyRepository)
                        startupError != null -> StartupErrorScreen(
                            message = startupError.orEmpty(),
                            onRetry = { initializeDatabase() }
                        )
                        isInitializing -> LoadingScreen()
                    }
                }
            }
        }

        initializeDatabase()
    }

    private fun initializeDatabase() {
        if (isInitializing && repository != null) return
        isInitializing = true
        startupError = null

        lifecycleScope.launch {
            try {
                val initializedRepository = withContext(Dispatchers.IO) {
                    AppDatabaseProvider.repository(applicationContext).also { it.initialize() }
                }
                repository = initializedRepository
            } catch (error: Exception) {
                startupError = error.localizedMessage ?: error.javaClass.simpleName
            } finally {
                isInitializing = false
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WriteRightApp(repository: CourseRepository) {
    val navController = rememberNavController()
    AppNavHost(navController = navController, repository = repository)
}

@androidx.compose.runtime.Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text("Preparing your lessons...", modifier = Modifier.padding(top = 16.dp))
    }
}

@androidx.compose.runtime.Composable
private fun StartupErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("WriteRight couldn't prepare the course", style = MaterialTheme.typography.titleLarge)
        Text(message, modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = onRetry) { Text("Try again") }
    }
}
