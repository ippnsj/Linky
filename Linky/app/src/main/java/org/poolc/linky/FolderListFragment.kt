package org.poolc.linky

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import org.json.JSONObject
import org.poolc.linky.databinding.FoldernameDialogBinding
import org.poolc.linky.databinding.FragmentFoldersBinding

class FolderListFragment : Fragment() {
    private lateinit var binding : FragmentFoldersBinding
    private val folders = ArrayList<String>()
    private lateinit var folderListAdapter : FolderListAdapter
    private lateinit var path : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        path = arguments?.getString("path")!!

        val jsonStr = arguments?.getString("jsonStr")
        val jsonObj = JSONObject(jsonStr)
        val foldersArr = jsonObj.getJSONArray("folders")
        for(idx in 0 until foldersArr.length()) {
            val folderObj = foldersArr.getJSONObject(idx)
            val folderName = folderObj.getString("folderName")

            folders.add(folderName)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // activity에 path값 넘김
        val activity = activity as SelectPathActivity
        activity.setCurrentPath(path)

        return inflater.inflate(R.layout.fragment_folders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFoldersBinding.bind(view)

        folderListAdapter = FolderListAdapter(folders, object : FolderListAdapter.OnItemClickListener {
            override fun onItemClick(folderName: String) {
                val newPath = "${path}${folderName} / "
                val activity = activity as SelectPathActivity
                activity.createFragment(newPath)
            }
        })

        with(binding) {
            currentPath.text = path

            folderListRecycler.adapter = folderListAdapter
            folderListRecycler.addItemDecoration(DividerItemDecoration(activity, 1))

            addFolder.setOnClickListener {
                val builder = android.app.AlertDialog.Builder(activity)
                builder.setTitle("추가할 폴더명을 입력해주세요")

                val dialogView = layoutInflater.inflate(R.layout.foldername_dialog, null)
                val dialogBinding = FoldernameDialogBinding.bind(dialogView)

                builder.setView(dialogView)

                builder.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                    // TODO 글자수가 1자 이상이며 10자를 넘지 않는지 확인
                    folders.add(dialogBinding.newFolderName.text.toString())
                    folderListAdapter.notifyItemInserted(folders.size - 1)

                    val toast = Toast.makeText(activity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.BOTTOM, 0, 0)
                    toast.show()
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
}