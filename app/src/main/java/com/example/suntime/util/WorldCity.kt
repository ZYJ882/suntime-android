package com.example.suntime.util

/**
 * 世界城市（含时区 ID），用于世界时钟搜索与显示。
 */
data class WorldCity(
    val name: String,        // 中文名
    val nameEn: String,      // 英文名
    val country: String,     // 国家/地区
    val zoneId: String       // java.util.TimeZone ID
)

object CityData {
    val cities: List<WorldCity> = listOf(
        WorldCity("北京", "Beijing", "中国", "Asia/Shanghai"),
        WorldCity("上海", "Shanghai", "中国", "Asia/Shanghai"),
        WorldCity("香港", "Hong Kong", "中国", "Asia/Hong_Kong"),
        WorldCity("台北", "Taipei", "中国", "Asia/Taipei"),
        WorldCity("东京", "Tokyo", "日本", "Asia/Tokyo"),
        WorldCity("首尔", "Seoul", "韩国", "Asia/Seoul"),
        WorldCity("新加坡", "Singapore", "新加坡", "Asia/Singapore"),
        WorldCity("曼谷", "Bangkok", "泰国", "Asia/Bangkok"),
        WorldCity("吉隆坡", "Kuala Lumpur", "马来西亚", "Asia/Kuala_Lumpur"),
        WorldCity("雅加达", "Jakarta", "印尼", "Asia/Jakarta"),
        WorldCity("马尼拉", "Manila", "菲律宾", "Asia/Manila"),
        WorldCity("河内", "Hanoi", "越南", "Asia/Ho_Chi_Minh"),
        WorldCity("新德里", "New Delhi", "印度", "Asia/Kolkata"),
        WorldCity("迪拜", "Dubai", "阿联酋", "Asia/Dubai"),
        WorldCity("莫斯科", "Moscow", "俄罗斯", "Europe/Moscow"),
        WorldCity("伊斯坦布尔", "Istanbul", "土耳其", "Europe/Istanbul"),
        WorldCity("伦敦", "London", "英国", "Europe/London"),
        WorldCity("巴黎", "Paris", "法国", "Europe/Paris"),
        WorldCity("柏林", "Berlin", "德国", "Europe/Berlin"),
        WorldCity("罗马", "Rome", "意大利", "Europe/Rome"),
        WorldCity("马德里", "Madrid", "西班牙", "Europe/Madrid"),
        WorldCity("苏黎世", "Zurich", "瑞士", "Europe/Zurich"),
        WorldCity("纽约", "New York", "美国", "America/New_York"),
        WorldCity("华盛顿", "Washington", "美国", "America/New_York"),
        WorldCity("芝加哥", "Chicago", "美国", "America/Chicago"),
        WorldCity("丹佛", "Denver", "美国", "America/Denver"),
        WorldCity("洛杉矶", "Los Angeles", "美国", "America/Los_Angeles"),
        WorldCity("旧金山", "San Francisco", "美国", "America/Los_Angeles"),
        WorldCity("多伦多", "Toronto", "加拿大", "America/Toronto"),
        WorldCity("温哥华", "Vancouver", "加拿大", "America/Vancouver"),
        WorldCity("墨西哥城", "Mexico City", "墨西哥", "America/Mexico_City"),
        WorldCity("圣保罗", "Sao Paulo", "巴西", "America/Sao_Paulo"),
        WorldCity("里约热内卢", "Rio de Janeiro", "巴西", "America/Sao_Paulo"),
        WorldCity("布宜诺斯艾利斯", "Buenos Aires", "阿根廷", "America/Argentina/Buenos_Aires"),
        WorldCity("悉尼", "Sydney", "澳大利亚", "Australia/Sydney"),
        WorldCity("墨尔本", "Melbourne", "澳大利亚", "Australia/Melbourne"),
        WorldCity("珀斯", "Perth", "澳大利亚", "Australia/Perth"),
        WorldCity("奥克兰", "Auckland", "新西兰", "Pacific/Auckland"),
        WorldCity("开罗", "Cairo", "埃及", "Africa/Cairo"),
        WorldCity("约翰内斯堡", "Johannesburg", "南非", "Africa/Johannesburg"),
        WorldCity("内罗毕", "Nairobi", "肯尼亚", "Africa/Nairobi"),
        WorldCity("檀香山", "Honolulu", "美国", "Pacific/Honolulu")
    )
}
