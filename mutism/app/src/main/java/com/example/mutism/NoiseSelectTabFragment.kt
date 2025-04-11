package com.example.mutism

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip

class NoiseSelectTabFragment : Fragment() {
    private var listener: TagSelectionListener? = null

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_TAGS = "tags"

        fun newInstance(
            title: String,
            tags: List<String>,
        ): NoiseSelectTabFragment {
            val fragment = NoiseSelectTabFragment()
            val args =
                Bundle().apply {
                    putString(ARG_TITLE, title)
                    putStringArrayList(ARG_TAGS, ArrayList(tags))
                }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TagSelectionListener) {
            listener = context
            Log.d("NoiseSelectTab", "✅ listener 연결됨")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tab, container, false)
        val tagContainer = view.findViewById<LinearLayout>(R.id.tagContainer)

        val tags = arguments?.getStringArrayList(ARG_TAGS) ?: emptyList<String>()
        for (tag in tags) {
            val chip =
                Chip(requireContext()).apply {
                    text = tag
                    isClickable = true
                    isCheckable = true
                    setOnClickListener {
                        Log.d("NoiseSelectTab", "Clicked: $tag, isChecked=$isChecked")
                        if (isChecked) {
                            listener?.onTagSelected(tag)
                        } else {
                            listener?.onTagDeselected(tag)
                        }
                    }
                }
            tagContainer.addView(chip)
        }

        return view
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
