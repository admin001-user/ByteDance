# 技术文档

## 1. 项目概述

本项目是一个类似于 TikTok 的短视频应用。它具有用于浏览的双列视频流和用于沉浸式观看体验的单列视频播放器。用户还可以在视频上查看和添加评论。

## 2. 架构

该应用程序遵循模型-视图-视图模型 (MVVM) 架构模式。关键组件是：

*   **模型**：表示应用程序的数据和业务逻辑。这包括 `Video` 和 `Comment` 数据类。
*   **视图**：应用程序的 UI，由活动和片段组成。视图观察视图模型的数据更改并相应地更新 UI。
*   **视图模型**：充当模型和视图之间的桥梁。它将模型中的数据公开给视图并处理用户交互。

## 3. 功能实现

### 3.1. 双列视频流

*   **布局**：使用带有 `GridLayoutManager` 的 `RecyclerView` 实现，以显示两列视频缩略图。
*   **适配器**：`VideoAdapter` 负责将视频数据绑定到 `item_video.xml` 布局。
*   **导航**：单击视频缩略图会将用户导航到 `PlayerActivity`。
### 3.2. 单列视频播放器

*   **布局**：使用 `ViewPager2` 实现，以允许用户在视频之间垂直滑动。
*   **视频播放**：`ExoPlayer` 用于视频播放。`ViewPager2` 中的每个视频都有自己的 `ExoPlayer` 实例。
*   **适配器**：`VideoPlayerAdapter` 负责管理 `ExoPlayer` 实例并将视频数据绑定到 `item_video_player.xml` 布局。

### 3.3. 评论面板

*   **UI**：评论面板实现为从屏幕底部向上滑动的 `BottomSheetDialogFragment`。
*   **布局**：`fragment_comment_panel.xml` 布局包含一个用于显示评论的 `RecyclerView` 和一个用于添加新评论的 `EditText`。
*   **适配器**：`CommentAdapter` 负责将评论数据绑定到 `item_comment.xml` 布局。
*   **功能**：用户可以添加新评论，然后将其添加到 `RecyclerView` 并显示在列表底部。
### 3.4. `res` 目录

`res` 目录包含了所有的应用程序资源，这些资源被划分为不同的子目录：

*   `drawable/` 和 `drawable-v24/`：存放图像资源，例如图标和背景。不同版本的目录用于提供与特定 Android 版本兼容的资源。
*   `layout/`：存放定义用户界面的布局 XML 文件。
    *   `activity_main.xml`：主活动的布局。
    *   `activity_player.xml`：视频播放器活动的布局。
    *   `fragment_comment_panel.xml`：评论面板片段的布局。
    *   `item_comment.xml`：评论列表中单个评论项的布局。
    *   `item_video.xml`：视频列表中单个视频项的布局。
    *   `item_video_player.xml`：视频播放器中视频项的布局。
*   `menu/`：存放定义应用程序菜单的 XML 文件。
    *   `bottom_nav_menu.xml`：主活动底部导航栏的菜单项。
*   `mipmap-*/`：存放不同屏幕密度的启动器图标。
*   `values/` 和 `values-night/`：存放各种值的 XML 文件，例如字符串、颜色和主题样式。`values-night` 目录用于提供在夜间模式下使用的值。
    *   `colors.xml`：定义应用程序使用的颜色。
    *   `strings.xml`：定义应用程序使用的字符串。
    *   `themes.xml`：定义应用程序的主题和样式。
*   `xml/`：存放任意的 XML 配置文件。
    *   `backup_rules.xml`：定义自动备份的规则。
    *   `data_extraction_rules.xml`：定义数据提取的规则。

### 1. 项目结构总览与设计取舍

- 根目录
  - `.idea/`：IDE 工程配置（运行、编译、设备管理等）。保留以支持团队开发的一致性。
  - `.gradle/`：Gradle 构建缓存与元数据，提升增量构建速度。
  - `gradle/ wrapper/`、`gradlew`、`gradlew.bat`：Gradle Wrapper，保证构建环境一致与可复现。
  - `settings.gradle`：声明包含的模块（此项目仅 `app`）。
  - `build.gradle`（顶层）：项目级构建入口，通常用于版本目录与通用配置。
  - `gradle.properties`：全局属性开关（如并行构建、内存设置等）。
  - `local.properties`：本地 SDK 路径，仅本机有效，不纳入版本管理。
  - `documentation/`：项目文档，便于知识沉淀与交接。

- 模块
  - `app/`：应用主模块，包含源码、资源、构建文件与混淆规则。
  - 设计取舍：单模块结构更易教学与演示；若后续扩展（如 `feature-video`、`core-data`），可在 `settings.gradle` 中增加条目并拆分依赖。

### 2. 构建系统与配置详解

- 顶层构建
  - 使用版本目录 `gradle/libs.versions.toml` 与 `alias(libs.*)` 管理依赖与插件版本，统一升级、减少冲突。
  - 通过 `gradle.properties` 控制构建行为（如启用并行、配置 JVM 参数）。

- 模块级构建（`app/build.gradle`）
  - 插件：`com.android.application` 提供打包、签名、资源处理、AAPT2 等能力。
  - `compileSdk/targetSdk/minSdk`：分别决定编译 API、目标平台行为与最低设备支持范围。
  - `buildTypes`：`release` 目前关闭混淆以便调试；生产应开启并维护 `proguard-rules.pro`。
  - `buildFeatures.viewBinding`：生成类型安全绑定类，减少样板代码与空指针风险。
  - 依赖：`appcompat`、`material`（UI）；`recyclerview`、`viewpager2`（列表与分页）；`exoplayer`（视频）；`glide`（图片）；`navigation`（导航）；`junit/espresso`（测试）。

### 3. 模块与包结构职责划分

- `com.example.bytedance`
  - `MainActivity`：应用入口与主界面容器，负责列表/导航的展示与交互入口。
  - `PlayerActivity`：视频播放页，绑定 `PlayerView` 并管理 `ExoPlayer` 生命周期与状态（缓冲、播放、暂停）。
  - `adapter/`：RecyclerView 适配器与 ViewHolder，负责数据项绑定与点击事件分发（例如打开播放器或评论面板）。
  - `data/`：数据来源与仓库封装，屏蔽网络/本地数据差异，向 UI 提供统一接口。
  - `model/`：POJO 数据模型（如 `Video`、`Comment`），用于描述业务实体与序列化。
  - `ui/`：自定义视图与 Fragment（例如评论面板），承载具体交互与动画效果。

### 4. 资源与布局设计的考虑

- 列表项（`item_video.xml`）
  - 使用 `CardView` 提供阴影与圆角，提升视觉层次；内部 `ImageView` + `TextView` 结构简洁高效。

- 播放器项（`item_video_player.xml`）
  - `FrameLayout` 作为根容器，允许叠放控件（视频、加载进度、操作按钮）。
  - `PlayerView` 与 `ExoPlayer` 无缝协作；`ProgressBar` 反馈缓冲状态；底部 `LinearLayout` 承载点赞/评论/分享。

- 评论项（`item_comment.xml`）
  - 单 `TextView` + 合理内边距与字体大小，满足轻量级评论展示需求，列表性能最佳。

### 5. 关键实现流程（从启动到播放）

- 应用启动
  - `MainActivity` 加载主布局（`activity_main.xml`），初始化 `RecyclerView` 与 `adapter`，请求/装载视频数据列表（缩略图、标题等）。

- 列表与导航
  - 用户在视频流中滑动，点击某项触发适配器的点击回调，通过 Intent 携带视频标识/URL 跳转到 `PlayerActivity`。

- 播放器初始化
  - `PlayerActivity` 创建 `ExoPlayer` 实例，绑定到 `PlayerView`；根据视频源设置 `MediaItem` 并准备播放。
  - 监听缓冲与播放状态，显示/隐藏 `ProgressBar`；处理生命周期（`onStart/onStop` 释放与恢复）。

- 交互操作
  - 点赞/分享：点击 `ImageButton` 触发对应逻辑，可更新 UI 与数据层；评论按钮打开 `fragment_comment_panel.xml` 对应的面板（或 Fragment）。

### 6. 重要设计取舍与可扩展点

- 媒体播放选择：`ExoPlayer` 较 `MediaPlayer` 更适合流媒体与扩展场景（DRM、自适应码率、缓存策略）。
- 图片加载选择：`Glide` 在性能与生态上更成熟，默认磁盘/内存缓存策略可满足缩略图需求。
- 布局容器选择：
  - 播放器页用 `FrameLayout` 以便覆盖层控件；
  - 列表项用 `ConstraintLayout` 或 `LinearLayout` 简化层级，当前结构以性能为先。
- 构建策略：示例工程关闭混淆，便于教学与调试；生产环境需开启并设置规则，控制体积与安全性。

### 7. 总结

- 项目采用单模块、清晰包结构与成熟第三方库，快速实现“视频列表 → 视频播放 → 评论/交互”完整闭环。
- 构建配置与资源设计与功能一一对应，保证实现过程顺畅、行为可控、后续扩展成本可预期。