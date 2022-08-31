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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.FoldernameDialogBinding
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
    private val moveFolderResultLauncher = registerForActivityResult(
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
                        404 -> {
                            message = "존재하지 않는 폴더가 있습니다."
                        }
                        409 -> {
                            message = "이동하고자 하는 경로에 이미 해당 폴더가 존재합니다."
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
                val reason = result.data?.getStringExtra("reason") ?: ""
                if(reason == "same path") {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("이동 실패")
                    builder.setMessage("이동하고자 하는 경로와 현재 경로가 같습니다.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
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
        val view = inflater.inflate(R.layout.fragment_edit_linky, container, false)
        binding = FragmentEditLinkyBinding.bind(view)

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
        update()
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
            folders.clear()
            totalSelected = 0

            // json 파싱
            val jsonStr = app.read(path, false)

            if (jsonStr != "") {
                setFolders(jsonStr!!)
            }

            editActivity.runOnUiThread {
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

    fun edit() {
        if(totalSelected == 0) {
            val builder = AlertDialog.Builder(editActivity)

            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("수정 실패")
            builder.setMessage("수정할 폴더를 선택해주세요.")

            builder.setPositiveButton("확인", null)

            builder.show()
        }
        else if(totalSelected > 1) {
            val builder = AlertDialog.Builder(editActivity)

            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("수정 실패")
            builder.setMessage("수정할 폴더를 하나만 선택해주세요.")

            builder.setPositiveButton("확인", null)

            builder.show()
        }
        else {
            val builder = AlertDialog.Builder(editActivity)
            builder.setTitle("새로운 폴더명을 입력해주세요")
            builder.setIcon(R.drawable.edit_pink)

            val dialogView = layoutInflater.inflate(R.layout.foldername_dialog, null)
            val dialogBinding = FoldernameDialogBinding.bind(dialogView)

            builder.setView(dialogView)

            builder.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                val newFolderName = dialogBinding.newFolderName.text?.trim().toString()
                if(newFolderName == "") {
                    dialogBinding.newFolderName.error = "앞/뒤 공백 없이 1자 이상의 폴더명을 입력해주세요."
                }
                else if(newFolderName!!.contains("/")) {
                    dialogBinding.newFolderName.error = "폴더명에는 /가 포함될 수 없습니다."
                }
                else {
                    editFolder(newFolderName)
                }
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
    }

    private fun editFolder(newFolderName:String) {
        getSelectedFolders()
        val prevFolderName = selectedFolders[0]

        thread {
            val responseCode = app.editFolder("$path/$prevFolderName", newFolderName)

            if(responseCode == 200) {
                editActivity.runOnUiThread {
                    val toast = Toast.makeText(
                        editActivity,
                        "수정이 완료되었습니다~!",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                    update()
                }
            }
            else {
                var positiveButtonFunc: DialogInterface.OnClickListener? = null
                var message = ""

                when(responseCode) {
                    401 -> {
                        message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                editActivity.finishAffinity()
                                System.exit(0)
                            }
                        }
                    }
                    404 -> {
                        message = "존재하지 않는 폴더입니다."
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                update()
                            }
                        }
                    }
                    409 -> {
                        message = "이미 해당 폴더가 존재합니다."
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                update()
                            }
                        }
                    }
                    else -> {
                        message = "폴더 수정에 실패하였습니다."
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                update()
                            }
                        }
                    }
                }

                editActivity.runOnUiThread {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("수정 실패")
                    builder.setMessage(message)

                    builder.setPositiveButton("확인", positiveButtonFunc)

                    builder.show()
                }
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
            moveFolderResultLauncher.launch(intent)
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