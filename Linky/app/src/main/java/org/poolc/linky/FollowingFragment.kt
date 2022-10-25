package org.poolc.linky

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import org.poolc.linky.databinding.DialogInputtext10limitBinding
import org.poolc.linky.databinding.FragmentFollowingBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern
import kotlin.math.ceil

class FollowingFragment : Fragment() {
    private lateinit var binding : FragmentFollowingBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication
    private val followings = ArrayList<User>()
    private lateinit var followingAdapter : FollowAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_following, container, false)
        binding = FragmentFollowingBinding.bind(view)

        followingAdapter = FollowAdapter(followings, object : FollowAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(mainActivity, UserActivity::class.java)
                intent.putExtra("owner", "other")
                intent.putExtra("email", followings[pos].getEmail())
                intent.putExtra("path", "")
                startActivity(intent)
            }
        })

        with(binding) {
            followingRecycler.adapter = followingAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            followingRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

            addFriend.setOnClickListener {
                showDropdown(addFriend)
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
        mainActivity.setTopbarTitle("FollowingFragment")

        followings.clear()
        binding.totalFollowing.text = "0"

        val call = MyApplication.service.getFollowing()

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setFollowing(response.body()!!.asJsonObject)
                }
                else {
                    val title = "팔로우 정보 업로드 실패"
                    var message = "서버 문제로 인해 팔로우 정보를 가져오는데 실패하였습니다."
                    var listener: DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "해당 유저가 존재하지 않아 자동 로그아웃 됩니다."
                            listener = DialogInterface.OnDismissListener {
                                MyApplication.sharedPref.edit().remove("token").apply()

                                val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                        }
                    }

                    followingAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                followingAdapter.notifyDataSetChanged()

                val title = "팔로우 정보 업로드 실패"
                val message = "서버와의 통신 문제로 팔로우 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun setFollowing(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            val numberOfFollowing = jsonObj.get("numberOfFollowing").asString
            val followingsJsonArr = jsonObj.getAsJsonArray("followings")

            for (i in 0 until followingsJsonArr.size()) {
                val followingJsonObj = followingsJsonArr.get(i).asJsonObject
                val email = followingJsonObj.get("email").asString
                val nickname = followingJsonObj.get("nickname").asString
                val image = followingJsonObj.get("imageUrl")
                var imageUrl = ""
                if (!image.isJsonNull) {
                    imageUrl = followingJsonObj.get("imageUrl").asString
                }
                val isFollowing = followingJsonObj.get("isFollowing").asBoolean

                val follow = User(email, nickname, imageUrl, isFollowing)
                followings.add(follow)
            }

            binding.totalFollowing.text = numberOfFollowing
            followingAdapter.notifyDataSetChanged()
        }
        else {
            followingAdapter.notifyDataSetChanged()

            val title = "팔로우 정보 업로드 실패"
            val message = "서버 문제로 인해 팔로우 정보를 가져오는데 실패하였습니다."
            showDialog(title, message, null)
        }
    }

    private fun showDropdown(view:View) {
        val popup = PopupMenu(mainActivity, view, Gravity.END)
        popup.menuInflater.inflate(R.menu.menu_add_follow, popup.menu)
        popup.show()

        popup.setOnMenuItemClickListener(popupListener)
    }

    private val popupListener = object : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when(item?.itemId) {
                R.id.add_by_nickname -> {
                    val builder = AlertDialog.Builder(mainActivity)
                    builder.setIcon(R.drawable.ic_baseline_person_add_24)
                    builder.setTitle("팔로우할 닉네임을 입력해주세요")

                    val dialogView = layoutInflater.inflate(R.layout.dialog_inputtext_10limit, null)
                    val dialogBinding = DialogInputtext10limitBinding.bind(dialogView)

                    builder.setView(dialogView)

                    builder.setPositiveButton("추가") { dialogInterface: DialogInterface, i: Int ->
                        val nickname = dialogBinding.inputText.text?.trim().toString()
                        val nicknameTrim = nickname.trim()
                        val pattern = Pattern.compile("^[ㄱ-ㅣ가-힣a-zA-Z\\s]+$")

                        if(nicknameTrim == "") {
                            dialogBinding.inputText.error = "닉네임은 앞/뒤 공백 없이 1자 이상 입력해주세요."
                        }
                        else if(!pattern.matcher(nicknameTrim).matches()) {
                            dialogBinding.inputText.error = "닉네임은 한글, 영어, 사이 공백만 가능합니다."
                        }
                        else {
                            addFollowing(nicknameTrim)
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
                R.id.add_by_kakaotalk -> {

                }
            }

            return true
        }
    }

    private fun addFollowing(nickname: String) {
        val jsonObj = JSONObject()
        jsonObj.put("followedNickname", nickname)

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObj.toString())

        val call = MyApplication.service.follow(body)

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    val toast = Toast.makeText(mainActivity, "팔로우에 성공하였습니다~!", Toast.LENGTH_SHORT)
                    toast.show()
                    addNewFollowing(response.body()!!.asJsonObject)
                }
                else {
                    val title = "팔로우 실패"
                    var message = "서버 문제로 인해 팔로우에 실패하였습니다."

                    when(response.code()) {
                        404 -> {
                            message = "해당 유저가 존재하지 않습니다."
                        }
                        409 -> {
                            message = "이미 해당 유저를 팔로우 중입니다."
                        }
                    }

                    showDialog(title, message, null)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                val title = "팔로우 실패"
                val message = "서버와의 통신 문제로 팔로우에 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun addNewFollowing(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            val email = jsonObj.get("email").asString
            val nickname = jsonObj.get("nickname").asString
            val imageUrl = jsonObj.get("imageUrl").asString ?: ""
            val isFollowing = jsonObj.get("isFollowing").asBoolean

            val follow = User(email, nickname, imageUrl, isFollowing)
            followings.add(follow)

            binding.totalFollowing.text = followings.size.toString()
            followingAdapter.notifyItemInserted(followings.size - 1)
        }
        else {
            val title = "팔로우 정보 업로드 실패"
            val message = "서버 문제로 인해 추가된 팔로우 정보를 가져오는데 실패하였습니다."
            showDialog(title, message, null)
        }
    }
}