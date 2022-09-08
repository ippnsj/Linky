package org.poolc.linky

import android.content.Context
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.json.JSONArray
import org.poolc.linky.databinding.FragmentSearchMeResultLinkBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.ceil

class SearchMeResultLinkFragment : Fragment(), Observer<String> {
    private lateinit var binding: FragmentSearchMeResultLinkBinding
    private lateinit var searchMeActivity: SearchMeActivity
    private lateinit var app: MyApplication
    private val model: SearchViewModel by activityViewModels()

    private val links = ArrayList<Link>()
    private lateinit var linkAdapter: LinkyAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        searchMeActivity = context as SearchMeActivity
        app = searchMeActivity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_me_result_link, container, false)
        binding = FragmentSearchMeResultLinkBinding.bind(view)

        linkAdapter = LinkyAdapter(links, object : LinkyAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(links[pos].getUrl()))
                startActivity(intent)
            }
        }, false)

        binding.linkRecycler.adapter = linkAdapter

        model.searchText.observe(viewLifecycleOwner, this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
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

    private fun update() {
        if(model.searchText.value == "") {
            binding.guidetextLinkSearch.visibility = View.VISIBLE

            links.clear()
            linkAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextLinkSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }

    private fun setSearchResult(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            links.clear()
            val myLinks = jsonObj.getAsJsonArray("myLinks")

            for(i in 0 until myLinks.size()) {
                val myLink = myLinks[i].asJsonObject
                val linkId = myLink.get("linkId").asString
                val keywords = myLink.getAsJsonArray("keywords")
                val keywordJsonArr = JSONArray()
                for(keyword in keywords) {
                    keywordJsonArr.put(keyword)
                }
                val linkName = myLink.get("linkName").asString
                val imageUrl = myLink.get("imageUrl").asString
                val url = myLink.get("url").asString

                val nickName = myLink.get("nickName").asString
                val ownerId = myLink.get("ownerId").asString
                val path = myLink.get("path").asString

                val link = Link(linkId, keywordJsonArr, linkName, imageUrl, url, nickName, ownerId, path)
                links.add(link)
            }

            linkAdapter.notifyDataSetChanged()
        }
        else {
            links.clear()
            linkAdapter.notifyDataSetChanged()
        }
    }

    private fun getSearchResult() {
        val email = MyApplication.sharedPref.getString("email", "")
        val keyword = model.searchText.value
        val searchMe = "true"

        val call = MyApplication.service.searchLink(email!!, keyword!!, searchMe)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setSearchResult(response.body()!!.asJsonObject)
                }
                else {
                    searchMeActivity.runOnUiThread {
                        links.clear()
                        linkAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                searchMeActivity.runOnUiThread {
                    links.clear()
                    linkAdapter.notifyDataSetChanged()

                    val builder = AlertDialog.Builder(searchMeActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("검색 실패")
                    builder.setMessage("서버 문제로 검색 정보를 가져오는데 실패하였습니다.\n" +
                            "잠시후 다시 시도해주세요.")

                    builder.setPositiveButton("확인", null)

                    builder.show()
                }
            }
        })
    }

    override fun onChanged(t: String?) {
        if(t == "") {
            binding.guidetextLinkSearch.visibility = View.VISIBLE

            links.clear()
            linkAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextLinkSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }
}