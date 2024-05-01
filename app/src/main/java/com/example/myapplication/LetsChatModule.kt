package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.*
import java.util.concurrent.TimeUnit


interface ApiService {
    @Multipart
    @POST("predict")
    fun predictImage(
        @Part file: MultipartBody.Part
    ): retrofit2.Call<ResponseBody>
}

// Declare a global variable to store the timer
private var apiRequestTimer: Timer? = null

// Global variable to hold the string
var translatedMessageText = ""

class LetsChatModule : AppCompatActivity() {

    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView

//    private val BASE_URL = "http://52.90.174.3:5000/"
//    private val TAG = "MainActivity"

    lateinit var messageTextView: TextView

    //    private lateinit var tts: TextToSpeech
    private lateinit var languageOptionsSpinner: Spinner

    lateinit var tts: TextToSpeech

//    private var selectedLocale: Locale = Locale.getDefault()

    var count: Int = 0

    // Set the timeout values
    private val TIMEOUT_SECONDS = 600L // 180L

    // OkHttpClient instance with timeout configuration
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // Retrofit instance with OkHttpClient
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://52.91.34.97:5000/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

//    // Retrofit instance
//    private val retrofit = Retrofit.Builder()
//        .baseUrl("http://52.90.174.3:5000/")
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()

    private val apiService: ApiService = retrofit.create(ApiService::class.java)

    // Add this variable to store the most recent frame
    private var latestFrameData: ByteArray? = null

//    private var sourceLanguage = -1
//    private var targetLanguage = -1
    private var areModelsDownloaded = false
//    private var currentTranslator: FirebaseTranslator? = null

    var sourceLanguage = "ENGLISH"
    lateinit var destinationLanguage: String

    var selectedItemFromLanguageOptionsSpinner = ""

    lateinit var languageEncoding: String

    private lateinit var re_setButton: Button

    private val languageNames = arrayOf(
        "AFRIKAANS", "ARABIC", "BELARUSIAN", "BULGARIAN", "BENGALI", "CATALAN",
        "CZECH", "WELSH", "DANISH", "GERMAN", "GREEK", "ENGLISH", "ESPERANTO",
        "SPANISH", "ESTONIAN", "PERSIAN", "FINNISH", "FRENCH", "IRISH", "GALICIAN",
        "GUJARATI", "HEBREW", "HINDI", "CROATIAN", "HAITIAN", "HUNGARIAN", "INDONESIAN",
        "ICELANDIC", "ITALIAN", "JAPANESE", "GEORGIAN", "KANNADA", "KOREAN", "LITHUANIAN",
        "LATVIAN", "MACEDONIAN", "MARATHI", "MALAY", "MALTESE", "DUTCH", "NORWEGIAN",
        "POLISH", "PORTUGUESE", "ROMANIAN", "RUSSIAN", "SLOVAK", "SLOVENIAN", "ALBANIAN",
        "SWEDISH", "SWAHILI", "TAMIL", "TELUGU", "THAI", "TAGALOG", "TURKISH", "UKRANIAN",
        "URDU", "VIETNAMESE", "CHINESE"
    )

    private val LANGUAGE_CODES = arrayOf(
        "AF", "AR", "BE", "BG", "BN", "CA", "CS", "CY", "DA", "DE",
        "EL", "EN", "EO", "ES", "ET", "FA", "FI", "FR", "GA", "GL",
        "GU", "HE", "HI", "HR", "HT", "HU", "ID", "IS", "IT", "JA",
        "KA", "KN", "KO", "LT", "LV", "MK", "MR", "MS", "MT", "NL",
        "NO", "PL", "PT", "RO", "RU", "SK", "SL", "SQ", "SV", "SW",
        "TA", "TE", "TH", "TL", "TR", "UK", "UR", "VI", "ZH"
    )

    private lateinit var progressBar: ProgressBar

    private val languageNameToLanguageCodeMap = mapOf(
        "AFRIKAANS" to "AF",
        "ARABIC" to "AR",
        "BELARUSIAN" to "BE",
        "BULGARIAN" to "BG",
        "BENGALI" to "BN",
        "CATALAN" to "CA",
        "CZECH" to "CS",
        "WELSH" to "CY",
        "DANISH" to "DA",
        "GERMAN" to "DE",
        "GREEK" to "EL",
        "ENGLISH" to "EN",
        "ESPERANTO" to "EO",
        "SPANISH" to "ES",
        "ESTONIAN" to "ET",
        "PERSIAN" to "FA",
        "FINNISH" to "FI",
        "FRENCH" to "FR",
        "IRISH" to "GA",
        "GALICIAN" to "GL",
        "GUJARATI" to "GU",
        "HEBREW" to "HE",
        "HINDI" to "HI",
        "CROATIAN" to "HR",
        "HAITIAN" to "HT",
        "HUNGARIAN" to "HU",
        "INDONESIAN" to "ID",
        "ICELANDIC" to "IS",
        "ITALIAN" to "IT",
        "JAPANESE" to "JA",
        "GEORGIAN" to "KA",
        "KANNADA" to "KN",
        "KOREAN" to "KO",
        "LITHUANIAN" to "LT",
        "LATVIAN" to "LV",
        "MACEDONIAN" to "MK",
        "MARATHI" to "MR",
        "MALAY" to "MS",
        "MALTESE" to "MT",
        "DUTCH" to "NL",
        "NORWEGIAN" to "NO",
        "POLISH" to "PL",
        "PORTUGUESE" to "PT",
        "ROMANIAN" to "RO",
        "RUSSIAN" to "RU",
        "SLOVAK" to "SK",
        "SLOVENIAN" to "SL",
        "ALBANIAN" to "SQ",
        "SWEDISH" to "SV",
        "SWAHILI" to "SW",
        "TAMIL" to "TA",
        "TELUGU" to "TE",
        "THAI" to "TH",
        "TAGALOG" to "TL",
        "TURKISH" to "TR",
        "UKRANIAN" to "UK",
        "URDU" to "UR",
        "VIETNAMESE" to "VI",
        "CHINESE" to "ZH"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.letschat)

        get_permission()

        var handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        messageTextView = findViewById(R.id.messageTextView)

        // Find the Button by its ID
        val start_button: Button = findViewById(R.id.start_button)

        // Set onClick listener for the Button
        start_button.setOnClickListener {

            Log.d("API_CALL", "I'm Here")

            startAPIRequestTimer()
        }

        textureView = findViewById(R.id.textureViewLetsChat)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openFrontCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Find the Button by its ID
        val stop_button: Button = findViewById(R.id.stop_button)


        languageOptionsSpinner = findViewById(R.id.languageOptionsSpinner)

        // Define the initial array of languages
        var destinationLanguages = arrayOf(
            "US ENGLISH",
            "CANADA ENGLISH",
            "CANADA_FRENCH",
            "CHINESE",
            "ENGLISH",
            "FRENCH",
            "GERMAN",
            "ITALIAN",
            "JAPANESE",
            "KOREAN",
            "TAIWANESE",
            "UK ENGLISH",
            "HINDI"
        )

        // Get reference to the ProgressBar
        progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Get the default language of the device
        var defaultLanguage = Locale.getDefault().displayLanguage.toUpperCase(Locale.ROOT)

        Log.d("TRANSLATION_CHECK", defaultLanguage)

        // Check if the default language is in the array, if not, add it
        if (!destinationLanguages.contains(defaultLanguage)) {
            destinationLanguages += defaultLanguage
        }

        // Create an ArrayAdapter using the updated string array and a default spinner layout
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, destinationLanguages)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        languageOptionsSpinner.adapter = adapter

        // Set the default language as the selected item in the spinner
        val defaultLanguageIndex = adapter.getPosition(defaultLanguage)
        languageOptionsSpinner.setSelection(defaultLanguageIndex)

//        defaultLanguage = defaultLanguage.substring(0, 1).toUpperCase() + defaultLanguage.substring(1).toLowerCase()
//
//        defaultLanguage = mapFromPhoneLanguageToCorrespondingLanguageInEnglishLetters(defaultLanguage)

//        defaultLanguage = defaultLanguage.substring(0, 1).toUpperCase() + defaultLanguage.substring(1).toLowerCase()
//
//        Log.d("TRANSLATION_CHECK", defaultLanguage)
//
//        defaultLanguage = mapFromPhoneLanguageToCorrespondingLanguageInEnglishLetters(defaultLanguage)
//
//        Log.d("TRANSLATION_CHECK", defaultLanguage)


//        tts = TextToSpeech(this,this)


        stop_button.setOnClickListener {

            Log.d("TRANSLATION_CHECK", "From inside the OnClickListener for the stop_button")

            apiRequestTimer?.cancel()

            if (messageTextView.text.isBlank()){
                showAlert("No text to translate yet")
                return@setOnClickListener
            }

            Log.d("TRANSLATION_CHECK", defaultLanguage)

//            val languageCode = detectLanguage("Hello")

//                val languageCode = runBlocking {
//                    detectLanguage("Hello")
//                }

//            sourceLanguage = languageNameToLanguageCodeMap.entries.find { it.value == languageCode }?.key.toString()

            var selectedItem = languageOptionsSpinner.selectedItem as String


            if (selectedItem == destinationLanguages[destinationLanguages.size - 1])
            {
                selectedItem = selectedItem.substring(0, 1).toUpperCase() + selectedItem.substring(1).toLowerCase()

                Log.d("TRANSLATION_CHECK", selectedItem)

                selectedItem = mapFromPhoneLanguageToCorrespondingLanguageInEnglishLetters(selectedItem)

                Log.d("TRANSLATION_CHECK", selectedItem)
            }


            if(languageNames.contains(selectedItem))
            {
                Log.d("TRANSLATION_CHECK", "Inside languageNames.contains(defaultLanguage) Block")


//                val languageCode = detectLanguage("Hello")
//
////                val languageCode = runBlocking {
////                    detectLanguage("Hello")
////                }
//
//                sourceLanguage = languageNameToLanguageCodeMap.entries.find { it.value == languageCode }?.key.toString()


//                var selectedItem = languageOptionsSpinner.selectedItem as String


//                if (selectedItem == destinationLanguages[destinationLanguages.size - 1])
//                {
//                    selectedItem = selectedItem.substring(0, 1).toUpperCase() + selectedItem.substring(1).toLowerCase()
//
//                    Log.d("TRANSLATION_CHECK", selectedItem)
//
//                    selectedItem = mapFromPhoneLanguageToCorrespondingLanguageInEnglishLetters(selectedItem)
//
//                    Log.d("TRANSLATION_CHECK", selectedItem)
//                }


                destinationLanguage = selectedItem

//                prepareATranslatorWith("ENGLISH", defaultLanguage)

                if (sourceLanguage != destinationLanguage)
                {
                    prepareATranslatorWith(sourceLanguage, destinationLanguage)
                }
                else
                {
                    languageEncoding = languageNameToLanguageCodeMap[sourceLanguage]?.toLowerCase().toString()

                    tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                        if (it == TextToSpeech.SUCCESS) {

                            Log.d("TRANSLATION_CHECK", "From inside the TextToSpeech part here")

                            val result_here = tts.setLanguage(Locale(languageEncoding))

                            if (result_here == TextToSpeech.LANG_MISSING_DATA)  {
                                // Language data is missing or not supported
                                Log.d("TRANSLATION_CHECK", "LANG_MISSING_DATA one here")
                            }
                            else if (result_here == TextToSpeech.LANG_NOT_SUPPORTED) {
                                // Language data is missing or not supported
                                Log.d("TRANSLATION_CHECK", "Language not supported here")
                            } else{
                                // Language set successfully
                                tts.setSpeechRate(1.0f)
                                tts.speak(messageTextView.text.toString(), TextToSpeech.QUEUE_ADD, null)
                            }
                        }
                    })
                }
//
//
//
//                val languageEncoding = languageNameToLanguageCodeMap[defaultLanguage]?.toLowerCase()
//
//
//                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
//                    if (it == TextToSpeech.SUCCESS) {
//
//                        tts.language = Locale(languageEncoding)
//
//                        Log.d("TRANSLATION_CHECK", tts.language.toString())
//
//                        tts.setSpeechRate(1.0f)
//                        tts.speak(messageTextView.text.toString(), TextToSpeech.QUEUE_ADD, null)
//
//                    }
//                })
            }
        }

        languageOptionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

//                val selectedItem = parent?.getItemAtPosition(position).toString()
//
//                prepareATranslatorWith(defaultLanguage, selectedItem)
//
//                val languageEncoding = languageNameToLanguageCodeMap[selectedItem]?.toLowerCase()
//
//
//                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
//                    if (it == TextToSpeech.SUCCESS) {
//
//                        val locale = Locale(languageEncoding)
//
//                        tts.language = locale
//                        tts.setSpeechRate(1.0f)
//                        tts.speak(messageTextView.text.toString(), TextToSpeech.QUEUE_ADD, null)
//
//                    }
//                })
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing or handle the case when nothing is selected
            }
        }


        re_setButton = findViewById(R.id.re_set_button)

        re_setButton.setOnClickListener {

//            Log.d("TRANSLATION_CHECK", "Re-Set Button has been Triggered here")
//
//            languageEncoding = languageNameToLanguageCodeMap[defaultLanguage]?.toLowerCase().toString()
//
//            Log.d("TRANSLATION_CHECK", languageEncoding)
//
////            val locale = Locale(languageEncoding)
//
//            Log.d("TRANSLATION_CHECK", "yes")
//
////            tts.setLanguage(locale)
//
////            tts.language = Locale(languageEncoding)
//
//            Log.d("TRANSLATION_CHECK", "Yes, hi")
//
////            tts.language = Locale(languageEncoding)
//
////            val languageTobeExtractedFrom = Locale(languageEncoding).toString()
//
//            Log.d("TRANSLATION_CHECK", "Hello")
//
////            Log.d("TRANSLATION_CHECK", "Re-Set Button has been Triggered here")
//
////            Log.d("TRANSLATION_CHECK", languageTobeExtractedFrom)
////
////            val parts = languageTobeExtractedFrom?.split("_")
////
////            Log.d("TRANSLATION_CHECK", parts.toString())
////
////            val language = parts?.get(0)  // This will be "hin"
////            val country = parts?.get(1)   // This will be "IND"
////
////            Log.d("TRANSLATION_CHECK", "Hello Before")
////
////            val locale = Locale(language, country)
//
////            Log.d("TRANSLATION_CHECK", "Hello After")
//
////            val locale = Locale(languageEncoding)
//
////            Log.d("TRANSLATION_CHECK", locale.toString())
//
//            tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
//                if (it == TextToSpeech.SUCCESS) {
//
//                    Log.d("TRANSLATION_CHECK", "From inside the TextToSpeech part here")
//
////                    tts.language = Locale(languageEncoding)
//
////                    tts.setLanguage(locale)
//
////                    Log.d("TRANSLATION_CHECK", tts.setLanguage(locale).toString())
//
////                    Log.d("TRANSLATION_CHECK", locale.toString())
//
////                    tts.language = Locale(languageEncoding)
//                    val result_here = tts.setLanguage(Locale(languageEncoding))
//
//                    if (result_here == TextToSpeech.LANG_MISSING_DATA)  {
//                        // Language data is missing or not supported
//                        Log.d("TRANSLATION_CHECK", "LANG_MISSING_DATA one here")
//                    }
//                    else if (result_here == TextToSpeech.LANG_NOT_SUPPORTED) {
//                        // Language data is missing or not supported
//                        Log.d("TRANSLATION_CHECK", "Language not supported here")
//                    } else{
//                        // Language set successfully
//                        tts.setSpeechRate(1.0f)
//                        tts.speak(messageTextView.text.toString(), TextToSpeech.QUEUE_ADD, null)
//                    }
//
////                    tts.setSpeechRate(1.0f)
////                    tts.speak(messageTextView.text.toString(), TextToSpeech.QUEUE_ADD, null)
//
//                }
//            })

//            textToSpeechNow(defaultLanguage)

            messageTextView.text = ""

        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setSelectedItemId(R.id.LetsChat_nav)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Virtualwhitecane_nav -> {
                    startActivity(Intent(applicationContext, VirtualWhiteCaneModule::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.Home_nav -> {
                    startActivity(Intent(applicationContext, Dashboard::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.LetsChat_nav -> true
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

    @SuppressLint("MissingPermission")
    fun openFrontCamera() {
        val cameraIds = cameraManager.cameraIdList

        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(p0: CameraDevice) {
                        cameraDevice = p0

                        var surfaceTexture = textureView.surfaceTexture
                        var surface = Surface(surfaceTexture)

                        var captureRequest =
                            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequest.addTarget(surface)

                        cameraDevice.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
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
                            },
                            handler
                        )
                    }

                    override fun onDisconnected(p0: CameraDevice) {}

                    override fun onError(p0: CameraDevice, p1: Int) {}
                }, handler)
                break // Stop iterating once the front camera is found
            }
        }
    }

//    override fun onInit(intVar: Int) {
//
//        if(intVar==TextToSpeech.SUCCESS){
//
//            tts.language = Locale(languageEncoding)
//
//            val parts = languageEncoding?.split("_")
//            val language = parts?.get(0)  // This will be "hin"
//            val country = parts?.get(1)   // This will be "IND"
//
//
//            var local= Locale(language,country)
//
//            val resultado=tts!!.setLanguage(local)
//
//            if(resultado==TextToSpeech.LANG_MISSING_DATA)
//            {
//                Log.i(TAG,"lang not found")
//            }
//        }
//    }

    fun get_permission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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

    fun functionToTestNumberOfFunctionCalls() {
        count += 1
        Log.d("CHECKING", "Function Call Number: " + count.toString())
    }

    // Function to capture the current frame from the TextureView
    private fun captureFrame(textureView: TextureView): ByteArray? {
        val bitmap = textureView.bitmap ?: return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private fun sendFrameToApi(frameData: ByteArray) {
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), frameData)
        val filePart = MultipartBody.Part.createFormData("file", "frame.jpg", requestBody)

        apiService.predictImage(filePart).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    try {
                        // Parse response body as JSON object
                        val responseBody = response.body()?.string() ?: ""
                        val jsonObject = JSONObject(responseBody)

                        // Extract prediction value from JSON object
                        val prediction = jsonObject.optString("prediction", "")

                        // Observer for the other variable
                        if (prediction.isNotEmpty()) {
                            // Update the TextView with the global string
                            translatedMessageText += prediction
                            messageTextView.text = translatedMessageText
                        }

                        // Handle prediction value
                        Log.d("API_CALL", "API call successful. Prediction: $prediction")
                    } catch (e: Exception) {
                        // Handle JSON parsing error
                        Log.d("API_CALL", "Error parsing JSON response", e)
                    }
                } else {
                    // Handle unsuccessful response
                    Log.e("API_CALL", "API call failed: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                // Handle failure
                Log.e("API_CALL", "API call failed", t)
            }
        })
    }

    // Function to start the timer for API requests
    private fun startAPIRequestTimer() {

        Log.d("API_CALL", "I'm Here")

        // Cancel the previous timer if it exists
        apiRequestTimer?.cancel()

        // Schedule the timer to trigger API requests every 5 seconds
        apiRequestTimer = Timer()
        apiRequestTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Capture the latest frame data
                latestFrameData = captureFrame(textureView)

                // Check if frame data is available
                latestFrameData?.let { frameData ->
                    // Make API call with the captured frame data
                    sendFrameToApi(frameData)
                }
            }
        }, 0, 3000) // Start immediately, repeat every 5 seconds
    }

    override fun onPause() {
        super.onPause()
        cameraDevice?.close()
    }

    override fun onStop() {
        super.onStop()
        cameraDevice?.close()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//
//        cameraDevice?.close()
//
//        tts.stop()
//        tts.shutdown()
//    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
        textureView.surfaceTextureListener = null // Release the surface texture listener
        tts.stop()
        tts.shutdown()
    }



    private fun showAlert(message: String) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Error")
        dialog.setMessage(message)
        dialog.setPositiveButton(" OK ",
            { dialog, id -> dialog.dismiss() })
        dialog.show()

    }


    // Fetch a translator based on the language selected by the user.
    private fun prepareATranslatorWith(sourceLang: String, destinationLang: String ) {

        Log.d("TRANSLATION_CHECK", "Inside prepareATranslatorWith")


        val sourceLangCode = languageNameToLanguageCodeMap[sourceLang]?.toLowerCase()

        val destinationLangCode = languageNameToLanguageCodeMap[destinationLang]?.toLowerCase()


        Log.d("TRANSLATION_CHECK", "sup")

        if (sourceLangCode != null) {
            Log.d("TRANSLATION_CHECK", "SourceLangCode: " + sourceLangCode)
        }

        if (destinationLangCode != null) {
            Log.d("TRANSLATION_CHECK", "DestinationLangCode: " + destinationLangCode)
        }


        if ((sourceLangCode != null) && (destinationLangCode != null))
        {
            Log.d("TRANSLATION_CHECK", sourceLangCode)

            Log.d("TRANSLATION_CHECK", destinationLangCode)

            // Creating translator:
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLangCode)!!)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(destinationLangCode)!!)
                .build()

            val translator_new_testing = Translation.getClient(options)

            var conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            // Show the progress bar before starting translation
            progressBar.visibility = View.VISIBLE

            translator_new_testing.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    // Model downloaded successfully. Okay to start translating.
                    // (Set a flag, unhide the translation UI, etc.)

                    translator_new_testing.translate(messageTextView.text.toString())
                        .addOnSuccessListener { translatedText ->
                            // Translation successful.

                            Log.d("TRANSLATION_CHECK", translatedText)

                            // Hide the progress bar
                            progressBar.visibility = View.GONE

                            messageTextView.text = translatedText

                            sourceLanguage = destinationLanguage

                            languageEncoding = languageNameToLanguageCodeMap[destinationLang]?.toLowerCase().toString()

                            Log.d("TRANSLATION_CHECK", languageEncoding)


                            Log.d("TRANSLATION_CHECK", "yes")


                            Log.d("TRANSLATION_CHECK", "Yes, hi")


                            Log.d("TRANSLATION_CHECK", "Hello")


                            tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                                if (it == TextToSpeech.SUCCESS) {

                                    Log.d("TRANSLATION_CHECK", "From inside the TextToSpeech part here")

                                    val result_here = tts.setLanguage(Locale(languageEncoding))

                                    if (result_here == TextToSpeech.LANG_MISSING_DATA)  {
                                        // Language data is missing or not supported
                                        Log.d("TRANSLATION_CHECK", "LANG_MISSING_DATA one here")
                                    }
                                    else if (result_here == TextToSpeech.LANG_NOT_SUPPORTED) {
                                        // Language data is missing or not supported
                                        Log.d("TRANSLATION_CHECK", "Language not supported here")
                                    } else{
                                        // Language set successfully
                                        tts.setSpeechRate(1.0f)
                                        tts.speak(messageTextView.text.toString(), TextToSpeech.QUEUE_ADD, null)
                                    }


                                }
                            })

                        }
                        .addOnFailureListener { exception ->
                            // Error.
                            // ...
                        }
                }
                .addOnFailureListener { exception ->
                    // Model couldn’t be downloaded or other internal error.
                    // ...

                    // Hide the progress bar
                    progressBar.visibility = View.GONE
                }
        }
    }


    fun mapFromPhoneLanguageToCorrespondingLanguageInEnglishLetters(language: String): String {
        return when (language) {
            "अफ़्रीकांस" -> "AFRIKAANS"
            "العربية" -> "ARABIC"
            "Беларуская" -> "BELARUSIAN"
            "български" -> "BULGARIAN"
            "বাংলা" -> "BENGALI"
            "Català" -> "CATALAN"
            "Čeština" -> "CZECH"
            "Cymraeg" -> "WELSH"
            "Dansk" -> "DANISH"
            "Deutsch" -> "GERMAN"
            "Ελληνικά" -> "GREEK"
            "English" -> "ENGLISH"
            "Esperanto" -> "ESPERANTO"
            "Español" -> "SPANISH"
            "Eesti" -> "ESTONIAN"
            "فارسی" -> "PERSIAN"
            "Suomi" -> "FINNISH"
            "Français" -> "FRENCH"
            "Gaeilge" -> "IRISH"
            "Galego" -> "GALICIAN"
            "ગુજરાતી" -> "GUJARATI"
            "עברית" -> "HEBREW"
            "हिन्दी" -> "HINDI"
            "Hrvatski" -> "CROATIAN"
            "Kreyòl Ayisyen" -> "HAITIAN"
            "Magyar" -> "HUNGARIAN"
            "Bahasa Indonesia" -> "INDONESIAN"
            "Íslenska" -> "ICELANDIC"
            "Italiano" -> "ITALIAN"
            "日本語" -> "JAPANESE"
            "ქართული" -> "GEORGIAN"
            "ಕನ್ನಡ" -> "KANNADA"
            "한국어" -> "KOREAN"
            "Lietuvių" -> "LITHUANIAN"
            "Latviešu" -> "LATVIAN"
            "Македонски" -> "MACEDONIAN"
            "मराठी" -> "MARATHI"
            "Bahasa Melayu" -> "MALAY"
            "Malti" -> "MALTESE"
            "Nederlands" -> "DUTCH"
            "Norsk" -> "NORWEGIAN"
            "Polski" -> "POLISH"
            "Português" -> "PORTUGUESE"
            "Română" -> "ROMANIAN"
            "Русский" -> "RUSSIAN"
            "Slovenčina" -> "SLOVAK"
            "Slovenščina" -> "SLOVENIAN"
            "Shqip" -> "ALBANIAN"
            "Svenska" -> "SWEDISH"
            "Kiswahili" -> "SWAHILI"
            "தமிழ்" -> "TAMIL"
            "తెలుగు" -> "TELUGU"
            "ไทย" -> "THAI"
            "Tagalog" -> "TAGALOG"
            "Türkçe" -> "TURKISH"
            "Українська" -> "UKRANIAN"
            "اردو" -> "URDU"
            "Tiếng Việt" -> "VIETNAMESE"
            "中文" -> "CHINESE"
            else -> "none yet" // Default to original language if not translated
        }
    }

//    fun detectLanguage(stringForLanguageDetection: String): String {
//
//        val languageCode: String
//
//        val languageIdentifier = LanguageIdentification.getClient()
//        languageIdentifier.identifyLanguage(stringForLanguageDetection)
//            .addOnSuccessListener { languageCode ->
//                if (languageCode == "und") {
//                    Log.i(TAG, "Can't identify language.")
//                } else {
//                    Log.i(TAG, "Language: $languageCode")
//                }
//            }
//            .addOnFailureListener {
//                // Model couldn’t be loaded or other internal error.
//                // ...
//            }
//
//        return languageCode
//    }


    private fun detectLanguage(stringForLanguageDetection: String): String {

        var languageCodeDetected = ""

        val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()

        languageIdentifier.identifyLanguage(stringForLanguageDetection)
            .addOnSuccessListener { languageCode ->

                if (languageCode == "und") {
                    Log.d("TRANSLATION_CHECK", "Can't identify language.")
                } else {
                    Log.d("TRANSLATION_CHECK", "Language: $languageCode")

                    languageCodeDetected = languageCode

                }
            }
            .addOnFailureListener {
                // Model couldn’t be loaded or other internal error.
                // Handle failure as needed
                Log.d("TRANSLATION_CHECK", "Error identifying language: $it")
            }

        return languageCodeDetected
    }

    private fun getTranslateLanguageFrom(userSelection: String): Any {

        Log.d("TRANSLATION_CHECK", "Inside getTranslateLanguageFrom")

        when (userSelection){

            "US ENGLISH" -> return TranslateLanguage.ENGLISH
            "CANADA ENGLISH" -> return TranslateLanguage.ENGLISH
            "CANADA_FRENCH" -> return TranslateLanguage.FRENCH
            "CHINESE" -> return TranslateLanguage.CHINESE
            "ENGLISH" -> return TranslateLanguage.ENGLISH
            "FRENCH" -> return TranslateLanguage.FRENCH
            "GERMAN" -> return TranslateLanguage.GERMAN
            "ITALIAN" -> return TranslateLanguage.ITALIAN
            "JAPANESE" -> return TranslateLanguage.JAPANESE
            "KOREAN" -> return TranslateLanguage.KOREAN
            "UK ENGLISH" -> return TranslateLanguage.ENGLISH
            "HINDI" -> return TranslateLanguage.HINDI
            "AFRIKAANS" -> return TranslateLanguage.AFRIKAANS
            "ARABIC" -> return TranslateLanguage.ARABIC
            "BELARUSIAN" -> return TranslateLanguage.BELARUSIAN
            "BULGARIAN" -> return TranslateLanguage.BULGARIAN
            "BENGALI" -> return TranslateLanguage.BENGALI
            "CATALAN" -> return TranslateLanguage.CATALAN
            "CZECH" -> return TranslateLanguage.CZECH
            "WELSH" -> return TranslateLanguage.WELSH
            "DANISH" -> return TranslateLanguage.DANISH
            "GERMAN" -> return TranslateLanguage.GERMAN
            "GREEK" -> return TranslateLanguage.GREEK
            "ENGLISH" -> return TranslateLanguage.ENGLISH
            "ESPERANTO" -> return TranslateLanguage.ESPERANTO
            "SPANISH" -> return TranslateLanguage.SPANISH
            "ESTONIAN" -> return TranslateLanguage.ESTONIAN
            "PERSIAN" -> return TranslateLanguage.PERSIAN
            "FINNISH" -> return TranslateLanguage.FINNISH
            "FRENCH" -> return TranslateLanguage.FRENCH
            "IRISH" -> return TranslateLanguage.IRISH
            "GALICIAN" -> return TranslateLanguage.GALICIAN
            "GUJARATI" -> return TranslateLanguage.GUJARATI
            "HEBREW" -> return TranslateLanguage.HEBREW
            "HINDI" -> return TranslateLanguage.HINDI
            "CROATIAN" -> return TranslateLanguage.CROATIAN
            "HAITIAN" -> return TranslateLanguage.HAITIAN_CREOLE
            "HUNGARIAN" -> return TranslateLanguage.HUNGARIAN
            "INDONESIAN" -> return TranslateLanguage.INDONESIAN
            "ICELANDIC" -> return TranslateLanguage.ICELANDIC
            "ITALIAN" -> return TranslateLanguage.ITALIAN
            "JAPANESE" -> return TranslateLanguage.JAPANESE
            "GEORGIAN" -> return TranslateLanguage.GEORGIAN
            "KANNADA" -> return TranslateLanguage.KANNADA
            "KOREAN" -> return TranslateLanguage.KOREAN
            "LITHUANIAN" -> return TranslateLanguage.LITHUANIAN
            "LATVIAN" -> return TranslateLanguage.LATVIAN
            "MACEDONIAN" -> return TranslateLanguage.MACEDONIAN
            "MARATHI" -> return TranslateLanguage.MARATHI
            "MALAY" -> return TranslateLanguage.MALAY
            "MALTESE" -> return TranslateLanguage.MALTESE
            "DUTCH" -> return TranslateLanguage.DUTCH
            "NORWEGIAN" -> return TranslateLanguage.NORWEGIAN
            "POLISH" -> return TranslateLanguage.POLISH
            "PORTUGUESE" -> return TranslateLanguage.PORTUGUESE
            "ROMANIAN" -> return TranslateLanguage.ROMANIAN
            "RUSSIAN" -> return TranslateLanguage.RUSSIAN
            "SLOVAK" -> return TranslateLanguage.SLOVAK
            "SLOVENIAN" -> return TranslateLanguage.SLOVENIAN
            "ALBANIAN" -> return TranslateLanguage.ALBANIAN
            "SWEDISH" -> return TranslateLanguage.SWEDISH
            "SWAHILI" -> return TranslateLanguage.SWAHILI
            "TAMIL" -> return TranslateLanguage.TAMIL
            "TELUGU" -> return TranslateLanguage.TELUGU
            "THAI" -> return TranslateLanguage.THAI
            "TAGALOG" -> return TranslateLanguage.TAGALOG
            "TURKISH" -> return TranslateLanguage.TURKISH
            "UKRANIAN" -> return TranslateLanguage.UKRAINIAN
            "URDU" -> return TranslateLanguage.URDU
            "VIETNAMESE" -> return TranslateLanguage.VIETNAMESE
            "CHINESE" -> return TranslateLanguage.CHINESE
        }
        // Unknown
        return -1

    }

    fun textToSpeechNow(languageToSpeakIn: String ) {

        Log.d("TRANSLATION_CHECK", "From inside textToSpeechNow()")

        languageEncoding = languageNameToLanguageCodeMap[languageToSpeakIn]?.toLowerCase().toString()

        tts.language = Locale(languageEncoding)

        val parts = languageEncoding?.split("_")
        val language = parts?.get(0)  // This will be "hin"
        val country = parts?.get(1)   // This will be "IND"

        val locale = Locale(language, country)

        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {

//                                    tts.language = Locale(languageEncoding)

//                                    tts.setLanguage(locale)

                Log.d("TRANSLATION_CHECK", tts.setLanguage(locale).toString())

                tts.setSpeechRate(1.0f)
                tts.speak(messageTextView.text.toString(), TextToSpeech.QUEUE_ADD, null)

            }
        })

    }
}