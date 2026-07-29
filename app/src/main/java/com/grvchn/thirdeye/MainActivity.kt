package com.grvchn.thirdeye

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val colorScheme = if (isSystemInDarkTheme()) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // State to hold the cropped image bitmap
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val yoloManager = remember { YoloManager(context) }
    var detectionSummary by remember { mutableStateOf<String?>(null) }

    val cameraManager = rememberCameraManager(onPhotoTaken = {
        imageUri = it
        croppedBitmap = null
        detectionSummary = null
    })
    val mediaPickerManager = rememberMediaPickerManager(onImagePicked = {
        imageUri = it
        croppedBitmap = null
        detectionSummary = null
    })

    Column(modifier = Modifier.padding(16.dp)) {
        // Display either the cropped bitmap if available, otherwise the original image URI
        AsyncImage(
            model = croppedBitmap ?: imageUri,
            contentDescription = "Selected or Cropped photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (imageUri != null) {
            Button(
                onClick = {
                    val bitmap = getBitmapFromUri(context, imageUri!!)
                    if (bitmap != null) {
                        // 1. Run detection to get coordinates
                        val bestMatch = yoloManager.detect(bitmap)
                        
                        if (bestMatch != null) {
                            // 2. Crop the image based on detection coordinates
                            croppedBitmap = yoloManager.cropDetection(bitmap, bestMatch)
                            detectionSummary = "Class: ${bestMatch.classId}, Confidence: ${bestMatch.confidence}"
                        } else {
                            detectionSummary = "No objects detected."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Detect & Crop Object")
            }
        }

        if (detectionSummary != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = detectionSummary!!, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { cameraManager.takePhoto() }) {
                Text("Click Photo")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { mediaPickerManager.pickImage() }) {
                Text("Select Photo")
            }
        }
    }
}

// Helper function to safely convert a content Uri to a Bitmap
private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}