package fewshot.android

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

class CameraManager(
    private val context: Context,
    private val launcher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val tempUriState: MutableState<Uri?>
) {
    fun takePhoto() {
        val imgDir = File(context.cacheDir, "images")
        if (!imgDir.exists()) {
            imgDir.mkdirs()
        }

        val tempFile = File.createTempFile("photo_", ".jpg", imgDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )

        tempUriState.value = uri
        launcher.launch(uri)
    }
}

@Composable
fun rememberCameraManager(onPhotoTaken: (Uri) -> Unit): CameraManager {
    val context = LocalContext.current
    val tempUri = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempUri.value?.let { onPhotoTaken(it) }
        }
    }

    return remember(context, launcher) {
        CameraManager(context, launcher, tempUri)
    }
}
