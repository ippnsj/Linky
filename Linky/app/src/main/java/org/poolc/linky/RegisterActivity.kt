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
                val nickname = ""
                val profilePicture = ""
                register(email, password, nickname, profilePicture)
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

    private fun register(email:String, password:String, nickname:String, imageUrl:String) {
        val url = URL("http://${MyApplication.ip}:${MyApplication.port}/member")
        var conn : HttpURLConnection? = null

        thread {
            try {
                conn = url.openConnection() as HttpURLConnection
                conn!!.requestMethod = "POST"
                conn!!.setRequestProperty("Content-Type", "application/json")
                conn!!.setRequestProperty("Accept", "application/json")

                conn!!.doOutput = true
                conn!!.doInput = true

                val body = JSONObject()
                body.put("email", email)
                body.put("password", password)
                body.put("nickname", nickname)
                body.put("imageUrl", imageUrl)

                val os = conn!!.outputStream
                os.write(body.toString().toByteArray())
                os.flush()

                if(conn!!.responseCode == 200) {
                    val intent = Intent(this, LoginRegisterActivity::class.java)
                    intent.putExtra("result", "success register")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
            }
            catch (e:MalformedURLException) {
                Log.d("test", "올바르지 않은 URL 주소입니다.")
            } catch (e:IOException) {
                Log.d("test", "connection 오류")
            }finally {
                conn?.disconnect()
            }
        }
    }
}