package org.poolc.linky

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import org.poolc.linky.databinding.ActivityLoginRegisterBinding

class LoginRegisterActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(intent.getStringExtra("result") == "success register") {
            val toast = Toast.makeText(this, "가입이 완료되었습니다~!", Toast.LENGTH_SHORT)
            toast.show()
        }

        with(binding) {
            registerButton.setOnClickListener {
                val intent = Intent(this@LoginRegisterActivity, RegisterActivity::class.java)
                intent.putExtra("purpose", "register")
                startActivity(intent)
            }

            loginButton.setOnClickListener{
                val intent = Intent(this@LoginRegisterActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
}