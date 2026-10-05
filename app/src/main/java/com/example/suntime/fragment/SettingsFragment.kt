package com.example.suntime.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.suntime.R
import com.example.suntime.databinding.FragmentSettingsBinding
import com.example.suntime.util.SettingsStore

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()

        // 主题：根据已保存模式勾选对应单选
        when (SettingsStore.getThemeMode(ctx)) {
            SettingsStore.THEME_LIGHT -> binding.rbThemeLight.isChecked = true
            SettingsStore.THEME_DARK -> binding.rbThemeDark.isChecked = true
            else -> binding.rbThemeSystem.isChecked = true
        }

        // 回填已保存的 Key（原始用户输入，便于查看/修改）
        binding.etAmap.setText(SettingsStore.getRawAmapKey(ctx))
        binding.etTencent.setText(SettingsStore.getRawTencentKey(ctx))
        binding.etTencentSk.setText(SettingsStore.getRawTencentSk(ctx))
        binding.etBaidu.setText(SettingsStore.getRawBaiduKey(ctx))

        binding.btnSave.setOnClickListener {
            val amap = binding.etAmap.text.toString().trim()
            val tencent = binding.etTencent.text.toString().trim()
            val tencentSk = binding.etTencentSk.text.toString().trim()
            val baidu = binding.etBaidu.text.toString().trim()

            SettingsStore.setAmapKey(ctx, amap)
            SettingsStore.setTencentKey(ctx, tencent)
            SettingsStore.setTencentSk(ctx, tencentSk)
            SettingsStore.setBaiduKey(ctx, baidu)

            val mode = when (binding.rgTheme.checkedRadioButtonId) {
                R.id.rbThemeLight -> SettingsStore.THEME_LIGHT
                R.id.rbThemeDark -> SettingsStore.THEME_DARK
                else -> SettingsStore.THEME_SYSTEM
            }
            SettingsStore.setThemeMode(ctx, mode)

            // 应用主题：若与当前不同会重建 Activity 使新主题立即生效
            AppCompatDelegate.setDefaultNightMode(SettingsStore.toNightMode(mode))

            Toast.makeText(ctx, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
