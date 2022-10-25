package org.poolc.linky

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.poolc.linky.retrofit.client.RetrofitClient
import org.poolc.linky.retrofit.service.RetrofitService
import retrofit2.http.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MyApplication : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance: MyApplication
        lateinit var ip : String
        lateinit var port : String
        lateinit var sharedPref : SharedPreferences
        lateinit var service: RetrofitService
    }

    override fun onCreate() {
        val jsonStr = assets.open("server.json").reader().readText()
        val jsonObj = JSONObject(jsonStr)
        ip = jsonObj.getString("ip")
        port = jsonObj.getString("port")
        sharedPref = getSharedPreferences(
            getString(R.string.preference_key),
            AppCompatActivity.MODE_PRIVATE
        )
        service = RetrofitClient.retrofit.create(RetrofitService::class.java)

        super.onCreate()
    }

    fun tokenExpired() {
        Handler(applicationContext.mainLooper).post {
            Toast.makeText(applicationContext, "사용자 인증 오류로 인해 자동 로그아웃 되었습니다.", Toast.LENGTH_LONG).show()
            sharedPref.edit().remove("token").apply()
            val intent = Intent(applicationContext, LoginRegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    // Member
    fun getImageUrl(imageUrl:String) : Bitmap? {
        var image:Bitmap? = null

        try {
            val token = sharedPref!!.getString("token", "")
            val url: URL? = URL(imageUrl)
            val conn: HttpURLConnection? =
                url?.openConnection() as HttpURLConnection
            conn!!.setRequestProperty("X-AUTH-TOKEN", token)

            image = BitmapFactory.decodeStream(conn?.inputStream)
        }
        catch (e:Exception) {
            e.printStackTrace()
        }

        return image
    }
}