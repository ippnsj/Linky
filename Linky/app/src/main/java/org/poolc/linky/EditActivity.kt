package org.poolc.linky

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.poolc.linky.databinding.ActivityEditBinding
import kotlin.concurrent.thread


class EditActivity : AppCompatActivity() {
    private lateinit var binding : ActivityEditBinding
    private lateinit var path : String

    private lateinit var app : MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MyApplication

        setFragment()

        with(binding) {
            // topbar 설정
            setSupportActionBar(editTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            editTopbarTitle.text = "편집"

            // bottom navigatonbar 설정
            bottomNavigation.itemIconTintList = null
            bottomNavigation.setOnItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.move -> {

                    }
                    R.id.delete -> {

                    }
                }
                true
            }
        }
    }

    private fun createFragment(fragment: Fragment, bundle: Bundle?) {
        if(bundle != null) {
            fragment.arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.folderFragmentContainer, fragment)
            .commit()
    }

    private fun setFragment() {
        path = intent.getStringExtra("path") ?: ""
        var jsonStr = ""
        var bundle : Bundle? = null

        with(binding) {
            if(path == "") {
                thread {
                    jsonStr = app.readFolder(path)

                    bundle = Bundle()
                    bundle?.putString("path", path)
                    bundle?.putString("jsonStr", jsonStr)
                    createFragment(EditLinkyFragment(), bundle)
                }
            }
            else {
                thread {
                    jsonStr = app.read(path)

                    bundle = Bundle()
                    bundle?.putString("path", path)
                    bundle?.putString("jsonStr", jsonStr)
                    createFragment(LinkySubFragment(), bundle)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_linky_topbar, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}