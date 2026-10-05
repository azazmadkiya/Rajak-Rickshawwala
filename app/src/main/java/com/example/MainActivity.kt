package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.RajakRickshawwalaTheme
import com.example.viewmodel.RickshawViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: RickshawViewModel = viewModel()
      val isDark by viewModel.isDarkMode.collectAsState()
      val isLoggedIn by viewModel.isLoggedIn.collectAsState()
      val isAdmin by viewModel.isAdmin.collectAsState()
      val navController = rememberNavController()
      var showSplash by remember { mutableStateOf(true) }

      RajakRickshawwalaTheme(darkTheme = isDark) {
        if (showSplash) {
          SplashScreen(onSplashFinished = { showSplash = false })
        } else if (!isLoggedIn) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            ProfileScreen(
              viewModel = viewModel,
              onNavigateToAdmin = {
                navController.navigate("admin")
              }
            )
          }
        } else {
          val navBackStackEntry by navController.currentBackStackEntryAsState()
          val currentRoute = navBackStackEntry?.destination?.route ?: "home"

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
              NavigationBar {
                NavigationBarItem(
                  icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                  label = { Text("Home") },
                  selected = currentRoute == "home",
                  onClick = {
                    navController.navigate("home") {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                )
                NavigationBarItem(
                  icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Pay") },
                  label = { Text("UPI Pay") },
                  selected = currentRoute == "payment",
                  onClick = {
                    navController.navigate("payment") {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                )
                NavigationBarItem(
                  icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                  label = { Text("Profile") },
                  selected = currentRoute == "profile",
                  onClick = {
                    navController.navigate("profile") {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                )
                if (isAdmin) {
                  NavigationBarItem(
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                    label = { Text("Admin") },
                    selected = currentRoute == "admin",
                    onClick = {
                      navController.navigate("admin") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  )
                }
              }
            }
          ) { innerPadding ->
            NavHost(
              navController = navController,
              startDestination = "home",
              modifier = Modifier.padding(innerPadding)
            ) {
              composable("home") {
                HomeScreen(
                  viewModel = viewModel,
                  onNavigateToPayment = { navController.navigate("payment") },
                  onNavigateToAdmin = { navController.navigate("admin") }
                )
              }
              composable("payment") {
                PaymentScreen(viewModel = viewModel)
              }
              composable("profile") {
                ProfileScreen(
                  viewModel = viewModel,
                  onNavigateToAdmin = { navController.navigate("admin") }
                )
              }
              composable("admin") {
                AdminScreen(viewModel = viewModel)
              }
            }
          }
        }
      }
    }
  }
}
