package com.example.suntime.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.suntime.fragment.CalendarFragment
import com.example.suntime.fragment.ClockFragment
import com.example.suntime.fragment.CompassFragment
import com.example.suntime.fragment.SettingsFragment

/**
 * 主页面分页适配器：顺序必须与底部导航菜单（bottom_nav_menu.xml）保持一致。
 * 1 时钟 / 2 日历 / 3 指南针 / 4 设置
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> ClockFragment()
        1 -> CalendarFragment()
        2 -> CompassFragment()
        else -> SettingsFragment()
    }

    companion object {
        const val PAGE_COUNT = 4
    }
}
