# 技术文档

## 1. 项目概述

本项目是一个类似于 TikTok 的短视频应用。它具有用于浏览的双列视频流和用于沉浸式观看体验的单列视频播放器。用户还可以在视频上查看和添加评论。

## 2. 架构

该应用程序遵循 **模型-视图-视图模型 (MVVM)** 架构模式，利用 Android Jetpack 组件实现生命周期感知的数据管理。关键组件是：

*   **模型 (Model)**：表示应用程序的数据和业务逻辑。
    *   `VideoItem`：视频数据实体，包含视频 URL、封面、作者信息、点赞数及新增的 **评论数**。
    *   `Comment`：评论数据实体，包含作者信息、头像 URL、内容及时间戳。
*   **视图 (View)**：应用程序的 UI，由 Activity (`MainActivity`) 和 Fragment (`CommentPanelFragment`) 组成。视图观察 ViewModel 的数据变化并相应地更新 UI。
*   **视图模型 (ViewModel)**：充当模型和视图之间的桥梁，管理 UI 相关的数据并在配置更改（如旋转）时存活。
    *   `VideoViewModel`：管理视频列表数据，处理刷新 (`refreshVideos`) 和加载更多 (`loadMoreVideos`) 逻辑。
    *   `CommentViewModel`：管理评论数据，处理评论列表的加载和新评论的发布。

## 3. 功能实现

### 3.1. 双列视频流
*   **布局**：使用带有 `StaggeredGridLayoutManager` 的 `RecyclerView` 实现，以显示瀑布流式的两列视频缩略图。
*   **适配器**：`VideoAdapter` 负责将视频数据绑定到 `item_video.xml` 布局。
*   **交互**：点击视频缩略图会平滑过渡到单列播放模式 (`ViewPager2`)。

### 3.2. 单列视频播放器
*   **布局**：使用 `ViewPager2` 实现，以允许用户在视频之间垂直滑动，提供沉浸式体验。
*   **视频播放**：`ExoPlayer` 用于视频播放。`ViewPager2` 中的每个页面（`item_video_player.xml`）包含一个 `PlayerView`。
*   **优化**：
    *   **生命周期管理**：`MainActivity` 统一管理播放器的生命周期。
    *   **预加载与缓存**：利用 LRU 缓存预加载视频封面，提升滑动流畅度。
    *   **无缝切换**：通过 `attachProgressForPosition` 方法动态绑定/解绑播放器与视图，确保滑动时的播放体验。
    *   **解决黑屏问题**：实现了强制重置播放器的逻辑，解决了滑动返回时有声音无画面的问题。
*   **数据绑定**：`VideoPlayerAdapter` 负责绑定数据，包括新增的 **评论数显示**。

### 3.3. 评论面板
*   **UI**：评论面板实现为从屏幕底部向上滑动的 `BottomSheetDialogFragment` (`CommentPanelFragment`)。
*   **数据驱动**：完全由 `CommentViewModel` 驱动。
    *   **加载评论**：观察 `LiveData<List<Comment>>` 自动更新列表。
    *   **发布评论**：通过 ViewModel 添加评论，UI 自动响应数据变化。
*   **头像优化**：使用 `Glide` 加载网络真实头像资源，替代本地资源 ID。
*   **交互优化**：评论输入框采用了高对比度的文字颜色，提升可用性。

### 3.4. 目录结构说明

- `com.example.bytedance`
  - `MainActivity`：应用主入口，承载双列列表与单列播放器，集成 `VideoViewModel`。
  - `adapter/`：
    - `VideoAdapter`：双列列表适配器。
    - `VideoPlayerAdapter`：单列播放器适配器。
    - `CommentAdapter`：评论列表适配器。
  - `data/`：`MockData` 提供模拟数据（包含真实的视频/图片 URL）。
  - `model/`：`VideoItem`, `Comment` 实体类。
  - `ui/`：`CommentPanelFragment` 等 UI 组件。
  - `viewmodel/`：`VideoViewModel`, `CommentViewModel`。

## 4. 关键技术点与修改记录

### 4.1. MVVM 迁移
- 引入 `ViewModelProvider` 获取 ViewModel 实例。
- 使用 `MutableLiveData` 和 `Observer` 替代回调接口，解耦 UI 与数据逻辑。

### 4.2. 刷新与加载逻辑优化
- **单列刷新**：在 `VideoViewModel` 中实现数据随机化模拟刷新。
- **去重逻辑**：在 `MainActivity` 中添加逻辑，确保刷新后第一个视频与当前视频不同，提升用户体验。
- **加载更多**：实现了分页加载模拟，滑动到底部自动追加数据。

### 4.3. 播放器稳定性修复
- **Surface 绑定**：在 `ViewPager2` 滑动回调中，增加了对 `PlayerView` 的强制重置逻辑，确保 `TextureView` 的 Surface 正确绑定到 `ExoPlayer`，修复了“回滑黑屏”的 Bug。
- **ViewHolder 检查**：增加了对 ViewHolder 状态的空值检查与重试机制，防止空指针异常。

### 4.4. UI/UX 细节完善
- **评论数**：从静态 0 修改为真实数据绑定。
- **输入框**：修复了文字颜色不可见的问题。
- **头像**：接入网络图片资源，使界面更加真实。

## 5. 总结

项目已成功迁移至 MVVM 架构，增强了代码的可维护性和扩展性。针对用户反馈的刷新失败、播放异常、UI 显示错误等问题进行了全面的修复和优化，确保了应用的流畅运行和良好的用户体验。
