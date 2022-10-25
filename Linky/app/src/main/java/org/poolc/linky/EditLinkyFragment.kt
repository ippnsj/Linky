package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.poolc.linky.databinding.DialogInputtext10limitBinding
import org.poolc.linky.databinding.FragmentEditLinkyBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
                        "폴더 이동이 완료되었습니다~!",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                } else {
                    val title = "폴더 이동 실패"
                    var message = "서버 문제로 인해 폴더 이동에 실패하였습니다."
                    var listener: DialogInterface.OnDismissListener? = null

                    when (responseCode) {
                        404 -> {
                            message = "존재하지 않는 폴더가 있습니다."
                        }
                        409 -> {
                            message = "이동하고자 하는 경로에 이미 해당 폴더가 존재합니다."
                        }
                    }

                    showDialog(title, message, listener)
                }
            } else if(result.resultCode == AppCompatActivity.RESULT_CANCELED) {
                val reason = result.data?.getStringExtra("reason") ?: ""
                val title = "폴더 이동 실패"
                var message = ""

                when(reason) {
                    "same path" -> {
                        message = "이동하고자 하는 경로와 현재 경로가 같습니다."
                        showDialog(title, message, null)
                    }
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

            searchbarTextInput.setOnClickListener {
                val intent = Intent(editActivity, SearchMeActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(editActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    private fun setFolders(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            val foldersArr = jsonObj.getAsJsonArray("folderInfos")
            for (idx in 0 until foldersArr.size()) {
                val folderObj = foldersArr.get(idx).asJsonObject
                val folderName = folderObj.get("name").asString

                val folder = Folder(folderName, false)
                folders.add(folder)
            }
        }else {
            val title = "폴더 읽어오기 실패"
            val message = "서버 문제로 정보를 가져오는데 실패하였습니다.\n" +
                    "잠시후 다시 시도해주세요."
            showDialog(title, message, null)
        }

        folderAdapter.notifyDataSetChanged()
    }

    private fun update() {
        folders.clear()
        totalSelected = 0

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
                                editActivity.finish()
                            }
                        }
                    }

                    folderAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                val title = "폴더 읽어오기 실패"
                val message = "서버와의 통신 문제로 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."

                folderAdapter.notifyDataSetChanged()
                showDialog(title, message, null)
            }
        })
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
        val title = "폴더 수정 실패"
        var message = ""

        if(totalSelected == 0) {
            message = "수정할 폴더를 선택해주세요."
            showDialog(title, message, null)
        }
        else if(totalSelected > 1) {
            message = "수정할 폴더를 하나만 선택해주세요."
            showDialog(title, message, null)
        }
        else {
            val builder = AlertDialog.Builder(editActivity)
            builder.setTitle("새로운 폴더명을 입력해주세요")
            builder.setIcon(R.drawable.edit_pink)

            val dialogView = layoutInflater.inflate(R.layout.dialog_inputtext_10limit, null)
            val dialogBinding = DialogInputtext10limitBinding.bind(dialogView)

            builder.setView(dialogView)

            builder.setPositiveButton("수정") { dialogInterface: DialogInterface, i: Int ->
                val newFolderName = dialogBinding.inputText.text?.trim().toString()
                if(newFolderName == "") {
                    dialogBinding.inputText.error = "앞/뒤 공백 없이 1자 이상의 폴더명을 입력해주세요."
                }
                else if(newFolderName!!.contains("/")) {
                    dialogBinding.inputText.error = "폴더명에는 /가 포함될 수 없습니다."
                }
                else {
                    editFolder(newFolderName)
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

    private fun editFolder(newFolderName:String) {
        getSelectedFolders()
        val prevFolderName = selectedFolders[0]

        val jsonObj = JSONObject()
        jsonObj.put("path", "$prevFolderName/")
        jsonObj.put("newName", newFolderName)
        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

        val call = MyApplication.service.editFolder(body)

        call.enqueue(object: Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if(response.isSuccessful) {
                    val toast = Toast.makeText(
                        editActivity,
                        "폴더 수정이 완료되었습니다~!",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                    update()
                }
                else {
                    val title = "폴더 수정 실패"
                    var message = "서버 문제로 폴더 수정에 실패하였습니다."
                    var listener = DialogInterface.OnDismissListener {
                        update()
                    }

                    when(response.code()) {
                        400 -> {
                            message = "폴더이름 형식이 잘못되었습니다."
                        }
                        404 -> {
                            message = "현재 폴더가 존재하지 않는 폴더입니다."
                            listener = DialogInterface.OnDismissListener {
                                editActivity.finish()
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
                val title = "폴더 수정 실패"
                val message = "서버와의 통신 문제로 폴더 수정에 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    fun move() {
        val title = "폴더 이동 실패"
        var message = ""

        if(totalSelected == 0) {
            message = "이동할 폴더를 선택해주세요."
            showDialog(title, message, null)
        }
        else {
            getSelectedFolders()

            val intent = Intent(editActivity, SelectPathActivity::class.java)
            intent.putExtra("path", path)
            intent.putExtra("folders", selectedFolders)
            intent.putExtra("purpose", "move")
            intent.putExtra("target", "folder")
            moveFolderResultLauncher.launch(intent)
        }
    }

    fun delete() {
        val title = "폴더 삭제 실패"
        var message = ""

        if(totalSelected == 0) {
            message = "삭제할 폴더를 선택해주세요."
            showDialog(title, message, null)
        }
        else {
            getSelectedFolders()

            val jsonObj = JSONObject()
            jsonObj.put("paths", JSONArray(selectedFolders))
            val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

            val call = MyApplication.service.deleteFolder(body)

            call.enqueue(object: Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if(response.isSuccessful) {
                        val toast = Toast.makeText(
                            editActivity,
                            "폴더 삭제가 완료되었습니다~!",
                            Toast.LENGTH_SHORT
                        )
                        toast.show()
                        update()
                    }
                    else {
                        val title = "폴더 삭제 실패"
                        var message = "서버 문제로 인해 폴더 삭제에 실패하였습니다."
                        var listener = DialogInterface.OnDismissListener { update() }

                        when(response.code()) {
                            404 -> {
                                message = "존재하지 않는 폴더가 있습니다."
                            }
                        }

                        showDialog(title, message, listener)
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    val title = "폴더 삭제 실패"
                    var message = "서버와의 통신 문제로 인해 폴더 삭제에 실패하였습니다."
                    showDialog(title, message, null)
                }
            })
        }
    }
}