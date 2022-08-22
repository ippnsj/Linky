package org.poolc.linky

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import org.poolc.linky.databinding.FragmentMoreBinding

class MoreFragment : Fragment() {
    private lateinit var binding : FragmentMoreBinding
    private lateinit var mainActivity : MainActivity
    private lateinit var backCallback : OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMoreBinding.bind(view)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mainActivity.finish()
            }
        }
        mainActivity.onBackPressedDispatcher?.addCallback(this, backCallback)
    }

    override fun onDetach() {
        super.onDetach()
        backCallback.remove()
    }
}