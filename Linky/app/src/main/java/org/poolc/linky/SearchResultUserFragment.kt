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
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.poolc.linky.databinding.FragmentSearchResultUserBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val email = MyApplication.sharedPref.getString("email", "")
        val keyword = model.searchText.value

        val call = MyApplication.service.searchUser(email!!, keyword!!)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setSearchResult(response.body()!!.asJsonObject)
                }
                else {
                    mainActivity.runOnUiThread {
                        users.clear()
                        userSearchAdapter.notifyDataSetChanged()
                    }

                    var message = ""
                    var positiveButtonFunc: DialogInterface.OnClickListener? = null

                    when(response.code()) {
                        401 -> {
                            message = "사용자 인증 오류로 인해 자동 로그아웃 됩니다."
                            positiveButtonFunc = object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    val editSharedPref = MyApplication.sharedPref.edit()
                                    editSharedPref.remove("email").apply()

                                    val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                }
                            }
                        }
                        else -> {
                            message = "유저 검색에 실패하였습니다."
                        }
                    }

                    mainActivity.runOnUiThread {
                        val builder = AlertDialog.Builder(mainActivity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("유저 검색 실패")
                        builder.setMessage(message)

                        builder.setPositiveButton("확인", positiveButtonFunc)

                        builder.show()
                    }
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                mainActivity.runOnUiThread {
                    users.clear()
                    userSearchAdapter.notifyDataSetChanged()

                    val builder = AlertDialog.Builder(mainActivity)

                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("유저 검색 실패")
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
            users.clear()
            val usersArr = jsonObj.getAsJsonArray("users")

            for(i in 0 until usersArr.size()) {
                val userObj = usersArr[i].asJsonObject

                val email = userObj.get("email").asString
                val nickname = userObj.get("nickname").asString
                val imageUrl = userObj.get("imageUrl").asString
                val isFollowing = userObj.get("isFollowing").asString.toBoolean()

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