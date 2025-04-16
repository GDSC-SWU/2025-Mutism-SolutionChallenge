package com.example.mutism

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mutism.model.TagSection
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class NoiseSelectTabFragment : Fragment() {
    private var listener: TagSelectionListener? = null
    private lateinit var tagViewModel: TagViewModel
    private lateinit var chipMap: MutableMap<String, Chip>
    private lateinit var categoryContainer: LinearLayout

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
        categoryContainer = view.findViewById<LinearLayout>(R.id.categoryContainer)
        chipMap = mutableMapOf()

        tagViewModel.selectedTags.observe(viewLifecycleOwner) { selected ->
            refreshSelection(selected)
        }

        val sections = arguments?.getParcelableArrayList<Bundle>(ARG_SECTIONS)
        sections?.forEach { sectionBundle ->
            val category = sectionBundle.getString("category") ?: return@forEach
            val tags = sectionBundle.getStringArrayList("tags") ?: return@forEach
            addCategorySection(category, tags, inflater)
        }
        return view
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun addCategorySection(
        category: String,
        tags: List<String>,
        inflater: LayoutInflater,
    ) {
        val sectionView = inflater.inflate(R.layout.view_chip_section, categoryContainer, false)
        val titleTextView = sectionView.findViewById<TextView>(R.id.categoryTitle)
        val chipGroup = sectionView.findViewById<ChipGroup>(R.id.chipGroup)

        titleTextView.text = category

        for (tag in tags) {
            val chip = inflater.inflate(R.layout.view_custom_chip, chipGroup, false) as Chip
            chip.text = tag
            chip.setOnClickListener {
                if (chip.isChecked) {
                    tagViewModel.selectTag(tag)
                } else {
                    tagViewModel.deselectTag(tag)
                }
            }
            chipMap[tag] = chip
            chipGroup.addView(chip)
        }

        categoryContainer.addView(sectionView)
    }

    fun refreshSelection(selectedTags: Set<String>) {
        chipMap.forEach { (tag, chip) ->
            chip.isChecked = selectedTags.contains(tag)
            chip.jumpDrawablesToCurrentState()
            chip.invalidate()
        }
    }

    companion object {
        private const val ARG_SECTIONS = "sections"

        fun newInstance(
            title: String,
            sections: List<TagSection>,
        ): NoiseSelectTabFragment {
            val fragment = NoiseSelectTabFragment()
            val args =
                Bundle().apply {
                    putString("title", title)
                    putParcelableArrayList(
                        ARG_SECTIONS,
                        ArrayList(
                            sections.map {
                                Bundle().apply {
                                    putString("category", it.category)
                                    putStringArrayList("tags", ArrayList(it.tags))
                                }
                            },
                        ),
                    )
                }
            fragment.arguments = args
            return fragment
        }
    }
}
