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
import org.poolc.linky.databinding.FragmentSearchResultUserBinding
import org.poolc.linky.viewmodel.SearchViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.ceil

class SearchResultUserFragment : Fragment(), Observer<String> {
    private lateinit var binding: FragmentSearchResultUserBinding
    private lateinit var mainActivity: MainActivity
    private val model: SearchViewModel by activityViewModels()

    private val users = ArrayList<User>()
    private lateinit var userSearchAdapter: FollowAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_result_user, container, false)
        binding = FragmentSearchResultUserBinding.bind(view)

        userSearchAdapter = FollowAdapter(users, object : FollowAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(mainActivity, UserActivity::class.java)
                intent.putExtra("owner", "other")
                intent.putExtra("email", users[pos].getEmail())
                intent.putExtra("path", "")
                startActivity(intent)
            }
        })

        binding.userRecycler.adapter = userSearchAdapter

        model.searchText.observe(viewLifecycleOwner, this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            userRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    val size = parent.size
                    val rows = ceil((size / 3).toDouble())

                    outRect.left = 10
                    outRect.right = 10

                    if(pos > (rows - 1) * 3) {
                        outRect.top = 10
                        outRect.bottom = 20
                    }
                    else {
                        outRect.top = 10
                        outRect.bottom = 10
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
            binding.guidetextUserSearch.visibility = View.VISIBLE

            users.clear()
            userSearchAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextUserSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }

    private fun getSearchResult() {
        val keyword = model.searchText.value

        val call = MyApplication.service.searchUser(keyword!!)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setSearchResult(response.body()!!.asJsonObject)
                }
                else {
                    users.clear()
                    userSearchAdapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                users.clear()
                userSearchAdapter.notifyDataSetChanged()

                val title = "유저 검색 실패"
                val message = "서버와의 통신 문제로 검색 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun setSearchResult(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            users.clear()
            val usersArr = jsonObj.getAsJsonArray("users")

            for(i in 0 until usersArr.size()) {
                val userObj = usersArr[i].asJsonObject

                val email = userObj.get("email").asString
                val nickname = userObj.get("nickname").asString
                val imageUrl = userObj.get("imageUrl").asString
                val isFollowing = userObj.get("isFollowing").asBoolean

                val user = User(email, nickname, imageUrl, isFollowing)

                users.add(user)
            }

            userSearchAdapter.notifyDataSetChanged()
        }
        else {
            users.clear()
            userSearchAdapter.notifyDataSetChanged()
        }
    }

    override fun onChanged(t: String?) {
        if(t == "") {
            binding.guidetextUserSearch.visibility = View.VISIBLE

            users.clear()
            userSearchAdapter.notifyDataSetChanged()
        }
        else {
            binding.guidetextUserSearch.visibility = View.INVISIBLE

            getSearchResult()
        }
    }
}