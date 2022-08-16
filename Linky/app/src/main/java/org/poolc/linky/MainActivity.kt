package org.poolc.linky

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.poolc.linky.databinding.ActivityMainBinding

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
                            // folder json 파싱
                            val jsonStr = assets.open("folders.json").reader().readText()
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

            // add_linky로부터 온 intent
            if(intent.getStringExtra("from") == "add") {
                val toast = Toast.makeText(this@MainActivity, "새로운 링키가 추가되었습니다~!", Toast.LENGTH_SHORT)
                toast.show()
            }
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