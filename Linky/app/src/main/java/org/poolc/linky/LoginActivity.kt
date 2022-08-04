package org.poolc.linky

import android.content.Intent
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import org.poolc.linky.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // 비밀번호 재설정 도움글 색상 바꾸기
            val str = passwordReset.text.toString()
            val ssb = SpannableStringBuilder(str)
            val word = "재설정하기"
            val start = str.indexOf(word)
            val end = start + word.length

            ssb.apply {
                setSpan(ForegroundColorSpan(Color.rgb(25, 132, 126)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            passwordReset.text = ssb

            // 비밀번호 재설정 화면으로 이동
            passwordReset.setOnClickListener {
                val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                intent.putExtra("purpose", "reset")
                startActivity(intent)
            }
        }
    }
}