package com.example.mutism

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class NoiseViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val tabTitles: List<String>,
    private val tagContents: List<List<String>>,
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = tabTitles.size

    override fun createFragment(position: Int): Fragment = NoiseSelectTabFragment.newInstance(tabTitles[position], tagContents[position])
}
