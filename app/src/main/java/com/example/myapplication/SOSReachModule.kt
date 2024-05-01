package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class SOSReachModule : AppCompatActivity() {

    lateinit var policeButton: Button
    lateinit var ambulanceButton: Button

    private val CALL_PHONE_REQUEST_CODE = 123 // Or any request code you prefer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sos_reach)

        policeButton = findViewById(R.id.policeButton)
        ambulanceButton = findViewById(R.id.ambulanceButton)

        policeButton.setOnClickListener{
            startPoliceCall()
        }

        ambulanceButton.setOnClickListener {
            startAmbulanceCall()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setSelectedItemId(R.id.sos_nav)

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
                R.id.LetsChat_nav -> {
                    startActivity(Intent(applicationContext, LetsChatModule::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.sos_nav -> true
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

//
    }

    fun startPoliceCall() {
//        startCall("tel:911") // Replace with the actual emergency number
        makePhoneCall()
    }

    fun startAmbulanceCall() {
//        startCall("tel:123") // Replace with the actual ambulance number
        makePhoneCall()
    }

    private fun startCall(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse(phoneNumber)
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
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:xxx"))
                startActivity(intent)
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
            }
        }
    }
}