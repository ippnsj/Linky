package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.poolc.linky.databinding.ActivityUserBinding

class UserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserBinding
    private var userLinkyFragment: UserLinkyFragment? = null
    private var owner = "other"
    private var email = ""
    private var path = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        owner = intent.getStringExtra("owner") ?: "other"
        email = intent.getStringExtra("email") ?: ""
        path = intent.getStringExtra("path") ?: ""

        setUserLinkyFragment()
    }

    private fun setUserLinkyFragment() {
        userLinkyFragment = UserLinkyFragment()

        val tran = supportFragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putString("owner", owner)
        bundle.putString("email", email)
        bundle.putString("path", path)
        userLinkyFragment!!.arguments = bundle

        tran.replace(R.id.user_activity_fragment, userLinkyFragment!!)

        tran.commit()
    }

    override fun onBackPressed() {
        if(userLinkyFragment != null) {
            if(userLinkyFragment!!.childFragmentManager.backStackEntryCount > 0) {
                userLinkyFragment!!.childFragmentManager.popBackStackImmediate()
            }
            else {
                userLinkyFragment = null
                super.onBackPressed()
            }
        }
        else {
            super.onBackPressed()
        }
    }
}