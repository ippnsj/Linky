package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import org.poolc.linky.databinding.ActivitySelectPathBinding
import kotlin.concurrent.thread

class SelectPathActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySelectPathBinding
    private var path = ""
    private lateinit var app : MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectPathBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MyApplication

        with(binding) {
            // topbar 설정
            setSupportActionBar(selectPathTopbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            selectPathTopbarTitle.text = "경로선택"

            createFragment("")
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    fun createFragment(newPath:String) {
        val moveCurrentPath = intent.getStringExtra("path")
        var folders : ArrayList<String>? = null
        if(moveCurrentPath == newPath) {
            folders = intent.getStringArrayListExtra("folders")
        }

        // json 가져오기
        var jsonStr = ""
        thread {
            jsonStr = app.read(newPath, false)

            val fragment = FolderListFragment()
            val bundle = Bundle()
            bundle.putString("path", newPath)
            bundle.putString("jsonStr", jsonStr)
            bundle.putStringArrayList("folders", folders)
            fragment.arguments = bundle
            setFragment(newPath, fragment)
        }
    }

    fun setCurrentPath(path:String) {
        this.path = path
    }

    private fun setFragment(newPath: String, fragment:Fragment) {
        val tran = supportFragmentManager.beginTransaction()
        tran.replace(R.id.folderFragmentContainer, fragment)

        if(newPath != "") {
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