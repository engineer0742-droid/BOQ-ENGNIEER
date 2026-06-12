package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.SmartQuantityEstimatorTheme
import androidx.lifecycle.ViewModelProvider
import com.example.ui.viewmodel.QuantityViewModel
import com.example.ui.viewmodel.QuantityViewModelFactory
import com.example.data.local.AppDatabase
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Google Mobile Ads SDK
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            QuantityViewModelFactory(database.projectDao())
        )[QuantityViewModel::class.java]
        
        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            
            SmartQuantityEstimatorTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}
