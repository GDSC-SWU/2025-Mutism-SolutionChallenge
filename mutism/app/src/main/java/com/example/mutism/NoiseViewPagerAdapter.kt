package com.example.mutism

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mutism.model.TagSection

class NoiseViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val tabTitles: List<String>,
    private val tagContentMap: Map<String, List<TagSection>>,
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = tabTitles.size

    override fun createFragment(position: Int): Fragment {
        val tabTitle = tabTitles[position]
        val sections = tagContentMap[tabTitle] ?: emptyList()
        return NoiseSelectTabFragment.newInstance(tabTitle, sections)
    }
}
