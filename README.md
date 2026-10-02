# 时光工具箱（Suntime）· 安卓原生 App

一个使用 **Kotlin + AndroidX（ViewBinding + Material Components）** 开发的原生安卓应用，
底部导航栏包含三个页面：**时钟 / 日历 / 指南针**。

## 功能概览

| 页面 | 功能 |
|------|------|
| **时钟** | ① 本地时间实时显示；② 世界时钟——按城市/国家**搜索**各地当前时间、加★收藏（本地持久化）；③ **时间计算与跳转**——选定基准时间并加减天/时/分得到结果；④ 两个日期**相差天数**计算 |
| **日历** | ① 月历宫格（含农历小字、今日高亮、点击选中）；② 公历 + **农历**详情：干支年月日、生肖、星座、节气；③ **日期跳转**（输入 `YYYY-MM-DD`）；④ **日期相差**计算 |
| **指南针** | ① 磁传感器实时指北（带平滑滤波）；② 显示**经纬度**（GPS/网络定位，运行时申请权限）；③ 反查地址；④ 朝向角度与方位（北/东北/…） |

## 支持的农历范围

农历转换数据覆盖 **1900-01-31 ~ 2100-12-31**。其中的节气为
**简化算法**，仅供娱乐参考。

## 工程结构

```
Suntime/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/suntime/
        │   ├── MainActivity.kt
        │   ├── fragment/
        │   │   ├── ClockFragment.kt
        │   │   ├── CalendarFragment.kt
        │   │   └── CompassFragment.kt
        │   ├── util/
        │   │   ├── LunarCalendar.kt   # 公历转农历 + 黄历
        │   │   ├── WorldCity.kt       # 世界城市时区数据
        │   │   └── DateCalc.kt        # 日期差值/加减
        │   ├── view/
        │   │   └── CompassView.kt     # 自定义指南针 View
        │   └── adapter/
        │       ├── WorldClockAdapter.kt
        │       └── CalendarDayAdapter.kt
        └── res/
            ├── layout/ (activity_main / fragment_clock / fragment_calendar / fragment_compass / item_world_clock / item_calendar_day)
            ├── menu/bottom_nav_menu.xml
            ├── drawable/ (ic_clock / ic_calendar / ic_compass / ic_launcher)
            └── values/ (strings / colors / themes)
```

## 设计系统（Material 3 / Material You）

本次 UI 重构统一了三个页面的视觉语言，功能、数据逻辑、交互逻辑保持不变。

**颜色（浅色 / 深色双套，非简单反转）**

| Token | 浅色 | 深色 |
|------|------|------|
| Primary | `#1976D2` | `#7FB2E8` |
| Background | `#F7F8FA` | `#14161A` |
| Surface | `#FFFFFF` | `#1C1F24` |
| Primary Text | `#202124` | `#E6E8EB` |
| Secondary Text | `#6F7378` | `#A4A9B0` |
| Divider | `#E5E7EB` | `#2C3037` |

**统一规范（`res/values/dimens.xml`）**

- 页面水平边距统一 `16dp`；间距梯度 `4 / 8 / 12 / 16 / 24dp`
- 圆角统一：小 `10dp`、中 `16dp`、大 `20dp`
- 阴影克制：卡片 `elevation=0dp` + `1dp` 描边，替代堆叠阴影
- 字体层级（语义 token）：主显示 `56sp` / 读数 `40sp` / 标题 `20sp` / 次级 `17sp` / 数值 `18sp` / 正文 `15–16sp` / 说明 `13–14sp` / 微 `10–12sp`
- 图标统一 `24dp` vector，tint 走主题，深色自动适配
- 状态栏 / 导航栏透明并随主题明暗切换

**三页改动要点**

| 页面 | 主要变化 |
|------|----------|
| **底部导航** | 高度降至 `58dp`，选中态改用 M3 浅色胶囊容器（`primary_container`），未选中降低权重 |
| **时钟** | 本地时间作为视觉核心（`56sp` 细体）；日期/星期降为次级；世界时钟改轻量列表 + 细分割线（不再一城一卡）；搜索框轻量化；工具区去卡片化 |
| **日历** | 「2026年9月」保持单行；月份切换用图标按钮 + 紧凑「今天」；网格为核心、去外围卡片；选中态改 M3 圆形（Primary 实心 / 今天浅色圆）；公历-农历层级清晰（大数字为视觉中心 + 公历 + 农历·星期 + 干支弱化）；**已移除宜忌信息区**，详情聚焦干支 / 生肖 / 星座 / 节气；工具区轻量分组 |
| **指南针** | 删除大面积纯蓝圆盘，改浅色底 + 白/浅灰表盘；刻度环 + 方位文字（N/E/S/W 为主、NE 等弱化）；北针 Primary 蓝、南针红色；表盘静止、仅指针旋转；顶部红色指向标记；经纬度改轻量数据区；定位按钮降高 |

> 新增文件：`values/dimens.xml`、`values-night/colors.xml`、`color/nav_item.xml`、
> `values/styles_nav.xml`、`drawable/bg_*.xml`、`view/LightDividerDecoration.kt`。

## 版本更新

- **v1.4**（本次）：移除日历详情区的「宜忌」信息——布局宜忌块、Fragment 赋值、`LunarCalendar` 中的 `yi`/`ji` 字段与建除 `yiJi` 数组一并删除；同步移除仅服务于宜忌的孤儿色 `red`/`green`，调色板收敛为「品牌蓝 + 指南针专用色」（指南针红针使用独立 `compass_needle_n`，不受影响）；顺带修复一处旧引用 `@dimen/text_page_title`（已并入 `text_title`）；版本号 `versionCode 5 / versionName 1.4`。功能与交互逻辑不变。
- **v1.3**（本次）：① 重构字阶体系——将三页散落的 `10–18sp` 字号全部收敛为语义化 token（`text_display 56` / `text_reading 40` / `text_title 20` / `text_subtitle 17` / `text_value 18` / `text_body_lg 16` / `text_body 15` / `text_body_sm 14` / `text_caption 13` / `text_micro 12` / `text_tiny 11` / `text_week 10`），消除相邻字号随意感；② 组件尺寸魔数 token 化（`icon_button_size` / `input_height` / `button_height` / `chip_height` / `star_size` / `icon_search` / `row_min_height` 等）；③ 统一眉标→内容间距为 `8dp`，并清除时钟页分组标题上重复的 `textStyle="bold"`；④ 版本号 `versionCode 4 / versionName 1.3`。功能与交互逻辑不变。
- **v1.2**：① 启动图标主色统一为品牌 Primary `#1976D2`，消除唯一游离硬编码色；② 日历选中/今天圆点由 36dp 收敛至 40dp（`@dimen/calendar_dot`），更精致；③ 新增统一分组标题样式 `Text.Suntime.Header`（主色加粗），三页卡片/分组标题形成一致层级；④ 版本号 `versionCode 3 / versionName 1.2`。功能与交互逻辑不变。
- **v1.1**：延续 UI 重构后的细节打磨（构建脚本与若干类型修正）。
- **v1.0**：三页统一 Material 3 视觉重构（浅/深/跟随系统）。

## 如何运行

1. 用 **Android Studio（Hedgehog 或更新版本，内置 JDK 17）** 打开本工程根目录。
2. 连接安卓设备（Android 7.0+，API 24+）或启动模拟器。
3. 点击 **Run**（指南针需真机传感器；模拟器可测试方向与定位模拟）。
4. 首次进入指南针页会请求定位权限，请允许。

> 说明：本工程已自带 `gradlew` 包装器与 `gradle-wrapper.jar`，无需本地预装 Gradle。
> `settings.gradle` 中已配置阿里云镜像，国内/正常网络均可正常拉取依赖。

## 构建安装包（APK）

### 方式一：本地一键构建（推荐）

```bash
# 1) 安装并配置 Android SDK，设置环境变量
export ANDROID_SDK_ROOT=/path/to/android-sdk      # 或 ANDROID_HOME

# 2) 执行构建脚本（会自动安装 platform-34 / build-tools 34.0.0 并打包）
bash build_apk.sh
```

生成的安装包：

```
app/build/outputs/apk/debug/app-debug.apk
```

手机开启「未知来源」后可直接安装；如需发布版（release）请在 `app/build.gradle` 配置签名后运行 `./gradlew assembleRelease`。

### 方式二：GitHub Actions 云端构建

已附 `.github/workflows/build.yml`：将本仓库推送到 GitHub，在 **Actions → Build Debug APK → Run workflow**
即可在云端（正常网络）完成构建，并在产物（Artifacts）中下载 `app-debug.apk`。

> 前提：本机/CI 需能访问 Google Android 仓库（安装 `platforms;android-34` 与 `build-tools;34.0.0`）。
> 依赖解析走阿里云镜像，国内外均可。

## 主要依赖

- `androidx.appcompat` / `material:1.11.0`
- `androidx.recyclerview`
- 全部使用系统 API（传感器、LocationManager、Calendar、TimeZone），无需联网即可运行核心功能。


## 仓库安装包

仓库的 `releases/suntime-debug.apk` 是可直接安装的 Debug APK，适用于 Android 7.0（API 24）及以上设备。安装时可能需要允许安装未知来源应用。

如需重新构建，请先配置 `ANDROID_SDK_ROOT` 或 `ANDROID_HOME`，然后运行：

```bash
bash build_apk.sh
```
