package com.example.projectmanager.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.projectmanager.databinding.ActivitySplashBinding
import com.example.projectmanager.firebase.FirestoreClass

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val typeFace: Typeface = Typeface.createFromAsset(assets, "Raleway-ExtraBold.ttf")
        binding.tvAppName.typeface = typeFace

        Handler(Looper.getMainLooper()).postDelayed({

            var currentUserID = FirestoreClass().getCurrentUserID()
            if(currentUserID.isNotEmpty()){
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }else{
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }}, 2500)


    }
}