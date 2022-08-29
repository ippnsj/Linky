package org.poolc.linky

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private lateinit var friendAdapter : FriendAdapter
    private val friends = ArrayList<User>()

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

        friendAdapter = FriendAdapter(friends, object : FriendAdapter.OnItemClickListener {
            override fun onItemClick(pos: Int) {

            }
        })

        with(binding) {
            friendRecyclerPreview.adapter = friendAdapter
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

            viewAll.setOnClickListener {
                mainActivity.changeChildFragment(FriendFragment(), null, true)
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
            val jsonObj = JSONObject(jsonStr)
            val imageUrl = jsonObj.getString("imageUrl")

            if(imageUrl != "") {
                // TODO url로부터 이미지 가져오는 작업
            }

            mainActivity.runOnUiThread {
                with(binding) {
                    nickname.text = jsonObj.getString("nickname")

                    if(imageUrl != "") {
                        // TODO url로부터 가져온 이미지 display
                    }
                }
            }
        }

        thread {
            friends.clear()

            val jsonStr = app.getFriends()

            mainActivity.runOnUiThread {
                val jsonObj = JSONObject(jsonStr)
                val numberOfRequest = jsonObj.getString("numberOfRequest").toInt()
                val numberOfFriend = jsonObj.getString("numberOfFriend").toInt()
                val friendsJsonArr = jsonObj.getJSONArray("friends")
                var limit = numberOfFriend

                if(numberOfFriend == 0) {
                    binding.noFriendNotice.visibility = View.VISIBLE
                }
                else {
                    binding.noFriendNotice.visibility = View.INVISIBLE
                }

                if(limit > 5) {
                    limit = 5
                }

                for(i in 0 until limit) {
                    val friendJsonObj = friendsJsonArr.getJSONObject(i)
                    val email = friendJsonObj.getString("email")
                    val nickname = friendJsonObj.getString("nickname")
                    val imageUrl = friendJsonObj.getString("imageUrl")

                    val friend = User(email, nickname, imageUrl)
                    friends.add(friend)
                }

                friendAdapter.notifyDataSetChanged()
            }
        }
    }
}