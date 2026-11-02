package com.example.suntime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.suntime.adapter.MainPagerAdapter
import com.example.suntime.databinding.ActivityMainBinding
import com.example.suntime.util.Motion
import kotlin.math.abs

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
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 冷启动关键优化：只保留当前页 + 相邻 1 页。
        // 原实现 offscreenPageLimit = 3 会在首帧同步创建全部 4 个 Fragment
        //（时钟 43 行世界时钟 + 日历 42 格 + 指南针 + 设置整页表单），
        // 显著拖慢首屏。改为 1 后首帧只需准备时钟页与日历页。
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = MainPagerAdapter(this)

        // 点击底部导航 → 切到对应页
        binding.bottomNav.setOnItemSelectedListener { item ->
            val index = navIds.indexOf(item.itemId)
            if (index >= 0) {
                if (!syncingNav) switchPage(index)
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

        // 让底部导航与当前页一致（含 Activity 重建后的恢复）
        syncingNav = true
        binding.bottomNav.selectedItemId = navIds[binding.viewPager.currentItem]
        syncingNav = false
    }

    /**
     * 导航切页：
     * - 相邻页保留滑动动画，方向感自然；
     * - 跨页跳转（如时钟 → 设置）不依次滚过中间页：那会触发中间页创建并产生很长的位移动画。
     *   改为「快速淡出 → 瞬切 → 淡入」，既快又避免无谓的 Fragment 生命周期开销。
     */
    private fun switchPage(index: Int) {
        val current = binding.viewPager.currentItem
        if (index == current) return

        // 终止上一次未完成的淡入淡出，避免 alpha 停在中间值
        binding.viewPager.animate().cancel()
        binding.viewPager.alpha = 1f

        if (abs(index - current) <= 1) {
            binding.viewPager.setCurrentItem(index, true)
            return
        }

        if (!Motion.animationsEnabled(this)) {
            binding.viewPager.setCurrentItem(index, false)
            return
        }

        binding.viewPager.animate()
            .alpha(0f)
            .setDuration(90L)
            .withEndAction {
                binding.viewPager.setCurrentItem(index, false)
                binding.viewPager.animate()
                    .alpha(1f)
                    .setDuration(160L)
                    .start()
            }
            .start()
    }
}
