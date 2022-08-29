package org.poolc.linky

import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.AddLinkDialogBinding
import org.poolc.linky.databinding.FoldernameDialogBinding
import org.poolc.linky.databinding.FragmentLinkyInsideFolderBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class LinkyInsideFolderFragment : Fragment() {
    private lateinit var binding : FragmentLinkyInsideFolderBinding
    private val folders = ArrayList<Folder>()
    private val links = ArrayList<Link>()
    private lateinit var folderSubAdapter: FolderSubAdapter
    private lateinit var linkySubAdapter: LinkyAdapter
    private lateinit var path : String
    private lateinit var folderName : String
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
        path = arguments?.getString("path") ?: ""
        folderName = arguments?.getString("folderName") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_linky_inside_folder, container, false)
        binding = FragmentLinkyInsideFolderBinding.bind(view)

        folderSubAdapter = FolderSubAdapter(folders, object : FolderSubAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                val newFolderName = folders[pos].getFolderName()
                val newPath = "${path}${newFolderName}/"
                val bundle = Bundle()
                bundle.putString("path", newPath)
                bundle.putString("folderName", newFolderName)

                mainActivity.changeChildFragment(LinkyInsideFolderFragment(), bundle, true)
            }
        }, false)

        linkySubAdapter = LinkyAdapter(links, object : LinkyAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(links[pos].getUrl()))
                startActivity(intent)
            }

        }, false)

        with(binding) {
            folderSubRecycler.adapter = folderSubAdapter
            linkySubRecycler.adapter = linkySubAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isFabOpen = false

        with(binding) {
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

            edit.setOnClickListener {
                val intent = Intent(mainActivity, EditActivity::class.java)
                intent.putExtra("path", path)
                startActivity(intent)
            }

            addFolderSub.setOnClickListener {
                val builder = AlertDialog.Builder(mainActivity)
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

            addLinkySub.setOnClickListener {
                val builder = AlertDialog.Builder(mainActivity)
                builder.setTitle("추가할 링크를 입력해주세요")
                builder.setIcon(R.drawable.add_link_pink)

                val dialogView = layoutInflater.inflate(R.layout.add_link_dialog, null)
                val dialogBinding = AddLinkDialogBinding.bind(dialogView)

                builder.setView(dialogView)

                builder.setPositiveButton("추가", null)
                builder.setNegativeButton("취소", null)

                val dialog = builder.create()

                dialog.show()

                val possitiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                possitiveButton.setOnClickListener {
                    val url = dialogBinding.newUrl.text.toString()
                    if(url == "") {
                        dialogBinding.newUrl.error = "url을 입력해주세요."
                    }
                    else {
                        toggleFab()
                        val imm = mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(dialogBinding.newUrl.windowToken, 0)
                        dialog?.dismiss()
                        val intent = Intent(mainActivity, AddLinkyActivity::class.java)
                        intent.putExtra("url", url)
                        startActivity(intent)
                    }
                }

                dialogBinding.newUrl.setOnEditorActionListener { v, actionId, event ->
                    if(actionId == EditorInfo.IME_ACTION_DONE) {
                        possitiveButton.performClick()
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

    override fun onResume() {
        super.onResume()
        update()
    }

    fun update() {
        // activity에 path값 넘김
        mainActivity.setPath(path)
        mainActivity.setFolderName(folderName)

        mainActivity.setTopbarTitle("LinkyInsideFolderFragment")

        thread {
            val jsonStr = app.read(path, true)

            mainActivity.runOnUiThread {
                if (jsonStr != "") {
                    setFolders(jsonStr!!)
                    setLinks(jsonStr!!)
                }
                else {
                    folders.clear()
                    links.clear()
                }

                folderSubAdapter.notifyDataSetChanged()
                linkySubAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun toggleFab() {
        with(binding) {
            if (isFabOpen) {
                ObjectAnimator.ofFloat(addFolderSub, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(addLinkySub, "translationY", 0f).apply { start() }
                addSub.setImageResource(R.drawable.add_friend)
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
        val foldersArr = jsonObj.getJSONArray("folderInfos")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("name")

            val folder = Folder(folderName, false)
            folders.add(folder)
        }
    }

    private fun setLinks(jsonStr: String) {
        links.clear()
        val jsonObj = JSONObject(jsonStr)
        val linksArr = jsonObj.getJSONArray("linkInfos")
        for (idx in 0 until linksArr.length()) {
            val linkObj = linksArr.getJSONObject(idx)
            val id = linkObj.getString("id")
            val keywordsArr = linkObj.getJSONArray("keywords")
            val title = linkObj.getString("name")
            val imgUrl = linkObj.getString("imageUrl")
            val url = linkObj.getString("url")

            val link = Link(id, keywordsArr, title, imgUrl, url, false)
            links.add(link)
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
                            folderSubAdapter.notifyDataSetChanged()

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