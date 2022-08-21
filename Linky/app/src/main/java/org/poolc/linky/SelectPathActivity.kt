package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import org.json.JSONObject
import org.poolc.linky.databinding.ActivitySelectPathBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

class SelectPathActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySelectPathBinding
    private var path = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectPathBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // topbar 설정
            setSupportActionBar(selectPathTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            selectPathTopbarTitle.text = "경로선택"

            // json 가져오기
            val jsonStr = readFolder()

            val rootFragment = FolderListFragment()
            val bundle = Bundle()
            bundle.putString("path", path)
            bundle.putString("jsonStr", jsonStr)
            rootFragment.arguments = bundle
            setFragment(true, rootFragment)
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    private fun readFolder() : String {
        val sharedPref = getSharedPreferences(getString(R.string.preference_key), MODE_PRIVATE)
        val url = URL("http://${MyApplication.ip}:${MyApplication.port}/folder/readFolder")
        var conn : HttpURLConnection? = null
        var response : String = ""

        thread {
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
        }

        return response
    }

    fun createFragment(path:String) {
        // json 가져오기
        val jsonStr = readFolder()

        val nextFragment = FolderListFragment()
        val bundle = Bundle()
        bundle.putString("path", path)
        bundle.putString("jsonStr", jsonStr)
        nextFragment.arguments = bundle
        setFragment(false, nextFragment)
    }

    fun setCurrentPath(path:String) {
        this.path = path
    }

    private fun setFragment(isRoot:Boolean, fragment:Fragment) {
        val tran = supportFragmentManager.beginTransaction()
        tran.replace(R.id.folderFragmentContainer, fragment)

        if(!isRoot) {
            tran.addToBackStack(null)
        }

        tran.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_select_path_topbar, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.cancel -> {
                finish()
            }
            R.id.done -> {
                intent.putExtra("path", path)
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}