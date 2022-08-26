package org.poolc.linky

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.poolc.linky.databinding.ActivityEditBinding
import kotlin.concurrent.thread


class EditActivity : AppCompatActivity() {
    private lateinit var binding : ActivityEditBinding
    private lateinit var path : String
    private lateinit var editLinkyFragment : EditLinkyFragment
    private lateinit var editLinkySubFragment : EditLinkySubFragment
    private var allSelectButton : MenuItem? = null

    private lateinit var app : MyApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MyApplication

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
                        if(path == "") {
                            editLinkyFragment.move()
                        }
                        else {

                        }
                    }
                    R.id.delete -> {
                        if (path == "") {
                            var message = ""
                            if (path == "") {
                                message = "선택된 폴더를 모두 삭제하시겠습니까?"
                            } else {
                                message = "선택된 폴더 및 링크를 모두 삭제하시겠습니까?"
                            }

                            val builder = AlertDialog.Builder(this@EditActivity)
                            builder.setMessage(message)

                            builder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
                                editLinkyFragment.delete()
                            }

                            builder.setNegativeButton("취소", null)

                            builder.show()
                        }
                        else {

                        }
                    }
                }
                true
            }
        }
    }

    override fun onStart() {
        super.onStart()

        setFragment()
    }

    fun setAllSelectButton(isAllSelected:Boolean) {
        if(isAllSelected) {
            allSelectButton?.title = "모두취소"
        }
        else {
            allSelectButton?.title = "모두선택"
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
        var bundle : Bundle? = null

        with(binding) {
            if(path == "") {
                bundle = Bundle()
                bundle?.putString("path", path)
                editLinkyFragment = EditLinkyFragment()
                createFragment(editLinkyFragment, bundle)
            }
            else {
                bundle = Bundle()
                bundle?.putString("path", path)
                editLinkySubFragment = EditLinkySubFragment()
                createFragment(editLinkySubFragment, bundle)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_linky_topbar, menu)

        allSelectButton = menu?.findItem(R.id.selectAll)

        return super.onCreateOptionsMenu(menu)
    }

    private fun selectAllPopup() {
        val view = findViewById<View>(R.id.selectAll)
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_select_all, popup.menu)
        popup.show()

        popup.setOnMenuItemClickListener(popupListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.selectAll -> {
                if(path == "") {
                    editLinkyFragment.selectAll()
                }
                else {
                    selectAllPopup()
                }
            }
            R.id.done -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private val popupListener = object : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when(item?.itemId) {
                R.id.selectAllFolder -> {
                    editLinkySubFragment.selectAllFolders()
                }
                R.id.selectAllLink -> {
                    editLinkySubFragment.selectAllLinks()
                }
            }

            return true
        }
    }
}