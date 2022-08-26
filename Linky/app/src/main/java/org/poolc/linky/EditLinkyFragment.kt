package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentEditLinkyBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class EditLinkyFragment : Fragment() {
    private lateinit var binding : FragmentEditLinkyBinding
    private lateinit var app : MyApplication
    private val folders = ArrayList<Folder>()
    private var totalSelected = 0
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var path : String
    private lateinit var editActivity: EditActivity
    private var selectedFolders = ArrayList<String>()
    private val folderResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == AppCompatActivity.RESULT_OK) {
                val responseCode = result.data?.getIntExtra("responseCode", -1)

                if (responseCode == 200) {
                    val toast = Toast.makeText(
                        editActivity,
                        "이동이 완료되었습니다~!",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                } else {
                    val builder = AlertDialog.Builder(editActivity)
                    var positiveButtonFunc: DialogInterface.OnClickListener? = null
                    var message = ""

                    when (responseCode) {
                        400 -> {
                            message = "폴더 이동에 실패하였습니다."
                        }
                        404 -> {
                            message = "존재하지 않는 폴더가 있습니다."
                        }
                        401 -> {
                            message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                            positiveButtonFunc =
                                object : DialogInterface.OnClickListener {
                                    override fun onClick(
                                        dialog: DialogInterface?,
                                        which: Int
                                    ) {
                                        editActivity.finishAffinity()
                                        System.exit(0)
                                    }
                                }
                        }
                        else -> {
                            message = "폴더 이동에 실패하였습니다."
                        }
                    }

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("이동 실패")
                    builder.setMessage(message)

                    builder.setPositiveButton("확인", positiveButtonFunc)

                    builder.show()
                }
            } else if(result.resultCode == AppCompatActivity.RESULT_CANCELED) {
                val builder = AlertDialog.Builder(editActivity)

                builder.setIcon(R.drawable.ic_baseline_warning_8)
                builder.setTitle("이동 실패")
                builder.setMessage("이동하고자 하는 경로와 현재 경로가 같습니다.")

                builder.setPositiveButton("확인", null)

                builder.show()
            }
        }

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
        return inflater.inflate(R.layout.fragment_edit_linky, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditLinkyBinding.bind(view)

        thread {
            // json 파싱
            val jsonStr = app.read(path, false)

            editActivity.runOnUiThread {
                if(jsonStr != "") {
                    setFolders(jsonStr!!)
                }
                else {
                    folders.clear()
                    totalSelected = 0
                }

                folderAdapter = FolderAdapter(folders, object : FolderAdapter.OnItemClickListener {
                    override fun onItemClick(pos:Int) {
                        folders[pos].switchIsSelected()
                        totalSelected = if(folders[pos].getIsSelected()) { totalSelected + 1 } else { totalSelected - 1 }
                        folderAdapter.notifyItemChanged(pos)

                        if(totalSelected == folders.size) {
                            editActivity.setAllSelectButton(true)
                        }
                        else {
                            editActivity.setAllSelectButton(false)
                        }
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
        }
    }

    private fun setFolders(jsonStr:String) {
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folderInfos")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("name")

            val folder = Folder(folderName, false)
            folders.add(folder)
        }
    }

    private fun update() {
        thread {
            // json 파싱
            val jsonStr = app.read(path, false)

            editActivity.runOnUiThread {
                folders.clear()
                totalSelected = 0

                if (jsonStr != "") {
                    setFolders(jsonStr!!)
                }

                folderAdapter.notifyDataSetChanged()
            }
        }
    }

    fun selectAll() {
        if(totalSelected == folders.size) {
            for (pos in 0 until folders.size) {
                folders[pos].setIsSelected(false)
                totalSelected--
                folderAdapter.notifyItemChanged(pos)
            }

            editActivity.setAllSelectButton(false)
        }
        else {
            for (pos in 0 until folders.size) {
                if (!folders[pos].getIsSelected()) {
                    folders[pos].setIsSelected(true)
                    totalSelected++
                    folderAdapter.notifyItemChanged(pos)
                }
            }

            editActivity.setAllSelectButton(true)
        }
    }

    private fun getSelectedFolders() {
        selectedFolders.clear()

        for(pos in 0 until folders.size) {
            if(folders[pos].getIsSelected()) {
                selectedFolders.add(folders[pos].getFolderName())
            }
        }
    }

    fun move() {
        if(totalSelected == 0) {
            val builder = AlertDialog.Builder(editActivity)

            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("이동 실패")
            builder.setMessage("이동할 폴더를 선택해주세요.")

            builder.setPositiveButton("확인", null)

            builder.show()
        }
        else {
            getSelectedFolders()

            val intent = Intent(editActivity, SelectPathActivity::class.java)
            intent.putExtra("path", path)
            intent.putExtra("folders", selectedFolders)
            intent.putExtra("purpose", "move")
            folderResultLauncher.launch(intent)
        }
    }

    fun delete() {
        if(totalSelected == 0) {
            val builder = AlertDialog.Builder(editActivity)

            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("삭제 실패")
            builder.setMessage("삭제할 폴더를 선택해주세요.")

            builder.setPositiveButton("확인", null)

            builder.show()
        }
        else {
            var completeDeletion = true
            var positiveButtonFunc: DialogInterface.OnClickListener? = null
            var message = ""

            thread {
                getSelectedFolders()

                val responseCode = app.deleteFolder(selectedFolders)

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