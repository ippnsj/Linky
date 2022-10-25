package org.poolc.linky

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import org.poolc.linky.databinding.ActivitySplashBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val token = MyApplication.sharedPref.getString("token", "")
            if(token == "") {
                var intent = Intent(this, LoginRegisterActivity::class.java)
                startActivity(intent)
            }
            else {
                val call = MyApplication.service.verifyToken()
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            var intent = Intent(this@SplashActivity, MainActivity::class.java)
                            startActivity(intent)
                        }
                        else if(response.code() != 403) {
                            val title = "서버 오류"
                            val message = "서버 문제로 인해 앱 실행이 불가합니다."
                            val listener = DialogInterface.OnDismissListener { finish() }
                            showDialog(title,message, listener)
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        val title = "서버 통신 오류"
                        val message = "서버와의 통신 문제로 앱 실행이 불가합니다.\n" +
                                "잠시후 다시 시도해주세요."
                        val listener = DialogInterface.OnDismissListener { finish() }
                        showDialog(title,message, listener)
                    }
                })
            }
        }, 1000)
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this@SplashActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }
}