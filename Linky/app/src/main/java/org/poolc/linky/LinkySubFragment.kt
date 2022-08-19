package org.poolc.linky

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentLinkySubBinding
import kotlin.math.ceil

class LinkySubFragment : Fragment() {
    private lateinit var binding : FragmentLinkySubBinding
    private val folders = ArrayList<String>()
    private val linkys = ArrayList<HashMap<String, Any>>()
    private lateinit var folderSubAdapter: FolderSubAdapter
    private lateinit var linkySubAdapter: LinkyAdapter
    private lateinit var path : String

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
        linkys.clear()
        // json 파싱
        val jsonStr = arguments?.getString("jsonStr")
        if(jsonStr != "") {
            val jsonObj = JSONObject(jsonStr)
            val foldersArr = jsonObj.getJSONArray("folders")
            val linkysArr = jsonObj.getJSONArray("links")
            for (idx in 0 until foldersArr.length()) {
                val folderObj = foldersArr.getJSONObject(idx)
                val folderName = folderObj.getString("folderName")

                folders.add(folderName)
            }

            for (idx in 0 until linkysArr.length()) {
                val linkyObj = linkysArr.getJSONObject(idx)
                val keywordsArr = linkyObj.getJSONArray("keywords")
                val title = linkyObj.getString("title")
                val imgUrl = linkyObj.getString("imgUrl")
                val url = linkyObj.getString("url")

                val linky = HashMap<String, Any>()
                linky.put("keywords", keywordsArr)
                linky.put("title", title)
                linky.put("imgUrl", imgUrl)
                linky.put("url", url)
                linkys.add(linky)
            }
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

        linkySubAdapter = LinkyAdapter(linkys)

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
        }
    }
}