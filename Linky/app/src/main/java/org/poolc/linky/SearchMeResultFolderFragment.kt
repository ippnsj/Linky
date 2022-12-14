package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
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
import org.poolc.linky.databinding.FragmentSearchMeResultFolderBinding
import org.poolc.linky.viewmodel.SearchViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.ceil

class SearchMeResultFolderFragment : Fragment(), Observer<String> {
    private lateinit var binding: FragmentSearchMeResultFolderBinding
    private lateinit var searchMeActivity: SearchMeActivity
    private lateinit var app: MyApplication
    private val model: SearchViewModel by activityViewModels()

    private val folders = ArrayList<Folder>()
    private lateinit var folderAdapter: FolderAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        searchMeActivity = context as SearchMeActivity
        app = searchMeActivity.application as MyApplication
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_me_result_folder, container, false)
        binding = FragmentSearchMeResultFolderBinding.bind(view)

        folderAdapter = FolderAdapter(folders, object : FolderAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                searchMeActivity.goToUserLinky(folders[pos].getOwnerEmail(), folders[pos].getPath())
            }
        }, false)

        binding.folderRecycler.adapter = folderAdapter

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
        val builder = AlertDialog.Builder(searchMeActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("??????", null)

        builder.show()
    }

    private fun update() {
        if(model.searchText.value == "") {
            binding.guidetextFolderSearch.visibility = View.VISIBLE

            folders.clear()
            folderAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextFolderSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }

    private fun setSearchResult(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            folders.clear()
            val myFolders = jsonObj.getAsJsonArray("myFolders")

            for(i in 0 until myFolders.size()) {
                val myFolder = myFolders[i].asJsonObject
                val name = myFolder.get("name").asString
                val nickName = myFolder.get("nickName").asString
                val ownerEmail = myFolder.get("ownerEmail").asString
                val path = myFolder.get("path").asString

                val folder = Folder(name, nickName, ownerEmail, path)
                folders.add(folder)
            }

            folderAdapter.notifyDataSetChanged()
        }
        else {
            folders.clear()
            folderAdapter.notifyDataSetChanged()
        }
    }

    private fun getSearchResult() {
        val keyword = model.searchText.value
        val searchMe = "true"

        val call = MyApplication.service.searchFolder(keyword!!, searchMe)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setSearchResult(response.body()!!.asJsonObject)
                }
                else {
                    folders.clear()
                    folderAdapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                folders.clear()
                folderAdapter.notifyDataSetChanged()

                val title = "?????? ?????? ??????"
                val message = "???????????? ?????? ????????? ?????? ????????? ??????????????? ?????????????????????.\n" +
                        "????????? ?????? ??????????????????."
                showDialog(title, message, null)
            }
        })
    }

    override fun onChanged(t: String?) {
        if(t == "") {
            binding.guidetextFolderSearch.visibility = View.VISIBLE

            folders.clear()
            folderAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextFolderSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }
}