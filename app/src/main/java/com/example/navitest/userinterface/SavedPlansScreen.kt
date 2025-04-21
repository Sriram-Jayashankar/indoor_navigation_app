package com.example.navitest.userinterface

import android.content.Intent
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
        .padding(16.dp)) {

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
