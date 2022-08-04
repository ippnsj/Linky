package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import org.poolc.linky.databinding.ActivityRegisterBinding

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
        }
    }
}