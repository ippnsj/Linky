package org.poolc.linky

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import org.jsoup.Jsoup
import org.poolc.linky.databinding.ActivityAddLinkyBinding
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class AddLinkyActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAddLinkyBinding
    private var folders = arrayOf("선택안함", "음식", "고양이", "직접입력")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLinkyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // topbar 설정
            setSupportActionBar(addLinkyTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            addLinkyTopbarTitle.text = "링크 추가하기"

            // spinner 설정
            val spinnerAdapter = ArrayAdapter(this@AddLinkyActivity, android.R.layout.simple_spinner_item, folders)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            folderSpinner.adapter = spinnerAdapter
            folderSpinner.onItemSelectedListener = spinnerListener
            folderTextInput.isEnabled = false

            // 공유하기로부터 온 intent 처리
            if(intent.action == Intent.ACTION_SEND && intent.type != null) {
                if(intent.type == "text/plain") {
                    veil.visibility = View.VISIBLE
                    val txt = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
                    val pattern = Pattern.compile("((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(txt)
                    if(matcher.find()) {
                        val url = txt.substring(matcher.start(0), matcher.end(0))

                        // 메타데이터 추출
                        thread {
                            val doc = Jsoup.connect(url).get()
                            val title =
                                doc.select("meta[property=og:title]").first()?.attr("content");
                            val image =
                                doc.select("meta[property=og:image]").get(0).attr("content")
                            Log.d("test-title", title!!)
                            Log.d("test-imageUrl", image)

                            val imageUrl = URL(image)
                            val conn = imageUrl.openConnection() as HttpURLConnection
                            val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                            var resizedBitmap:Bitmap? = null
                            if(bitmap != null) {
                                // resizedBitmap = resizeBitmap(1024, bitmap)
                                resizedBitmap = resizeBitmap(resources.displayMetrics.widthPixels - 100, bitmap)
                            }

                            runOnUiThread {
                                veil.visibility = View.INVISIBLE

                                // 링크주소
                                linkAddressTextInput.setText(url)
                                linkAddressTextInput.isEnabled = false

                                // 제목
                                titleTextInput.setText(title ?: "")

                                // 대표이미지
                                if(resizedBitmap != null) {
                                    linkImage.setImageBitmap(resizedBitmap)
                                }
                            }
                        }
                    }
                    else {
                        Log.d("test", "NO URL")
                    }
                }
            }
        }
    }

    // 사진의 사이즈를 조정하는 메서드
    fun resizeBitmap(targetWidth:Int, img: Bitmap) : Bitmap {
        // 이미지의 비율을 계산한다.
        val ratio = targetWidth.toDouble() / img.width.toDouble()
        // 보정될 세로 길이를 구한다.
        val targetHeight = (img.height * ratio).toInt()
        // 크기를 조정한 bitmap 객체를 생성한다.
        val result = Bitmap.createScaledBitmap(img, targetWidth, targetHeight, false)
        return result
    }

    val spinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            when(parent?.id) {
                R.id.folderSpinner -> {
                    if(position == folders.size - 1) {
                        binding.folderTextInput.setText("")
                        binding.folderTextInput.isEnabled = true
                    }
                    else {
                        binding.folderTextInput.setText(folders[position])
                        binding.folderTextInput.isEnabled = false
                    }
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_linky_topbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId) {
            R.id.add_link -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }
}