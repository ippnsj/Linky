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
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.poolc.linky.databinding.FragmentFollowerBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread
import kotlin.math.ceil

class FollowerFragment : Fragment() {
    private lateinit var binding: FragmentFollowerBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication
    private val followers = ArrayList<User>()
    private lateinit var followerAdapter : FollowAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_follower, container, false)
        binding = FragmentFollowerBinding.bind(view)

        followerAdapter = FollowAdapter(followers, object : FollowAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(mainActivity, UserActivity::class.java)
                intent.putExtra("owner", "other")
                intent.putExtra("email", followers[pos].getEmail())
                intent.putExtra("path", "")
                startActivity(intent)
            }
        })

        with(binding) {
            followerRecycler.adapter = followerAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            followerRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        mainActivity.setTopbarTitle("FollowerFragment")

        thread {
            followers.clear()
            mainActivity.runOnUiThread {
                binding.totalFollower.text = "0"
            }

            val email = MyApplication.sharedPref.getString("email", "")
            val call = MyApplication.service.getFollower(email!!)

            call.enqueue(object : Callback<JsonElement> {
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    if(response.isSuccessful) {
                        setFollower(response.body()!!.asJsonObject)
                    }
                    else {
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
                            404 -> {
                                message = "해당 유저가 존재하지 않아 자동 로그아웃 됩니다."
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
                                mainActivity.runOnUiThread {
                                    followerAdapter.notifyDataSetChanged()
                                }

                                message = "서버 문제로 팔로우 정보를 가져오는데 실패하였습니다.\n" +
                                        "잠시후 다시 시도해주세요."
                            }
                        }

                        mainActivity.runOnUiThread {
                            val builder = AlertDialog.Builder(mainActivity)

                            builder.setIcon(R.drawable.ic_baseline_warning_8)
                            builder.setTitle("팔로우 정보 업로드 실패")
                            builder.setMessage(message)

                            builder.setPositiveButton("확인", positiveButtonFunc)

                            builder.show()
                        }
                    }
                }

                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    mainActivity.runOnUiThread {
                        followerAdapter.notifyDataSetChanged()

                        val builder = AlertDialog.Builder(mainActivity)

                        builder.setIcon(R.drawable.ic_baseline_warning_8)
                        builder.setTitle("팔로우 정보 업로드 실패")
                        builder.setMessage("서버 문제로 팔로우 정보를 가져오는데 실패하였습니다.\n" +
                                "잠시후 다시 시도해주세요.")

                        builder.setPositiveButton("확인", null)

                        builder.show()
                    }
                }
            })
        }
    }

    private fun setFollower(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            val numberOfFollower = jsonObj.get("numberOfFollower").asString
            val followersJsonArr = jsonObj.getAsJsonArray("followers")

            for (i in 0 until followersJsonArr.size()) {
                val followerJsonObj = followersJsonArr.get(i).asJsonObject
                val email = followerJsonObj.get("email").asString
                val nickname = followerJsonObj.get("nickname").asString
                val image = followerJsonObj.get("imageUrl")
                var imageUrl = ""
                if (!image.isJsonNull) {
                    imageUrl = followerJsonObj.get("imageUrl").asString
                }
                val following = followerJsonObj.get("following").asBoolean

                val follow = User(email, nickname, imageUrl, following)
                followers.add(follow)
            }

            binding.totalFollower.text = numberOfFollower
            followerAdapter.notifyDataSetChanged()
        }
        else {
            followerAdapter.notifyDataSetChanged()

            val builder = AlertDialog.Builder(mainActivity)

            builder.setIcon(R.drawable.ic_baseline_warning_8)
            builder.setTitle("팔로우 정보 업로드 실패")
            builder.setMessage("서버 문제로 팔로우 정보를 가져오는데 실패하였습니다.\n" +
                    "잠시후 다시 시도해주세요.")

            builder.setPositiveButton("확인", null)

            builder.show()
        }
    }
}