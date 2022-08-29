package org.poolc.linky

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.json.JSONObject
import org.poolc.linky.databinding.FragmentInformationBinding
import kotlin.concurrent.thread

class InformationFragment : Fragment() {
    private lateinit var binding : FragmentInformationBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            profileContainer.setOnClickListener {
                mainActivity.changeChildFragment(EditProfileFragment(), null, true)
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
    }
}