package org.poolc.linky

import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.poolc.linky.databinding.AddLinkDialogBinding
import org.poolc.linky.databinding.DialogInputtext10limitBinding
import org.poolc.linky.databinding.FragmentLinkyInsideFolderBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.ceil

class LinkyInsideFolderFragment : Fragment() {
    private lateinit var binding : FragmentLinkyInsideFolderBinding
    private val folders = ArrayList<Folder>()
    private val links = ArrayList<Link>()
    private lateinit var folderSubAdapter: FolderSubAdapter
    private lateinit var linkySubAdapter: LinkyAdapter
    private lateinit var path : String
    private lateinit var folderName : String
    private var isFabOpen = false
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = arguments?.getString("path") ?: ""
        folderName = arguments?.getString("folderName") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_linky_inside_folder, container, false)
        binding = FragmentLinkyInsideFolderBinding.bind(view)

        folderSubAdapter = FolderSubAdapter(folders, object : FolderSubAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                val newFolderName = folders[pos].getFolderName()
                val newPath = "${path}${newFolderName}/"
                val bundle = Bundle()
                bundle.putString("path", newPath)
                bundle.putString("folderName", newFolderName)

                mainActivity.changeChildFragment(LinkyInsideFolderFragment(), bundle, true)
            }
        }, false)

        linkySubAdapter = LinkyAdapter(links, object : LinkyAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(links[pos].getUrl()))
                startActivity(intent)
            }
        }, false)

        with(binding) {
            folderSubRecycler.adapter = folderSubAdapter
            linkySubRecycler.adapter = linkySubAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isFabOpen = false

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
                val intent = Intent(mainActivity, SearchMeActivity::class.java)
                startActivity(intent)
            }

            edit.setOnClickListener {
                val intent = Intent(mainActivity, EditActivity::class.java)
                intent.putExtra("path", path)
                startActivity(intent)
            }

            addFolderSub.setOnClickListener {
                val builder = AlertDialog.Builder(mainActivity)
                builder.setTitle("추가할 폴더명을 입력해주세요")

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

            addLinkySub.setOnClickListener {
                val builder = AlertDialog.Builder(mainActivity)
                builder.setTitle("추가할 링크를 입력해주세요")
                builder.setIcon(R.drawable.add_link_pink)

                val dialogView = layoutInflater.inflate(R.layout.add_link_dialog, null)
                val dialogBinding = AddLinkDialogBinding.bind(dialogView)

                builder.setView(dialogView)

                builder.setPositiveButton("추가", null)
                builder.setNegativeButton("취소", null)

                val dialog = builder.create()

                dialog.show()

                val possitiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                possitiveButton.setOnClickListener {
                    val url = dialogBinding.newUrl.text.toString()
                    if(url == "") {
                        dialogBinding.newUrl.error = "url을 입력해주세요."
                    }
                    else {
                        toggleFab()
                        val imm = mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(dialogBinding.newUrl.windowToken, 0)
                        dialog?.dismiss()
                        val intent = Intent(mainActivity, AddLinkyActivity::class.java)
                        intent.putExtra("url", url)
                        startActivity(intent)
                    }
                }

                dialogBinding.newUrl.setOnEditorActionListener { v, actionId, event ->
                    if(actionId == EditorInfo.IME_ACTION_DONE) {
                        possitiveButton.performClick()
                        true
                    }
                    false
                }
            }

            addSub.setOnClickListener {
                toggleFab()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(mainActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    fun update() {
        // activity에 path값 넘김
        mainActivity.setPath(path)
        mainActivity.setFolderName(folderName)

        mainActivity.setTopbarTitle("LinkyInsideFolderFragment")

        folders.clear()
        links.clear()

        val call = MyApplication.service.read(path, "true")

        call.enqueue(object: Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setFolders(response.body()!!.asJsonObject)
                    setLinks(response.body()!!.asJsonObject)
                }
                else {
                    val title = "폴더 읽어오기 실패"
                    var message = "서버 문제로 인해 정보를 가져오는데 실패하였습니다."
                    var listener: DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "현재 폴더가 존재하지 않는 폴더입니다."
                            listener = DialogInterface.OnDismissListener {
                                requireParentFragment().childFragmentManager.popBackStack()
                            }
                        }
                    }

                    folderSubAdapter.notifyDataSetChanged()
                    linkySubAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                folderSubAdapter.notifyDataSetChanged()
                linkySubAdapter.notifyDataSetChanged()

                val title = "폴더 읽어오기 실패"
                val message = "서버와의 통신 문제로 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun toggleFab() {
        with(binding) {
            if (isFabOpen) {
                ObjectAnimator.ofFloat(addFolderSub, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(addLinkySub, "translationY", 0f).apply { start() }
                addSub.setImageResource(R.drawable.add)
            } else {
                ObjectAnimator.ofFloat(addFolderSub, "translationY", -400f).apply { start() }
                ObjectAnimator.ofFloat(addLinkySub, "translationY", -200f).apply { start() }
                addSub.setImageResource(R.drawable.close)
            }
        }

        isFabOpen = !isFabOpen
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
        }
        else {
            val title = "폴더 읽어오기 실패"
            val message = "서버 문제로 인해 정보를 가져오는데 실패하였습니다."
            showDialog(title, message, null)
        }

        folderSubAdapter.notifyDataSetChanged()
    }

    private fun setLinks(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            val linksArr = jsonObj.getAsJsonArray("linkInfos")
            for (idx in 0 until linksArr.size()) {
                val linkObj = linksArr.get(idx).asJsonObject
                val id = linkObj.get("id").asString
                val keywords = linkObj.getAsJsonArray("keywords")
                val keywordsArr = JSONArray()
                for(keyword in keywords) {
                    keywordsArr.put(keyword.asString)
                }
                val title = linkObj.get("name").asString
                val imgUrl = linkObj.get("imageUrl").asString
                val url = linkObj.get("url").asString
                val isPublic = linkObj.get("isPublic").asString

                val link = Link(id, keywordsArr, title, imgUrl, url, isPublic, false)
                links.add(link)
            }
        }

        linkySubAdapter.notifyDataSetChanged()
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
                        Toast.makeText(mainActivity, "새 폴더가 추가되었습니다!", Toast.LENGTH_SHORT)
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
                                requireParentFragment().childFragmentManager.popBackStack()
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