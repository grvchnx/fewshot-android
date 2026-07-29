package com.grvchn.thirdeye

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class MediaPickerManager(
    val launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>
) {
    fun pickImage() {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

@Composable
fun rememberMediaPickerManager(onImagePicked: (Uri) -> Unit): MediaPickerManager {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImagePicked(it) }
    }

    return remember(launcher) {
        MediaPickerManager(launcher)
    }
}