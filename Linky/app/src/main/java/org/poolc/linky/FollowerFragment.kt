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

    private fun showDialog(title:String, message:String, listener:DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(mainActivity)
        builder.setOnDismissListener(listener)

        builder.setIcon(R.drawable.ic_baseline_warning_8)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("??????", null)

        builder.show()
    }

    fun update() {
        mainActivity.setTopbarTitle("FollowerFragment")

        followers.clear()
        binding.totalFollower.text = "0"

        val call = MyApplication.service.getFollower()

        call.enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if(response.isSuccessful) {
                    setFollower(response.body()!!.asJsonObject)
                }
                else {
                    val title = "????????? ?????? ????????? ??????"
                    var message = "?????? ????????? ????????? ????????? ??????????????? ?????????????????????."
                    var listener: DialogInterface.OnDismissListener? = null

                    when(response.code()) {
                        404 -> {
                            message = "?????? ????????? ???????????? ?????? ?????? ???????????? ?????????."
                            listener = DialogInterface.OnDismissListener {
                                MyApplication.sharedPref.edit().remove("token").apply()

                                val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                        }
                    }

                    followerAdapter.notifyDataSetChanged()
                    showDialog(title, message, listener)
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                followerAdapter.notifyDataSetChanged()

                val title = "????????? ?????? ????????? ??????"
                val message = "???????????? ?????? ????????? ????????? ????????? ??????????????? ?????????????????????.\n" +
                        "????????? ?????? ??????????????????."
                showDialog(title, message, null)
            }
        })
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
                val isFollowing = followerJsonObj.get("isFollowing").asBoolean

                val follow = User(email, nickname, imageUrl, isFollowing)
                followers.add(follow)
            }

            binding.totalFollower.text = numberOfFollower
            followerAdapter.notifyDataSetChanged()
        }
        else {
            followerAdapter.notifyDataSetChanged()

            val title = "????????? ?????? ????????? ??????"
            val message = "?????? ????????? ????????? ????????? ??????????????? ?????????????????????.\n" +
                    "????????? ?????? ??????????????????."
            showDialog(title, message, null)
        }
    }
}