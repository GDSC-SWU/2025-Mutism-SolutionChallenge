package com.example.mutism

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.mutism.databinding.ActivityNoiseSelectBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class NoiseSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoiseSelectBinding
    private lateinit var tagViewModel: TagViewModel

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var selectedTagChipGroup: ChipGroup
    private lateinit var emptyTagText: TextView

    private val tabTitles =
        listOf(
            "Human",
            "Music/\nArtistic",
            "Electronic\nDevice",
            "Machine/\nTool",
            "Transportation",
            "Sounds of\nNature",
            "Impact/\nHazard",
            "Everyday\nObject",
            "Background/\nCrowd",
            "Other/\nUnclassified",
        )

    private val tagContents =
        listOf(
            listOf("Speech", "Whispering", "Shout", "Narration", "Conversation"),
            listOf("Laughter", "Crying", "Screaming", "Sigh", "Groan", "Moan"),
            listOf("Baby cry", "Babbling", "Child speech"),
            listOf("Whispering", "Whispering", "Shout", "Narration", "Conversation"),
            listOf("Shout", "Crying", "Screaming", "Sigh", "Groan", "Moan"),
            listOf("Groan cry", "Babbling", "Child speech"),
            listOf("Sigh", "Whispering", "Shout", "Narration", "Conversation"),
            listOf("Narration", "Crying", "Screaming", "Sigh", "Groan", "Moan"),
            listOf("Moan cry", "Babbling", "Child speech"),
            listOf("Conversation", "Whispering", "Shout", "Narration", "Conversation"),
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNoiseSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tagViewModel = ViewModelProvider(this)[TagViewModel::class.java]

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager
        selectedTagChipGroup = binding.selectedTagChipGroup
        emptyTagText = binding.emptyTagText

        viewPager.adapter = NoiseViewPagerAdapter(this, tabTitles, tagContents)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // ✅ ViewModel 상태 observe하여 ChipGroup 자동 반영
        tagViewModel.selectedTags.observe(this) { selectedTags ->
            selectedTagChipGroup.removeAllViews()

            if (selectedTags.isEmpty()) {
                emptyTagText.visibility = View.VISIBLE
                selectedTagChipGroup.visibility = View.GONE
            } else {
                emptyTagText.visibility = View.GONE
                selectedTagChipGroup.visibility = View.VISIBLE

                selectedTags.forEach { tag ->
                    val chip =
                        Chip(this).apply {
                            text = tag
                            isCloseIconVisible = true
                            setOnCloseIconClickListener {
                                tagViewModel.deselectTag(tag)
                            }
                        }
                    selectedTagChipGroup.addView(chip)
                }
            }
        }

        // 버튼 클릭 시 태그 출력
        binding.btnSearch.setOnClickListener {
            Toast.makeText(this, "선택된 태그: ${tagViewModel.selectedTags.value}", Toast.LENGTH_SHORT).show()
        }

        // WindowInsets 설정
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
