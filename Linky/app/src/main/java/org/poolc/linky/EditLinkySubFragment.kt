package org.poolc.linky

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentEditLinkySubBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class EditLinkySubFragment : Fragment() {
    private lateinit var binding : FragmentEditLinkySubBinding
    private lateinit var editActivity: EditActivity
    private lateinit var app : MyApplication
    private val folders = ArrayList<Folder>()
    private val links = ArrayList<Link>()
    private var totalSelectedFolder = 0
    private var totalSelectedLink = 0
    private lateinit var folderSubAdapter : FolderSubAdapter
    private lateinit var linkySubAdapter: LinkyAdapter
    private lateinit var path : String
    private var moveFolders = ArrayList<String>()
    private var moveLinks = ArrayList<String>()
    private lateinit var folderResultLauncher: ActivityResultLauncher<Intent>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        editActivity = context as EditActivity
        app = editActivity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = arguments?.getString("path")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_linky_sub, container, false)
        binding = FragmentEditLinkySubBinding.bind(view)

        folderSubAdapter = FolderSubAdapter(folders, object : FolderSubAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                folders[pos].switchIsSelected()
                totalSelectedFolder = if(folders[pos].getIsSelected()) { totalSelectedFolder + 1 } else { totalSelectedFolder - 1 }
                folderSubAdapter.notifyItemChanged(pos)
            }
        }, true)

        linkySubAdapter = LinkyAdapter(links, object : LinkyAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                links[pos].switchIsSelected()
                totalSelectedLink = if(links[pos].getIsSelected()) { totalSelectedLink + 1 } else { totalSelectedLink - 1 }
                linkySubAdapter.notifyItemChanged(pos)
            }
        }, true)

        with(binding) {
            folderSubRecycler.adapter = folderSubAdapter
            linkySubRecycler.adapter = linkySubAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        thread {
            val jsonStr = app.read(path, true)

            editActivity.runOnUiThread {
                update(jsonStr)

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
                }
            }
        }
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

    fun update(jsonStr:String) {
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

    fun selectAllFolders() {
        if(totalSelectedFolder == folders.size) {
            for (pos in 0 until folders.size) {
                folders[pos].setIsSelected(false)
                totalSelectedFolder--
                folderSubAdapter.notifyItemChanged(pos)
            }
        }
        else {
            for (pos in 0 until folders.size) {
                if (!folders[pos].getIsSelected()) {
                    folders[pos].setIsSelected(true)
                    totalSelectedFolder++
                    folderSubAdapter.notifyItemChanged(pos)
                }
            }
        }
    }

    fun selectAllLinks() {
        if(totalSelectedLink == links.size) {
            for (pos in 0 until links.size) {
                links[pos].setIsSelected(false)
                totalSelectedLink--
                linkySubAdapter.notifyItemChanged(pos)
            }
        }
        else {
            for (pos in 0 until links.size) {
                if (!links[pos].getIsSelected()) {
                    links[pos].setIsSelected(true)
                    totalSelectedLink++
                    linkySubAdapter.notifyItemChanged(pos)
                }
            }
        }
    }
}