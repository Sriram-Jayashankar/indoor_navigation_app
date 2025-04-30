package com.example.navitest.userinterface

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
)  {
    var showRenameDialogFor by remember { mutableStateOf<File?>(null) }
    var newNameInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val savedFiles = remember {
        context.filesDir
            .listFiles { file -> file.name.startsWith("floorplan_") && file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .systemBarsPadding()) {

        Text("Saved Floor Plans", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(savedFiles) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { /* Load logic to be added */ },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium)
                        Text("Saved: ${formatTimestamp(file.lastModified())}", style = MaterialTheme.typography.bodySmall)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                shareFile(context, file)
                            }) {
                                Text("Share")
                            }
                            OutlinedButton(onClick = {
                                showRenameDialogFor = file
                            }) {
                                Text("Rename")
                            }
                            Button(onClick = {
                                performLogicAndNavigate(context, file, navController, viewModel)
                            }) {
                                Text("Open")
                            }
                        }
                    }
                }
            }
        }
    }
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
                    label = { Text("New Filename (no extension)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val original = showRenameDialogFor!!
                    val safeName = newNameInput.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val newFile = File(context.filesDir, "floorplan_${safeName}.json")

                    if (original != newFile) {
                        original.copyTo(newFile, overwrite = true)
                    }

                    showRenameDialogFor = null
                    newNameInput = ""
                }) {
                    Text("Save As")
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

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun shareFile(context: android.content.Context, file: File) {
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
    navController.navigate(Screen.UserLocation.route)
}

