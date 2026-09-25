package com.example.suntime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.suntime.R
import com.example.suntime.util.WorldCity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WorldClockAdapter(
    private val fullList: List<WorldCity>,
    private var favorites: MutableSet<String>,
    private val onToggleFavorite: (WorldCity) -> Unit
) : RecyclerView.Adapter<WorldClockAdapter.VH>() {

    private var keyword = ""
    // 世界时钟不显示秒，避免视觉跳动
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

    fun setKeyword(k: String) {
        keyword = k.trim()
        notifyDataSetChanged()
    }

    fun setFavorites(f: MutableSet<String>) {
        favorites = f
        notifyDataSetChanged()
    }

    private fun currentList(): List<WorldCity> {
        val fav = fullList.filter { favorites.contains(it.zoneId) }
        val rest = fullList.filter { !favorites.contains(it.zoneId) }
            .filter {
                if (keyword.isEmpty()) true
                else (it.name.contains(keyword) || it.nameEn.contains(keyword, true)
                        || it.country.contains(keyword, true) || it.zoneId.contains(keyword, true))
            }
        return fav + rest
    }

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

    override fun getItemCount(): Int = currentList().size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val city = currentList()[position]
        val zone = TimeZone.getTimeZone(city.zoneId)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
        timeFmt.timeZone = zone
        val cityOffset = zone.getOffset(now)

        holder.tvCity.text = city.name
        // 二级信息精简：只保留 国家 · GMT+8，不再堆 zoneId 全串
        holder.tvZone.text = "${city.country} · ${gmtLabel(cityOffset)}"
        holder.tvTime.text = timeFmt.format(cal.time)

        val localOffset = TimeZone.getDefault().getOffset(now)
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
