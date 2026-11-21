package com.writeright.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.writeright.ai.core.design.WriteRightTheme
import com.writeright.ai.core.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WriteRightTheme {
                Surface {
                    WriteRightApp()
                }
            }
        }
    }
}

@Composable
private fun WriteRightApp() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}
