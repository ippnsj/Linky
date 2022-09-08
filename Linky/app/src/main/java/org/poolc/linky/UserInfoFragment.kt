package org.poolc.linky

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.poolc.linky.databinding.FragmentUserInfoBinding
import kotlin.concurrent.thread

class UserInfoFragment : Fragment() {
    private lateinit var binding: FragmentUserInfoBinding
    private lateinit var userActivity: UserActivity
    private lateinit var app: MyApplication
    private lateinit var userEmail: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        userActivity = context as UserActivity
        app = userActivity.application as MyApplication
        userEmail = arguments?.getString("userEmail") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_info, container, false)
        binding = FragmentUserInfoBinding.bind(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun update() {
        thread {

        }
    }
}