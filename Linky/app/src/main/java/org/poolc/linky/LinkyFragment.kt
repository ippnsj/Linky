package org.poolc.linky

import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentLinkyBinding
import kotlin.math.ceil

class LinkyFragment : Fragment() {
    private lateinit var binding : FragmentLinkyBinding
    private val folders = ArrayList<String>()
    private lateinit var folderAdapter: FolderAdapter
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
        // json 파싱
        val jsonStr = arguments?.getString("jsonStr")
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folders")
        for(idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("folderName")

            folders.add(folderName)
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
        }
    }
}