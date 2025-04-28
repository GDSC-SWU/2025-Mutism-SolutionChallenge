package com.example.mutism.controller.noiseSelectPage

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.mutism.R
import com.example.mutism.databinding.ActivityNoiseSelectBinding
import com.example.mutism.model.tagContents
import com.example.mutism.model.tagTabTitles
import com.example.mutism.viewmodel.TagViewModel
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
    private lateinit var selectButton: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNoiseSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel 초기화
        tagViewModel = ViewModelProvider(this)[TagViewModel::class.java]

        val sharedPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
        val savedTags = sharedPrefs.getStringSet(KEY_SELECTED_NOISE_TAGS, emptySet())

        savedTags?.forEach { tag ->
            tagViewModel.selectTag(tag)
        }

        // 뷰 초기화
        tabLayout = binding.tabLayout
        viewPager = binding.viewPager
        selectedTagChipGroup = binding.selectedTagChipGroup
        emptyTagText = binding.emptyTagText
        selectButton = binding.btnSelect

        // 어댑터 연결
        viewPager.adapter =
            NoiseViewPagerAdapter(
                fragmentActivity = this,
                tabTitles = tagTabTitles,
                tagContentMap = tagContents,
            )

        // 탭 타이틀 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tagTabTitles[position]
        }.attach()

        // ViewModel에 따라 선택된 칩 그룹 UI 업데이트
        tagViewModel.selectedTags.observe(this) { selectedTags ->
            selectedTagChipGroup.removeAllViews()

            if (selectedTags.isEmpty()) {
                emptyTagText.visibility = View.VISIBLE
                selectedTagChipGroup.visibility = View.GONE

                // 버튼 비활성화
                selectButton.setBackgroundResource(R.drawable.btn_unselect_noise)
                selectButton.isEnabled = false
                selectButton.isClickable = false
            } else {
                emptyTagText.visibility = View.GONE
                selectedTagChipGroup.visibility = View.VISIBLE

                // 버튼 활성화
                selectButton.setBackgroundResource(R.drawable.btn_selected_noise)
                selectButton.isEnabled = true
                selectButton.isClickable = true

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

        // 검색 버튼 클릭 이벤트
        binding.btnSelect.setOnClickListener {
            val selectedTags = tagViewModel.selectedTags.value ?: emptyList()

            if (selectedTags.isNotEmpty()) {
                val sharedPrefs = getSharedPreferences("NoiseSelectPrefs", MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putStringSet(KEY_SELECTED_NOISE_TAGS, selectedTags.toSet())
                    apply()
                }
                Toast.makeText(this, "태그가 저장되었습니다!", Toast.LENGTH_SHORT).show()

                setResult(RESULT_OK) // 여기 추가!
                finish()
            } else {
                Toast.makeText(this, "선택된 태그가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        // 시스템 UI 여백 처리
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        private const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
    }
}
