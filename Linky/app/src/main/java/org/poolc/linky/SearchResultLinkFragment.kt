package org.poolc.linky

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import org.poolc.linky.databinding.FragmentSearchResultLinkBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.ceil

class SearchResultLinkFragment : Fragment(), Observer<String> {
    private lateinit var binding: FragmentSearchResultLinkBinding
    private lateinit var mainActivity: MainActivity
    private val model: SearchViewModel by activityViewModels()

    private val links = ArrayList<Link>()
    private lateinit var linkSearchAdapter: LinkySearchAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_result_link, container, false)
        binding = FragmentSearchResultLinkBinding.bind(view)

        linkSearchAdapter = LinkySearchAdapter(links, object : LinkySearchAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(links[pos].getUrl()))
                startActivity(intent)
            }
        })

        binding.linkRecycler.adapter = linkSearchAdapter

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

    fun update() {
        if(model.searchText.value == "") {
            binding.guidetextLinkSearch.visibility = View.VISIBLE

            links.clear()
            linkSearchAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextLinkSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }

    private fun getSearchResult() {
        val email = MyApplication.sharedPref.getString("email", "")
        val keyword = model.searchText.value
        val searchMe = "false"

        val call = MyApplication.service.searchLink(email!!, keyword!!, searchMe)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setSearchResult(response.body()!!.asJsonObject)
                }
                else {
                    mainActivity.runOnUiThread {
                        links.clear()
                        linkSearchAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                mainActivity.runOnUiThread {
                    links.clear()
                    linkSearchAdapter.notifyDataSetChanged()

                    val builder = AlertDialog.Builder(mainActivity)

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

    private fun setSearchResult(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            links.clear()
            val followingLinks = jsonObj.getAsJsonArray("followingLinks")
            val notFollowingLinks = jsonObj.getAsJsonArray("notFollowingLinks")

            for(i in 0 until followingLinks.size()) {
                val followingLink = followingLinks[i].asJsonObject
                val linkId = followingLink.get("linkId").asString
                val keywords = followingLink.getAsJsonArray("keywords")
                val keywordJsonArr = JSONArray()
                for(keyword in keywords) {
                    keywordJsonArr.put(keyword)
                }
                val linkName = followingLink.get("linkName").asString
                val imageUrl = followingLink.get("imageUrl").asString
                val url = followingLink.get("url").asString

                val nickName = followingLink.get("nickName").asString
                val ownerId = followingLink.get("ownerId").asString
                val path = followingLink.get("path").asString

                val link = Link(linkId, keywordJsonArr, linkName, imageUrl, url, nickName, ownerId, path, true)
                links.add(link)
            }

            for(i in 0 until notFollowingLinks.size()) {
                val notFollowingLink = notFollowingLinks[i].asJsonObject
                val linkId = notFollowingLink.get("linkId").asString
                val keywords = notFollowingLink.getAsJsonArray("keywords")
                val keywordJsonArr = JSONArray()
                for(keyword in keywords) {
                    keywordJsonArr.put(keyword)
                }
                val linkName = notFollowingLink.get("linkName").asString
                val imageUrl = notFollowingLink.get("imageUrl").asString
                val url = notFollowingLink.get("url").asString

                val nickName = notFollowingLink.get("nickName").asString
                val ownerId = notFollowingLink.get("ownerId").asString
                val path = notFollowingLink.get("path").asString

                val link = Link(linkId, keywordJsonArr, linkName, imageUrl, url, nickName, ownerId, path, false)
                links.add(link)
            }

            linkSearchAdapter.notifyDataSetChanged()
        }
        else {
            links.clear()
            linkSearchAdapter.notifyDataSetChanged()
        }
    }

    override fun onChanged(t: String?) {
        if(t == "") {
            binding.guidetextLinkSearch.visibility = View.VISIBLE

            links.clear()
            linkSearchAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextLinkSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }
}