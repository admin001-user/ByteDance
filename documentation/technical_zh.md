**项目概览**
- 这是一个短视频体验应用，包含首页双列瀑布流卡片、单列沉浸播放页、点赞/评论/分享、时间轴拖动缩略图预览、计数显示与抖音风格格式化、主题色适配等功能。

**技术栈**
- `RecyclerView`：首页列表承载，支持单列与双列瀑布流。
- `StaggeredGridLayoutManager`：双列瀑布流布局，卡片高度自适应；禁用自动间隙调整避免跳位。
- `ExoPlayer` 与 `PlayerView`：视频播放组件（`com.google.android.exoplayer2:*`），外部持有播放器，避免离屏项影响当前播放。
- `ViewBinding`：布局安全绑定，如 `ItemVideoBinding`、`ItemVideoPlayerBinding`。
- `Glide`：图片加载，用于封面与拖动预览缩略图。
- `FileProvider`：分享缩略图文件，安全暴露缓存目录。
- `ConstraintLayout`：卡片自适应布局（宽高比约束）。
- `VectorDrawable` + 统一颜色资源：抖音风格图标与暗/亮主题适配。

**核心目录与文件**
- 代码入口：`app/src/main/java/com/example/bytedance/MainActivity.java`
- 首页适配器：`app/src/main/java/com/example/bytedance/adapter/VideoAdapter.java`
- 播放页适配器：`app/src/main/java/com/example/bytedance/adapter/VideoPlayerAdapter.java`
- 数据模型：`app/src/main/java/com/example/bytedance/model/VideoItem.java`
- 首页卡片布局：`app/src/main/res/layout/item_video.xml`
- 播放页布局：`app/src/main/res/layout/item_video_player.xml`
- 图标与颜色：`app/src/main/res/drawable/`、`app/src/main/res/values/colors.xml`、`app/src/main/res/values-night/colors.xml`
- 文件共享声明：`app/src/main/AndroidManifest.xml`、`app/src/main/res/xml/file_paths.xml`

**首页双列瀑布流**
- 布局管理：在 `MainActivity` 中用 `StaggeredGridLayoutManager(isTwoColumn ? 2 : 1, RecyclerView.VERTICAL)` 切换双列；`setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE)` 禁止自动间隙处理；`recyclerView.setItemAnimator(null)` 避免高度变化导致跳位。
- 卡片布局：`item_video.xml` 使用 `ConstraintLayout`；封面 `ImageView` 以 `app:layout_constraintDimensionRatio="1:1.5"` 控制高度随宽度变化；文案 `TextView` 两行显示、超出省略；作者 `TextView` 一行显示、超出省略；整体 `CardView` 高度 `wrap_content` 自适应。
- 加载更多：通过 `findLastVisibleItemPositions(null)` 获取各列最后可见位置的最大值，接近末尾时触发 `loadMoreVideos()`。

**播放页与交互**
- `VideoPlayerAdapter` 仅绑定数据，播放器对象在外层控制，确保只有可见项持有播放器，避免资源争用。
- 文案显示：`descriptionText` 两行省略；右侧操作区包含点赞/评论/分享图标与计数文本。
- 计数格式化：`formatCount(int)`，将数字格式化为抖音风格（如 `1.2W`）。
- 评论面板：在播放页通过 `CommentPanelFragment` 弹出，点击评论按钮触发。

**分享缩略图**
- 缩略图生成与缓存：在拖动进度条时生成缩略图并缓存；也支持预生成若干帧提升体验。
- 通过 `FileProvider` 共享：在 `AndroidManifest.xml` 中声明 `<provider>`，`res/xml/file_paths.xml` 指定缓存共享路径；调用系统分享意图时附加 `content://` URI。

**右侧图标与样式**
- 图标尺寸：播放页右侧 `ImageButton`（点赞/评论/分享）统一为 `36dp × 36dp`。
- 颜色：统一使用 `@color/douyinIcon`、`@color/douyinIconLiked`、`@color/douyinText`，在夜间模式下对应切换。
- 文本：计数文本与描述文本使用与主题一致的颜色与字号（描述支持省略）。

**数据模型**
- `VideoItem`：`videoUrl`、`thumbnailUrl`、`description`、`author`。
- 首页 `VideoAdapter` 绑定：`binding.description.setText(video.description)`、`binding.author.setText(video.author)`，封面用 `Glide.load(video.thumbnailUrl)`。

**构建与运行**
- 构建：在项目根目录执行 `./gradlew assembleDebug -x test`。
- 安装运行：将生成的 APK 安装到设备或通过 Android Studio 运行。
- 注意：Gradle 8.x 提示部分弃用 API，不影响当前构建；如需查看详细警告可加 `--warning-mode all`。

**可配置项与常用约定**
- 列数切换：`isTwoColumn` 控制是否双列；也可根据屏幕宽度/方向动态设置。
- 瀑布流稳定性：保持 `GAP_HANDLING_NONE` 与关闭 `ItemAnimator`，并避免在绑定时频繁请求布局。
- 封面比例：默认 `1:1.5`，如需更紧凑可设为 `1:1.2` 或 `4:3`（需同时评估页面可视密度）。

**验证建议**
- 首页：检查一屏展示的卡片数量、不同内容高度的稳定性、滚动加载的平滑度。
- 播放页：检查描述两行省略、作者一行省略、右侧操作按钮尺寸与计数显示、评论面板弹出与分享缩略图可用。