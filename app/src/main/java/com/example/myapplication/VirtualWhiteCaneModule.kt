package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.TextToSpeech.SUCCESS
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.ml.AutoModel1
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.*
import kotlin.concurrent.fixedRateTimer

class VirtualWhiteCaneModule : AppCompatActivity(), OnInitListener {

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

    // Variables to store information about the previous object
    var previousObjectName = ""
    var previousObjectDistance = Float.MAX_VALUE

    var isAudioPlaying = false // Flag to track if audio feedback is currently playing

//    private lateinit var waterHandler: Handler
//    private lateinit var breathHandler: Handler
//    //    private lateinit var startButton: Button
//    private var waterReminderActive = false
//    private var breathReminderActive = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.virtual_white_cane)

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

                Log.d("CHECK", isAudioPlaying.toString())

//                if (::textureView.isInitialized && !isAudioPlaying) {  // Check if audio is not currently playing

                if (!isAudioPlaying) { // Check if audio is not currently playing

                    detectObjects()

                }

//                if (::textureView.isInitialized && !isAudioPlaying) {  // Check if audio is not currently playing
//                    // Existing object detection logic
//                    bitmap = textureView.bitmap!!
//                    var image = TensorImage.fromBitmap(bitmap)
//                    image = imageProcessor.process(image)
//
//                    val outputs = model.process(image)
//                    val locations = outputs.locationsAsTensorBuffer.floatArray
//                    val classes = outputs.classesAsTensorBuffer.floatArray
//                    val scores = outputs.scoresAsTensorBuffer.floatArray
//                    val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray
//
//                    var maxArea = Float.MIN_VALUE
//                    var maxIndex = -1
//                    var objectDetected = false // Flag to track if the largest object has been detected
//
//                    scores.forEachIndexed { index, fl ->
//                        val x = index * 4
//
//                        if (fl > 0.5 && !objectDetected) {
//                            val boundingBox = RectF(
//                                locations[x + 1] * bitmap.width,
//                                locations[x] * bitmap.height,
//                                locations[x + 3] * bitmap.width,
//                                locations[x + 2] * bitmap.height
//                            )
//
//                            val area = (boundingBox.right - boundingBox.left) * (boundingBox.bottom - boundingBox.top)
//
//                            if (area > maxArea) {
//                                maxArea = area
//                                maxIndex = index
//                            }
//                        }
//                    }
//
//                    if (maxIndex != -1) {
//                        val boundingBox = RectF(
//                            locations[maxIndex * 4 + 1] * bitmap.width,
//                            locations[maxIndex * 4] * bitmap.height,
//                            locations[maxIndex * 4 + 3] * bitmap.width,
//                            locations[maxIndex * 4 + 2] * bitmap.height
//                        )
//
//                        val message = "${labels[classes[maxIndex].toInt()]} is at $maxArea meters"
//                        speakOut(message)
//
////                    val vibrationEffect = createWaveform(longArrayOf(0, maxArea.toLong(), 100, 100), -1)
////                    vibrator.vibrate(vibrationEffect)
//
//                        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//                        val canvas = Canvas(mutable)
//                        paint.style = Paint.Style.STROKE
//                        paint.color = colors[maxIndex]
//                        paint.strokeWidth = mutable.height / 85f
//                        canvas.drawRect(boundingBox, paint)
//
//                        imageView.setImageBitmap(mutable)
//
//                        objectDetected = true // Set the flag to true after detecting the largest object
//                    }
//                }
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        textToSpeech = TextToSpeech(this, this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

//        // Continuous Object Detection using a fixed-rate timer
//        fixedRateTimer("objectDetector", true, 0, 1000) {
//            // You can adjust the interval (1000 milliseconds in this example) based on your needs
//            textureView.post {
//                textureView.invalidate()
//            }
//        }
        fixedRateTimer("objectDetector", true, 0, 1000) {
            if (::textureView.isInitialized) { // Check if textureView is initialized
                textureView.post {
                    if (!isAudioPlaying) { // Check if audio is not currently playing
//                        textureView.invalidate()
                        detectObjects()
                    }
                }
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setSelectedItemId(R.id.Virtualwhitecane_nav)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Virtualwhitecane_nav -> true
                R.id.Home_nav -> {
                    startActivity(Intent(applicationContext, Dashboard::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.LetsChat_nav -> {
                    startActivity(Intent(applicationContext, LetsChatModule::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.sos_nav -> {
                    startActivity(Intent(applicationContext, SOSReachModule::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.wellness_nav -> {
//                    startActivity(Intent(applicationContext, SOSReachModule::class.java))

                    val intent = Intent(this, Dashboard::class.java)
                    intent.putExtra("fromForWellnessAwareEnabler", true) // Pass extra to indicate coming back from Activity2
                    startActivity(intent)

                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

    }

//    private fun speakOut(message: String) {
//        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
//    }

//    private fun speakOut(message: String) {
//        isAudioPlaying = true // Set flag to true before speaking
//        val utteranceId = "UniqueUtteranceId" // Assign a unique utterance ID
//        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
//        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//            override fun onDone(utteranceId: String?) {
//                Log.d("TextToSpeech", "onDone: Audio finished playing")
//                isAudioPlaying = false // Set flag to false after audio finishes playing
//                // Trigger object detection again after the audio finishes playing
//                detectObjects()
//            }
//
//            override fun onError(utteranceId: String?) {
//                Log.e("TextToSpeech", "onError: Error occurred while speaking")
//                isAudioPlaying = false // Set flag to false if there's an error in speaking
//            }
//
//            override fun onStart(utteranceId: String?) {
//                Log.d("TextToSpeech", "onStart: Audio started playing")
//                // Do nothing on start
//            }
//        })
//    }

//    private fun speakOut(message: String, boundingBox: RectF) {
//        val centerX = textureView.width / 2f
//        val objectCenterX = boundingBox.centerX()
//
//        var direction = ""
//        if (objectCenterX < centerX - centerX / 3) {
//            direction = "to your West"
//        } else if (objectCenterX > centerX + centerX / 3) {
//            direction = "to your East"
//        } else {
//            direction = "to your North"
//        }
//
//        val finalMessage = "$message $direction"
//        isAudioPlaying = true // Set flag to true before speaking
//        val utteranceId = "UniqueUtteranceId" // Assign a unique utterance ID
//        textToSpeech.speak(finalMessage, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
//        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//            override fun onDone(utteranceId: String?) {
//                Log.d("TextToSpeech", "onDone: Audio finished playing")
//                isAudioPlaying = false // Set flag to false after audio finishes playing
//                // Trigger object detection again after the audio finishes playing
//                detectObjects()
//            }
//
//            override fun onError(utteranceId: String?) {
//                Log.e("TextToSpeech", "onError: Error occurred while speaking")
//                isAudioPlaying = false // Set flag to false if there's an error in speaking
//            }
//
//            override fun onStart(utteranceId: String?) {
//                Log.d("TextToSpeech", "onStart: Audio started playing")
//                // Do nothing on start
//            }
//        })
//    }

    // Function to calculate the distance of the detected object based on the bounding box dimensions
    private fun calculateDistance(boundingBox: RectF): Float {
        // Here, you can implement your own logic to calculate the distance based on the dimensions of the bounding box
        // This could involve using the known dimensions of the object or employing techniques like triangulation
        // For demonstration purposes, let's assume a simple calculation using the width of the bounding box
        val objectWidth = boundingBox.width() // Width of the detected object
        val focalLength = 100 // Focal length of the camera (example value)
        val distance = focalLength / objectWidth // Simple distance calculation (example logic)
        return distance
    }

//    private fun speakOut(message: String, boundingBox: RectF) {
//        val centerX = textureView.width / 2f
//        val objectCenterX = boundingBox.centerX()
//
//        var direction = ""
//        if (objectCenterX < centerX - centerX / 3) {
//            direction = "to your West"
//        } else if (objectCenterX > centerX + centerX / 3) {
//            direction = "to your East"
//        } else {
//            direction = "to your North"
//        }
//
//        val distance = calculateDistance(boundingBox) // Calculate the distance
//        val distanceString = "%.2f".format(distance) // Format distance to two decimal places
//        val finalMessage = "$message $direction at a distance of $distanceString meters"
//
//        isAudioPlaying = true // Set flag to true before speaking
//        val utteranceId = "UniqueUtteranceId" // Assign a unique utterance ID
//        textToSpeech.speak(finalMessage, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
//        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//            override fun onDone(utteranceId: String?) {
//                Log.d("TextToSpeech", "onDone: Audio finished playing")
//                isAudioPlaying = false // Set flag to false after audio finishes playing
//                // Trigger object detection again after the audio finishes playing
//                detectObjects()
//            }
//
//            override fun onError(utteranceId: String?) {
//                Log.e("TextToSpeech", "onError: Error occurred while speaking")
//                isAudioPlaying = false // Set flag to false if there's an error in speaking
//            }
//
//            override fun onStart(utteranceId: String?) {
//                Log.d("TextToSpeech", "onStart: Audio started playing")
//                // Do nothing on start
//            }
//        })
//    }

    private fun speakOut(message: String, boundingBox: RectF, label: String) {
        val centerX = textureView.width / 2f
        val objectCenterX = boundingBox.centerX()

        var direction = ""
        if (objectCenterX < centerX - centerX / 3) {
            direction = "to your West"
        } else if (objectCenterX > centerX + centerX / 3) {
            direction = "to your East"
        } else {
            direction = "to your North"
        }

        val distance = calculateDistance(boundingBox) // Calculate the distance
        val distanceString = "%.2f".format(distance) // Format distance to two decimal places

        var finalMessage = "$message $direction at a distance of $distanceString meters"

        // Check if the detected object is the same as the previous one and if the current distance is decreasing
        if (label == previousObjectName && distance < previousObjectDistance) {
            finalMessage = "You are getting closer to where the $message"
        }

        // Calculate the vibration pattern based on the distance
        val vibrationPattern = calculateVibrationPattern(distance)
        val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1)
        vibrator.vibrate(vibrationEffect)

        isAudioPlaying = true // Set flag to true before speaking
        val utteranceId = "UniqueUtteranceId" // Assign a unique utterance ID
        textToSpeech.speak(finalMessage, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                Log.d("TextToSpeech", "onDone: Audio finished playing")
                isAudioPlaying = false // Set flag to false after audio finishes playing
                // Trigger object detection again after the audio finishes playing
                detectObjects()
            }

            override fun onError(utteranceId: String?) {
                Log.e("TextToSpeech", "onError: Error occurred while speaking")
                isAudioPlaying = false // Set flag to false if there's an error in speaking
            }

            override fun onStart(utteranceId: String?) {
                Log.d("TextToSpeech", "onStart: Audio started playing")
                // Do nothing on start
            }
        })

        // Update the previous object information
        previousObjectName = boundingBox.toString()
        previousObjectDistance = distance
    }



//    private fun speakOut(message: String, boundingBox: RectF, label: String) {
//        val centerX = textureView.width / 2f
//        val objectCenterX = boundingBox.centerX()
//
//        var direction = ""
//        if (objectCenterX < centerX - centerX / 3) {
//            direction = "to your West"
//        } else if (objectCenterX > centerX + centerX / 3) {
//            direction = "to your East"
//        } else {
//            direction = "to your North"
//        }
//
//        val distance = calculateDistance(boundingBox) // Calculate the distance
//        val distanceString = "%.2f".format(distance) // Format distance to two decimal places
//
//        var finalMessage = "$message $direction at a distance of $distanceString meters"
//
//        // Calculate the vibration pattern based on the distance
//        val vibrationPattern = calculateVibrationPattern(distance)
//        val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1)
//        vibrator.vibrate(vibrationEffect)
//
//        isAudioPlaying = true // Set flag to true before speaking
//        val utteranceId = "UniqueUtteranceId" // Assign a unique utterance ID
//        textToSpeech.speak(finalMessage, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
//        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//            override fun onDone(utteranceId: String?) {
//                Log.d("TextToSpeech", "onDone: Audio finished playing")
//                isAudioPlaying = false // Set flag to false after audio finishes playing
//                // Trigger object detection again after the audio finishes playing
//                detectObjects()
//            }
//
//            override fun onError(utteranceId: String?) {
//                Log.e("TextToSpeech", "onError: Error occurred while speaking")
//                isAudioPlaying = false // Set flag to false if there's an error in speaking
//            }
//
//            override fun onStart(utteranceId: String?) {
//                Log.d("TextToSpeech", "onStart: Audio started playing")
//                // Do nothing on start
//            }
//        })
//
//        // Update the previous object information
//        previousObjectName = label
//        previousObjectDistance = distance
//    }

    // Function to calculate the vibration pattern based on the distance
    private fun calculateVibrationPattern(distance: Float): LongArray {
        // Here, you can implement your own logic to calculate the vibration pattern based on the distance
        // For demonstration purposes, let's assume a simple linear increase in vibration frequency as the distance decreases
        val initialFrequency = 100L // Initial vibration frequency
        val frequencyIncrement = 10L // Frequency increment per meter
        val maxFrequency = 500L // Maximum vibration frequency

        val numSteps = (distance / frequencyIncrement).toInt()
        val pattern = LongArray(numSteps + 1) { initialFrequency + it * frequencyIncrement }
        // Ensure the vibration frequency does not exceed the maximum frequency
        for (i in pattern.indices) {
            if (pattern[i] > maxFrequency) {
                pattern[i] = maxFrequency
            }
        }

        return pattern
    }

    override fun onInit(status: Int) {
        if (status == SUCCESS) {
            Log.d("TextToSpeech", "Text-to-speech initialization successful")
            val result = textToSpeech.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Text-to-speech language not supported.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Text-to-speech initialization failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detectObjects()
    {
        if (::textureView.isInitialized) { // Check if textureView is initialized
            textureView.post {
                // Existing object detection logic
                // Existing object detection logic
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                var maxArea = Float.MIN_VALUE
                var maxIndex = -1
                var objectDetected = false // Flag to track if the largest object has been detected

                scores.forEachIndexed { index, fl ->
                    val x = index * 4

                    if (fl > 0.5 && !objectDetected) {
                        val boundingBox = RectF(
                            locations[x + 1] * bitmap.width,
                            locations[x] * bitmap.height,
                            locations[x + 3] * bitmap.width,
                            locations[x + 2] * bitmap.height
                        )

                        val area = (boundingBox.right - boundingBox.left) * (boundingBox.bottom - boundingBox.top)

                        if (area > maxArea) {
                            maxArea = area
                            maxIndex = index
                        }
                    }
                }

                if (maxIndex != -1) {
                    val boundingBox = RectF(
                        locations[maxIndex * 4 + 1] * bitmap.width,
                        locations[maxIndex * 4] * bitmap.height,
                        locations[maxIndex * 4 + 3] * bitmap.width,
                        locations[maxIndex * 4 + 2] * bitmap.height
                    )

//                    val message = "${labels[classes[maxIndex].toInt()]} is at $maxArea meters"
                    val message = "${labels[classes[maxIndex].toInt()]} is"
                    previousObjectName = message.split(" ")[0]
                    speakOut(message, boundingBox, message.split(" ")[0])

//                    val vibrationEffect = createWaveform(longArrayOf(0, maxArea.toLong(), 100, 100), -1)
//                    vibrator.vibrate(vibrationEffect)

                    val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutable)
                    paint.style = Paint.Style.STROKE
                    paint.color = colors[maxIndex]
                    paint.strokeWidth = mutable.height / 85f
                    canvas.drawRect(boundingBox, paint)

                    imageView.setImageBitmap(mutable)

                    objectDetected = true // Set the flag to true after detecting the largest object
                }
            }
        }
//        if (::textureView.isInitialized && !isAudioPlaying) {  // Check if audio is not currently playing
//        }
    }

//    override fun onResume() {
//        super.onResume()
//        cameraDevice?.close()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        cameraDevice?.close()
//    }

    override fun onPause() {
        super.onPause()
        cameraDevice?.close()
        previousObjectName = ""
        previousObjectDistance = 0.0f
    }

    override fun onStop() {
        super.onStop()
        cameraDevice?.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
        cameraDevice?.close()
        textureView.surfaceTextureListener = null // Release the surface texture listener
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