package com.example.myapplication

import android.Manifest.permission.RECORD_AUDIO
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

//// Step 1: Define your service
//class VoiceRecognitionService : Service() {
//
//    private lateinit var speechRecognizer: SpeechRecognizer
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        startListening()
//        return START_STICKY
//    }
//
//    private fun startListening() {
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//        speechRecognizer.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(params: Bundle?) {}
//            override fun onBeginningOfSpeech() {}
//            override fun onRmsChanged(rmsdB: Float) {}
//            override fun onBufferReceived(buffer: ByteArray?) {}
//            override fun onEndOfSpeech() {}
//            override fun onError(error: Int) {}
//            override fun onResults(results: Bundle?) {
//                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                matches?.let {
//                    Log.d("SPEECH_INPUT_CHECKING", matches[0])
//                    processVoiceCommand(matches[0])
//                }
//                startListening() // Start listening again
//            }
//
//            override fun onPartialResults(partialResults: Bundle?) {}
//            override fun onEvent(eventType: Int, params: Bundle?) {}
//        })
//
//        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
//        speechRecognizer.startListening(recognizerIntent)
//    }
//
//    private fun processVoiceCommand(command: String) {
//        // Handle the voice command here
//
//        Log.d("SPEECH_INPUT_CHECKING", command)
//
//        if (command == "your_command") {
//            // Perform the action corresponding to the recognized command
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        speechRecognizer.destroy()
//    }
//}


class Dashboard : AppCompatActivity(), TextToSpeech.OnInitListener { //

//    lateinit var speechRecognizer: SpeechRecognizer

    private val RQ_SPEECH_REC = 102

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var intentRecognizer: Intent

    private val CALL_PHONE_REQUEST_CODE = 123 // Or any request code you prefer

    private lateinit var waterHandler: Handler
    private lateinit var breathHandler: Handler
    private lateinit var textView: TextView
//    private lateinit var startButton: Button
    private lateinit var tts: TextToSpeech
    private var waterReminderActive = false
    private var breathReminderActive = false

    private lateinit var wellnessAwareEnabler: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        tts = TextToSpeech(this, this)

        ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), PackageManager.PERMISSION_GRANTED)

//        textView = findViewById(R.id.textView)
        intentRecognizer = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(i: Int) {
//                // Handle error and reset recognizer if necessary
//                restartListening()
            }
            override fun onResults(bundle: Bundle) {
                val matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                var string = ""
                if (!matches.isNullOrEmpty()) {
                    string = matches[0]
//                    textView.text = string
                    Log.d("SPEECH_INPUT_CHECKING", string)

                    if (string.compareTo("Helen Police", ignoreCase = true) == 0)
                    {
                        makePhoneCall()
                    }
                    else if (string.compareTo("Helen Ambulance", ignoreCase = true) == 0)
                    {
                        makePhoneCall()
                    }

//                    restartListening() // Reset recognizer after processing command
                }
            }
            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle?) {}
        })

//        speak("Welcome to HelenHands")

        val toVirtualWhiteCaneRelativeLayout = findViewById<RelativeLayout>(R.id.toVirtualWhiteCaneModule)
        toVirtualWhiteCaneRelativeLayout.setOnClickListener {

            val intent = Intent(this, VirtualWhiteCaneModule::class.java)
            startActivity(intent)

            // Code to be executed when the RelativeLayout is clicked
            // For example, you can open a new activity, show a toast, etc.
        }

        textView = findViewById(R.id.WellnessName)

//        textView.setTextColor(Color.parseColor("#8692f7"))

        // Retrieve the text passed from Activity1
        val textToDisplay = intent.getStringExtra("textToDisplay")

        if (textToDisplay != "")
        {
            textView.text = textToDisplay // Set the text of TextView
        }

        val toLetsChatRelativeLayout = findViewById<RelativeLayout>(R.id.toLetsChatModule)
        toLetsChatRelativeLayout.setOnClickListener {

            val intent = Intent(this, LetsChatModule::class.java)
            startActivity(intent)

            // Code to be executed when the RelativeLayout is clicked
            // For example, you can open a new activity, show a toast, etc.
        }

        val toSOSReachRelativeLayout = findViewById<RelativeLayout>(R.id.toSOSReachModule)
        toSOSReachRelativeLayout.setOnClickListener {

//            val intent = Intent(this, SOSReachModule::class.java)
//            startActivity(intent)

            val intent = Intent(this, SOSReachModule::class.java)
            startActivity(intent)


            // Code to be executed when the RelativeLayout is clicked
            // For example, you can open a new activity, show a toast, etc.


        }

//        if (!waterReminderActive && !breathReminderActive) {
//            startWaterReminder()
//            startBreathReminder()
//            waterReminderActive = true
//            breathReminderActive = true
//            textView.text = "Turn Off Reminders"
////                wellnessAwareEnabler.setBackgroundColor(Color.parseColor(("#8ED17D")))
////                textView.setTextColor((Color.parseColor("#FFFFFF")))
//        }
//
        textView.text = "Wellness Aware - Enabler"

        wellnessAwareEnabler = findViewById<RelativeLayout>(R.id.wellnessAwareEnabler)
        wellnessAwareEnabler.setOnClickListener {

            if (!waterReminderActive && !breathReminderActive) {
                startWaterReminder()
                startBreathReminder()
                waterReminderActive = true
                breathReminderActive = true
                textView.text = "Turn Off Reminders"
                wellnessAwareEnabler.setBackgroundColor(Color.parseColor(("#8ED17D")))
                textView.setTextColor((Color.parseColor("#FFFFFF")))
            } else {
                stopWaterReminder()
                stopBreathReminder()
                waterReminderActive = false
                breathReminderActive = false
                textView.text = "Start Reminders"
                wellnessAwareEnabler.setBackgroundColor(Color.parseColor(("#FFFFFF")))
                textView.setTextColor(Color.parseColor("#8692f7"))
            }


            // Code to be executed when the RelativeLayout is clicked
            // For example, you can open a new activity, show a toast, etc.
        }

        // Check and request permissions if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
                return
            }
        }

//        // Start listening for speech input
//        startListening()
//        askSpeechInput()
//        val serviceIntent = Intent(this, VoiceRecognitionService::class.java)
//        startService(serviceIntent)

//        val doubleClick = DoubleClick(object : DoubleClickListener {
//            override fun onSingleClickEvent(view: View?) {
//                // DO STUFF SINGLE CLICK
//
//                val intent = Intent(view?.context, SOSReachModule::class.java)
//                startActivity(intent)
//
//            }
//
//            override fun onDoubleClickEvent(view: View?) {
//                // DO STUFF DOUBLE CLICK
//
//                var SOSName: TextView
//                SOSName = findViewById(R.id.SOSName)
//
//                if (SOSName.text == "SOS Reach")
//                {
//                    startButton(toSOSReachRelativeLayout)
//                    SOSName.text == "SOS Reach Protection - Enabled"
//                }
//                else
//                {
//                    stopButton(toSOSReachRelativeLayout)
//                }
//            }
//        })

//        toSOSReachRelativeLayout.setOnClickListener(doubleClick)

        val switchMode = findViewById<Switch>(R.id.listening_switch)
        switchMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Switch is ON
                // Perform actions for ON mode
                startButton()
            } else {
                // Switch is OFF
                // Perform actions for OFF mode
                stopButton()
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setSelectedItemId(R.id.Home_nav)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Virtualwhitecane_nav -> {
                    startActivity(Intent(applicationContext, VirtualWhiteCaneModule::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.Home_nav -> true
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

                    if (!waterReminderActive && !breathReminderActive) {
                        startWaterReminder()
                        startBreathReminder()
                        waterReminderActive = true
                        breathReminderActive = true
                        textView.text = "Turn Off Reminders"
                        wellnessAwareEnabler.setBackgroundColor(Color.parseColor(("#8ED17D")))
                        textView.setTextColor((Color.parseColor("#FFFFFF")))

                    } else {
                        stopWaterReminder()
                        stopBreathReminder()
                        waterReminderActive = false
                        breathReminderActive = false
                        textView.text = "Start Reminders"
                        wellnessAwareEnabler.setBackgroundColor(Color.parseColor(("#FFFFFF")))
                        textView.setTextColor((Color.parseColor("#8692f7")))
                    }

                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

//    private fun startListening() {
//        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
//
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//        speechRecognizer.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(params: Bundle?) {}
//            override fun onBeginningOfSpeech() {}
//            override fun onRmsChanged(rmsdB: Float) {}
//            override fun onBufferReceived(buffer: ByteArray?) {}
//            override fun onEndOfSpeech() {}
//            override fun onError(error: Int) {}
//            override fun onResults(results: Bundle?) {
//                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                matches?.let {
//                    for (result in it) {
//                        Log.d("SPEECH_INPUT_CHECKING", "I'm here, Hello")
//                        if (result.contains("Helen, Police")) {
////                            startCall("tel:911") // Replace with the actual emergency number
//                            Log.d("SPEECH_INPUT_CHECKING", "I'm here, Hi")
//                            makePhoneCall()
//                        } else if (result.contains("Helen, Ambulance")) {
////                            startCall("tel:123") // Replace with the actual ambulance number
//                            makePhoneCall()
//                        } else if (result.contains("Enable Wellness-Aware Enabler")) {
//                            if (!waterReminderActive && !breathReminderActive) {
//                                startWaterReminder()
//                                startBreathReminder()
//                                waterReminderActive = true
//                                breathReminderActive = true
//                                textView.text = "Turn Off Reminders"
//                            }
//                        } else if (result.contains("Disable Wellness-Aware Enabler")) {
//                            stopWaterReminder()
//                            stopBreathReminder()
//                            waterReminderActive = false
//                            breathReminderActive = false
//                            textView.text = "Start Reminders"
//                        }
//                    }
//                }
//            }
//
//            override fun onPartialResults(partialResults: Bundle?) {}
//            override fun onEvent(eventType: Int, params: Bundle?) {}
//        })
//
//        speechRecognizer.startListening(intent)
//    }

    fun startButton() {
        speechRecognizer.startListening(intentRecognizer)
    }

    fun stopButton() {
        speechRecognizer.stopListening()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RQ_SPEECH_REC && resultCode == Activity.RESULT_OK) {

            val result_gotten = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            val result_in_String = result_gotten?.get(0).toString()

            Log.d("SPEECH_INPUT_CHECKING", result_in_String)

            if (result_in_String == "Helen, Police")
            {
                Log.d("SPEECH_INPUT_CHECKING", "I'm here, Hi")
            }


        }

    }

//    // Function to restart listening
//    private fun restartListening() {
//        speechRecognizer.cancel() // Cancel current listening
//        speechRecognizer.startListening(intent) // Start listening again
//    }

    private fun askSpeechInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Here for you")
            startActivityForResult(i, RQ_SPEECH_REC)
        }
    }

    private fun startCall(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = android.net.Uri.parse(phoneNumber)
        startActivity(callIntent)
    }

    // Check for permission and request if not granted
    fun makePhoneCall() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with making the call
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:8310106438"))
            startActivity(intent)
        } else {
            // Permission not yet granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
        }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with making the call
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:8310106438"))
                startActivity(intent)
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
            }
        }
    }

//    // Check for permission and request if not granted
//    fun makePhoneCall() {
//
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
//            // Permission already granted, proceed with making the call
//            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:8310106438"))
//            startActivity(intent)
//        } else {
//            // Permission not yet granted, request it
//            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
//        }
//    }
//
//    // Handle permission request result
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CALL_PHONE_REQUEST_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted, proceed with making the call
//                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:xxx"))
//                startActivity(intent)
//            } else {
//                // Permission denied, handle accordingly (e.g., show a message)
//            }
//        }
//    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle this case accordingly.
            }
            else
            {
                // TextToSpeech engine initialized successfully, you can start using it here
                speak("Welcome to the HelenHands Dashboard")
            }

        } else {
            // Handle this case accordingly.


        }
    }

    override fun onResume() {
        super.onResume()

        // Check if the user came back to this activity from Activity2
        if (intent.getBooleanExtra("fromForWellnessAwareEnabler", false)) {
            // Do something when the user comes back from Activity2
            // This will be executed only when the user comes back from Activity2
            // and not when the activity is initially created

//            doSomething()

            Log.d("Hi", "Hi")

            if (!waterReminderActive && !breathReminderActive) {
                startWaterReminder()
                startBreathReminder()
                waterReminderActive = true
                breathReminderActive = true
                textView.text = "Turn Off Reminders"
                wellnessAwareEnabler.setBackgroundColor(Color.parseColor(("#8ED17D")))
                textView.setTextColor((Color.parseColor("#FFFFFF")))

            } else {
                stopWaterReminder()
                stopBreathReminder()
                waterReminderActive = false
                breathReminderActive = false
                textView.text = "Start Reminders"
                wellnessAwareEnabler.setBackgroundColor(Color.parseColor(("#FFFFFF")))
                textView.setTextColor((Color.parseColor("#8692f7")))
            }

        }
    }

    private fun startWaterReminder() {
        waterHandler = Handler()
        waterHandler.postDelayed({
            showAlertDialog("Water Reminder", "It's time to drink water.")
            speak("It's time to drink water.")
            waterHandler.postDelayed({
                startWaterReminder()
            }, 10 * 60 * 1000) // 1 hour interval // 60 * 60 * 1000
        }, 0)
    }

    private fun stopWaterReminder() {
        waterHandler.removeCallbacksAndMessages(null)
    }

    private fun startBreathReminder() {
        breathHandler = Handler()
        breathHandler.postDelayed({
            showAlertDialog("Breath Reminder", "Don't forget to take a deep breath.")
            speak("Don't forget to take a deep breath.")
            breathHandler.postDelayed({
                startBreathReminder()
            }, 3 * 60 * 1000) // 30 minutes interval // 30 * 60 * 1000
        }, 1 * 60 * 1000) // Start after 15 minutes // 15 * 60 * 1000
    }

    private fun stopBreathReminder() {
        breathHandler.removeCallbacksAndMessages(null)
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun showAlertDialog(title: String, message: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
