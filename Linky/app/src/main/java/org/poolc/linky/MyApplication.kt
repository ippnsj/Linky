package org.poolc.linky

import android.app.Application
import org.json.JSONObject

class MyApplication : Application() {
    companion object {
        lateinit var ip : String
        lateinit var port : String
    }

    override fun onCreate() {
        val jsonStr = assets.open("server.json").reader().readText()
        val jsonObj = JSONObject(jsonStr)
        ip = jsonObj.getString("ip")
        port = jsonObj.getString("port")
        super.onCreate()
    }
}