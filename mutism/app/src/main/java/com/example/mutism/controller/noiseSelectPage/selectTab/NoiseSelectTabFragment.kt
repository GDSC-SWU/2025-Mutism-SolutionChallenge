package com.example.mutism.controller.noiseSelectPage.selectTab

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mutism.R
import com.example.mutism.model.noiseTag.TagSection
import com.example.mutism.viewmodel.TagViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class NoiseSelectTabFragment : Fragment() {
    private lateinit var tagViewModel: TagViewModel
    private lateinit var chipMap: MutableMap<String, Chip>
    private lateinit var categoryContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.noise_select_fragment_tab, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel and views
        tagViewModel = ViewModelProvider(requireActivity())[TagViewModel::class.java]
        chipMap = mutableMapOf()
        categoryContainer = view.findViewById(R.id.categoryContainer)

        // Observe selected tags and update UI when changed
        tagViewModel.selectedTags.observe(viewLifecycleOwner) { selected ->
            refreshSelection(selected)
        }

        // Retrieve section data from arguments (tag groupings)
        val sections: List<TagSection> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getParcelableArrayList(ARG_SECTIONS, TagSection::class.java)
            } else {
                @Suppress("DEPRECATION")
                arguments?.getParcelableArrayList(ARG_SECTIONS)
            } ?: emptyList()

        // For each section, add its title and tags as chips
        sections.forEach { section ->
            addCategorySection(section.category, section.tags, layoutInflater)
        }
    }

    // Adds a section view with a title and a ChipGroup containing all its tags
    private fun addCategorySection(
        category: String,
        tags: List<String>,
        inflater: LayoutInflater,
    ) {
        val sectionView = inflater.inflate(R.layout.view_chip_section, categoryContainer, false)
        val titleTextView = sectionView.findViewById<TextView>(R.id.categoryTitle)
        val chipGroup = sectionView.findViewById<ChipGroup>(R.id.chipGroup)

        titleTextView.text = category

        tags.forEach { tag ->
            val chip = createChip(inflater, chipGroup, tag)
            chipGroup.addView(chip)
            chipMap[tag] = chip
        }

        categoryContainer.addView(sectionView)
    }

    // Creates a Chip view for a given tag and sets selection behavior
    private fun createChip(
        inflater: LayoutInflater,
        parent: ViewGroup,
        tag: String,
    ): Chip {
        val chip = inflater.inflate(R.layout.view_custom_chip, parent, false) as Chip
        chip.text = tag
        chip.setOnClickListener {
            if (chip.isChecked) {
                tagViewModel.selectTag(tag) // Add to selected tags
            } else {
                tagViewModel.deselectTag(tag) // Remove from selected tags
            }
        }
        return chip
    }

    // Updates chip checked states to reflect current ViewModel selected tags
    private fun refreshSelection(selectedTags: Set<String>) {
        chipMap.forEach { (tag, chip) ->
            chip.isChecked = selectedTags.contains(tag)
            chip.jumpDrawablesToCurrentState()
            chip.invalidate()
        }
    }

    companion object {
        private const val ARG_SECTIONS = "sections"
        private const val ARG_TITLE = "title"

        // Creates a new fragment instance with given tab title and tag sections
        fun newInstance(
            title: String,
            sections: List<TagSection>,
        ): NoiseSelectTabFragment =
            NoiseSelectTabFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_TITLE, title)
                        putParcelableArrayList(ARG_SECTIONS, ArrayList(sections))
                    }
            }
    }
}
