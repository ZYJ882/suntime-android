# Changelog

本项目所有值得注意的版本变更记录在此文件。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本 2.0.0](https://semver.org/lang/zh-CN/)（X.Y.Z，多位数，如 `2.6.9 → 2.6.10`）。

> 日期说明：本项目以「源码交付」方式迭代，`v1.0.0`–`v1.5.0` 未做正式 Git Tag 发布，
> 下列日期为开发序列的近似时间；`v1.6.0` 为首次按规范整理 CHANGELOG 的版本（实际交付日 2026-10-05）。
> 正式发版后请以 Git Tag 日期为准。

## [1.10.0] - 2026-11-02

### Added

- 时钟页右上角新增**秒表入口图标**，点击弹出「计时器 / 秒表」小菜单
  - 新增 `TimerActivity`（倒计时：时/分/秒输入，开始 / 暂停 / 继续 / 重置，结束提示）
  - 新增 `StopwatchActivity`（秒表：开始 / 暂停 / 继续 / 重置，精确到 0.01 秒，基于 `SystemClock.elapsedRealtime()`）
  - 新增 `drawable/ic_stopwatch.xml`、`menu/clock_tools_menu.xml`、`layout/activity_timer.xml`、`layout/activity_stopwatch.xml`
  - `ClockFragment` 接入 `PopupMenu` 并跳转；`AndroidManifest.xml` 注册两个 Activity

## [1.9.0] - 2026-11-02

### Added

- 四个页面支持**左右滑动切换**（`ViewPager2` + `FragmentStateAdapter`）
  - 新增 `adapter/MainPagerAdapter.kt`（时钟 / 日历 / 指南针 / 设置 四页）
  - `MainActivity` 改为 ViewPager2 托管，并与底部导航**双向同步**：滑动更新选中态、点击导航切页，带 `syncingNav` 防循环守卫
  - `activity_main.xml` 的 `FrameLayout` 容器替换为 `ViewPager2`（id `viewPager`）
  - 新增依赖 `androidx.viewpager2:viewpager2:1.0.0`

## [1.8.0] - 2026-11-02

### Added

- 安卓多端（手机 / 平板 / 小屏圆形设备）响应式适配
  - 新增 `util/WindowSize.kt`：按窗口宽度(dp) 分类 Compact(<600) / Medium(600–839) / Expanded(≥840)
  - 新增 `util/ScreenInfo.kt`：读取屏幕分辨率(px)、DPI、density、scaledDensity 并归类尺寸等级（设置页「设备信息」卡只读展示）
  - 资源限定符：`values-w600dp` / `values-w840dp` / `values-sw200dp`（平板限宽居中、大屏放大核心尺寸、小屏尺寸降级）；`layout-w600dp/fragment_settings`（双栏卡片）、`layout-w840dp/fragment_calendar`、`layout-w840dp/fragment_compass`（大屏双栏）
  - `ClockFragment` 世界时钟按尺寸等级用 `GridLayoutManager` 切换列数（紧凑 1 / 中等 2 / 展开 3 列）
  - 各页内容限最大宽度并居中，避免平板拉伸
- 新增 `app/lint.xml`：将 `DpInsteadOfPx`、`PxUsage` 设为 error，构建期拦截 px 硬编码

## [1.7.0] - 2026-10-18

### Added

- 日历新增「年 / 月 / 日」三视图切换：顶部 年/月/日 分段栏，一键在三种视图间切换
  - **年视图**：12 宫格小月历总览全年（每月含农历/节气小字、今日高亮），点击某月直达该月月视图
  - **月视图**：保留原有 7 列月历网格与选中日详情
  - **日视图**：以选中日为核心展示公历/农历/星期/干支/生肖/星座/节气，支持上一天/下一天连续切换

## [1.6.0] - 2026-10-05

### Added

- 新增「设置」页（底栏第 4 个 Tab）
  - 主题切换：浅色 / 深色 / 跟随系统（启动时即应用、保存即时生效）
  - 逆地理 API Key 配置：高德 / 腾讯 / 百度，保存在本机 `SharedPreferences`，可由设置页覆盖 `MapKeys` 默认值，保存即生效
  - 版本信息展示
- 新增 `util/SettingsStore.kt`（全局设置持久化：API Key + 主题）
- 新增 `fragment/SettingsFragment.kt`、`layout/fragment_settings.xml`、`drawable/ic_settings.xml`
- 底栏新增 `nav_settings` 入口

### Changed

- `MainActivity` 接入第 4 个 Fragment，并在启动处应用已存主题
- `AddressResolver` 改为从 `SettingsStore` 读取用户设置的 Key（未设置则回退 `MapKeys` 默认值）
- 版本号按 SemVer 升 Minor：`versionCode 10006000 / versionName 1.6.0`

## [1.5.0] - 2026-10-02

### Added

- 重做指南针「具体地址」反查，目标国内/全球都能用
  - 新增 `util/MapKeys.kt`（高德/腾讯/百度 Key 占位）
  - 新增 `util/CoordTransform.kt`（WGS-84 ↔ GCJ-02 ↔ BD-09 坐标转换）
  - 新增 `util/AddressResolver.kt`：境外走系统 Geocoder（全球覆盖），境内走高德 → 腾讯 → 百度（任一成功即用），后台线程请求、主线程回调；界面显示「地址来源」与「定位精度」

### Fixed

- 修复旧版在主线程同步调用 Geocoder 的 ANR 隐患
- 按位移/时间节流反查请求，避免无谓请求

### Changed

- `AndroidManifest.xml` 补 `INTERNET` 权限
- 版本号 `versionCode 10005000 / versionName 1.5.0`

## [1.4.0] - 2026-09-30

### Removed

- 移除日历详情区的「宜忌」信息：布局宜忌块、`CalendarFragment` 赋值、`LunarCalendar` 中的 `yi`/`ji` 字段与建除 `yiJi` 数组一并删除
- 移除仅服务于宜忌的孤儿色 `red` / `green`，调色板收敛为「品牌蓝 + 指南针专用色」

### Fixed

- 修复一处旧引用 `@dimen/text_page_title`（已并入 `text_title`）

## [1.3.0] - 2026-09-28

### Changed

- 重构字阶体系：三页散落的 10–18sp 字号全部收敛为语义化 token
- 组件尺寸「魔数」token 化
- 统一眉标 → 内容间距为 8dp，清除时钟页分组标题上重复的 `textStyle="bold"`

## [1.2.0] - 2026-09-25

### Changed

- 启动图标主色统一为品牌 Primary `#1976D2`
- 日历选中/今天圆点收敛至 40dp
- 新增统一分组标题样式 `Text.Suntime.Header`

## [1.1.0] - 2026-09-22

### Changed

- 延续 UI 重构后的细节打磨（构建脚本与若干类型修正）

## [1.0.0] - 2026-09-20

### Added

- 三页（时钟/日历/指南针）统一 Material 3 视觉重构（浅色 / 深色 / 跟随系统）
