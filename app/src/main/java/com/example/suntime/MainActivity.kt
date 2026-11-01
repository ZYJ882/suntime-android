package com.example.suntime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.example.suntime.adapter.MainPagerAdapter
import com.example.suntime.databinding.ActivityMainBinding
import com.example.suntime.util.SettingsStore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** 防止「滑动同步导航」与「点击导航切页」互相触发形成循环。 */
    private var syncingNav = false

    /** 分页顺序，必须与 MainPagerAdapter 一致。 */
    private val navIds = intArrayOf(
        R.id.nav_clock,
        R.id.nav_calendar,
        R.id.nav_compass,
        R.id.nav_settings
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 setContentView 之前应用已保存的主题，避免闪烁
        AppCompatDelegate.setDefaultNightMode(
            SettingsStore.toNightMode(SettingsStore.getThemeMode(this))
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainPagerAdapter(this)
        // 预加载全部页面，左右滑动时页面状态（滚动位置 / 选中日等）不丢失
        binding.viewPager.offscreenPageLimit = 3

        // 点击底部导航 → 切到对应页
        binding.bottomNav.setOnItemSelectedListener { item ->
            val index = navIds.indexOf(item.itemId)
            if (index >= 0) {
                if (!syncingNav) binding.viewPager.setCurrentItem(index, true)
                true
            } else {
                false
            }
        }

        // 左右滑动 → 同步底部导航选中态
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                syncingNav = true
                binding.bottomNav.selectedItemId = navIds[position]
                syncingNav = false
            }
        })

        if (savedInstanceState == null) {
            binding.viewPager.setCurrentItem(0, false)
        }
        // 恢复 / 首次进入时，让底部导航与当前页一致
        syncingNav = true
        binding.bottomNav.selectedItemId = navIds[binding.viewPager.currentItem]
        syncingNav = false
    }
}
