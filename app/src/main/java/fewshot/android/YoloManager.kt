package fewshot.android

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class Detection(
    val classId: Int,
    val confidence: Float,
    val xCenter: Float,
    val yCenter: Float,
    val width: Float,
    val height: Float
)

class YoloManager(context: Context) {

    private val interpreter: Interpreter = context.assets.openFd("yolo26n.tflite").let { afd ->
        FileInputStream(afd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength
        ).let(::Interpreter)
    }

    fun detect(bitmap: Bitmap): Detection? {
        val resizedBitmap = bitmap.scale(640, 640)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
        val outputBuffer = Array(1) { Array(84) { FloatArray(8400) } }

        interpreter.run(inputBuffer, outputBuffer)

        val outputs = outputBuffer[0]
        val rows = outputs.size
        val cols = outputs[0].size

        var maxConfidence = 0f
        var bestCol = -1
        var bestClassId = -1

        for (col in 0 until cols) {
            var maxClassScore = 0f
            var classId = -1

            for (row in 4 until rows) {
                val score = outputs[row][col]
                if (score > maxClassScore) {
                    maxClassScore = score
                    classId = row - 4
                }
            }

            if (maxClassScore > maxConfidence) {
                maxConfidence = maxClassScore
                bestCol = col
                bestClassId = classId
            }
        }

        if (bestCol == -1 || maxConfidence == 0f) return null

        return Detection(
            classId = bestClassId,
            confidence = maxConfidence,
            xCenter = outputs[0][bestCol],
            yCenter = outputs[1][bestCol],
            width = outputs[2][bestCol],
            height = outputs[3][bestCol]
        )
    }

    fun cropDetection(bitmap: Bitmap, detection: Detection): Bitmap? {
        val resizedBitmap = bitmap.scale(640, 640)

        // 1. Check if coordinates are normalized (0.0 to 1.0) and scale to 640x640 pixels if needed
        val isNormalized = detection.xCenter <= 1.0f && detection.width <= 1.0f

        val xCenter = if (isNormalized) detection.xCenter * 640f else detection.xCenter
        val yCenter = if (isNormalized) detection.yCenter * 640f else detection.yCenter
        val widthPx = if (isNormalized) detection.width * 640f else detection.width
        val heightPx = if (isNormalized) detection.height * 640f else detection.height

        // 2. Convert center/dimensions to absolute boundary coordinates
        val left = (xCenter - (widthPx / 2f)).coerceIn(0f, 640f)
        val top = (yCenter - (heightPx / 2f)).coerceIn(0f, 640f)
        val right = (xCenter + (widthPx / 2f)).coerceIn(0f, 640f)
        val bottom = (yCenter + (heightPx / 2f)).coerceIn(0f, 640f)

        val width = (right - left).toInt()
        val height = (bottom - top).toInt()

        if (width <= 0 || height <= 0) return null

        // 3. Ensure safe integer bounds to prevent out-of-bounds crashes
        val safeLeft = left.toInt().coerceIn(0, 639)
        val safeTop = top.toInt().coerceIn(0, 639)
        val safeWidth = width.coerceIn(1, 640 - safeLeft)
        val safeHeight = height.coerceIn(1, 640 - safeTop)

        // 4. Create and return the cropped bitmap
        return Bitmap.createBitmap(
            resizedBitmap,
            safeLeft,
            safeTop,
            safeWidth,
            safeHeight
        )
    }
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 3 * 640 * 640 * 4) // 4 bytes per float
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(640 * 640)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val floatValues = FloatArray(3 * 640 * 640)
        for (i in intValues.indices) {
            val valPixel = intValues[i]
            // Normalize RGB values to [0.0, 1.0] and arrange in NCHW order
            floatValues[i] = ((valPixel shr 16) and 0xFF) / 255.0f
            floatValues[640 * 640 + i] = ((valPixel shr 8) and 0xFF) / 255.0f
            floatValues[2 * 640 * 640 + i] = (valPixel and 0xFF) / 255.0f
        }

        for (v in floatValues) {
            byteBuffer.putFloat(v)
        }
        return byteBuffer
    }
}
