package org.poolc.linky

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject
import org.poolc.linky.databinding.ActivitySetProfileBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

class SetProfileActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySetProfileBinding
    private var image : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            register.setOnClickListener {
                val nickname = nicknameTextInput.text.toString()

                if(nickname == "") {
                    val builder = AlertDialog.Builder(this@SetProfileActivity)
                    builder.setMessage("닉네임을 입력해주세요.")
                    builder.setPositiveButton("확인", null)
                    builder.show()
                }
                else {
                    // TODO 닉네임 중복 확인

                    val email = intent.getStringExtra("email")
                    val password = intent.getStringExtra("password")
                    var imageUrl = ""

                    if (image != null) {
                        // TODO image를 url로 변환하는 api 호출
                    }

                    register(email!!, password!!, nickname, imageUrl)
                }
            }

            nicknameTextInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    register.performClick()
                    true
                }
                false
            }
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
            catch (e: MalformedURLException) {
                Log.d("test", "올바르지 않은 URL 주소입니다.")
            } catch (e: IOException) {
                Log.d("test", "connection 오류")
            }finally {
                conn?.disconnect()
            }
        }
    }
}