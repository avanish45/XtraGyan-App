package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.XtraGyanApp
import com.example.ui.XtraGyanViewModel
import com.example.ui.XtraGyanViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Database & Repository
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(database.appDao())

        // Instantiate ViewModel
        val factory = XtraGyanViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[XtraGyanViewModel::class.java]

        setContent {
            MyApplicationTheme {
                XtraGyanApp(viewModel = viewModel)
            }
        }
    }
}
