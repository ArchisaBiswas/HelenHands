package com.example.myapplication

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var topLogo: ImageView
    private lateinit var appName: ImageView
    private lateinit var backSplashImage: ImageView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen_layout)

        topLogo = findViewById(R.id.logo)
        appName = findViewById(R.id.app_name)
        backSplashImage = findViewById(R.id.img)
        lottieAnimationView = findViewById(R.id.lottie)

        mediaPlayer = MediaPlayer.create(this, R.raw.splash_audio)
        mediaPlayer.start()

        val startDelay = 2000 // Adjust the start delay to your preference

        backSplashImage.animate().translationY(-1600f).setDuration(3000).setStartDelay(startDelay.toLong())
        topLogo.animate().translationY(1400f).setDuration(3000).setStartDelay(startDelay.toLong())
        appName.animate().translationY(1400f).setDuration(3000).setStartDelay(startDelay.toLong())
        lottieAnimationView.animate().translationY(1400f).setDuration(3000).setStartDelay(startDelay.toLong())

        handler = Handler()
        handler.postDelayed({
            val intent = Intent(this@SplashScreenActivity, Dashboard::class.java)
            startActivity(intent)
            finish()
        }, 5000)

        // Use a Handler to stop audio playback after a delay
        /*Handler().postDelayed({
            mediaPlayer.stop()
            mediaPlayer.release()
        }, 5000) // Delay for 5000 milliseconds (5 seconds)*/
    }
}