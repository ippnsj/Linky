package org.poolc.linky

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.poolc.linky.databinding.ActivityMainBinding

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

            // add_linky로부터 온 intent
            Log.d("test", intent.getStringExtra("from").toString())
            if(intent.getStringExtra("from") == "add") {
                val toast = Toast.makeText(this@MainActivity, "새로운 링키가 추가되었습니다~!", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    private fun changeFragment(fragment:Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.folderFragmentContainer, fragment)
            .commit()
    }
}