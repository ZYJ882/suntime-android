# 时光工具箱（Suntime）· 安卓原生 App

---

## 项目介绍

一个使用 **Kotlin + AndroidX（ViewBinding + Material Components）** 开发的原生安卓应用，
底部导航栏包含四个页面：**时钟 / 日历 / 指南针 / 设置**。

### 功能概览

| 页面 | 功能 |
|------|------|
| **时钟** | ① 本地时间实时显示；② 世界时钟——按城市/国家**搜索**各地当前时间、加★收藏（本地持久化）；③ **时间计算与跳转**——选定基准时间并加减天/时/分得到结果；④ 两个日期**相差天数**计算 |
| **日历** | ① 月历宫格（含农历小字、今日高亮、点击选中）；② 公历 + **农历**详情：干支年月日、生肖、星座、节气；③ **日期跳转**（输入 `YYYY-MM-DD`）；④ **日期相差**计算 |
| **指南针** | ① 磁传感器实时指北（带平滑滤波）；② 显示**经纬度**（GPS/网络定位，运行时申请权限）；③ **反查具体地址**（国内走高德/腾讯/百度、海外走系统 Geocoder，全球覆盖，并标明地址来源）；④ 朝向角度与方位（北/东北/…） |
| **设置** | ① **主题切换**：浅色 / 深色 / 跟随系统（启动时即应用）；② **逆地理 API Key 配置**：高德 / 腾讯 / 百度，保存在本机，提升国内地址解析成功率与精度；③ 版本信息 |

### 支持的农历范围

农历转换数据覆盖 **1900-01-31 ~ 2100-12-31**。其中的节气为
**简化算法**，仅供娱乐参考。

### 工程结构

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
        │   │   ├── CompassFragment.kt
        │   │   └── SettingsFragment.kt
        │   ├── util/
        │   │   ├── LunarCalendar.kt   # 公历转农历 + 黄历
        │   │   ├── WorldCity.kt       # 世界城市时区数据
        │   │   ├── DateCalc.kt        # 日期差值/加减
        │   │   ├── MapKeys.kt         # 高德/腾讯/百度 逆地理 Key 内置默认（可被设置页覆盖）
        │   │   ├── SettingsStore.kt   # 全局设置持久化（API Key + 主题）
        │   │   ├── CoordTransform.kt  # WGS-84↔GCJ-02↔BD-09 坐标转换
        │   │   └── AddressResolver.kt # 多服务商逆地理（全球覆盖）
        │   ├── view/
        │   │   └── CompassView.kt     # 自定义指南针 View
        │   └── adapter/
        │       ├── WorldClockAdapter.kt
        │       └── CalendarDayAdapter.kt
        └── res/
            ├── layout/ (activity_main / fragment_clock / fragment_calendar / fragment_compass / fragment_settings / item_world_clock / item_calendar_day)
            ├── menu/bottom_nav_menu.xml
            ├── drawable/ (ic_clock / ic_calendar / ic_compass / ic_settings / ic_launcher)
            └── values/ (strings / colors / themes)
```

### 设计系统（Material 3 / Material You）

统一了四个页面的视觉语言，功能、数据逻辑、交互逻辑保持不变。

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

**四页改动要点**

| 页面 | 主要变化 |
|------|----------|
| **底部导航** | 高度降至 `58dp`，选中态改用 M3 浅色胶囊容器（`primary_container`），未选中降低权重；共 4 个入口 |
| **时钟** | 本地时间作为视觉核心（`56sp` 细体）；日期/星期降为次级；世界时钟改轻量列表 + 细分割线；搜索框轻量化；工具区去卡片化 |
| **日历** | 「2026年9月」保持单行；月份切换用图标按钮 + 紧凑「今天」；网格为核心、去外围卡片；选中态改 M3 圆形；公历-农历层级清晰（大数字为视觉中心）；**已移除宜忌信息区**；工具区轻量分组 |
| **指南针** | 浅色底 + 白/浅灰表盘；北针 Primary 蓝、南针红色；表盘静止、仅指针旋转；经纬度改轻量数据区；定位按钮降高 |
| **设置** | 浅色/深色/跟随系统三态主题（启动时应用、即时切换）；高德/腾讯/百度逆地理 Key 表单（本机持久化、保存即生效）；版本信息 |

### 地址解析（全球覆盖）

指南针页会把当前经纬度反查成**具体地址**，目标是「国内 / 全球任何地方都能用」。

**策略（`util/AddressResolver.kt`）**：读取 `util/SettingsStore.kt` 中用户设置的 Key（未设置则回退 `MapKeys` 默认值）；

| 场景 | 走哪个服务商 | 说明 |
|------|------------|------|
| 中国境外 | 系统 Geocoder | 平台地理编码后端（海外全球覆盖），直接走，避免无谓超时 |
| 中国境内、已配置国内 Key | 高德 → 腾讯 → 百度（按顺序，任一成功即用） | 国内服务在国内最可靠 |
| 中国境内、未配置 Key | 系统 Geocoder 兜底 | 部分国产 ROM 仍可用 |

**坐标转换（`util/CoordTransform.kt`）**：GPS 原始是 WGS-84，高德/腾讯要 GCJ-02、百度要 BD-09，请求前自动转换，否则国内会有几十~几百米偏差。

**如何开启国内精确地址（可选）**：在 App 内「设置」页填写任一家的 Key（均免费申请），或预先在 `util/MapKeys.kt` 填入默认值：
- 高德：`AMAP_KEY`（创建「Web服务」类型 Key）
- 腾讯：`TENCENT_KEY` + `TENCENT_SK`（签名 SK 必填，否则请求被拒）
- 百度：`BAIDU_KEY`（创建「服务端」应用，勾选逆地理编码）

三处都留空也能用（靠系统 Geocoder 兜底）；填了哪家，界面「地址来源」就会显示对应服务商。
> 注意：Key 属敏感信息，正式发布前建议改为从 gradle / 本地配置读取，勿硬编码进公开仓库。

### 版本号规则（SemVer 2.0.0）

版本号采用 **[语义化版本 2.0.0](https://semver.org/lang/zh-CN/)** 的 `X.Y.Z`：

- **X.Y.Z 各段都是可持续增长的非负整数，不限于一位数**，且 **禁止前导零、禁止四段及以上**。
  合法示例：`2.6.6`、`2.6.9`、`2.6.10`、`2.6.65`、`2.10.0`、`10.6.65`。
  特别注意：`2.6.9` 的下一次 Patch 是 `2.6.10`（**不是** `2.7.0`）；连续修 Bug 可累积到 `2.6.65`；只有新增向后兼容功能才升 Minor（`2.6.65 → 2.7.0`）；只有破坏性变更才升 Major（`2.6.65 → 3.0.0`）。

| 位 | 名称 | 何时递增 | 规则 |
|----|------|----------|------|
| **X** | Major | **不向后兼容的破坏性变更** | `X+1`，**Y、Z 归零** |
| **Y** | Minor | **向后兼容地新增功能** | `Y+1`，**Z 归零** |
| **Z** | Patch | **仅向后兼容的 Bug 修复 / 内部修正 / 小幅优化** | `Z+1` |

**优先级（一次迭代含多种变更时取最高）**：`Major > Minor > Patch`。**按兼容性影响判断，而非代码行数、文件数或工作量。**

**公开 API 范围**：不仅含函数/类，还包括 HTTP/RPC/GraphQL/WebSocket 接口、SDK 导出、参数与数据结构、配置/环境变量/命令行参数、数据库/导入导出/网络协议格式、插件接口/权限模型/认证方式、稳定文档承诺的行为、用户依赖的默认行为与已有操作流程。任一变化导致已有用户/脚本/插件/配置/调用方无法正常工作 → 优先判 Major。

**`versionName`**：一律三段式（如 `1.6.0`），不用 `1.6` 这类省略补零写法，也不用四段写法。

**`versionCode`**（Android 内部升级序号，必须为单调递增整数）：由 SemVer 推导
`versionCode = X*10000000 + Y*10000 + Z`（例：`1.6.0 → 10006000`，`2.6.65 → 20606500`）。
该公式在 **X<215、Y<10000、Z<10000** 范围内严格保持 `X.Y.Z` 的字典序递增、无碰撞，可正确容纳多位数。

**初始开发与 1.0.0**：本项目已处于 `1.y.z`（≥1.0.0），默认视为 API 已稳定。**AI 严禁擅自将 `0.y.z → 1.0.0`**；若未来出现 `0.y.z` 阶段，须先询问用户“公开 API 是否已稳定并准备正式承诺兼容性？是否确认发布 1.0.0？”，待用户明确同意才升级。

**预发布 / 构建元数据**（可选）：先定 `X.Y.Z` 再追加 —— 预发布 `1.6.0-alpha` / `1.6.0-beta.1` / `1.6.0-rc.1`（低于对应正式版，不替代 X/Y/Z 判断）；构建元数据 `1.6.0+build.20261005`（记录构建号/提交哈希，不影响优先级）。

### 如何运行

1. 用 **Android Studio（Hedgehog 或更新版本，内置 JDK 17）** 打开本工程根目录。
2. 连接安卓设备（Android 7.0+，API 24+）或启动模拟器。
3. 点击 **Run**（指南针需真机传感器；模拟器可测试方向与定位模拟）。
4. 首次进入指南针页会请求定位权限，请允许。

> 说明：本工程已自带 `gradlew` 包装器与 `gradle-wrapper.jar`，无需本地预装 Gradle。
> `settings.gradle` 中已配置阿里云镜像，国内/正常网络均可正常拉取依赖。

### 构建安装包（APK）

#### 方式一：本地一键构建（推荐）

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

#### 方式二：GitHub Actions 云端构建

已附 `.github/workflows/build.yml`：将本仓库推送到 GitHub，在 **Actions → Build Debug APK → Run workflow**
即可在云端（正常网络）完成构建，并在产物（Artifacts）中下载 `app-debug.apk`。

> 前提：本机/CI 需能访问 Google Android 仓库（安装 `platforms;android-34` 与 `build-tools;34.0.0`）。
> 依赖解析走阿里云镜像，国内外均可。

### 主要依赖

- `androidx.appcompat` / `material:1.11.0`
- `androidx.recyclerview`
- 全部使用系统 API（传感器、LocationManager、Calendar、TimeZone），无需联网即可运行核心功能。

---

## 版本变更记录

本项目的**逐版本迭代历史**（每个版本新增 / 变更 / 移除 / 修复了什么）已移到独立的
[CHANGELOG.md](./CHANGELOG.md)，按 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范维护，
**不在本仓库介绍里重复罗列**。版本号规则见上方「版本号规则（SemVer 2.0.0）」。

**发版流程**（GitHub Releases）：

1. 在 `CHANGELOG.md` 顶部把 `[Unreleased]` 内容整理进新的 `## [X.Y.Z] - YYYY-MM-DD` 段落；
2. 推送 `vX.Y.Z` 标签：`git tag v1.6.0 && git push origin v1.6.0`；
3. 已附的 [`.github/workflows/release.yml`](./.github/workflows/release.yml) 会自动读取 CHANGELOG 中该版本段落，创建对应的 Release（同一版本号对应同一份说明，无需手抄）。

> 也可手动发版：仓库 **Releases → Draft a new release**，选/建 tag `vX.Y.Z`，粘贴 CHANGELOG 对应段落即可。
> 注意：只是 `git push` 代码**不会**自动生成 Releases，需打 Tag 并创建 Release。
