package org.poolc.linky

import android.animation.ObjectAnimator
import android.content.DialogInterface
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
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FoldernameDialogBinding
import org.poolc.linky.databinding.FragmentLinkyBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class LinkyFragment : Fragment() {
    private lateinit var binding : FragmentLinkyBinding
    private val folders = ArrayList<String>()
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var path : String
    private var isFabOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = arguments?.getString("path")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // activity에 path값 넘김
        val activity = activity as MainActivity
        activity.setPath(path)

        folders.clear()
        // json 파싱
        val jsonStr = arguments?.getString("jsonStr")
        if(jsonStr != "") {
            setFolders(jsonStr!!)
        }

        return inflater.inflate(R.layout.fragment_linky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLinkyBinding.bind(view)

        folderAdapter = FolderAdapter(folders, object : FolderAdapter.OnItemClickListener {
            override fun onItemClick(folderName: String) {
                val newPath = "${path}${folderName}/"
                val activity = activity as MainActivity
                activity.createFragment(newPath)
            }
        })

        with(binding) {
            folderRecycler.adapter = folderAdapter

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

            addFolder.setOnClickListener {
                val builder = android.app.AlertDialog.Builder(activity)
                builder.setTitle("추가할 폴더명을 입력해주세요")

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

    private fun toggleFab() {
        with(binding) {
            if (isFabOpen) {
                ObjectAnimator.ofFloat(addFolder, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(addLinky, "translationY", 0f).apply { start() }
                add.setImageResource(R.drawable.add)
            } else {
                ObjectAnimator.ofFloat(addFolder, "translationY", -400f).apply { start() }
                ObjectAnimator.ofFloat(addLinky, "translationY", -200f).apply { start() }
                add.setImageResource(R.drawable.close)
            }
        }

        isFabOpen = !isFabOpen
    }

    private fun setFolders(jsonStr:String) {
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folders")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("folderName")

            folders.add(folderName)
        }
    }

    private fun createFolder(folderName:String) {
        val app = activity?.application as MyApplication

        thread {
            val responseCode = app.createFolder(folderName, path)

            if(responseCode == 200) {
                var jsonStr = ""
                thread {
                    jsonStr = app.readFolder(path)
                }

                if (jsonStr != "") {
                    folders.clear()
                    setFolders(jsonStr)
                    folderAdapter.notifyDataSetChanged()

                    val toast = Toast.makeText(activity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.BOTTOM, 0, 0)
                    toast.show()
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