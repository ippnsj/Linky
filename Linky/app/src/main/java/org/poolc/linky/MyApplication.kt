package org.poolc.linky

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

class MyApplication : Application() {
    companion object {
        lateinit var ip : String
        lateinit var port : String
        lateinit var sharedPref : SharedPreferences
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
        super.onCreate()
    }

    fun createFolder(folderName:String, path:String) : Int {
        val url = URL("http://$ip:$port/folder/create")
        var conn : HttpURLConnection? = null
        var responseCode = -1

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "POST"
            conn!!.connectTimeout = 10000;
            conn!!.readTimeout = 100000;
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true

            val body = JSONObject()
            body.put("userEmail", sharedPref!!.getString("userEmail", ""))
            body.put("path", path)
            body.put("folderName", folderName)

            val os = conn!!.outputStream
            os.write(body.toString().toByteArray())
            os.flush()

            responseCode =  conn!!.responseCode
        } catch (e: MalformedURLException) {
            Log.d("test", "올바르지 않은 URL 주소입니다.")
        } catch (e: IOException) {
            Log.d("test", "connection 오류")
        } finally {
            conn?.disconnect()
        }

        return responseCode
    }

    fun createLink(public:Boolean, keywords:ArrayList<String>, path:String, linkTitle:String, linkImage:String, linkUrl:String) : Int {
        val url = URL("http://$ip:$port/link/create")
        var conn : HttpURLConnection? = null
        var responseCode = -1

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "POST"
            conn!!.connectTimeout = 10000;
            conn!!.readTimeout = 100000;
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true

            val keywordsJsonArr = JSONArray(keywords)

            val body = JSONObject()
            body.put("userEmail", sharedPref!!.getString("userEmail", ""))
            body.put("isPublic", public)
            body.put("keywords", keywordsJsonArr)
            body.put("path", path)
            body.put("linkTitle", linkTitle)
            body.put("linkImage", linkImage)
            body.put("linkUrl", linkUrl)

            val os = conn!!.outputStream
            os.write(body.toString().toByteArray())
            os.flush()

            responseCode =  conn!!.responseCode
        } catch (e: MalformedURLException) {
            Log.d("test", "올바르지 않은 URL 주소입니다.")
        } catch (e: IOException) {
            Log.d("test", "connection 오류")
        } finally {
            conn?.disconnect()
        }

        return responseCode
    }

    fun readFolder(path:String) : String {
        val url = URL("http://$ip:$port/folder/readFolder")
        var conn : HttpURLConnection? = null
        var response : String = ""

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "POST"
            conn!!.connectTimeout = 10000;
            conn!!.readTimeout = 100000;
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true
            conn!!.doInput = true

            val body = JSONObject()
            body.put("userEmail", sharedPref!!.getString("userEmail", ""))
            body.put("path", path)

            val os = conn!!.outputStream
            os.write(body.toString().toByteArray())
            os.flush()

            if(conn!!.responseCode == 200) {
                response = conn!!.inputStream.reader().readText()
            }
            else if(conn!!.responseCode == 400) {
                Log.d("test", "Bad request")
            }
            else if(conn!!.responseCode == 404) {
                Log.d("test", "Not Found")
            }
            else if(conn!!.responseCode == 401) {
                Log.d("test", "Unauthorized")
            }
        }
        catch (e: MalformedURLException) {
            Log.d("test", "올바르지 않은 URL 주소입니다.")
        } catch (e: IOException) {
            Log.d("test", "connection 오류")
        }finally {
            conn?.disconnect()
        }

        return response
    }

    fun read(path:String) : String {
        val url = URL("http://$ip:$port/folder/read")
        var conn : HttpURLConnection? = null
        var response : String = ""

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "POST"
            conn!!.connectTimeout = 10000;
            conn!!.readTimeout = 100000;
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true
            conn!!.doInput = true

            val body = JSONObject()
            body.put("userEmail", sharedPref.getString("userEmail", ""))
            body.put("path", path)

            val os = conn!!.outputStream
            os.write(body.toString().toByteArray())
            os.flush()

            if(conn!!.responseCode == 200) {
                response = conn!!.inputStream.reader().readText()
            }
            else if(conn!!.responseCode == 400) {
                Log.d("test", "Bad request")
            }
            else if(conn!!.responseCode == 404) {
                Log.d("test", "Not Found")
            }
            else if(conn!!.responseCode == 401) {
                Log.d("test", "Unauthorized")
            }
        }
        catch (e: MalformedURLException) {
            Log.d("test", "올바르지 않은 URL 주소입니다.")
        } catch (e: IOException) {
            Log.d("test", "connection 오류")
        }finally {
            conn?.disconnect()
        }

        return response
    }
}