package com.example.mutism.controller.noiseSelectPage

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mutism.R
import com.example.mutism.databinding.ActivityNoiseSelectBinding
import com.example.mutism.manager.TagSelectionManager
import com.example.mutism.model.noiseTag.tagContents
import com.example.mutism.model.noiseTag.tagTabTitles
import com.example.mutism.view.adapter.NoiseViewPagerAdapter
import com.example.mutism.viewmodel.TagViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator

class NoiseSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoiseSelectBinding
    private lateinit var tagViewModel: TagViewModel
    private lateinit var tagManager: TagSelectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNoiseSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel and tag manager
        tagViewModel = ViewModelProvider(this)[TagViewModel::class.java]
        tagManager = TagSelectionManager(this)

        initSavedTags() // Load previously selected tags
        setupViewPagerAndTabs() // Setup ViewPager2 + TabLayout with tag data
        observeSelectedTags() // Observe tag selection changes
        setupListeners() // click listeners
        handleSystemInsets() // Handle status/navigation bar padding
    }

    // Loads previously saved tags from SharedPreferences into ViewModel
    private fun initSavedTags() {
        val savedTags = tagManager.loadSelectedTags()
        savedTags.forEach { tagViewModel.selectTag(it) }
    }

    // Initializes ViewPager with tab titles and contents
    private fun setupViewPagerAndTabs() {
        binding.viewPager.adapter =
            NoiseViewPagerAdapter(
                fragmentActivity = this,
                tabTitles = tagTabTitles,
                tagContentMap = tagContents,
            )
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tagTabTitles[position]
        }.attach()
    }

    // Observes ViewModel for tag selection updates and updates chip UI accordingly
    private fun observeSelectedTags() {
        tagViewModel.selectedTags.observe(this) { selectedTags ->
            updateSelectedTagsUI(selectedTags)
        }
    }

    // Updates the top chip group showing selected tags
    private fun updateSelectedTagsUI(selectedTags: Set<String>) {
        val chipGroup = binding.selectedTagChipGroup
        chipGroup.removeAllViews()

        if (selectedTags.isEmpty()) {
            // No tags selected
            binding.emptyTagText.visibility = View.VISIBLE
            chipGroup.visibility = View.GONE
            binding.btnSelect.apply {
                setBackgroundResource(R.drawable.btn_select_noise_unselect)
                isEnabled = false
            }
        } else {
            // Tags selected
            binding.emptyTagText.visibility = View.GONE
            chipGroup.visibility = View.VISIBLE
            binding.btnSelect.apply {
                setBackgroundResource(R.drawable.btn_select_noise_select)
                isEnabled = true
            }

            selectedTags.forEach { tag ->
                val chip =
                    Chip(this).apply {
                        text = tag
                        isCloseIconVisible = true
                        setOnCloseIconClickListener { tagViewModel.deselectTag(tag) }
                    }
                chipGroup.addView(chip)
            }
        }
    }

    // Handles Select and Back button clicks
    private fun setupListeners() {
        binding.btnSelect.setOnClickListener {
            val selectedTags = tagViewModel.selectedTags.value.orEmpty()
            if (selectedTags.isNotEmpty()) {
                // Save selected tags and return
                tagManager.saveSelectedTags(selectedTags.toSet())
                Toast.makeText(this, "Tags have been saved!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "No tags selected.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun handleSystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
