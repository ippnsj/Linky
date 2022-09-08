package org.poolc.linky

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentUserLinkyBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class UserLinkyFragment : Fragment() {
    private lateinit var binding: FragmentUserLinkyBinding
    private lateinit var activity: Activity
    private lateinit var app: MyApplication

    private lateinit var owner : String
    private lateinit var email : String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as Activity
        app = activity.application as MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        owner = arguments?.getString("owner") ?: ""
        email = arguments?.getString("email") ?: ""
        val path = arguments?.getString("path") ?: ""

        setFragment(path, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_linky, container, false)
        binding = FragmentUserLinkyBinding.bind(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun update() {
        when(owner) {
            "me" -> {
                setProfile()
            }
            "other" -> {

            }
        }
    }

    private fun setProfile() {
        thread {
            val jsonStr = app.getProfile()

            if (jsonStr != "") {
                val jsonObj = JSONObject(jsonStr)

                val nickname = jsonObj.getString("nickname")
                val imageUrl = jsonObj.getString("imageUrl")

                activity.runOnUiThread {
                    binding.userNickname.text = nickname
                }

                if (imageUrl != "") {
                    try {
                        val url: URL? = URL(imageUrl)
                        val conn: HttpURLConnection? =
                            url?.openConnection() as HttpURLConnection
                        val image = BitmapFactory.decodeStream(conn?.inputStream)
                        activity.runOnUiThread {
                            binding.userImage.setImageBitmap(image)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                activity.runOnUiThread {
                    val builder = AlertDialog.Builder(activity)
                    builder.setIcon(R.drawable.ic_baseline_warning_8)
                    builder.setTitle("유저 정보 불러오기 실패")
                    builder.setMessage("유저 정보를 불러오는데 실패하여 이전으로 돌아갑니다.")
                    builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                        activity.finish()
                    }
                    builder.show()
                }
            }
        }
    }

    fun setFragment(path: String, addToBackStack: Boolean) {
        var fragment : Fragment? = null

        when(path) {
            "" -> {
                fragment = UserLinkyOutsideFolderFragment()
            }
            else -> {
                fragment = UserLinkyInsideFolderFragment()
            }
        }

        val tran = childFragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putString("owner", owner)
        bundle.putString("email", email)
        bundle.putString("path", path)
        fragment.arguments = bundle

        tran.replace(R.id.user_linky_fragment_container, fragment)

        if(addToBackStack) {
            tran.addToBackStack(null)
        }

        tran.commit()
    }
}