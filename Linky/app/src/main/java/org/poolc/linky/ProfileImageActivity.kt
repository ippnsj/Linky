package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.poolc.linky.databinding.ActivityProfileImageBinding

class ProfileImageActivity : AppCompatActivity() {
    private lateinit var binding : ActivityProfileImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            val imageUrl = intent.getStringExtra("imageUrl")

            if (imageUrl != "") {

            }

            closeImage.setOnClickListener {
                finish()
            }
        }
    }
}