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

    fun createFolder(name:String, path:String) : Int {
        val url = URL("http://$ip:$port/folder")
        var conn : HttpURLConnection? = null
        var responseCode = -1

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "POST"
            conn!!.connectTimeout = 10000
            conn!!.readTimeout = 100000
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("path", path)
            body.put("name", name)

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

    fun createLink(public:Boolean, keywords:ArrayList<String>, path:String, name:String, imageUrl:String, linkUrl:String) : Int {
        val url = URL("http://$ip:$port/link")
        var conn : HttpURLConnection? = null
        var responseCode = -1

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "POST"
            conn!!.connectTimeout = 10000
            conn!!.readTimeout = 100000
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true

            val keywordsJsonArr = JSONArray(keywords)

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("isPublic", public)
            body.put("keywords", keywordsJsonArr)
            body.put("path", path)
            body.put("name", name)
            body.put("imageUrl", imageUrl)
            body.put("url", linkUrl)

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

    fun read(path:String, showLink:Boolean) : String {
        val email = sharedPref!!.getString("email", "")
        val paramsUrl = "email=$email&path=$path&showLink=$showLink"
        val url = URL("http://$ip:$port/folder?$paramsUrl")
        var conn : HttpURLConnection? = null
        var response : String = ""

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "GET"
            conn!!.connectTimeout = 10000
            conn!!.readTimeout = 100000
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doInput = true

            if(conn!!.responseCode == 200) {
                response = conn!!.inputStream.reader().readText()
            }
            else if(conn!!.responseCode == 400) {
                Log.d("test", "Bad request")
            }
            else if(conn!!.responseCode == 401) {
                Log.d("test", "Unauthorized")
            }
            else if(conn!!.responseCode == 404) {
                Log.d("test", "Not Found")
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

    fun moveFolder(originalPath:ArrayList<String>, modifiedPath:String) : Int {
        val url = URL("http://$ip:$port/folder/path")
        var conn : HttpURLConnection? = null
        var responseCode = -1

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "PUT"
            conn!!.connectTimeout = 10000
            conn!!.readTimeout = 100000
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true

            val originalPathJsonArr = JSONArray(originalPath)

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("originalPaths", originalPathJsonArr)
            body.put("modifiedPath", modifiedPath)

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

    fun deleteFolder(selectedFolders:ArrayList<String>) : Int {
        val url = URL("http://$ip:$port/folder")
        var conn : HttpURLConnection? = null
        var responseCode = -1

        try {
            conn = url.openConnection() as HttpURLConnection
            conn!!.requestMethod = "DELETE"
            conn!!.connectTimeout = 10000
            conn!!.readTimeout = 100000
            conn!!.setRequestProperty("Content-Type", "application/json")
            conn!!.setRequestProperty("Accept", "application/json")

            conn!!.doOutput = true

            val paths = JSONArray(selectedFolders)

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("paths", paths)

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
}