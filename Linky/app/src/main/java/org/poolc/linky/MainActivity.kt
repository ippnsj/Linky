package org.poolc.linky

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.jsoup.Jsoup
import org.poolc.linky.databinding.ActivityMainBinding
import java.util.regex.Pattern
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // 공유하기로부터 온 intent 처리
            if(intent.action == Intent.ACTION_SEND && intent.type != null) {
                if(intent.type == "text/plain") {
                    val txt = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
                    val pattern = Pattern.compile("((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(txt)
                    if(matcher.find()) {
                        val url = txt.substring(matcher.start(0), matcher.end(0))
                        Log.d("test1", txt)
                        Log.d("test2", url)

                        // 메타데이터 추출
                        thread {
                            val doc = Jsoup.connect(url).get()
                            val title = doc.select("meta[property=og:title]").first()?.attr("content");
                            val description = doc.select("meta[property=og:description]").get(0).attr("content")
                            val imageUrl = doc.select("meta[property=og:image]").get(0).attr("content")
                            Log.d("test-title", title!!)
                            Log.d("test-description", description)
                            Log.d("test-imageUrl", imageUrl)
                        }
                    }
                    else {
                        Log.d("test3", "NO URL")
                    }
                }
            }
        }
    }
}