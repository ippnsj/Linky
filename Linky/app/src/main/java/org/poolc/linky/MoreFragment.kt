package org.poolc.linky

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.poolc.linky.databinding.FragmentMoreBinding

class MoreFragment : Fragment() {
    private lateinit var binding : FragmentMoreBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var app : MyApplication

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_more, container, false)
        binding = FragmentMoreBinding.bind(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            mainActivity.changeChildFragment(InformationFragment(), null, false)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if(!hidden) {
            val fragment = childFragmentManager.findFragmentById(R.id.more_fragment_container)

            if(fragment is InformationFragment) {
                fragment.update()
            }
            else if(fragment is EditProfileFragment) {
                fragment.update()
            }
            else if(fragment is FollowingFragment) {
                fragment.update()
            }
        }
    }
}