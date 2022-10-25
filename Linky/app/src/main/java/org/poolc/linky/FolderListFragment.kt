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
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import org.poolc.linky.databinding.DialogInputtext10limitBinding
import org.poolc.linky.databinding.FragmentFoldersBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(selectPathActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    private fun update() {
        folders.clear()

        val call = MyApplication.service.read(path, "false")

        call.enqueue(object: Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setFolders(response.body()!!.asJsonObject)
                }
                else {
                    val title = "폴더 읽어오기 실패"
                    var message = "서버 문제로 인해 정보를 가져오는데 실패하였습니다."
                    var listener: DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "현재 폴더가 존재하지 않는 폴더입니다."
                            listener = DialogInterface.OnDismissListener {
                                selectPathActivity.supportFragmentManager.popBackStack()
                            }
                        }
                    }

                    folderListAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                folderListAdapter.notifyDataSetChanged()

                val title = "폴더 읽어오기 실패"
                val message = "서버와의 통신 문제로 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun setFolders(jsonObj: JsonObject) {
        val notAllowedFolders = arguments?.getStringArrayList("folders")

        if(!jsonObj.isJsonNull) {
            val foldersArr = jsonObj.getAsJsonArray("folderInfos")
            for (idx in 0 until foldersArr.size()) {
                val folderObj = foldersArr.get(idx).asJsonObject
                val folderName = folderObj.get("name").asString
                val folderNameWithPath = "$path$folderName"

                if (notAllowedFolders == null || !notAllowedFolders.contains(folderNameWithPath)) {
                    folders.add(folderName)
                }
            }
        }
        else {
            val title = "폴더 읽어오기 실패"
            val message = "서버 문제로 정보를 가져오는데 실패하였습니다.\n" +
                    "잠시후 다시 시도해주세요."
            showDialog(title, message, null)
        }

        folderListAdapter.notifyDataSetChanged()
    }

    private fun createFolder(folderName:String) {
        val jsonObj = JSONObject()
        jsonObj.put("path", path)
        jsonObj.put("name", folderName)

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

        val call = MyApplication.service.createFolder(body)

        call.enqueue(object: Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if(response.isSuccessful) {
                    val toast =
                        Toast.makeText(selectPathActivity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.BOTTOM, 0, 0)
                    toast.show()
                    update()
                }
                else {
                    val title = "폴더 추가 실패"
                    var message = "서버 문제로 인해 폴더 추가에 실패하였습니다."
                    var listener = DialogInterface.OnDismissListener { update() }

                    when(response.code()) {
                        400 -> {
                            message = "폴더이름이 형식과 맞지 않습니다.\n" +
                                    "폴더이름은 최대 10자만 가능하며, /는 포함될 수 없습니다."
                        }
                        404 -> {
                            message = "현재 폴더가 존재하지 않는 폴더입니다."
                            listener = DialogInterface.OnDismissListener {
                                selectPathActivity.supportFragmentManager.popBackStack()
                            }
                        }
                        409 -> {
                            message = "이미 해당 폴더가 존재합니다."
                        }
                    }

                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                val title = "폴더 추가 실패"
                val message = "서버와의 통신 문제로 폴더 추가에 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }
}