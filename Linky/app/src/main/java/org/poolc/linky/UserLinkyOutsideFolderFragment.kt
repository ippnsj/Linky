package org.poolc.linky

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentUserLinkyOutsideFolderBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class UserLinkyOutsideFolderFragment : Fragment() {
    private lateinit var binding: FragmentUserLinkyOutsideFolderBinding
    private lateinit var activity: Activity
    private lateinit var app: MyApplication

    private val folders = ArrayList<Folder>()
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var owner : String
    private lateinit var email : String
    private lateinit var path : String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("test", (context is SearchMeActivity).toString())
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
        val view = inflater.inflate(R.layout.fragment_user_linky_outside_folder, container, false)
        binding = FragmentUserLinkyOutsideFolderBinding.bind(view)

        folderAdapter = FolderAdapter(folders, object : FolderAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                val folderName = folders[pos].getFolderName()
                val newPath = "${path}${folderName}/"
                val bundle = Bundle()
                bundle.putString("owner", owner)
                bundle.putString("email", email)
                bundle.putString("path", newPath)

                // mainActivity.changeChildFragment(LinkyInsideFolderFragment(), bundle, true)
            }
        }, false)

        with(binding) {
            folderRecycler.adapter = folderAdapter
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
        }
    }

    override fun onResume() {
        super.onResume()

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
            val jsonStr = app.read(path, false)

//            mainActivity.runOnUiThread {
//                if (jsonStr != "") {
//                    setFolders(jsonStr!!)
//                }
//                else {
//                    folders.clear()
//                }
//
//                folderAdapter.notifyDataSetChanged()
//            }
        }
    }

    private fun readOther() {
        thread {
            val jsonStr = app.readOther(email, path)

//            mainActivity.runOnUiThread {
//                if (jsonStr != "") {
//                    setFolders(jsonStr!!)
//                }
//                else {
//                    folders.clear()
//                }
//
//                folderAdapter.notifyDataSetChanged()
//            }
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
}