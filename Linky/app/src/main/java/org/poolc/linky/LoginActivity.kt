package org.poolc.linky

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.gson.JsonElement
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import org.poolc.linky.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

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

            emailTextInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_NEXT) {
                    verifyEmailAddress(v.text.toString().trim())
                }
                false
            }

            passwordTextInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_NEXT) {
                    if(verifyPassword(v.text.toString())) {
                        login()
                    }
                    true
                }
                false
            }

            passwordTextInput.filters = arrayOf(InputFilter { charSequence: CharSequence, i: Int, i1: Int, spanned: Spanned, i2: Int, i3: Int ->
                val ps = Pattern.compile("^[a-zA-Z0-9!@#$%^&*]+$")
                if(!ps.matcher(charSequence).matches()) ""
                else charSequence
            }, InputFilter.LengthFilter(15))

            passwordTextInput.doAfterTextChanged {
                passwordLayout.error = null
            }

            loginButton.setOnClickListener {
                login()
            }
        }
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    private fun verifyEmailAddress(email:String) : Boolean {
        if(email.isEmpty()) {
            binding.emailTextInput.nextFocusForwardId = R.id.emailTextInput
            binding.emailTextInput.error = "이메일 주소를 입력해주세요."
            return false
        }
        else {
            val pattern = Patterns.EMAIL_ADDRESS
            if (pattern.matcher(email).matches()) {
                binding.emailTextInput.nextFocusForwardId = R.id.passwordTextInput
                return true
            } else {
                binding.emailTextInput.nextFocusForwardId = R.id.emailTextInput
                binding.emailTextInput.error = "이메일 형식이 올바르지 않습니다."
                return false
            }
        }
    }

    private fun verifyPassword(password:String) : Boolean {
        if(password.isEmpty()) {
            binding.passwordTextInput.nextFocusForwardId = R.id.passwordTextInput
            binding.passwordLayout.error = "비밀번호를 입력해주세요."
            return false
        }
        else {
            if(password.length < 6) {
                binding.passwordTextInput.nextFocusForwardId = R.id.passwordTextInput
                binding.passwordLayout.error = "비밀번호는 6자리 이상이어야 합니다."
                return false
            }
            else{
                binding.passwordTextInput.nextFocusDownId = R.id.loginButton
                return true
            }
        }
    }

    private fun login() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        if(verifyEmailAddress(binding.emailTextInput.text.toString()) && verifyPassword(binding.passwordTextInput.text.toString())) {
            val email = binding.emailTextInput.text.toString()
            val password = binding.passwordTextInput.text.toString()

            val jsonObj = JSONObject()
            jsonObj.put("email", email)
            jsonObj.put("password", password)
            val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

            val call = MyApplication.service.login(body)

            call.enqueue(object:Callback<JsonElement> {
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    if(response.isSuccessful) {
                        val responseJson = response.body()!!.asJsonObject

                        if(!responseJson.isJsonNull) {
                            MyApplication.sharedPref.edit()
                                .putString("token", responseJson.get("token").asString).apply()

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("from", "login")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        else {
                            val title = "로그인 실패"
                            val message = "서버 문제로 인해 로그인에 실패하였습니다."
                            showDialog(title, message, null)
                        }
                    }
                    else {
                        val title = "로그인 실패"
                        var message = "서버 문제로 인해 로그인에 실패하였습니다."

                        when(response.code()) {
                            400 -> {
                                message = "이메일 또는 비밀번호 형식이 잘못되었습니다."
                            }
                            404 -> {
                                message = "이메일 또는 비밀번호가 잘못되었습니다.\n" +
                                        "다시 확인 후 로그인 해주세요."
                            }
                            409 -> {
                                message = "전송된 인증 이메일 확인 후, 로그인해주세요."
                            }
                        }

                        showDialog(title, message, null)
                    }
                }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    val title = "로그인 실패"
                    val message = "서버와의 통신 문제로 로그인에 실패하였습니다.\n" +
                            "잠시후 다시 시도해주세요."
                    showDialog(title, message, null)
                }
            })
        }
        else {
            val title = "로그인 실패"
            val message = "입력값에 에러가 존재합니다.\n" +
                    "확인 후 다시 입력해주세요."
            showDialog(title, message, null)
        }
    }
}