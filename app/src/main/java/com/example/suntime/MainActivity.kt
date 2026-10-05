package com.example.suntime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.suntime.databinding.ActivityMainBinding
import com.example.suntime.fragment.CalendarFragment
import com.example.suntime.fragment.ClockFragment
import com.example.suntime.fragment.CompassFragment
import com.example.suntime.fragment.SettingsFragment
import com.example.suntime.util.SettingsStore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 setContentView 之前应用已保存的主题，避免闪烁
        AppCompatDelegate.setDefaultNightMode(
            SettingsStore.toNightMode(SettingsStore.getThemeMode(this))
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            switchFragment(ClockFragment())
            binding.bottomNav.selectedItemId = R.id.nav_clock
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_clock -> switchFragment(ClockFragment())
                R.id.nav_calendar -> switchFragment(CalendarFragment())
                R.id.nav_compass -> switchFragment(CompassFragment())
                R.id.nav_settings -> switchFragment(SettingsFragment())
                else -> false
            }
            true
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
