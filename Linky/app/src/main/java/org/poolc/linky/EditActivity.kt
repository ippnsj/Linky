package org.poolc.linky

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.poolc.linky.databinding.ActivityEditBinding
import org.poolc.linky.databinding.EditBottomSheetLayoutBinding
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
                    R.id.edit -> {
                        if(path == "") {
                            editLinkyFragment.edit()
                        }
                        else {
                            val bottomSheetView = layoutInflater.inflate(R.layout.edit_bottom_sheet_layout, null)
                            val bottomSheetBinding = EditBottomSheetLayoutBinding.bind(bottomSheetView)
                            val bottomSheetDialog = BottomSheetDialog(this@EditActivity)
                            bottomSheetDialog.setContentView(bottomSheetView)

                            with(bottomSheetBinding) {
                                folderButton.text = getString(R.string.edit_folder)
                                linkButton.text = getString(R.string.edit_link)

                                folderButton.setOnClickListener {
                                    bottomSheetDialog.dismiss()
                                    editLinkySubFragment.edit("folder")
                                }
                                linkButton.setOnClickListener {
                                    bottomSheetDialog.dismiss()
                                    editLinkySubFragment.edit("link")
                                }
                            }

                            bottomSheetDialog.show()
                        }
                    }
                    R.id.move -> {
                        if(path == "") {
                            editLinkyFragment.move()
                        }
                        else {
                            val bottomSheetView = layoutInflater.inflate(R.layout.edit_bottom_sheet_layout, null)
                            val bottomSheetBinding = EditBottomSheetLayoutBinding.bind(bottomSheetView)
                            val bottomSheetDialog = BottomSheetDialog(this@EditActivity)
                            bottomSheetDialog.setContentView(bottomSheetView)

                            with(bottomSheetBinding) {
                                folderButton.text = getString(R.string.move_folder)
                                linkButton.text = getString(R.string.move_link)

                                folderButton.setOnClickListener {
                                    bottomSheetDialog.dismiss()
                                    editLinkySubFragment.move("folder")
                                }
                                linkButton.setOnClickListener {
                                    bottomSheetDialog.dismiss()
                                    editLinkySubFragment.move("link")
                                }
                            }

                            bottomSheetDialog.show()
                        }
                    }
                    R.id.delete -> {
                        if (path == "") {
                            val builder = AlertDialog.Builder(this@EditActivity)
                            builder.setMessage("선택된 폴더를 모두 삭제하시겠습니까?")

                            builder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
                                editLinkyFragment.delete()
                            }

                            builder.setNegativeButton("취소", null)

                            builder.show()
                        }
                        else {
                            val bottomSheetView = layoutInflater.inflate(R.layout.edit_bottom_sheet_layout, null)
                            val bottomSheetBinding = EditBottomSheetLayoutBinding.bind(bottomSheetView)
                            val bottomSheetDialog = BottomSheetDialog(this@EditActivity)
                            bottomSheetDialog.setContentView(bottomSheetView)

                            with(bottomSheetBinding) {
                                folderButton.text = getString(R.string.delete_folder)
                                linkButton.text = getString(R.string.delete_link)

                                folderButton.setOnClickListener {
                                    bottomSheetDialog.dismiss()

                                    val builder = AlertDialog.Builder(this@EditActivity)
                                    builder.setMessage("선택된 폴더를 모두 삭제하시겠습니까?")

                                    builder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
                                        editLinkySubFragment.delete("folder")
                                    }

                                    builder.setNegativeButton("취소", null)

                                    builder.show()
                                }
                                linkButton.setOnClickListener {
                                    bottomSheetDialog.dismiss()

                                    val builder = AlertDialog.Builder(this@EditActivity)
                                    builder.setMessage("선택된 링크를 모두 삭제하시겠습니까?")

                                    builder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
                                        editLinkySubFragment.delete("link")
                                    }

                                    builder.setNegativeButton("취소", null)

                                    builder.show()
                                }
                            }

                            bottomSheetDialog.show()
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