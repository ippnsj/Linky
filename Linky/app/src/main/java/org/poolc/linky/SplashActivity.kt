package org.poolc.linky

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.poolc.linky.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val userEmail = MyApplication.sharedPref.getString("userEmail", "")
            if(userEmail == "") {
                var intent = Intent(this, LoginRegisterActivity::class.java)
                startActivity(intent)
            }
            else {
                var intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }, 1000)
    }
}