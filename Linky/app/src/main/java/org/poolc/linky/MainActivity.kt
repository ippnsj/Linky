package org.poolc.linky

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.jsoup.Jsoup
import org.poolc.linky.databinding.ActivityMainBinding
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // topbar 설정
            setSupportActionBar(topbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            topbarTitle.text = "내 링키"
            topbarFoldername.visibility = View.INVISIBLE

            // bottom navigatonbar 설정
            bottomNavigation.itemIconTintList = null
            bottomNavigation.setOnItemSelectedListener { item ->
                changeFragment(
                    when(item.itemId) {
                        R.id.linky -> {
                            topbarTitle.text = "내 링키"
                            LinkyFragment()
                        }
                        R.id.search -> {
                            topbarTitle.text = "둘러보기"
                            SearchFragment()
                        }
                        R.id.more -> {
                            topbarTitle.text = "설정"
                            MoreFragment()
                        }
                        else -> {
                            topbarTitle.text = "내 링키"
                            LinkyFragment()
                        }
                    }
                )
                true
            }
            bottomNavigation.selectedItemId = R.id.linky
        }
    }

    private fun changeFragment(fragment:Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}