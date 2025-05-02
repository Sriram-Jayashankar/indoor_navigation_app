package com.example.navitest.userinterface

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.navitest.NavitestViewModel
import com.example.navitest.model.Router
import com.example.navitest.pipeline.PipelineExecutor
import com.example.navitest.pipeline.StepType
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ExecutionScreen(
    file: File,
    navController: NavHostController,
    viewModel: NavitestViewModel
) {
    val context = LocalContext.current

    val jsonString = remember { file.readText() }
    val jsonObject = remember { JSONObject(jsonString) }

    val base64Image = jsonObject.getString("imageBase64")
    val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

    val routers = remember {
        val arr = jsonObject.getJSONArray("routers")
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    Router(
                        obj.getInt("id"),
                        obj.getDouble("x").toFloat(),
                        obj.getDouble("y").toFloat(),
                        obj.getString("ssid")
                    )
                )
            }
        }
    }

    var useFilter by remember { mutableStateOf(false) }
    var useCentroid by remember { mutableStateOf(false) }
    var userPos by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(useFilter, useCentroid) {
        while (true) {
            val steps = listOf(
                StepType.GetRssi,
                if (useFilter) StepType.Kalman1D else null,
                if (useCentroid) StepType.CentroidTriangulation else StepType.NormalTriangulation
            ).filterNotNull()

            val result = PipelineExecutor.runPipeline(context, routers, steps)
            if (result == Offset.Zero) {
                Toast.makeText(context, "Could not retrieve any router RSSI values", Toast.LENGTH_SHORT).show()
            } else {
                userPos = result
            }
            delay(2000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).systemBarsPadding()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Filter")
            Switch(checked = useFilter, onCheckedChange = { useFilter = it })
            Text(if (useFilter) "Kalman 1D" else "No Filter")
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Triangulation")
            RadioButton(selected = !useCentroid, onClick = { useCentroid = false })
            Text("Normal")
            RadioButton(selected = useCentroid, onClick = { useCentroid = true })
            Text("Centroid")
        }

        Spacer(Modifier.height(16.dp))

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(bitmap.asImageBitmap())

                userPos?.let {
                    drawCircle(Color.Magenta, radius = 12f, center = it)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = { navController.popBackStack() }, Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back")
        }
    }
}
