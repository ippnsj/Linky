package org.poolc.linky

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FoldernameDialogBinding
import org.poolc.linky.databinding.FragmentLinkySubBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class LinkySubFragment : Fragment() {
    private lateinit var binding : FragmentLinkySubBinding
    private val folders = ArrayList<String>()
    private val links = ArrayList<HashMap<String, Any>>()
    private lateinit var folderSubAdapter: FolderSubAdapter
    private lateinit var linkySubAdapter: LinkyAdapter
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

        // json 파싱
        val jsonStr = arguments?.getString("jsonStr")
        if(jsonStr != "") {
            setFolders(jsonStr!!)
            setLinks(jsonStr!!)
        }

        return inflater.inflate(R.layout.fragment_linky_sub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLinkySubBinding.bind(view)

        folderSubAdapter = FolderSubAdapter(folders, object : FolderSubAdapter.OnItemClickListener {
            override fun onItemClick(folderName: String) {
                val newPath = "${path}${folderName}/"
                val activity = activity as MainActivity
                activity.createFragment(newPath)
            }
        })

        linkySubAdapter = LinkyAdapter(links)

        with(binding) {
            folderSubRecycler.adapter = folderSubAdapter
            linkySubRecycler.adapter = linkySubAdapter

            folderSubRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)

                    if(pos != 0) {
                        outRect.left = 60
                    }
                }
            })

            linkySubRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    val size = parent.size
                    val rows = ceil((size / 2).toDouble())

                    if(pos % 2 == 0) {
                        outRect.right = 20
                    }
                    else {
                        outRect.left = 20
                    }

                    if(pos > (rows - 1) * 2) {
                        outRect.top = 20
                        outRect.bottom = 40
                    }
                    else {
                        outRect.top = 20
                        outRect.bottom = 20
                    }
                }
            })

            addFolderSub.setOnClickListener {
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

            addSub.setOnClickListener {
                toggleFab()
            }
        }
    }

    private fun toggleFab() {
        with(binding) {
            if (isFabOpen) {
                ObjectAnimator.ofFloat(addFolderSub, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(addLinkySub, "translationY", 0f).apply { start() }
                addSub.setImageResource(R.drawable.add)
            } else {
                ObjectAnimator.ofFloat(addFolderSub, "translationY", -400f).apply { start() }
                ObjectAnimator.ofFloat(addLinkySub, "translationY", -200f).apply { start() }
                addSub.setImageResource(R.drawable.close)
            }
        }

        isFabOpen = !isFabOpen
    }

    private fun setFolders(jsonStr:String) {
        folders.clear()
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folders")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("folderName")

            folders.add(folderName)
        }
    }

    private fun setLinks(jsonStr: String) {
        links.clear()
        val jsonObj = JSONObject(jsonStr)
        val linksArr = jsonObj.getJSONArray("links")
        for (idx in 0 until linksArr.length()) {
            val linkObj = linksArr.getJSONObject(idx)
            val keywordsArr = linkObj.getJSONArray("keywords")
            val title = linkObj.getString("title")
            val imgUrl = linkObj.getString("imgUrl")
            val url = linkObj.getString("url")

            val link = HashMap<String, Any>()
            link.put("keywords", keywordsArr)
            link.put("title", title)
            link.put("imgUrl", imgUrl)
            link.put("url", url)
            links.add(link)
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
                    folderSubAdapter.notifyDataSetChanged()

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