package org.poolc.linky

import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FoldernameDialogBinding
import org.poolc.linky.databinding.FragmentLinkyOutsideFolderBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class LinkyOutsideFolderFragment : Fragment() {
    private lateinit var binding : FragmentLinkyOutsideFolderBinding
    private val folders = ArrayList<Folder>()
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var path : String
    private var isFabOpen = false
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_linky_outside_folder, container, false)
        binding = FragmentLinkyOutsideFolderBinding.bind(view)

        folderAdapter = FolderAdapter(folders, object : FolderAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                val folderName = folders[pos].getFolderName()
                val newPath = "${path}${folderName}/"
                val bundle = Bundle()
                bundle.putString("path", newPath)
                bundle.putString("folderName", folderName)
                mainActivity.changeChildFragment(LinkyInsideFolderFragment(), bundle, true)
            }
        }, false)

        with(binding) {
            folderRecycler.adapter = folderAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isFabOpen = false

        with(binding) {
            folderRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    val size = parent.size
                    val rows = ceil((size / 3).toDouble())

                    if(pos > (rows - 1) * 3) {
                        outRect.top = 20
                        outRect.bottom = 40
                    }
                    else {
                        outRect.top = 20
                        outRect.bottom = 20
                    }
                }
            })

            edit.setOnClickListener {
                val intent = Intent(mainActivity, EditActivity::class.java)
                intent.putExtra("path", path)
                startActivity(intent)
            }

            addFolder.setOnClickListener {
                val builder = AlertDialog.Builder(mainActivity)
                builder.setTitle("추가할 폴더명을 입력해주세요")
                builder.setIcon(R.drawable.add_folder_pink)

                val dialogView = layoutInflater.inflate(R.layout.foldername_dialog, null)
                val dialogBinding = FoldernameDialogBinding.bind(dialogView)

                builder.setView(dialogView)

                builder.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                    // TODO 글자수가 1자 이상이며 10자를 넘지 않는지 확인
                    createFolder(dialogBinding.newFolderName.text.toString())
                }
                builder.setNegativeButton("취소", null)

                val dialog = builder.create()

                dialog.show()

                dialogBinding.newFolderName.setOnEditorActionListener { v, actionId, event ->
                    if(actionId == EditorInfo.IME_ACTION_DONE) {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                        true
                    }
                    false
                }
            }

            add.setOnClickListener {
                toggleFab()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    fun update() {
        // activity에 path값 넘김
        mainActivity.setPath(path)
        mainActivity.setFolderName("")

        mainActivity.setTopbarTitle("LinkyOutsideFolderFragment")

        thread {
            val jsonStr = app.read(path, false)

            mainActivity.runOnUiThread {
                if (jsonStr != "") {
                    setFolders(jsonStr!!)
                }
                else {
                    folders.clear()
                }

                folderAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun toggleFab() {
        with(binding) {
            if (isFabOpen) {
                ObjectAnimator.ofFloat(addFolder, "translationY", 0f).apply { start() }
                add.setImageResource(R.drawable.add)
            } else {
                ObjectAnimator.ofFloat(addFolder, "translationY", -200f).apply { start() }
                add.setImageResource(R.drawable.close)
            }
        }

        isFabOpen = !isFabOpen
    }

    private fun setFolders(jsonStr:String) {
        folders.clear()
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folderInfos")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("name")

            val folder = Folder(folderName, false)
            folders.add(folder)
        }
    }

    private fun createFolder(folderName:String) {
        thread {
            val responseCode = app.createFolder(folderName, path)

            if(responseCode == 200) {
                var jsonStr = ""
                thread {
                    jsonStr = app.read(path, false)

                    if (jsonStr != "") {
                        mainActivity.runOnUiThread {
                            folders.clear()
                            setFolders(jsonStr)
                            folderAdapter.notifyDataSetChanged()

                            val toast =
                                Toast.makeText(activity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
                            toast.setGravity(Gravity.BOTTOM, 0, 0)
                            toast.show()
                        }
                    }
                }
            }
            else if(responseCode == 400) {
                Log.d("test", "Bad request")
            }
            else if(responseCode == 404) {
                Log.d("test", "Not Found")
            }
            else if(responseCode == 401) {
                Log.d("test", "Unauthorized")
            }
        }
    }
}