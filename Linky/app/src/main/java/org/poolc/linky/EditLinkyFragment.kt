package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentEditLinkyBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class EditLinkyFragment : Fragment() {
    private lateinit var binding : FragmentEditLinkyBinding
    private val folders = ArrayList<String>()
    private val isSelected = ArrayList<Boolean>()
    private var totalSelected = 0
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

        // json 파싱
        val jsonStr = arguments?.getString("jsonStr")
        if(jsonStr != "") {
            setFolders(jsonStr!!)
        }
        else {
            folders.clear()
            isSelected.clear()
            totalSelected = 0
        }

        return inflater.inflate(R.layout.fragment_edit_linky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditLinkyBinding.bind(view)

        folderAdapter = FolderAdapter(folders, isSelected, object : FolderAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                isSelected[pos] = !isSelected[pos]
                totalSelected = if(isSelected[pos]) { totalSelected + 1 } else { totalSelected - 1 }
                folderAdapter.notifyItemChanged(pos)
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
        folders.clear()
        isSelected.clear()
        totalSelected = 0
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folderInfos")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("folderName")

            folders.add(folderName)
            isSelected.add(false)
        }
    }

    private fun update() {
        val app = editActivity.application as MyApplication

        thread {
            // json 파싱
            val jsonStr = app.readFolder(path)

            editActivity.runOnUiThread {
                if (jsonStr != "") {
                    setFolders(jsonStr!!)
                } else {
                    folders.clear()
                    isSelected.clear()
                    totalSelected = 0
                }

                folderAdapter.notifyDataSetChanged()
            }
        }
    }

    fun selectAll() {
        for(pos in 0 until isSelected.size) {
            if(!isSelected[pos]) {
                isSelected[pos] = true
                totalSelected++
                folderAdapter.notifyItemChanged(pos)
            }
        }
    }

    fun delete() {
        if(totalSelected == 0) {
            val builder = AlertDialog.Builder(editActivity)

            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("삭제 실패")
            builder.setMessage("삭제할 폴더를 선택해주세요")

            builder.setPositiveButton("확인", null)

            builder.show()
        }
        else {
            val app = editActivity.application as MyApplication
            var completeDeletion = true
            var responseCode = -1
            var positiveButtonFunc: DialogInterface.OnClickListener? = null
            var message = ""

            thread {
                for (pos in 0 until isSelected.size) {
                    if (isSelected[pos]) {
                        responseCode = app.deleteFolder("${folders[pos]}/")

                        if (responseCode != 200) {
                            when (responseCode) {
                                400 -> {
                                    message = "폴더 삭제에 실패하였습니다."
                                    positiveButtonFunc = object : DialogInterface.OnClickListener {
                                        override fun onClick(dialog: DialogInterface?, which: Int) {
                                            update()
                                        }
                                    }
                                }
                                404 -> {
                                    message = "존재하지 않는 폴더가 있습니다."
                                    positiveButtonFunc = object : DialogInterface.OnClickListener {
                                        override fun onClick(dialog: DialogInterface?, which: Int) {
                                            update()
                                        }
                                    }
                                }
                                401 -> {
                                    message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                                    positiveButtonFunc = object : DialogInterface.OnClickListener {
                                        override fun onClick(dialog: DialogInterface?, which: Int) {
                                            editActivity.finishAffinity()
                                            System.exit(0)
                                        }
                                    }
                                }
                                else -> {
                                    message = "폴더 삭제에 실패하였습니다."
                                    positiveButtonFunc = object : DialogInterface.OnClickListener {
                                        override fun onClick(dialog: DialogInterface?, which: Int) {
                                            update()
                                        }
                                    }
                                }
                            }

                            completeDeletion = false
                            break
                        }
                    }
                }

                editActivity.runOnUiThread {
                    if (completeDeletion) {
                        val toast = Toast.makeText(
                            editActivity,
                            "삭제가 완료되었습니다~!",
                            Toast.LENGTH_SHORT
                        )
                        toast.show()
                        update()
                    } else {
                        val builder = AlertDialog.Builder(editActivity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("삭제 실패")
                        builder.setMessage(message)

                        builder.setPositiveButton("확인", positiveButtonFunc)

                        builder.show()
                    }
                }
            }
        }
    }
}