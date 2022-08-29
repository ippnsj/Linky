package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import org.json.JSONObject
import org.poolc.linky.databinding.FoldernameDialogBinding
import org.poolc.linky.databinding.FragmentFoldersBinding
import kotlin.concurrent.thread

class FolderListFragment : Fragment() {
    private lateinit var binding : FragmentFoldersBinding
    private val folders = ArrayList<String>()
    private lateinit var folderListAdapter : FolderListAdapter
    private lateinit var path : String
    private lateinit var selectPathActivity : SelectPathActivity

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

        val jsonStr = arguments?.getString("jsonStr")
        if (jsonStr != "") {
            setFolders(jsonStr!!)
        }

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

                val dialogView = layoutInflater.inflate(R.layout.foldername_dialog, null)
                val dialogBinding = FoldernameDialogBinding.bind(dialogView)

                builder.setView(dialogView)

                builder.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                    // TODO 글자수가 1자 이상이며 10자를 넘지 않는지 확인
                    createFolder(dialogBinding.newFolderName.text.toString())
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
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        selectPathActivity = context as SelectPathActivity
    }

    private fun setFolders(jsonStr:String) {
        val notAllowedFolders = arguments?.getStringArrayList("folders")

        folders.clear()
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folderInfos")
        for (idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("name")

            if(notAllowedFolders == null || !notAllowedFolders.contains(folderName)) {
                folders.add(folderName)
            }
        }
    }

    private fun createFolder(folderName:String) {
        val app = activity?.application as MyApplication

        thread {
            val responseCode = app.createFolder(folderName, path)

            if(responseCode == 200) {
                var jsonStr = ""
                thread {
                    jsonStr = app.read(path, false)

                    if (jsonStr != "") {
                        selectPathActivity.runOnUiThread {
                            folders.clear()
                            setFolders(jsonStr)
                            folderListAdapter.notifyDataSetChanged()

                            val toast =
                                Toast.makeText(activity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
                            toast.setGravity(Gravity.BOTTOM, 0, 0)
                            toast.show()
                        }
                    }
                }
            }
            else if(responseCode == 400) {
                Log.d("test", "Bad request")
            }
            else if(responseCode == 404) {
                Log.d("test", "Not Found")
            }
            else if(responseCode == 401) {
                Log.d("test", "Unauthorized")
            }
        }
    }
}