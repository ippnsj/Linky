package org.poolc.linky

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.ContentInfoCompat
import org.json.JSONObject
import org.poolc.linky.databinding.ActivityRegisterBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // purpose에 따라 다르게 setting
            when(intent.getStringExtra("purpose")) {
                "register" -> {
                    terms.visibility = View.VISIBLE
                    doneButton.text = "가입하기"
                }
                "reset" -> {
                    terms.visibility = View.INVISIBLE
                    doneButton.text = "비밀번호 재설정"
                }
            }

            emailTextInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    sendEmail(v.text.toString())
                    true
                }
                false
            }

            doneButton.setOnClickListener {
                val email = emailTextInput.text.toString()
                val password = passwordTextInput.text.toString()

                val intent = Intent(this@RegisterActivity, SetProfileActivity::class.java)
                intent.putExtra("email", email)
                intent.putExtra("password", password)
                startActivity(intent)
            }
        }
    }

    private fun sendEmail(email:String) {
        val pattern = Patterns.EMAIL_ADDRESS
        if(pattern.matcher(email).matches()) {
            // 이메일 전송
            val toast = Toast.makeText(this, "이메일이 전송되었습니다.", Toast.LENGTH_SHORT)
            toast.show()
        }else {
            binding.emailTextInput.error = "이메일 형식이 올바르지 않습니다."
        }
    }
}