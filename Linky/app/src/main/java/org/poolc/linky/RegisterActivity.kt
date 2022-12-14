package org.poolc.linky

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.*
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import org.poolc.linky.databinding.ActivityRegisterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

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
                    termsAgree.visibility = View.VISIBLE
                    space2.visibility = View.VISIBLE
                    doneButton.text = "프로필 설정하기"

                    doneButton.setOnClickListener {
                        val email = emailTextInput.text.toString().trim()
                        val password = passwordTextInput.text.toString()
                        val passwordCheck = passwordCheckTextInput.text.toString()
                        val title = "에러메세지"

                        if(verifyEmailAddress(email) && verifyPassword(password) && password == passwordCheck) {
                            if(termsAgree.isSelected) {
                                goToSetProfile(email, password)
                            }
                            else {
                                val message = "개인정보처리방침 및 서비스 이용약관에 동의해주세요."
                                showDialog(title, message, null)
                            }
                        }
                        else {
                            val message = "입력값에 에러가 존재합니다."
                            showDialog(title, message, null)
                        }
                    }

                    terms.setOnClickListener {
                        val intent = Intent(this@RegisterActivity, TermsActivity::class.java)
                        startActivity(intent)
                    }

                    termsAgree.setOnClickListener {
                        termsAgree.isSelected = !termsAgree.isSelected
                    }
                }
                "reset" -> {
                    terms.visibility = View.GONE
                    termsAgree.visibility = View.GONE
                    space2.visibility = View.GONE
                    doneButton.text = "비밀번호 재설정"

                    doneButton.setOnClickListener {
                        val email = emailTextInput.text.toString().trim()
                        val password = passwordTextInput.text.toString()
                        val passwordCheck = passwordCheckTextInput.text.toString()
                        val title = "에러메세지"

                        if(verifyEmailAddress(email) && verifyPassword(password) && password == passwordCheck) {
                            // TODO 비밀번호 변경
                        }
                        else {
                            val message = "입력값에 에러가 존재합니다."
                            showDialog(title, message, null)
                        }
                    }
                }
            }

            emailTextInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_NEXT) {
                    verifyEmailAddress(v.text.toString().trim())
                }
                false
            }

            passwordTextInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_NEXT) {
                    verifyPassword(v.text.toString())
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

            passwordCheckTextInput.filters = arrayOf(InputFilter { charSequence: CharSequence, i: Int, i1: Int, spanned: Spanned, i2: Int, i3: Int ->
                val ps = Pattern.compile("^[a-zA-Z0-9!@#$%^&*]+$")
                if(!ps.matcher(charSequence).matches()) ""
                else charSequence
            }, InputFilter.LengthFilter(15))

            passwordCheckTextInput.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val password = passwordTextInput.text.toString()
                    val passwordCheck = passwordCheckTextInput.text.toString()
                    if(password == passwordCheck) {
                        passwordCheckLayout.error = null
                    }
                    else {
                        passwordCheckLayout.error = "비밀번호가 일치하지 않습니다."
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
        }
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this@RegisterActivity)
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
                binding.passwordTextInput.nextFocusDownId = R.id.terms
                return true
            }
        }
    }

    private fun goToSetProfile(email:String, password:String) {
        val call = MyApplication.service.verifyEmail(email)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(
                call: Call<Void>,
                response: Response<Void>
            ) {
                if (response.isSuccessful) {
                    val intent = Intent(
                        this@RegisterActivity,
                        SetProfileActivity::class.java
                    )
                    intent.putExtra("email", email)
                    intent.putExtra("password", password)
                    startActivity(intent)
                } else {
                    val title = "이메일 중복"
                    val message = "이미 존재하는 이메일입니다."
                    showDialog(title, message, null)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                val title = "이메일 중복 확인 실패"
                val message = "서버와의 통신 문제로 인해 이메일 중복 확인에 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }
}