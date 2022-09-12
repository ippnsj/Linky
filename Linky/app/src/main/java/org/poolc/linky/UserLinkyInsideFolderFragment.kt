package org.poolc.linky

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentUserLinkyInsideFolderBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class UserLinkyInsideFolderFragment : Fragment() {
    private lateinit var binding: FragmentUserLinkyInsideFolderBinding
    private lateinit var activity: Activity
    private lateinit var app: MyApplication

    private val folders = ArrayList<Folder>()
    private val links = ArrayList<Link>()
    private lateinit var folderAdapter: FolderSubAdapter
    private lateinit var linkAdapter: LinkyAdapter
    private lateinit var owner : String
    private lateinit var email : String
    private lateinit var path : String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as Activity
        app = activity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        owner = arguments?.getString("owner") ?: ""
        email = arguments?.getString("email") ?: ""
        path = arguments?.getString("path") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_linky_inside_folder, container, false)
        binding = FragmentUserLinkyInsideFolderBinding.bind(view)

        folderAdapter = FolderSubAdapter(folders, object : FolderSubAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                val folderName = folders[pos].getFolderName()
                val newPath = "${path}${folderName}/"

                val parent = parentFragment as UserLinkyFragment
                parent.setFragment(newPath, true)
            }
        }, false)

        linkAdapter = LinkyAdapter(links, object : LinkyAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(links[pos].getUrl()))
                startActivity(intent)
            }
        }, false)

        with(binding) {
            folderRecycler.adapter = folderAdapter
            linkRecycler.adapter = linkAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            folderRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

            linkRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun update() {
        when(owner) {
            "me" -> {
                readMe()
            }
            "other" -> {
                readOther()
            }
        }
    }

    private fun readMe() {
        thread {
            val jsonStr = app.read(path, true)

            activity.runOnUiThread {
                if (jsonStr != "") {
                    setFolders(jsonStr!!)
                    setLinks(jsonStr!!)
                }
                else {
                    folders.clear()
                    links.clear()
                }

                folderAdapter.notifyDataSetChanged()
                linkAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun readOther() {
        thread {
            val jsonStr = app.readOther(email, path)

            activity.runOnUiThread {
                if (jsonStr != "") {
                    setFolders(jsonStr!!)
                    setLinks(jsonStr!!)
                }
                else {
                    folders.clear()
                    links.clear()
                }

                folderAdapter.notifyDataSetChanged()
                linkAdapter.notifyDataSetChanged()
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
            val isPublic = linkObj.getString("isPublic")

            val link = Link(id, keywordsArr, title, imgUrl, url, isPublic, false)
            links.add(link)
        }
    }
}