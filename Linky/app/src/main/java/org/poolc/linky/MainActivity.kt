package org.poolc.linky

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.poolc.linky.databinding.ActivityMainBinding
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private var path = ""
    private var folderName = ""
    private var now = ""
    private lateinit var fm : FragmentManager
    private val linkyFragment = LinkyFragment()
    private val searchFragment = SearchFragment()
    private val moreFragment = MoreFragment()
    private var newb = true

    private lateinit var app : MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fm = supportFragmentManager
        app = application as MyApplication

        with(binding) {
            // topbar 설정
            setSupportActionBar(topbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            topbarTitle.text = "내 링키"

            // bottom navigatonbar 설정
            var bundle : Bundle? = null
            bottomNavigation.itemIconTintList = null
            bottomNavigation.setOnItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.linky -> {
                        if (now != "linky" && now != "sub") {
                            // json 가져오기
                            var jsonStr = ""
                            topbarTitle.text = "내 링키"
                            if (path == "") {
                                now = "linky"

                                thread {
                                    jsonStr = app.readFolder(path)

                                    bundle = Bundle()
                                    bundle?.putString("path", path)
                                    bundle?.putString("folderName", folderName)
                                    bundle?.putString("jsonStr", jsonStr)
                                    changeFragment(linkyFragment, bundle, false)
                                }
                            } else {
                                val fragment = fm.findFragmentByTag(path)
                                now = "sub"

                                thread {
                                    jsonStr = app.read(path)

                                    bundle = Bundle()
                                    bundle?.putString("path", path)
                                    bundle?.putString("folderName", folderName)
                                    bundle?.putString("jsonStr", jsonStr)
                                    changeFragment(fragment!!, bundle, false)
                                }
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

    override fun onStart() {
        super.onStart()
        if(!newb) {
            if (now == "linky" || now == "sub") {
                var bundle: Bundle? = null
                // json 가져오기
                var jsonStr = ""
                if (path == "") {
                    thread {
                        jsonStr = app.readFolder(path)

                        runOnUiThread {
                            linkyFragment.update(jsonStr)
                        }
                    }
                } else {
                    val fragment = fm.findFragmentByTag(path) as LinkySubFragment
                    thread {
                        jsonStr = app.read(path)

                        runOnUiThread {
                            fragment.update(jsonStr)
                        }
                    }
                }
            }
        }
        else {
            newb = false
        }
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

    fun setFolderName(folderName:String) {
        this.folderName = folderName
        if(folderName == "") {
            binding.topbarFoldername.visibility = View.INVISIBLE
        }
        else {
            binding.topbarFoldername.visibility = View.VISIBLE
            binding.topbarFoldername.text = folderName
        }
    }

    fun createFragment(path:String, folderName:String) {
        // folder json 파싱
        var jsonStr = ""
        thread {
            jsonStr = app.read(path)

            val nextFragment = LinkySubFragment()
            val bundle = Bundle()
            bundle.putString("path", path)
            bundle.putString("folderName", folderName)
            bundle.putString("jsonStr", jsonStr)
            this.path = path
            this.folderName = folderName
            now = "sub"
            changeFragment(nextFragment, bundle, true)
        }
    }
}