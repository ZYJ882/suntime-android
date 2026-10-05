package com.example.suntime.util

/**
 * 国内逆地理服务 Key 占位。
 *
 * 三家服务（高德 / 腾讯 / 百度）**任选其一**填入即可在国内（系统 Geocoder 不可用时）
 * 解析出具体地址（省 / 市 / 区 / 街道 / 门牌号，部分还带 POI 与语义描述）。
 * 三处都留空时，App 会直接走系统 Geocoder（海外可用，国内多数 ROM 不可用）。
 *
 * 申请地址（均免费，个人工具额度足够）：
 *  - 高德：https://console.amap.com/dev/key/app      创建「Web服务」类型 Key
 *  - 腾讯：https://lbs.qq.com/dev/console/key/manage  创建「WebServiceAPI」Key，并配置签名 SK
 *  - 百度：https://lbsyun.baidu.com/apiconsole/key     创建「服务端」应用，勾选逆地理编码
 *
 * 注意：Key 属于敏感信息，发布前请勿硬编码到公开仓库；建议改为从 gradle / 本地配置读取。
 */
object MapKeys {
    const val AMAP_KEY: String = ""      // 高德 Web 服务 Key
    const val TENCENT_KEY: String = ""   // 腾讯 WebService Key
    const val TENCENT_SK: String = ""    // 腾讯签名 SK（必填，否则请求会被拒）
    const val BAIDU_KEY: String = ""     // 百度服务端 Key
}
