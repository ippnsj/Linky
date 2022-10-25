package org.poolc.linky

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import org.poolc.linky.databinding.ActivityProfileImageBinding
import kotlin.concurrent.thread

class ProfileImageActivity : AppCompatActivity() {
    private lateinit var binding : ActivityProfileImageBinding
    private lateinit var app:MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MyApplication

        with(binding) {
            val imageUrl = intent.getStringExtra("imageUrl") ?: ""
            val uri = intent.getParcelableExtra<Uri>("uri")

            if(uri != null) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 안드로이드 10 버전부터
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source)
                    profileImageZoomin.setImageBitmap(bitmap)
                }
                else {
                    // 안드로이드 9 버전까지
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    if(cursor != null) {
                        cursor.moveToNext()
                        // 이미지 경로를 가져온다.
                        val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        val source = cursor.getString(index)
                        // 이미지를 생성한다.
                        val bitmap = BitmapFactory.decodeFile(source)
                        profileImageZoomin.setImageBitmap(bitmap)
                    }
                }
            } else if (imageUrl != "") {
                thread {
                    val image = app.getImageUrl(imageUrl)
                    if(image != null) {
                        runOnUiThread {
                            profileImageZoomin.setImageBitmap(image)
                        }
                    }
                    else {
                        runOnUiThread {
                            profileImageZoomin.setImageResource(R.drawable.profile)
                        }
                    }
                }
            }
            else {
                profileImageZoomin.setImageResource(R.drawable.profile)
            }

            closeImage.setOnClickListener {
                finish()
            }
        }
    }
}