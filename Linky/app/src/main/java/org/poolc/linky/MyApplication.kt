package org.poolc.linky

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

interface RetrofitService {
    // register
    @Multipart
    @POST("/member")
    fun register(
        @PartMap params: HashMap<String, RequestBody>,
        @Part multipartFile: MultipartBody.Part?
    ): Call<String>

    // edit profile
    @Multipart
    @PUT("/member/me")
    fun editProfile(
        @PartMap params: HashMap<String, RequestBody>,
        @Part newMultipartFile: MultipartBody.Part?
    ): Call<String>

    // create link
    @POST("/link")
    fun createLink(
        @Body body: RequestBody
    ): Call<String>

    // edit link
    @PUT("/link")
    fun editLink(
        @Body body: RequestBody
    ): Call<String>
}

class MyApplication : Application() {
    companion object {
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

        val retrofit = Retrofit.Builder().baseUrl("http://$ip:$port")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(RetrofitService::class.java)

        super.onCreate()
    }

    // Folder & Link
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

    fun createLink(path:String, link:Link) : Int {
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

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("isPublic", link.getIsPublic())
            body.put("keywords", link.getKeywords())
            body.put("path", path)
            body.put("name", link.getLinkTitle())
            body.put("imageUrl", link.getImgUrl())
            body.put("url", link.getUrl())

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

    fun getLinkInfo(path:String, id:String) : String {
        val email = sharedPref!!.getString("email", "")
        val paramsUrl = "email=$email&path=$path&id=$id"
        val url = URL("http://$ip:$port/link?$paramsUrl")
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

    fun editFolder(path:String, newName:String) : Int {
        val url = URL("http://$ip:$port/folder")
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

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("path", path)
            body.put("newName", newName)

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

    fun editLink(path:String, link:Link) : Int {
        val url = URL("http://$ip:$port/link")
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

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("path", path)
            body.put("id", link.getId())
            body.put("name", link.getLinkTitle())
            body.put("url", link.getUrl())
            body.put("imageUrl", link.getImgUrl())
            body.put("keywords", link.getKeywords())
            body.put("isPublic", link.getIsPublic())

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

    fun moveLink(originalPath:String, selectedLinks:ArrayList<String>, modifiedPath:String) : Int {
        val url = URL("http://$ip:$port/link/path")
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

            val ids = JSONArray(selectedLinks)

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("originalPath", originalPath)
            body.put("originalIds", ids)
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

    fun deleteLink(path:String, selectedLinks:ArrayList<String>) : Int {
        val url = URL("http://$ip:$port/link")
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

            val ids = JSONArray(selectedLinks)

            val body = JSONObject()
            body.put("email", sharedPref!!.getString("email", ""))
            body.put("path", path)
            body.put("ids", ids)

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

    // Member
    fun getProfile() : String {
        val email = sharedPref!!.getString("email", "")
        val paramsUrl = "email=$email"
        val url = URL("http://$ip:$port/member/me?$paramsUrl")
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

    fun getFollowPreview() : String {
        val email = sharedPref!!.getString("email", "")
        val paramsUrl = "email=$email"
        val url = URL("http://$ip:$port/follow/small?$paramsUrl")
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
            }else if(conn!!.responseCode == 404) {
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