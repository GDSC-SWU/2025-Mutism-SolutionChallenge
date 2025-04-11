package com.example.mutism

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip

class NoiseSelectTabFragment : Fragment() {
    private var listener: TagSelectionListener? = null
    private var chipMap: MutableMap<String, Chip> = mutableMapOf()

    private lateinit var tagViewModel: TagViewModel

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

        tagViewModel = ViewModelProvider(requireActivity())[TagViewModel::class.java]

        if (context is TagSelectionListener) {
            listener = context
            Log.d("NoiseSelectTab", "listener 연결됨")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tab, container, false)
        val tagContainer = view.findViewById<LinearLayout>(R.id.tagContainer)

        tagViewModel.selectedTags.observe(viewLifecycleOwner) { selected ->
            refreshSelection(selected)
        }

        val tags = arguments?.getStringArrayList(ARG_TAGS) ?: emptyList<String>()
        for (tag in tags) {
            val chip =
                LayoutInflater
                    .from(requireContext())
                    .inflate(R.layout.view_custom_chip, tagContainer, false) as Chip
            chip.text = tag
            chip.setOnClickListener {
                Log.d("NoiseSelectTab", "Clicked: $tag, isChecked=${chip.isChecked}")
                if (chip.isChecked) {
                    tagViewModel.selectTag(tag)
                } else {
                    tagViewModel.deselectTag(tag)
                }
            }
            chipMap[tag] = chip
            tagContainer.addView(chip)
        }

        return view
    }

    fun refreshSelection(selectedTags: Set<String>) {
        chipMap.forEach { (tag, chip) ->
            chip.isChecked = selectedTags.contains(tag)
            chip.jumpDrawablesToCurrentState()
            chip.invalidate()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
