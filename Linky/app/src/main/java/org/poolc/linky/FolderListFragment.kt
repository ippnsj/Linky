package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import org.json.JSONObject
import org.poolc.linky.databinding.DialogInputtext10limitBinding
import org.poolc.linky.databinding.FragmentFoldersBinding
import kotlin.concurrent.thread

class FolderListFragment : Fragment() {
    private lateinit var binding : FragmentFoldersBinding
    private val folders = ArrayList<String>()
    private lateinit var folderListAdapter : FolderListAdapter
    private lateinit var path : String
    private lateinit var selectPathActivity : SelectPathActivity
    private lateinit var app : MyApplication

    override fun onAttach(context: Context) {
        super.onAttach(context)
        selectPathActivity = context as SelectPathActivity
        app = selectPathActivity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        path = arguments?.getString("path")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // activity에 path값 넘김
        selectPathActivity.setCurrentPath(path)

        val view = inflater.inflate(R.layout.fragment_folders, container, false)
        binding = FragmentFoldersBinding.bind(view)

        folderListAdapter = FolderListAdapter(folders, object : FolderListAdapter.OnItemClickListener {
            override fun onItemClick(folderName: String) {
                val newPath = "${path}${folderName}/"
                selectPathActivity.createFragment(newPath)
            }
        })

        with(binding) {
            folderListRecycler.adapter = folderListAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            currentPath.text = path

            folderListRecycler.addItemDecoration(DividerItemDecoration(activity, 1))

            addFolder.setOnClickListener {
                val builder = AlertDialog.Builder(selectPathActivity)
                builder.setTitle("추가할 폴더명을 입력해주세요")
                builder.setIcon(R.drawable.add_folder_pink)

                val dialogView = layoutInflater.inflate(R.layout.dialog_inputtext_10limit, null)
                val dialogBinding = DialogInputtext10limitBinding.bind(dialogView)

                builder.setView(dialogView)

                builder.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                    val newFolderName = dialogBinding.inputText.text?.trim().toString()
                    if(newFolderName == "") {
                        dialogBinding.inputText.error = "앞/뒤 공백 없이 1자 이상의 폴더명을 입력해주세요."
                    }
                    else if(newFolderName!!.contains("/")) {
                        dialogBinding.inputText.error = "폴더명에는 /가 포함될 수 없습니다."
                    }
                    else {
                        createFolder(newFolderName)
                    }
                }
                builder.setNegativeButton("취소", null)

                val dialog = builder.create()

                dialog.show()

                dialogBinding.inputText.setOnEditorActionListener { v, actionId, event ->
                    if(actionId == EditorInfo.IME_ACTION_DONE) {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                        true
                    }
                    false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun update() {
        thread {
            val jsonStr = app.read(path, false)

            if (jsonStr != "") {
                setFolders(jsonStr!!)
            }else {
                folders.clear()
            }

            selectPathActivity.runOnUiThread {
                folderListAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setFolders(jsonStr:String) {
        val notAllowedFolders = arguments?.getStringArrayList("folders")

        folders.clear()
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folderInfos")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("name")
            val folderNameWithPath = "$path$folderName"

            if(notAllowedFolders == null || !notAllowedFolders.contains(folderNameWithPath)) {
                folders.add(folderName)
            }
        }
    }

    private fun createFolder(folderName:String) {
        thread {
            val responseCode = app.createFolder(folderName, path)

            if(responseCode == 200) {
                selectPathActivity.runOnUiThread {
                    val toast =
                        Toast.makeText(selectPathActivity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.BOTTOM, 0, 0)
                    toast.show()
                    update()
                }
            }
            else {
                var positiveButtonFunc: DialogInterface.OnClickListener? = null
                var message = ""

                when(responseCode) {
                    400 -> {
                        message = "폴더이름이 형식과 맞지 않습니다.\n" +
                                "폴더이름은 최대 10자만 가능하며, /는 포함될 수 없습니다."
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
                                selectPathActivity.finishAffinity()
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
                        message = "폴더 추가에 실패하였습니다."
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                update()
                            }
                        }
                    }
                }

                selectPathActivity.runOnUiThread {
                    val builder = AlertDialog.Builder(selectPathActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("추가 실패")
                    builder.setMessage(message)

                    builder.setPositiveButton("확인", positiveButtonFunc)

                    builder.show()
                }
            }
        }
    }
}