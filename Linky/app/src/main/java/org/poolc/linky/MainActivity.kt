package org.poolc.linky

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.json.JSONObject
import org.poolc.linky.databinding.ActivityMainBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private var path = ""
    private var now = ""
    private lateinit var fm : FragmentManager
    private val linkyFragment = LinkyFragment()
    private val searchFragment = SearchFragment()
    private val moreFragment = MoreFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fm = supportFragmentManager

        with(binding) {
            // topbar 설정
            setSupportActionBar(topbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            topbarTitle.text = "내 링키"
            topbarFoldername.visibility = View.INVISIBLE

            // bottom navigatonbar 설정
            var bundle : Bundle? = null
            bottomNavigation.itemIconTintList = null
            bottomNavigation.setOnItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.linky -> {
                        if (now != "linky" && now != "sub") {
                            // json 가져오기
                            val jsonStr = readLink()
                            bundle = Bundle()
                            bundle?.putString("path", path)
                            bundle?.putString("jsonStr", jsonStr)

                            topbarTitle.text = "내 링키"
                            if (path == "") {
                                now = "linky"
                                changeFragment(linkyFragment, bundle, false)
                            } else {
                                val fragment = fm.findFragmentByTag(path)
                                now = "sub"
                                changeFragment(fragment!!, bundle, false)
                            }
                        }
                    }
                    R.id.search -> {
                        if(now != "search") {
                            topbarTitle.text = "둘러보기"
                            now = "search"
                            changeFragment(searchFragment, bundle, false)
                        }
                    }
                    R.id.more -> {
                        if(now != "more") {
                            topbarTitle.text = "설정"
                            now = "more"
                            changeFragment(moreFragment, bundle, false)
                        }
                    }
                }
                true
            }
            bottomNavigation.selectedItemId = R.id.linky

            // intent 처리
            when(intent.getStringExtra("from")) {
                "add" -> {
                    val toast = Toast.makeText(this@MainActivity, "새로운 링키가 추가되었습니다~!", Toast.LENGTH_SHORT)
                    toast.show()
                }
                "login" -> {
                    val toast = Toast.makeText(this@MainActivity, "로그인되었습니다~!", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }

    private fun readLink() : String {
        val sharedPref = getSharedPreferences(getString(R.string.preference_key), MODE_PRIVATE)
        val url = URL("http://${MyApplication.ip}:${MyApplication.port}/folder/readLink")
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

    private fun changeFragment(fragment:Fragment, bundle: Bundle?, addToStack:Boolean) {
        val tran = fm.beginTransaction()

        if(bundle != null) {
            fragment.arguments = bundle
        }

        if(addToStack) {
            if(now == "sub") {
                tran.replace(R.id.folderFragmentContainer, fragment, path)
                tran.addToBackStack(null)
            }
        }
        else {
            tran.replace(R.id.folderFragmentContainer, fragment)
        }

        tran.commit()
    }

    fun setPath(path:String) {
        this.path = path
    }

    fun createFragment(path:String) {
        // folder json 파싱
        val jsonStr = assets.open("folders.json").reader().readText()

        val nextFragment = LinkySubFragment()
        val bundle = Bundle()
        bundle.putString("path", path)
        bundle.putString("jsonStr", jsonStr)
        this.path = path
        now = "sub"
        changeFragment(nextFragment, bundle, true)
    }
}