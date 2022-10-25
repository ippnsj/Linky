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
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.poolc.linky.databinding.FragmentSearchResultFolderBinding
import org.poolc.linky.viewmodel.SearchViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.collections.ArrayList
import kotlin.math.ceil

class SearchResultFolderFragment : Fragment(), Observer<String> {
    private lateinit var binding: FragmentSearchResultFolderBinding
    private lateinit var mainActivity: MainActivity
    private val model: SearchViewModel by activityViewModels()

    private val folders = ArrayList<Folder>()
    private lateinit var folderSearchAdapter: FolderSearchAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_result_folder, container, false)
        binding = FragmentSearchResultFolderBinding.bind(view)

        folderSearchAdapter = FolderSearchAdapter(folders, object : FolderSearchAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(mainActivity, UserActivity::class.java)
                intent.putExtra("owner", "other")
                intent.putExtra("email", folders[pos].getOwnerEmail())
                intent.putExtra("path", folders[pos].getPath())
                startActivity(intent)
            }
        })

        binding.folderRecycler.adapter = folderSearchAdapter

        model.searchText.observe(viewLifecycleOwner, this)

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

                    if(pos < 3) {
                        outRect.top = 40
                        outRect.bottom = 20
                    }
                    else if(pos > (rows - 1) * 3) {
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
        val builder = AlertDialog.Builder(mainActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    fun update() {
        if(model.searchText.value == "") {
            binding.guidetextFolderSearch.visibility = View.VISIBLE

            folders.clear()
            folderSearchAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextFolderSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }

    private fun setSearchResult(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            folders.clear()
            val followingFolders = jsonObj.getAsJsonArray("followingFolders")
            val notFollowingFolders = jsonObj.getAsJsonArray("notFollowingFolders")

            for(i in 0 until followingFolders.size()) {
                val followingFolder = followingFolders[i].asJsonObject
                val name = followingFolder.get("name").asString
                val nickName = followingFolder.get("nickName").asString
                val ownerEmail = followingFolder.get("ownerEmail").asString
                val path = followingFolder.get("path").asString

                val folder = Folder(name, nickName, ownerEmail, path, true)
                folders.add(folder)
            }

            for(i in 0 until notFollowingFolders.size()) {
                val notFollowingFolder = notFollowingFolders[i].asJsonObject
                val name = notFollowingFolder.get("name").asString
                val nickName = notFollowingFolder.get("nickName").asString
                val ownerEmail = notFollowingFolder.get("ownerEmail").asString
                val path = notFollowingFolder.get("path").asString

                val folder = Folder(name, nickName, ownerEmail, path, false)
                folders.add(folder)
            }

            folderSearchAdapter.notifyDataSetChanged()
        }
        else {
            folders.clear()
            folderSearchAdapter.notifyDataSetChanged()
        }
    }

    private fun getSearchResult() {
        val keyword = model.searchText.value
        val searchMe = "false"

        val call = MyApplication.service.searchFolder(keyword!!, searchMe)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setSearchResult(response.body()!!.asJsonObject)
                }
                else {
                    folders.clear()
                    folderSearchAdapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                folders.clear()
                folderSearchAdapter.notifyDataSetChanged()

                val title = "폴더 검색 실패"
                val message = "서버와의 통신 문제로 검색 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    override fun onChanged(t: String?) {
        if(t == "") {
            binding.guidetextFolderSearch.visibility = View.VISIBLE

            folders.clear()
            folderSearchAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextFolderSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }
}