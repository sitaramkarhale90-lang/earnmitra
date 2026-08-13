package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.EarnMitraRepository
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.EarnMitraTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.EarnMitraViewModelFactory
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var repository: EarnMitraRepository
    private lateinit var authViewModel: AuthViewModel
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = EarnMitraRepository(applicationContext)
        val factory = EarnMitraViewModelFactory(repository)

        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            EarnMitraTheme {
                MainAppNavigation(
                    authViewModel = authViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}
