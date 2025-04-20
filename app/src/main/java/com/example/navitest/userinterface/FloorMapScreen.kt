package com.example.navitest.userinterface

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.navigation.Screen
import com.yalantis.ucrop.UCrop
import java.io.File

@Composable
fun FloorMapScreen(
    navController: NavHostController,
    viewModel: NavitestViewModel = viewModel()
) {
    val context = LocalContext.current

    var croppedImageUri by remember { mutableStateOf<Uri?>(null) }
    var widthInput by remember { mutableStateOf(viewModel.floorWidthMeters.value.takeIf { it > 0 }?.toString() ?: "") }
    var heightInput by remember { mutableStateOf(viewModel.floorHeightMeters.value.takeIf { it > 0 }?.toString() ?: "") }

    // 👉 Crop result handler
    val cropResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(it.data!!)
                croppedImageUri = resultUri
                viewModel.floorMapUri.value = resultUri
            }
        }
    )

    // 👉 Pick image, then crop using entered dimensions
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { sourceUri ->
            if (sourceUri != null && widthInput.toFloatOrNull() != null && heightInput.toFloatOrNull() != null) {
                val width = widthInput.toFloat()
                val height = heightInput.toFloat()

                val destFile = File(context.filesDir, "floorplan.png") // ✅ permanent storage
                val destUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destFile
                )

                val uCrop = UCrop.of(sourceUri, destUri)
                    .withAspectRatio(width, height) // 👈 Enforce real-world ratio
                    .withMaxResultSize(1000, 1000)

                cropResultLauncher.launch(uCrop.getIntent(context))
            }
        }
    )

    // UI layout
    Column(Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()) {
        Text("📏 Enter Real-world Dimensions", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = widthInput,
            onValueChange = { widthInput = it },
            label = { Text("Width (m)") },
            modifier = Modifier.fillMaxWidth(),
            isError = widthInput.toFloatOrNull() == null || widthInput.toFloat() <= 0
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = heightInput,
            onValueChange = { heightInput = it },
            label = { Text("Height (m)") },
            modifier = Modifier.fillMaxWidth(),
            isError = heightInput.toFloatOrNull() == null || heightInput.toFloat() <= 0
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (widthInput.toFloatOrNull() != null && heightInput.toFloatOrNull() != null) {
                    pickImageLauncher.launch("image/*")
                }
            },
            enabled = widthInput.toFloatOrNull() != null && widthInput.toFloat() > 0 &&
                    heightInput.toFloatOrNull() != null && heightInput.toFloat() > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select and Crop Image")
        }

        croppedImageUri?.let { uri ->
            Spacer(Modifier.height(16.dp))
            val bitmap = remember(uri) {
                val input = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(input)
            }
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Floor Plan",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.floorMapUri.value = croppedImageUri
                viewModel.floorWidthMeters.value = widthInput.toFloat()
                viewModel.floorHeightMeters.value = heightInput.toFloat()
                val prefs = context.getSharedPreferences("navitest_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("hasPreviousImage", true)
                    putString("imagePath", "floorplan.png")
                    putFloat("widthMeters", widthInput.toFloat())
                    putFloat("heightMeters", heightInput.toFloat())
                    apply()
                }
                navController.navigate(Screen.Trilateration.route) // or PathGraphEditor if defined
            },
            enabled = croppedImageUri != null &&
                    widthInput.toFloatOrNull() != null && widthInput.toFloat() > 0 &&
                    heightInput.toFloatOrNull() != null && heightInput.toFloat() > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next: Create Path Graph")
        }
    }
}
