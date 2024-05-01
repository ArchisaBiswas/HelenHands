import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.ml.AutoModel1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.Locale
import kotlin.concurrent.fixedRateTimer

class VirtualWhiteCaneModule : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var labels: List<String>

    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )

    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model: AutoModel1

    lateinit var textToSpeech: TextToSpeech
    lateinit var vibrator: Vibrator
    var lastDistance = Float.MAX_VALUE

    // Variables to store information about the closest object
    var closestObjectName = ""
    var closestObjectArea = 0.0f // Float.MAX_VALUE
    var closestObjectDirection = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.virtual_white_cane_module_earlier)

        get_permission()

        labels = FileUtil.loadLabels(this, "labels.txt")

        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

        model = AutoModel1.newInstance(this)

        var handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)

        textureView = findViewById(R.id.textureViewLetsChat)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)

                val h = mutable.height
                val w = mutable.width

                paint.textSize = h / 15f
                paint.strokeWidth = h / 85f

                var x = 0

                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4

                    if (fl > 0.5) {
                        val labelIndex = classes.getOrNull(index)?.toInt()
                        if (labelIndex != null && labelIndex < labels.size) {
                            val currentLabel = labels[labelIndex]
                            paint.setColor(colors.get(index))
                            paint.style = Paint.Style.STROKE

                            val boundingBox = RectF(
                                locations.get(x + 1) * w,
                                locations.get(x) * h,
                                locations.get(x + 3) * w,
                                locations.get(x + 2) * h
                            )

                            canvas.drawRect(boundingBox, paint)

                            paint.style = Paint.Style.FILL

                            canvas.drawText(
                                currentLabel + " " + fl.toString(),
                                boundingBox.centerX(),
                                boundingBox.top,
                                paint
                            )

                            // Calculate area of the bounding box
                            val area = (boundingBox.right - boundingBox.left) * (boundingBox.bottom - boundingBox.top)

                            Log.d("CHECKING", "Area: " + area.toString() + "; ClosestObjectArea: " + closestObjectArea.toString())

                            //if (fl > 0.5 && area > closestObjectArea) {
                            if (area > closestObjectArea) {
                                closestObjectArea = area
                                closestObjectName = currentLabel
                                closestObjectDirection = if (lastDistance < closestObjectArea) "closer" else "further away"

                                // Trigger vibration with increasing frequency
                                val vibrationEffect = VibrationEffect.createWaveform(
                                    longArrayOf(0, closestObjectArea.toLong(), 100, 100),
                                    -1
                                )
                                vibrator.vibrate(vibrationEffect)

                                // Speak the information
                                val message =
                                    "$closestObjectName is at $closestObjectArea meters on your $closestObjectDirection"
                                speakOut(message)

                                Log.d("CHECKING", "ClosestObjectName: " + closestObjectName)

                            }
                        }
                    }
                }

                /*scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4

                    if (fl > 0.5) {
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE

                        val boundingBox = RectF(
                            locations.get(x + 1) * w,
                            locations.get(x) * h,
                            locations.get(x + 3) * w,
                            locations.get(x + 2) * h
                        )

                        canvas.drawRect(boundingBox, paint)

                        /*canvas.drawRect(
                            RectF(
                                locations.get(x + 1) * w,
                                locations.get(x) * h,
                                locations.get(x + 3) * w,
                                locations.get(x + 2) * h
                            ), paint
                        )*/
                        paint.style = Paint.Style.FILL

                        canvas.drawText(
                            labels.get(classes.get(index).toInt()) + " " + fl.toString(),
                            boundingBox.centerX(),
                            boundingBox.top,
                            paint
                        )

                        /*canvas.drawText(
                            labels.get(classes.get(index).toInt()) + " " + fl.toString(),
                            locations.get(x + 1) * w,
                            locations.get(x) * h,
                            paint
                        )*/

                        // Calculate area of the bounding box
                        val area = (boundingBox.right - boundingBox.left) * (boundingBox.bottom - boundingBox.top)

                        if (fl > 0.5 && area > closestObjectArea) {
                            closestObjectArea = area
                            closestObjectName = labels.get(classes.get(index).toInt())
                            closestObjectDirection = if (lastDistance < closestObjectArea) "closer" else "further away"
                        }

                    }

                    /*if (fl > 0.5 && locations.get(x + 3) * w - locations.get(x + 1) * w < closestObjectDistance) {
                        closestObjectDistance = locations.get(x + 3) * w - locations.get(x + 1) * w
                        closestObjectName = labels.get(classes.get(index).toInt())
                        closestObjectDirection =
                            if (lastDistance < closestObjectDistance) "closer" else "further away"
                    }*/
                }*/

                imageView.setImageBitmap(mutable)

                Log.d("CHECKING", closestObjectName)

                // Use the closestObjectArea in the condition
                //if (closestObjectArea > 0 && closestObjectArea > lastDistance) {

                Log.d("CHECKING", "I am inside Closest-Object-Has-Been-Found condition yes place")

                /*// Trigger vibration with increasing frequency
                val vibrationEffect = createWaveform(
                    longArrayOf(0, closestObjectArea.toLong(), 100, 100),
                    -1
                )
                vibrator.vibrate(vibrationEffect)

                // Speak the information
                val message =
                    "$closestObjectName is at $closestObjectArea meters on your $closestObjectDirection"
                speakOut(message)*/

                lastDistance = closestObjectArea
                //}

                /*if (closestObjectDistance < 30 && closestObjectDistance < lastDistance) {
                    // Trigger vibration with increasing frequency
                    val vibrationEffect = createWaveform(
                        longArrayOf(0, closestObjectDistance.toLong(), 100, 100),
                        -1
                    )
                    vibrator.vibrate(vibrationEffect)

                    // Speak the information
                    val message =
                        "$closestObjectName is at $closestObjectDistance meters on your $closestObjectDirection"
                    speakOut(message)

                    lastDistance = closestObjectDistance
                }*/
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        textToSpeech = TextToSpeech(this, this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Continuous Object Detection using a fixed-rate timer
        fixedRateTimer("objectDetector", true, 0, 1000) {
            // You can adjust the interval (1000 milliseconds in this example) based on your needs
            textureView.post {
                textureView.invalidate()
            }
        }
    }

    private fun speakOut(message: String) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Text-to-speech language not supported.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Text-to-speech initialization failed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()

        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    @SuppressLint("MissingPermission")
    fun open_camera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        val captureRequestBuilder =
                            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequestBuilder.addTarget(surface)

                        session.setRepeatingRequest(
                            captureRequestBuilder.build(),
                            null,
                            handler
                        )
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {}

            override fun onError(p0: CameraDevice, p1: Int) {}
        }, handler)
    }

    fun get_permission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            get_permission()
        }
    }
}