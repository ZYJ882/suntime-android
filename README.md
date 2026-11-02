# 时光工具箱（Suntime）· 安卓原生 App

---

## 项目介绍

一个使用 **Kotlin + AndroidX（ViewBinding + Material Components）** 开发的原生安卓应用，
底部导航栏包含四个页面：**时钟 / 日历 / 指南针 / 设置**。
页面由 **ViewPager2** 托管，支持**左右滑动切换**，并与底部导航双向同步（滑动更新选中态、点击导航切页）。

### 功能概览

| 页面 | 功能 |
|------|------|
| **时钟** | ① 本地时间实时显示；② 世界时钟——按城市/国家**搜索**各地当前时间、加★收藏（本地持久化）；③ **时间计算与跳转**——选定基准时间并加减天/时/分得到结果；④ 两个日期**相差天数**计算；⑤ **计时器 / 秒表**——右上角秒表图标点击弹出小菜单进入 |
| **日历** | ① **年 / 月 / 日 三视图切换**（顶部分段栏，一键切换）；② **年视图**：多列宫格小月历总览全年（手机 2 列 / 平板 3 列，每格公历数字、今日高亮，点击某月直达月视图）；③ 月历宫格（含农历小字、今日高亮、点击选中）；④ 公历 + **农历**详情：干支年月日、生肖、星座、节气；⑤ **日视图**：选中日详情 + 上一天/下一天切换；⑥ **日期跳转**（输入 `YYYY-MM-DD`）；⑦ **日期相差**计算 |
| **指南针** | ① 实时指北：优先用融合传感器 `TYPE_ROTATION_VECTOR`，叠加**磁偏角修正**（磁北→真北），读数与系统指南针对齐；② 旋转式表盘（N/E/S/W 始终指向真实北方，顶部固定指针为手机朝向），刻度含 2°/10°/30° 与度数标注；③ 显示**经纬度**（GPS/网络定位，运行时申请权限）；④ **反查具体地址**（国内走高德/腾讯/百度、海外走系统 Geocoder，全球覆盖，并标明地址来源）；⑤ 朝向角度与方位（北 N / 东北 NE / …），磁场精度低时提示校准 |
| **设置** | ① **主题切换**：浅色 / 深色 / 跟随系统（启动时即应用）；② **逆地理 API Key 配置**：高德 / 腾讯 / 百度，保存在本机，提升国内地址解析成功率与精度；③ **设备信息**（只读）：分辨率 / DPI / 密度 / 窗口尺寸等级；④ 版本信息 |

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
        │   ├── TimerActivity.kt        # 计时器（倒计时）页面
        │   ├── StopwatchActivity.kt    # 秒表页面
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
        │   │   ├── AddressResolver.kt # 多服务商逆地理（全球覆盖）
        │   │   ├── WindowSize.kt      # 窗口尺寸等级分类（Compact/Medium/Expanded）
        │   │   └── ScreenInfo.kt      # 读取屏幕分辨率/DPI/密度，归类尺寸等级
        │   ├── view/
        │   │   ├── CompassView.kt     # 自定义指南针 View
        │   │   └── MiniMonthView.kt   # 年视图迷你月历（单 View Canvas 绘制）
        │   └── adapter/
        │       ├── MainPagerAdapter.kt   # 四页 ViewPager2 分页（滑动切页）
        │       ├── WorldClockAdapter.kt
        │       └── CalendarDayAdapter.kt
        └── res/
            ├── layout/ (activity_main / activity_timer / activity_stopwatch / fragment_clock / fragment_calendar / fragment_compass / fragment_settings / item_world_clock / item_calendar_day)
            ├── menu/ (bottom_nav_menu / clock_tools_menu)
            ├── drawable/ (ic_clock / ic_calendar / ic_compass / ic_settings / ic_stopwatch / ic_launcher)
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
| **指南针** | 浅色底 + 白/浅灰表盘；**表盘随方位旋转、顶部固定指针**（与系统指南针一致）；N 用暖红强调，其余方位中性灰；经纬度改轻量数据区；定位按钮降高 |
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

**优先级（一次迭代含多种变更时取最高）**：`Major > Minor > Patch`。**按兼容性影响判断，而非代码行数、文件数或工作量。** 涉及破坏性变更时，**AI 须先向用户说明影响并确认，不得擅自升 Major。**

**变更分类示例**（判定级别时对照，按兼容性影响而非工作量）：

| 级别 | 典型变更（包括但不限于） |
|------|------------------------|
| **Major** | 删除/重命名/修改已有公开 API；修改参数、返回值或接口结构导致原有调用失效；可选参数改为必填；删除配置项、环境变量、命令行参数或已有功能；修改数据库、文件、网络协议或数据格式导致旧版本不兼容；修改默认行为、权限、认证、核心业务规则或已有用户流程，导致现有集成失效 |
| **Minor** | 新增页面、模块、功能、API 或业务能力；新增可选参数/可选配置（旧用法仍有效）；新增命令、导出格式、通知方式或第三方集成；新增数据字段（旧数据仍可读）；新增权限或角色但不改变已有行为；将公开 API 标记为 deprecated 但暂仍可用 |
| **Patch** | 修复崩溃、异常、错误提示、逻辑、计算、校验、排序、过滤、格式化或显示问题；修复兼容性或安全问题且不破坏公开 API；优化性能、内存或内部实现但不新增功能；修正文档、测试或构建配置问题 |

**公开 API 范围**：不仅含函数/类，还包括 HTTP/RPC/GraphQL/WebSocket 接口、SDK 导出、参数与数据结构、配置/环境变量/命令行参数、数据库/导入导出/网络协议格式、插件接口/权限模型/认证方式、稳定文档承诺的行为、用户依赖的默认行为与已有操作流程。任一变化导致已有用户/脚本/插件/配置/调用方无法正常工作 → 优先判 Major。

**`versionName`**：一律三段式（如 `1.6.0`），不用 `1.6` 这类省略补零写法，也不用四段写法。

**`versionCode`**（Android 内部升级序号，必须为单调递增整数）：由 SemVer 推导
`versionCode = X*10000000 + Y*10000 + Z`（例：`1.6.0 → 10006000`，`2.6.65 → 20606500`）。
该公式在 **X<215、Y<10000、Z<10000** 范围内严格保持 `X.Y.Z` 的字典序递增、无碰撞，可正确容纳多位数。

**`0.y.z` 初始开发阶段与 1.0.0**（本项目已处于 `1.y.z`，即 ≥1.0.0，默认视为 API 已稳定）：

- 该阶段公开 API 通常不稳定：Bug 修复 `0.4.2 → 0.4.3`；向后兼容新功能 `0.4.2 → 0.5.0`；不兼容变更按项目约定升 Minor。
- 进入 `1.0.0` 无固定的版本号/时间/功能数量条件；当**公开 API 已确定、核心功能可用、准备向用户/生产环境承诺稳定兼容性、已有用户依赖当前 API、团队愿意对后续破坏性变更使用 Major** 时，可**建议**发布 `1.0.0`（如 `0.9.5 → 1.0.0`）。
- **AI 严禁擅自将 `0.y.z → 1.0.0`**：必须先询问用户"公开 API 是否已稳定，并准备正式承诺兼容性？是否确认发布 1.0.0？"，待用户明确同意后才升级。

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

### 多端适配（响应式）

一套代码同时适配 **手机 / 平板 / 小屏圆形设备（手表同 APK 侧载）**，遵循 Android 官方响应式规范，核心原则：

- **尺寸用 `dp`、文字用 `sp`**：所有尺寸/字号统一走 `res/values/dimens.xml` 的语义 token，**禁止**在资源里直接写 `px`/`dip` 硬编码；`sp` 随系统字号偏好缩放，`dp` 随屏幕 DPI 缩放。
- **禁止全局缩放界面**：**绝不**修改 `DisplayMetrics.density`、调用 `updateConfiguration` 缩放，或对 `setContentView` 做整体缩放——所有适配都基于 dp/sp + 资源限定符 + 代码内尺寸等级切换，保证不同设备一致、不模糊、不破坏无障碍。
- **依据窗口尺寸分类切换布局**：采用 Android **Window Size Class** 规范，`util/WindowSize.kt` 按「窗口宽度(dp)」分类：
  | 等级 | 宽度(dp) | 典型设备 |
  |------|----------|----------|
  | `COMPACT` 紧凑 | < 600 | 手机竖屏 |
  | `MEDIUM` 中等 | 600–839 | 手机横屏 / 小平板 / 折叠屏展开 |
  | `EXPANDED` 展开 | ≥ 840 | 平板 / 桌面 / ChromeOS |

**资源限定符目录（自动选布局/尺寸，无需写分支）**

| 目录 | 作用 |
|------|------|
| `res/values-w600dp/dimens.xml` | Medium+：内容限宽 `600dp`、加大边距 |
| `res/values-w840dp/dimens.xml` | Expanded：限宽 `1080dp`、放大指南针/主数字 |
| `res/values-sw200dp/dimens.xml` | 小屏/圆形设备：整体尺寸降级，防溢出 |
| `res/layout-w600dp/fragment_settings.xml` | 设置页主题卡 + API 卡**并排双栏** |
| `res/layout-w840dp/fragment_calendar.xml` | 日历**双栏**（左月历 / 右详情+工具） |
| `res/layout-w840dp/fragment_compass.xml` | 指南针**双栏**（左表盘 / 右读数） |

> 尺寸限定符（`w*`/`sw*`）与夜间主题（`night`）是正交的，二者自动叠加，无需额外 `values-night-w*` 目录。

**代码内自适应**：`ClockFragment` 的世界时钟列表按尺寸等级用 `GridLayoutManager` 切换列数（紧凑 1 列 / 中等 2 列 / 展开 3 列）；`SettingsFragment` 通过 `ScreenInfoReader` 读取分辨率/DPI/密度并在「设备信息」卡展示。

**静态守卫**：`app/lint.xml` 已将 `DpInsteadOfPx`、`PxUsage` 设为 `error`，构建期即拦截 px 硬编码；交付前静态校验脚本同样会扫描 px 与全局缩放反模式。

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
