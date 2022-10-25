package org.poolc.linky

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.json.JSONArray
import org.poolc.linky.databinding.FragmentUserLinkyInsideFolderBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.ceil

class UserLinkyInsideFolderFragment : Fragment() {
    private lateinit var binding: FragmentUserLinkyInsideFolderBinding
    private lateinit var activity: Activity
    private lateinit var app: MyApplication

    private val folders = ArrayList<Folder>()
    private val links = ArrayList<Link>()
    private lateinit var folderAdapter: FolderSubAdapter
    private lateinit var linkAdapter: LinkyAdapter
    private lateinit var owner : String
    private lateinit var email : String
    private lateinit var path : String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as Activity
        app = activity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        owner = arguments?.getString("owner") ?: ""
        email = arguments?.getString("email") ?: ""
        path = arguments?.getString("path") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_linky_inside_folder, container, false)
        binding = FragmentUserLinkyInsideFolderBinding.bind(view)

        folderAdapter = FolderSubAdapter(folders, object : FolderSubAdapter.OnItemClickListener {
            override fun onItemClick(pos:Int) {
                val folderName = folders[pos].getFolderName()
                val newPath = "${path}${folderName}/"

                val parent = parentFragment as UserLinkyFragment
                parent.setFragment(newPath, true)
            }
        }, false)

        linkAdapter = LinkyAdapter(links, object : LinkyAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(links[pos].getUrl()))
                startActivity(intent)
            }
        }, false)

        with(binding) {
            folderRecycler.adapter = folderAdapter
            linkRecycler.adapter = linkAdapter
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

                    if(pos != 0) {
                        outRect.left = 60
                    }
                }
            })

            linkRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(activity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    private fun update() {
        when(owner) {
            "me" -> {
                readMe()
            }
            "other" -> {
                readOther()
            }
        }
    }

    private fun readMe() {
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
                    var listener: DialogInterface.OnDismissListener? = null
                    var message = "서버 문제로 인해 정보를 가져오는데 실패하였습니다."

                    when(response.code()) {
                        404 -> {
                            message = "현재 폴더가 존재하지 않는 폴더입니다."
                            listener = DialogInterface.OnDismissListener {
                                when(activity) {
                                    is SearchMeActivity -> {
                                        (activity as SearchMeActivity).supportFragmentManager.popBackStack()
                                    }
                                    is UserActivity -> {
                                        activity.finish()
                                    }
                                }
                            }
                        }
                    }

                    folderAdapter.notifyDataSetChanged()
                    linkAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                folderAdapter.notifyDataSetChanged()
                linkAdapter.notifyDataSetChanged()

                val title = "폴더 읽어오기 실패"
                val message =  "서버와의 통신 문제로 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun readOther() {
        folders.clear()
        links.clear()

        val call = MyApplication.service.readOther(email, path)

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
                                when(activity) {
                                    is SearchMeActivity -> {
                                        (activity as SearchMeActivity).supportFragmentManager.popBackStack()
                                    }
                                    is UserActivity -> {
                                        activity.finish()
                                    }
                                }
                            }
                        }
                    }

                    folderAdapter.notifyDataSetChanged()
                    linkAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                folderAdapter.notifyDataSetChanged()
                linkAdapter.notifyDataSetChanged()

                val title = "폴더 읽어오기 실패"
                val message = "서버와의 통신 문제로 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
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

        folderAdapter.notifyDataSetChanged()
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

        linkAdapter.notifyDataSetChanged()
    }
}