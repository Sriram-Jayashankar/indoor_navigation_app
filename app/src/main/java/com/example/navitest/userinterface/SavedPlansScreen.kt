package com.example.navitest.userinterface

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Router
import com.example.navitest.navigation.Screen
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavedPlansScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val context = LocalContext.current

    // 1️⃣ Load saved files into a mutable state list so we can update it
    val savedFiles = remember {
        mutableStateListOf<File>().apply {
            addAll(loadFloorplanFiles(context))
        }
    }

    var showRenameDialogFor by remember { mutableStateOf<File?>(null) }
    var newNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        Text(
            text = "Saved Floor Plans",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(savedFiles) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium)
                        Text("Saved: ${formatTimestamp(file.lastModified())}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))

                        // Row with Share / Rename / Delete
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { shareFile(context, file) }) {
                                Text("Share")
                            }

                            OutlinedButton(onClick = {
                                showRenameDialogFor = file
                                newNameInput = file.name.removePrefix("floorplan_").removeSuffix(".json")
                            }) {
                                Text("Rename")
                            }

                            OutlinedButton(onClick = {
                                if (file.delete()) {
                                    savedFiles.remove(file)
                                    Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to delete ${file.name}", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Delete")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // New row with Open + Preview
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                performLogicAndNavigate(context, file, navController, viewModel)
                            }) {
                                Text("Open")
                            }

                            OutlinedButton(onClick = {
                                navController.navigate(Screen.Preview.createRoute(file.name))

                            }) {
                                Text("Preview")
                            }
                        }
                    }
                }
            }
        }

    }

    // 2️⃣ Rename dialog
    if (showRenameDialogFor != null) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialogFor = null
                newNameInput = ""
            },
            title = { Text("Rename Floor Plan") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("New Name (no extension)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val original = showRenameDialogFor!!
                    val safeBase = newNameInput
                        .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        .trim()
                    if (safeBase.isBlank()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val newFile = File(context.filesDir, "floorplan_${safeBase}.json")
                    when {
                        newFile.exists() -> {
                            Toast.makeText(
                                context,
                                "A floorplan with that name already exists",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        original.renameTo(newFile) -> {
                            // Update list
                            savedFiles.remove(original)
                            savedFiles.add(newFile)
                            savedFiles.sortByDescending { it.lastModified() }
                            Toast.makeText(
                                context,
                                "Renamed to ${newFile.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showRenameDialogFor = null
                    newNameInput = ""
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialogFor = null
                    newNameInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper: load JSON floorplan files
fun loadFloorplanFiles(context: Context): List<File> =
    context.filesDir
        .listFiles { f -> f.name.startsWith("floorplan_") && f.name.endsWith(".json") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

// Format timestamp for display
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Share via FileProvider
fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Floor Plan"))
}

// Reuse your existing function
// fun performLogicAndNavigate( ... ) { ... }

fun performLogicAndNavigate(
    context: Context,
    file: File,
    navController: NavHostController,
    viewModel: NavitestViewModel
) {
    val jsonString = file.readText()
    val jsonObject = JSONObject(jsonString)

    val width = jsonObject.getDouble("widthMeters").toFloat()
    val height = jsonObject.getDouble("heightMeters").toFloat()
    val routersJsonArray = jsonObject.getJSONArray("routers")
    val ssids = mutableListOf<String>()
    val routers = mutableListOf<Router>()

    for (i in 0 until routersJsonArray.length()) {
        val routerObject = routersJsonArray.getJSONObject(i)
        val id = routerObject.getInt("id")
        val x = routerObject.getDouble("x").toFloat()
        val y = routerObject.getDouble("y").toFloat()
        val ssid = routerObject.getString("ssid")
        routers.add(Router(id, x, y, ssid))
        ssids.add(ssid)
    }

    // Store width, height and routers in ViewModel
    viewModel.floorWidthMeters.value = width
    viewModel.floorHeightMeters.value = height
    viewModel.routers.clear()
    viewModel.routers.addAll(routers)

    val rssiMap = com.example.navitest.wifi.WifiChecker.getRssiReadings(context, ssids)

    if (rssiMap.size < 1) {
        val foundList = rssiMap.keys
        val notFoundList = ssids.filterNot { it in foundList }

        val message = buildString {
            append("❌ Only ${rssiMap.size} / ${ssids.size} routers found.\n\n")
            append("✅ Found:\n")
            foundList.forEach { append("- $it\n") }
            append("❌ Missing:\n")
            notFoundList.forEach { append("- $it\n") }
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        return
    }

    // Save selected floorplan path
    val prefs = context.getSharedPreferences("navitest_prefs", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putBoolean("hasPreviousImage", true)
        putString("imagePath", file.name.replace(".json", ".png"))
        putFloat("widthMeters", width)
        putFloat("heightMeters", height)
        apply()
    }

    // ✅ Navigate to UserLocationScreen
    //navController.navigate(Screen.UserLocation.route)
}

