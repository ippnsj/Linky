package org.poolc.linky

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentInformationBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class InformationFragment : Fragment() {
    private lateinit var binding : FragmentInformationBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication
    private lateinit var followingAdapter : FollowAdapter
    private lateinit var followerAdapter : FollowAdapter
    private val followings = ArrayList<User>()
    private val followers = ArrayList<User>()

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

        followingAdapter = FollowAdapter(followings, object : FollowAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {

            }
        })

        followerAdapter = FollowAdapter(followers, object : FollowAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {

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

            viewAllFollower.setOnClickListener {

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

    fun update() {
        mainActivity.setTopbarTitle("InformationFragment")

        thread {
            val jsonStr = app.getProfile()
            if(jsonStr != "") {
                val jsonObj = JSONObject(jsonStr)
                val imageUrl = jsonObj.getString("imageUrl")

                mainActivity.runOnUiThread {
                    binding.nickname.text = jsonObj.getString("nickname")
                }

                if (imageUrl != "") {
                    try {
                        val url: URL? = URL(imageUrl)
                        val conn: HttpURLConnection? =
                            url?.openConnection() as HttpURLConnection
                        val image = BitmapFactory.decodeStream(conn?.inputStream)
                        mainActivity.runOnUiThread {
                            binding.profileImage.setImageBitmap(image)
                        }
                    }
                    catch (e:Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        thread {
            followings.clear()
            followers.clear()

            val jsonStr = app.getFollowPreview()

            if(jsonStr != "") {
                val jsonObj = JSONObject(jsonStr)
                val numberOfFollowing = jsonObj.getString("numberOfFollowing").toInt()
                val numberOfFollower = jsonObj.getString("numberOfFollower").toInt()

                mainActivity.runOnUiThread {
                    binding.followings.text = numberOfFollowing.toString()
                    binding.followers.text = numberOfFollower.toString()
                }

                if(numberOfFollowing == 0) {
                    mainActivity.runOnUiThread {
                        binding.noFollowingNotice.visibility = View.VISIBLE
                    }
                }
                else {
                    mainActivity.runOnUiThread {
                        binding.noFollowingNotice.visibility = View.INVISIBLE
                    }

                    val followingJsonArr = jsonObj.getJSONArray("followings")

                    for(i in 0 until followingJsonArr.length()) {
                        val followingJsonObj = followingJsonArr.getJSONObject(i)
                        val email = followingJsonObj.getString("email")
                        val nickname = followingJsonObj.getString("nickname")
                        val imageUrl = followingJsonObj.getString("imageUrl")
                        val following = followingJsonObj.getString("following").toBoolean()

                        val follow = User(email, nickname, imageUrl, following)
                        followings.add(follow)
                    }
                }

                if(numberOfFollower == 0) {
                    mainActivity.runOnUiThread {
                        binding.noFollowerNotice.visibility = View.VISIBLE
                    }
                }
                else {
                    mainActivity.runOnUiThread {
                        binding.noFollowerNotice.visibility = View.INVISIBLE
                    }

                    val followerJsonArr = jsonObj.getJSONArray("followers")

                    for(i in 0 until followerJsonArr.length()) {
                        val followerJsonObj = followerJsonArr.getJSONObject(i)
                        val email = followerJsonObj.getString("email")
                        val nickname = followerJsonObj.getString("nickname")
                        val imageUrl = followerJsonObj.getString("imageUrl")
                        val following = followerJsonObj.getString("following").toBoolean()

                        val follow = User(email, nickname, imageUrl, following)
                        followers.add(follow)
                    }
                }
            }

            mainActivity.runOnUiThread {
                followingAdapter.notifyDataSetChanged()
                followerAdapter.notifyDataSetChanged()
            }
        }
    }
}