package org.poolc.linky

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import org.poolc.linky.databinding.ActivityUserBinding

class UserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userEmail = intent.getStringExtra("userEmail")
        val bundle = Bundle()
        bundle.putString("userEmail", userEmail)
        changeFragment(UserInfoFragment(), bundle, false)

        with(binding) {

        }
    }

    fun changeFragment(fragment: Fragment, bundle:Bundle?, addToStack:Boolean) {
        val tran = supportFragmentManager.beginTransaction()

        if(bundle != null) {
            fragment.arguments = bundle
        }

        tran.replace(R.id.user_activity_container, fragment)

        if(addToStack) {
            tran.addToBackStack(null)
        }

        tran.commit()
    }
}