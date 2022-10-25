package org.poolc.linky

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.poolc.linky.databinding.FragmentLinkyBinding

class LinkyFragment : Fragment() {
    private lateinit var binding: FragmentLinkyBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var app: MyApplication

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        app = mainActivity.application as MyApplication
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_linky, container, false)
        binding = FragmentLinkyBinding.bind(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            mainActivity.changeChildFragment(LinkyOutsideFolderFragment(), null, false)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden) {
            val fragment = childFragmentManager.findFragmentById(R.id.linky_fragment_container)

            if(fragment is LinkyOutsideFolderFragment) {
                fragment.update()
            }
            else if(fragment is LinkyInsideFolderFragment) {
                fragment.update()
            }
        }
    }
}