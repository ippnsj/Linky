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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import org.poolc.linky.databinding.DialogInputtext10limitBinding
import org.poolc.linky.databinding.FragmentEditLinkySubBinding
import kotlin.concurrent.thread
import kotlin.math.ceil

class EditLinkySubFragment : Fragment() {
    private lateinit var binding : FragmentEditLinkySubBinding
    private lateinit var editActivity: EditActivity
    private lateinit var app : MyApplication
    private lateinit var path : String
    private val folders = ArrayList<Folder>()
    private val links = ArrayList<Link>()
    private lateinit var folderSubAdapter : FolderSubAdapter
    private lateinit var linkySubAdapter: LinkyAdapter
    private var totalSelectedFolder = 0
    private var totalSelectedLink = 0
    private var selectedFolders = ArrayList<String>()
    private var selectedLinks = ArrayList<String>()
    private val editLinkResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == AppCompatActivity.RESULT_OK) {
            val responseCode = result.data?.getIntExtra("responseCode", -1)

            if (responseCode == 200) {
                val toast = Toast.makeText(
                    editActivity,
                    "링크 수정이 완료되었습니다~!",
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
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(
                                dialog: DialogInterface?,
                                which: Int
                            ) {
                                val editSharedPref = MyApplication.sharedPref.edit()
                                editSharedPref.remove("email").apply()

                                val intent = Intent(editActivity, LoginRegisterActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                        }
                    }
                    else -> {
                        message = "서버 문제로 링크 수정에 실패하였습니다.\n" +
                                "잠시후 다시 시도해주세요."
                    }
                }

                builder.setIcon(R.drawable.ic_baseline_warning_8)
                builder.setTitle("수정 실패")
                builder.setMessage(message)

                builder.setPositiveButton("확인", positiveButtonFunc)

                builder.show()
            }
        }
    }
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
    private val moveLinkResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == AppCompatActivity.RESULT_OK) {
                val responseCode = result.data?.getIntExtra("responseCode", -1)

                if (responseCode == 200) {
                    val toast = Toast.makeText(
                        editActivity,
                        "링크 이동이 완료되었습니다~!",
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
                            message = "이동하고자 하는 링크 중 존재하지 않는 링크가 있습니다."
                        }
                        else -> {
                            message = "링크 이동에 실패하였습니다."
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
        val view = inflater.inflate(R.layout.fragment_edit_linky_sub, container, false)
        binding = FragmentEditLinkySubBinding.bind(view)

        folderSubAdapter = FolderSubAdapter(folders, object : FolderSubAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                folders[pos].switchIsSelected()
                totalSelectedFolder = if(folders[pos].getIsSelected()) { totalSelectedFolder + 1 } else { totalSelectedFolder - 1 }
                folderSubAdapter.notifyItemChanged(pos)
            }
        }, true)

        linkySubAdapter = LinkyAdapter(links, object : LinkyAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                links[pos].switchIsSelected()
                totalSelectedLink = if(links[pos].getIsSelected()) { totalSelectedLink + 1 } else { totalSelectedLink - 1 }
                linkySubAdapter.notifyItemChanged(pos)
            }
        }, true)

        with(binding) {
            folderSubRecycler.adapter = folderSubAdapter
            linkySubRecycler.adapter = linkySubAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            folderSubRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)

                    if(pos != 0) {
                        outRect.left = 60
                    }
                }
            })

            linkySubRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    val size = parent.size
                    val rows = ceil((size / 2).toDouble())

                    if(pos % 2 == 0) {
                        outRect.right = 20
                    }
                    else {
                        outRect.left = 20
                    }

                    if(pos > (rows - 1) * 2) {
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

    private fun setLinks(jsonStr: String) {
        val jsonObj = JSONObject(jsonStr)
        val linksArr = jsonObj.getJSONArray("linkInfos")
        for (idx in 0 until linksArr.length()) {
            val linkObj = linksArr.getJSONObject(idx)
            val id = linkObj.getString("id")
            val keywordsArr = linkObj.getJSONArray("keywords")
            val title = linkObj.getString("name")
            val imgUrl = linkObj.getString("imageUrl")
            val url = linkObj.getString("url")
            val isPublic = linkObj.getString("isPublic")

            val link = Link(id, keywordsArr, title, imgUrl, url, isPublic, false)
            links.add(link)
        }
    }

    fun update() {
        thread {
            folders.clear()
            links.clear()
            totalSelectedFolder = 0
            totalSelectedLink = 0

            val jsonStr = app.read(path, true)

            if (jsonStr != "") {
                setFolders(jsonStr!!)
                setLinks(jsonStr!!)
            }

            editActivity.runOnUiThread {
                folderSubAdapter.notifyDataSetChanged()
                linkySubAdapter.notifyDataSetChanged()
            }
        }
    }

    fun selectAllFolders() {
        if(totalSelectedFolder == folders.size) {
            for (pos in 0 until folders.size) {
                folders[pos].setIsSelected(false)
                totalSelectedFolder--
                folderSubAdapter.notifyItemChanged(pos)
            }
        }
        else {
            for (pos in 0 until folders.size) {
                if (!folders[pos].getIsSelected()) {
                    folders[pos].setIsSelected(true)
                    totalSelectedFolder++
                    folderSubAdapter.notifyItemChanged(pos)
                }
            }
        }
    }

    fun selectAllLinks() {
        if(totalSelectedLink == links.size) {
            for (pos in 0 until links.size) {
                links[pos].setIsSelected(false)
                totalSelectedLink--
                linkySubAdapter.notifyItemChanged(pos)
            }
        }
        else {
            for (pos in 0 until links.size) {
                if (!links[pos].getIsSelected()) {
                    links[pos].setIsSelected(true)
                    totalSelectedLink++
                    linkySubAdapter.notifyItemChanged(pos)
                }
            }
        }
    }

    private fun getSelectedFolders() {
        selectedFolders.clear()

        for(pos in 0 until folders.size) {
            if(folders[pos].getIsSelected()) {
                selectedFolders.add("$path${folders[pos].getFolderName()}")
            }
        }
    }

    private fun getSelectedLinks() {
        selectedLinks.clear()

        for(pos in 0 until links.size) {
            if(links[pos].getIsSelected()) {
                selectedLinks.add(links[pos].getId())
            }
        }
    }

    fun edit(target:String) {
        when(target) {
            "folder" -> {
                if(totalSelectedFolder == 0) {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("수정 실패")
                    builder.setMessage("수정할 폴더를 선택해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
                else if(totalSelectedFolder > 1) {
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
            "link" -> {
                if(totalSelectedLink == 0) {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("수정 실패")
                    builder.setMessage("수정할 링크를 선택해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
                else if(totalSelectedLink > 1) {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("수정 실패")
                    builder.setMessage("수정할 링크를 하나만 선택해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
                else {
                    editLink()
                }
            }
        }
    }

    private fun editFolder(newFolderName:String) {
        getSelectedFolders()
        val prevPath = selectedFolders[0]

        thread {
            val responseCode = app.editFolder("$prevPath/", newFolderName)

            if(responseCode == 200) {
                editActivity.runOnUiThread {
                    val toast = Toast.makeText(
                        editActivity,
                        "폴더 수정이 완료되었습니다~!",
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

    private fun editLink() {
        getSelectedLinks()
        val id = selectedLinks[0]
        val intent = Intent(editActivity, AddLinkyActivity::class.java)
        intent.putExtra("purpose", "edit")
        intent.putExtra("path", path)
        intent.putExtra("id", id)
        editLinkResultLauncher.launch(intent)
    }

    fun move(target:String) {
        when(target) {
            "folder" -> {
                if(totalSelectedFolder == 0) {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("이동 실패")
                    builder.setMessage("이동할 폴더를 선택해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
                else {
                    moveFolder()
                }
            }
            "link" -> {
                if(totalSelectedLink == 0) {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("이동 실패")
                    builder.setMessage("이동할 링크를 선택해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
                else {
                    moveLink()
                }
            }
        }
    }

    private fun moveFolder() {
        getSelectedFolders()

        val intent = Intent(editActivity, SelectPathActivity::class.java)
        intent.putExtra("path", path)
        intent.putExtra("folders", selectedFolders)
        intent.putExtra("purpose", "move")
        intent.putExtra("target", "folder")
        moveFolderResultLauncher.launch(intent)
    }

    private fun moveLink() {
        getSelectedLinks()

        val intent = Intent(editActivity, SelectPathActivity::class.java)
        intent.putExtra("path", path)
        intent.putExtra("links", selectedLinks)
        intent.putExtra("purpose", "move")
        intent.putExtra("target", "link")
        moveLinkResultLauncher.launch(intent)
    }

    fun delete(target:String) {
        when(target) {
            "folder" -> {
                if(totalSelectedFolder == 0) {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("삭제 실패")
                    builder.setMessage("삭제할 폴더를 선택해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
                else {
                    deleteFolder()
                }
            }
            "link" -> {
                if(totalSelectedLink == 0) {
                    val builder = AlertDialog.Builder(editActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("삭제 실패")
                    builder.setMessage("삭제할 링크를 선택해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
                else {
                    deleteLink()
                }
            }
        }
    }

    private fun deleteFolder() {
        thread {
            getSelectedFolders()

            val responseCode = app.deleteFolder(selectedFolders)

            if(responseCode == 200) {
                editActivity.runOnUiThread {
                    val toast = Toast.makeText(
                        editActivity,
                        "폴더 삭제가 완료되었습니다~!",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                    update()
                }
            }
            else if (responseCode != 200) {
                var positiveButtonFunc: DialogInterface.OnClickListener? = null
                var message = ""

                when (responseCode) {
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
                        message = "존재하지 않는 폴더가 있습니다."
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                update()
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

                editActivity.runOnUiThread {
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

    private fun deleteLink() {
        thread {
            getSelectedLinks()

            val responseCode = app.deleteLink(path, selectedLinks)

            if(responseCode == 200) {
                editActivity.runOnUiThread {
                    val toast = Toast.makeText(
                        editActivity,
                        "링크 삭제가 완료되었습니다~!",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                    update()
                }
            }
            else if (responseCode != 200) {
                var positiveButtonFunc: DialogInterface.OnClickListener? = null
                var message = ""

                when (responseCode) {
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
                        message = "해당 경로에 삭제하고자 하는 링크가 존재하지 않습니다."
                        positiveButtonFunc = object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                update()
                            }
                        }
                    }
                    else -> {
                        message = "링크 삭제에 실패하였습니다."
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
                    builder.setTitle("삭제 실패")
                    builder.setMessage(message)

                    builder.setPositiveButton("확인", positiveButtonFunc)

                    builder.show()
                }
            }
        }
    }
}