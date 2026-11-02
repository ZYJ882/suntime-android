package com.example.suntime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.suntime.R
import com.example.suntime.util.WorldCity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WorldClockAdapter(
    private val fullList: List<WorldCity>,
    private var favorites: MutableSet<String>,
    private val onToggleFavorite: (WorldCity) -> Unit
) : RecyclerView.Adapter<WorldClockAdapter.VH>() {

    private var keyword = ""

    /**
     * 过滤结果缓存。
     *
     * 原实现每次 `getItemCount()` / `onBindViewHolder()` 都重新 filter 整个列表，
     * 43 行列表在一次测量中会被调用上百次，产生大量临时 List 与字符串比较。
     * 现在只在「关键词 / 收藏」变化时重建一次。
     */
    private val visible = ArrayList<WorldCity>(fullList.size)
    private val zoneCache = HashMap<String, TimeZone>()

    // 世界时钟不显示秒，避免视觉跳动；仅在主线程使用
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val localTz = TimeZone.getDefault()

    init {
        rebuild()
    }

    fun setKeyword(k: String) {
        val next = k.trim()
        if (next == keyword) return
        keyword = next
        rebuild()
        notifyDataSetChanged()
    }

    fun setFavorites(f: MutableSet<String>) {
        favorites = f
        rebuild()
        notifyDataSetChanged()
    }

    private fun rebuild() {
        visible.clear()
        // 常用城市置顶
        for (city in fullList) {
            if (favorites.contains(city.zoneId)) visible.add(city)
        }
        val kw = keyword
        for (city in fullList) {
            if (favorites.contains(city.zoneId)) continue
            if (kw.isEmpty() || matches(city, kw)) visible.add(city)
        }
    }

    private fun matches(city: WorldCity, kw: String): Boolean =
        city.name.contains(kw) ||
            city.nameEn.contains(kw, true) ||
            city.country.contains(kw, true) ||
            city.zoneId.contains(kw, true)

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStar: TextView = itemView.findViewById(R.id.tvStar)
        val tvCity: TextView = itemView.findViewById(R.id.tvCity)
        val tvZone: TextView = itemView.findViewById(R.id.tvZone)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvOffset: TextView = itemView.findViewById(R.id.tvOffset)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_world_clock, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = visible.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val city = visible[position]
        val zone = zoneCache.getOrPut(city.zoneId) { TimeZone.getTimeZone(city.zoneId) }
        val now = System.currentTimeMillis()

        // 不再为每行创建 Calendar：设置时区后直接 format 时间戳即可
        timeFmt.timeZone = zone
        val cityOffset = zone.getOffset(now)

        holder.tvCity.text = city.name
        // 二级信息精简：只保留 国家 · GMT+8，不再堆 zoneId 全串
        holder.tvZone.text = "${city.country} · ${gmtLabel(cityOffset)}"
        holder.tvTime.text = timeFmt.format(Date(now))

        val localOffset = localTz.getOffset(now)
        val diffH = (cityOffset - localOffset) / 3600000f
        holder.tvOffset.text = if (diffH == 0f) "与本地相同" else "本地${if (diffH > 0) "+" else ""}$diffH 小时"

        holder.tvStar.text = if (favorites.contains(city.zoneId)) "★" else "☆"
        holder.tvStar.setOnClickListener { onToggleFavorite(city) }
    }

    /** 将毫秒偏移格式化为 GMT+8 / GMT+5:30 / GMT-4 形式 */
    private fun gmtLabel(offsetMs: Int): String {
        val totalMin = offsetMs / 60000
        val sign = if (totalMin >= 0) "+" else "-"
        val absMin = kotlin.math.abs(totalMin)
        val h = absMin / 60
        val m = absMin % 60
        return if (m == 0) "GMT$sign$h" else "GMT$sign$h:${"%02d".format(m)}"
    }
}
