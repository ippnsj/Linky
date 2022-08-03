package org.poolc.linky

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.poolc.linky.databinding.ActivityLoginRegisterBinding

class LoginRegisterActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            loginButton.setOnClickListener{
                val intent = Intent(this@LoginRegisterActivity, LoginActivity::class.java)
                startActivity(intent)
            }

            registerButton.setOnClickListener {
                val intent = Intent(this@LoginRegisterActivity, RegisterActivity::class.java)
                intent.putExtra("purpose", "register")
                startActivity(intent)
            }
        }
    }
}