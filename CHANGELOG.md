# Changelog

本项目所有值得注意的版本变更记录在此文件。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本 2.0.0](https://semver.org/lang/zh-CN/)（X.Y.Z，多位数，如 `2.6.9 → 2.6.10`）。

> 日期说明：本项目以「源码交付」方式迭代，`v1.0.0`–`v1.5.0` 未做正式 Git Tag 发布，
> 下列日期为开发序列的近似时间；`v1.6.0` 为首次按规范整理 CHANGELOG 的版本（实际交付日 2026-10-05）。
> 正式发版后请以 Git Tag 日期为准。

## [Unreleased]

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
