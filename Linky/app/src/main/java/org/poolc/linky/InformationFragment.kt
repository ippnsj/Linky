package org.poolc.linky

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentInformationBinding
import kotlin.concurrent.thread

class InformationFragment : Fragment() {
    private lateinit var binding : FragmentInformationBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication
    private lateinit var followingAdapter : FriendAdapter
    private val followings = ArrayList<User>()

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
        val view = inflater.inflate(R.layout.fragment_information, container, false)
        binding = FragmentInformationBinding.bind(view)

        followingAdapter = FriendAdapter(followings, object : FriendAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {

            }
        })

        with(binding) {
            followingRecyclerPreview.adapter = followingAdapter
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            profileContainer.setOnClickListener {
                mainActivity.changeChildFragment(EditProfileFragment(), null, true)
            }

            logout.setOnClickListener {
                val editSharedPref = MyApplication.sharedPref.edit()
                editSharedPref.remove("email").apply()

                val toast = Toast.makeText(mainActivity, "로그아웃 되었습니다.", Toast.LENGTH_SHORT)
                toast.show()

                val intent = Intent(mainActivity, LoginRegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

            viewAllFollowing.setOnClickListener {
                mainActivity.changeChildFragment(FollowingFragment(), null, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    fun update() {
        mainActivity.setTopbarTitle("InformationFragment")

        thread {
            val jsonStr = app.getProfile()
            if(jsonStr != "") {
                val jsonObj = JSONObject(jsonStr)
                val imageUrl = jsonObj.getString("imageUrl")

                if (imageUrl != "") {
                    // TODO url로부터 이미지 가져오는 작업
                }

                mainActivity.runOnUiThread {
                    with(binding) {
                        nickname.text = jsonObj.getString("nickname")

                        if (imageUrl != "") {
                            // TODO url로부터 가져온 이미지 display
                        }
                    }
                }
            }
        }

        thread {
            followings.clear()

            val jsonStr = app.getFriends()

            if(jsonStr != "") {
                mainActivity.runOnUiThread {
                    val jsonObj = JSONObject(jsonStr)
                    val numberOfFollowing = jsonObj.getString("numberOfFollowing").toInt()
                    val numberOfFollower = jsonObj.getString("numberOfFollower").toInt()
                    var limit = numberOfFollowing

                    binding.followings.text = numberOfFollowing.toString()
                    binding.followers.text = numberOfFollower.toString()

                    if(numberOfFollowing == 0) {
                        binding.noFollowingNotice.visibility = View.VISIBLE
                    }
                    else {
                        binding.noFollowingNotice.visibility = View.INVISIBLE

                        val followingJsonArr = jsonObj.getJSONArray("followings")

                        if(limit > 5) {
                            limit = 5
                        }

                        for(i in 0 until limit) {
                            val followingJsonObj = followingJsonArr.getJSONObject(i)
                            val email = followingJsonObj.getString("email")
                            val nickname = followingJsonObj.getString("nickname")
                            val imageUrl = followingJsonObj.getString("imageUrl")

                            val friend = User(email, nickname, imageUrl)
                            followings.add(friend)
                        }

                        followingAdapter.notifyDataSetChanged()
                    }

                    if(numberOfFollower == 0) {
                        binding.noFollowerNotice.visibility = View.VISIBLE
                    }
                    else {
                        binding.noFollowerNotice.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }
}