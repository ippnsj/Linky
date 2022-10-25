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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.poolc.linky.databinding.FragmentInformationBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class InformationFragment : Fragment() {
    private lateinit var binding : FragmentInformationBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication
    private lateinit var followingAdapter : FollowPreviewAdapter
    private lateinit var followerAdapter : FollowPreviewAdapter
    private val followings = ArrayList<User>()
    private val followers = ArrayList<User>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_information, container, false)
        binding = FragmentInformationBinding.bind(view)

        followingAdapter = FollowPreviewAdapter(followings, object : FollowPreviewAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(mainActivity, UserActivity::class.java)
                intent.putExtra("owner", "other")
                intent.putExtra("email", followings[pos].getEmail())
                intent.putExtra("path", "")
                startActivity(intent)
            }
        })

        followerAdapter = FollowPreviewAdapter(followers, object : FollowPreviewAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {
                val intent = Intent(mainActivity, UserActivity::class.java)
                intent.putExtra("owner", "other")
                intent.putExtra("email", followers[pos].getEmail())
                intent.putExtra("path", "")
                startActivity(intent)
            }
        })

        with(binding) {
            followingRecyclerPreview.adapter = followingAdapter
            followerRecyclerPreview.adapter = followerAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            followingRecyclerPreview.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)

                    if(pos != 0) {
                        outRect.left = 20
                    }
                }
            })

            followerRecyclerPreview.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)

                    if(pos != 0) {
                        outRect.left = 20
                    }
                }
            })

            profileContainer.setOnClickListener {
                mainActivity.changeChildFragment(EditProfileFragment(), null, true)
            }

            logout.setOnClickListener {
                val editSharedPref = MyApplication.sharedPref.edit()
                editSharedPref.remove("token").apply()

                val toast = Toast.makeText(mainActivity, "로그아웃 되었습니다.", Toast.LENGTH_SHORT)
                toast.show()

                val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

            viewAllFollowing.setOnClickListener {
                mainActivity.changeChildFragment(FollowingFragment(), null, true)
            }

            viewAllFollower.setOnClickListener {
                mainActivity.changeChildFragment(FollowerFragment(), null, true)
            }

            viewTerms.setOnClickListener {
                mainActivity.changeChildFragment(TermsFragment(), null, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun showDialog(title:String, message:String, listener: DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(mainActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("확인", null)

        builder.show()
    }

    fun update() {
        mainActivity.setTopbarTitle("InformationFragment")

        val call = MyApplication.service.getProfile()

        call.enqueue(object: Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setProfile(response.body()!!.asJsonObject)
                    getFollowPreview()
                }
                else {
                    val title = "유저 정보 가져오기 실패"
                    var message = "서버 문제로 인해 유저 정보를 가져오는데 실패하였습니다."
                    var listener:DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "존재하지 않는 유저입니다.\n" +
                                    "자동 로그아웃됩니다."
                            listener = DialogInterface.OnDismissListener {
                                MyApplication.sharedPref.edit().remove("token").apply()
                                val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                        }
                    }

                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                val title = "유저 정보 가져오기 실패"
                val message = "서버와의 통신 문제로 유저 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun setProfile(jsonObj: JsonObject) {
        if(!jsonObj.isJsonNull) {
            val imageUrl = jsonObj.get("imageUrl").asString

            binding.nickname.text = jsonObj.get("nickname").asString

            if (imageUrl != "") {
                thread {
                    val image = app.getImageUrl(imageUrl)
                    if (image != null) {
                        mainActivity.runOnUiThread {
                            binding.profileImage.setImageBitmap(image)
                        }
                    }
                    else {
                        mainActivity.runOnUiThread {
                            binding.profileImage.setImageResource(R.drawable.profile)
                        }
                    }
                }
            }
            else {
                binding.profileImage.setImageResource(R.drawable.profile)
            }
        }
        else {
            val title = "유저 정보 가져오기 실패"
            var message = "서버 문제로 인해 유저 정보를 가져오는데 실패하였습니다."
            showDialog(title, message, null)
        }
    }

    private fun getFollowPreview() {
        followings.clear()
        followers.clear()

        val call = MyApplication.service.getFollowPreview()

        call.enqueue(object:Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setFollowPreview(response.body()!!.asJsonObject)
                }
                else {
                    val title = "유저 정보 가져오기 실패"
                    var message = "서버 문제로 인해 유저 정보를 가져오는데 실패하였습니다."
                    var listener:DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "존재하지 않는 유저입니다.\n" +
                                    "자동 로그아웃됩니다."
                            listener = DialogInterface.OnDismissListener {
                                MyApplication.sharedPref.edit().remove("token").apply()
                                val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                        }
                    }

                    followingAdapter.notifyDataSetChanged()
                    followerAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                followingAdapter.notifyDataSetChanged()
                followerAdapter.notifyDataSetChanged()

                val title = "유저 정보 가져오기 실패"
                val message = "서버와의 통신 문제로 유저 정보를 가져오는데 실패하였습니다.\n" +
                        "잠시후 다시 시도해주세요."
                showDialog(title, message, null)
            }
        })
    }

    private fun setFollowPreview(jsonObj:JsonObject) {
        if (!jsonObj.isJsonNull) {
            val numberOfFollowing = jsonObj.get("numberOfFollowing").asInt
            val numberOfFollower = jsonObj.get("numberOfFollower").asInt

            binding.followings.text = numberOfFollowing.toString()
            binding.followers.text = numberOfFollower.toString()

            if (numberOfFollowing == 0) {
                binding.noFollowingNotice.visibility = View.VISIBLE
            } else {
                binding.noFollowingNotice.visibility = View.INVISIBLE

                val followingJsonArr = jsonObj.get("followings").asJsonArray

                for (i in 0 until followingJsonArr.size()) {
                    val followingJsonObj = followingJsonArr.get(i).asJsonObject
                    val email = followingJsonObj.get("email").asString
                    val nickname = followingJsonObj.get("nickname").asString
                    val imageUrl = followingJsonObj.get("imageUrl").asString
                    val following = followingJsonObj.get("isFollowing").asBoolean

                    val follow = User(email, nickname, imageUrl, following)
                    followings.add(follow)
                }
            }

            if (numberOfFollower == 0) {
                binding.noFollowerNotice.visibility = View.VISIBLE
            } else {
                binding.noFollowerNotice.visibility = View.INVISIBLE

                val followerJsonArr = jsonObj.getAsJsonArray("followers")

                for (i in 0 until followerJsonArr.size()) {
                    val followerJsonObj = followerJsonArr.get(i).asJsonObject
                    val email = followerJsonObj.get("email").asString
                    val nickname = followerJsonObj.get("nickname").asString
                    val imageUrl = followerJsonObj.get("imageUrl").asString
                    val following = followerJsonObj.get("isFollowing").asBoolean

                    val follow = User(email, nickname, imageUrl, following)
                    followers.add(follow)
                }
            }
        }
        else {
            val title = "유저 정보 가져오기 실패"
            var message = "서버 문제로 인해 유저 정보를 가져오는데 실패하였습니다."
            showDialog(title, message, null)
        }

        followingAdapter.notifyDataSetChanged()
        followerAdapter.notifyDataSetChanged()
    }
}