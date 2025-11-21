### 7.2. 模块级 `build.gradle`

模块级的 `build.gradle` 文件用于配置 `app` 模块的构建设置，包括应用程序 ID、SDK 版本、依赖项等。

```groovy
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace 'com.example.bytedance'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.bytedance"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding true
    }
}

dependencies {

    implementation libs.appcompat
    implementation libs.material
    implementation libs.activity
    implementation libs.constraintlayout
    implementation libs.recyclerview
    implementation libs.viewpager2
    implementation libs.exoplayer
    implementation libs.glide
    implementation libs.navigation.fragment
    implementation libs.navigation.ui
    testImplementation libs.junit
    androidTestImplementation libs.ext.junit
    androidTestImplementation libs.espresso.core
}
```

**逐行讲解:**

*   `plugins { ... }`: 应用了 Android 应用程序插件。
*   `android { ... }`: 这个块包含了所有的 Android 特定配置。
*   `namespace 'com.example.bytedance'`: 定义了应用程序的命名空间。
*   `compileSdk 34`: 指定了用于编译应用程序的 Android SDK 版本。
*   `defaultConfig { ... }`: 定义了应用程序的核心配置。
*   `applicationId "com.example.bytedance"`: 应用程序的唯一标识符。
*   `minSdk 24`: 应用程序可以运行的最低 Android API 级别。
*   `targetSdk 34`: 应用程序的目标 Android API 级别。
*   `buildTypes { ... }`: 定义了如何构建和打包不同类型的应用程序（例如，`release` 或 `debug`）。
*   `compileOptions { ... }`: 配置了 Java 编译器的选项。
*   `buildFeatures { ... }`: 启用了视图绑定功能，这可以简化与视图的交互。
*   `dependencies { ... }`: 声明了项目所需的所有库和依赖项，例如 `appcompat`、`material`、`recyclerview`、`exoplayer` 和 `glide`。

#### 设计理由与取舍

- 构建插件与命名空间
  - `plugins { alias(libs.plugins.android.application) }`：使用版本目录（Version Catalog）管理插件与库版本，可集中升级并避免各处硬编码版本号。
  - `namespace 'com.example.bytedance'`：规范资源与类生成的命名空间，避免与其他模块/库产生冲突。

- SDK 与兼容性
  - `compileSdk 34`：以最新 API 进行编译，获取新特性与更佳的工具支持，同时不影响在低版本设备上的运行能力。
  - `minSdk 24`：覆盖主流设备，兼容现代库（如 Material、ExoPlayer）最低要求的同时，避免过低 API 带来的适配成本。
  - `targetSdk 34`：符合最新平台行为变更与 Google Play 政策，确保运行时行为与权限机制与当前系统一致。

- 默认配置与测试
  - `applicationId` 保证包名唯一，用于发布与设备安装识别；若区分 `debug`/`release` 可使用 `applicationIdSuffix`。
  - `testInstrumentationRunner` 指定 Android 仪器测试入口，方便在设备/模拟器上跑 UI 测试。

- 构建类型与混淆
  - `release.minifyEnabled false`：示例工程以可读性与调试优先，关闭 R8 混淆与压缩，便于问题定位。生产环境建议开启，并完善 `proguard-rules.pro` 以获得更小体积与更强保护。
  - `proguardFiles` 使用系统默认优化规则 + 项目自定义规则，兼顾安全性与兼容性。

- 编译选项与特性
  - `compileOptions JavaVersion.VERSION_1_8`：启用 Java 8 语法（如 Lambda），兼容 Android toolchain 的默认去糖设置；若项目需要现代 API（如 Records），可升级到 Java 17 并确认依赖兼容。
  - `buildFeatures.viewBinding true`：生成类型安全的视图绑定类，替代 `findViewById`，减少空指针与样板代码，提升开发效率。

- 依赖选择与用途
  - `appcompat` + `material`：提供现代 UI 组件与主题，支持 Material Design 规范与旧版兼容。
  - `activity`、`constraintlayout`：更佳的 Activity 生命周期集成与灵活布局能力，复杂界面时用约束替代多层嵌套。
  - `recyclerview`、`viewpager2`：高性能列表与滑动分页容器，实现视频流与评论列表等场景。
  - `exoplayer`：专业视频播放框架，支持自适应流媒体、缓存与 `PlayerView` 无缝集成，较原生 `MediaPlayer` 更易扩展与维护。
  - `glide`：高性能图片加载与缓存，支持缩略图、占位图与生命周期自动管理，用于视频封面与头像等资源。
  - `navigation.fragment`、`navigation.ui`：声明式导航与安全参数传递，减少 Fragment 事务样板代码（如未使用可后续移除）。
  - `junit`、`androidTest`、`espresso`：单元测试与 UI 测试基础设施，确保核心逻辑正确性与界面交互稳定。

#### 该文件在实现过程中的作用

- 构建阶段
  - 解析插件与依赖，生成 `R` 类、`BuildConfig`、`ViewBinding` 等构建产物。
  - 参与 AAPT2 资源打包、Java 编译与 dex 转换，决定最终 APK/AAB 的内容与行为。

- 运行阶段（间接影响）
  - 决定可用 API 范围（由 `compileSdk`/`targetSdk`）、主题资源与视图绑定类的可用性。
  - 依赖引入的能力（如 `ExoPlayer` 播放、`Glide` 图片加载）直接支撑功能实现。

#### 为什么这样写（设计取舍）

- 工程定位为视频播放与评论的教学/示例项目，优先可读性与开发效率：关闭混淆、开启 `viewBinding`、选用成熟稳定的 UI 与媒体库。
- 依赖与配置与资源、代码一一对应：如 `PlayerView` 需要 `exoplayer`，视频项封面使用 `glide`，列表用 `recyclerview` 与 `CardView`。
- 通过版本目录统一依赖版本，降低升级成本；通过较高 `compileSdk` 与 `targetSdk` 及时适配平台行为变更。

#### 与目录结构的关系

- 此文件仅影响 `app` 模块（在 `settings.gradle` 被包含）。顶层 `build.gradle` 与 `gradle/ libs.versions.toml` 管理版本与插件，`gradle.properties` 管理全局属性。
- 该文件与 `src/main`（代码与资源）、`proguard-rules.pro`（混淆）、`build/`（构建产物）形成构建闭环，决定开发到发布的完整流程。