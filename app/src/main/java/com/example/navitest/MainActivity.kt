package com.example.navitest

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.navitest.navigation.NavGraph
import com.example.navitest.navigation.Screen
import com.example.navitest.userinterface.theme.NavitestTheme
import java.io.File
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.navitest.utils.LocationPermissionHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load preferences
        val prefs = getSharedPreferences("navitest_prefs", Context.MODE_PRIVATE)
        val hasPreviousImage = prefs.getBoolean("hasPreviousImage", false)
        val width = prefs.getFloat("widthMeters", 0f)
        val height = prefs.getFloat("heightMeters", 0f)
        val imagePath = prefs.getString("imagePath", null)
        val imageFile = imagePath?.let { File(filesDir, it) }

        // Check if we have the needed location permissons before UI render
        if (!LocationPermissionHelper.hasLocationPermission(this)) {
            LocationPermissionHelper.requestLocationPermission(this)
        }

        // Create content
        setContent {
            val navController = rememberNavController()
            val viewModel: NavitestViewModel = viewModel()
            var showDialog by remember { mutableStateOf(hasPreviousImage && imageFile?.exists() == true) }

            NavitestTheme(useDarkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) {

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Resume previous session?") },
                            text = { Text("Do you want to continue with your last floor plan?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    val uri = FileProvider.getUriForFile(
                                        this@MainActivity,
                                        "${packageName}.provider",
                                        imageFile!!
                                    )
                                    viewModel.floorMapUri.value = uri
                                    viewModel.floorWidthMeters.value = width
                                    viewModel.floorHeightMeters.value = height
                                    showDialog = false
                                    navController.navigate(Screen.PathGraphEditor.route)
                                }) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("No")
                                }
                            }
                        )
                    }

                    NavGraph(navController = navController)
                }
            }
        }
    }
}
