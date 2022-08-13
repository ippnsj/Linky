package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import org.poolc.linky.databinding.ActivitySelectPathBinding

class SelectPathActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySelectPathBinding
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectPathBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // topbar 설정
            setSupportActionBar(selectPathTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            selectPathTopbarTitle.text = "경로선택"

            // folder json 파싱
            val jsonStr = assets.open("folders.json").reader().readText()

            val rootFragment = FoldersFragment()
            val bundle = Bundle()
            bundle.putString("path", currentPath)
            bundle.putString("jsonStr", jsonStr)
            rootFragment.arguments = bundle
            setFragment(true, rootFragment)
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    fun createFragment(path:String) {
        // folder json 파싱
        val jsonStr = assets.open("folders.json").reader().readText()

        val nextFragment = FoldersFragment()
        val bundle = Bundle()
        bundle.putString("path", path)
        bundle.putString("jsonStr", jsonStr)
        nextFragment.arguments = bundle
        setFragment(false, nextFragment)
    }

    fun setCurrentPath(path:String) {
        currentPath = path
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
                intent.putExtra("path", currentPath)
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}