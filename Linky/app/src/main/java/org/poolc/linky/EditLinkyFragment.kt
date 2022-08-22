package org.poolc.linky

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentEditLinkyBinding
import kotlin.math.ceil

class EditLinkyFragment : Fragment() {
    private lateinit var binding : FragmentEditLinkyBinding
    private val folders = ArrayList<String>()
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var path : String
    private lateinit var editActivity: EditActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        editActivity = context as EditActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = arguments?.getString("path")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        folders.clear()
        // json 파싱
        val jsonStr = arguments?.getString("jsonStr")
        if(jsonStr != "") {
            setFolders(jsonStr!!)
        }

        return inflater.inflate(R.layout.fragment_edit_linky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditLinkyBinding.bind(view)

        folderAdapter = FolderAdapter(folders, object : FolderAdapter.OnItemClickListener {
            override fun onItemClick(folderName: String) {
            }
        }, true)

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
        }
    }

    private fun setFolders(jsonStr:String) {
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folderInfos")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("folderName")

            folders.add(folderName)
        }
    }
}