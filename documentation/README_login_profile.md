# 登录与个人中心说明

本文档介绍本项目中登录模块与个人中心（头像、退出登录）的实现、关键代码位置、交互流程以及验证方法，并附带三张截图：登录界面、替换头像前、替换头像后。

## 功能概览

- 登录界面使用 `TextInputLayout` 与 `TextInputEditText` 提供更好的输入体验与错误提示。
- 输入校验：
  - 邮箱格式校验（必须符合 `name@domain` 格式）。
  - 密码长度校验（默认不少于 6 位）。
- 账号校验通过后，使用 `SharedPreferences` 持久化登录状态 `logged_in=true`，并保存基础用户信息（如 `username`）。
- 底部导航 “我的/Mine” 行为：
  - 未登录时，点击进入 `LoginActivity`。
  - 已登录时，点击进入 `ProfileActivity`。
  - 为避免退出后 “我的” 残留放大，`MainActivity#onResume()` 在未登录时会默认选中 `Home` 并触发统一缩放动画。
- 个人中心头像：支持从相册选择图片并自动居中裁剪为圆形，保存到内部存储，同时记录到 `SharedPreferences` 以便下次启动加载显示。
- 退出登录：在个人中心点击“退出登录”，清除 `SharedPreferences` 的 `logged_in` 标记与相关用户信息，并返回登录页。

## 关键代码位置

- 登录逻辑与校验
  - `app/src/main/java/com/example/bytedance/ui/LoginActivity.java`
  - `app/src/main/res/layout/activity_login.xml`
- 个人中心与头像
  - `app/src/main/java/com/example/bytedance/ui/ProfileActivity.java`
  - `app/src/main/res/layout/activity_profile.xml`
- 底部导航与页面跳转
  - `app/src/main/java/com/example/bytedance/MainActivity.java`
- 用户数据与校验（SQLite 示例）
  - `app/src/main/java/com/example/bytedance/db/UserDatabaseHelper.java`

## 交互流程

1. 打开应用，底部导航默认选中 `Home`，点击 “我的/Mine”。
2. 若未登录，进入登录页：
   - 输入邮箱与密码，若格式或长度不符合，错误文案会在 `TextInputLayout` 内联显示。
   - 校验通过后，写入 `SharedPreferences` 并跳转到个人中心页。
3. 在个人中心：
   - 点击头像选择图片，系统相册返回后自动居中裁剪、圆形展示并持久化保存。
   - 点击“退出登录”，清除登录标记并返回登录页；此时返回主界面时底部导航会自动重置选中到 `Home`，缩放效果统一。

## 截图

> 若图片在你本地仓库尚未显示，请参考文末“截图存放说明”将你提供的三张截图保存到指定路径与文件名。

![登录界面](./images/login_screen.png)

![替换头像前](./images/avatar_before.png)

![替换头像后](./images/avatar_after.png)

## 验证步骤

- 登录校验
  - 输入非法邮箱或过短密码，确认错误提示在组件内显示；修正后错误自动消失。
  - 登录成功后，重启应用仍保持登录状态（`SharedPreferences` 生效）。
- 底部导航缩放一致性
  - 切换各 Tab，均触发统一的放大动画。
  - 退出登录返回主界面时，`Home` 被选中且放大，`Mine` 不残留放大。
- 头像裁剪与持久化
  - 从相册选择不同比例图片（横图/竖图/方图），头像均被圆形边界裁剪并居中显示。
  - 重启应用，头像保持显示（内部存储与 `SharedPreferences` 记录生效）。

## 技术要点

- 使用 `TextInputLayout` 的 `setError()` 与 `setErrorEnabled()` 显示/隐藏错误状态。
- 通过 `SharedPreferences` 保存 `logged_in` 与基础用户信息，并在 `MainActivity` 阶段性读取决定导航行为。
- 头像 `ImageView` 设置 `android:scaleType="centerCrop"` 与 `android:clipToOutline="true"`，配合圆形 `drawable` 背景实现圆形裁剪。
- 为避免依赖受限 API，底部导航的缩放动画通过 `Menu` 索引与通用 `ViewGroup` 遍历实现。

## 截图存放说明

- 将你提供的三张截图保存到以下路径与文件名：
  - `documentation/images/login_screen.png`（登录界面截图）
  - `documentation/images/avatar_before.png`（替换头像前）
  - `documentation/images/avatar_after.png`（替换头像后）
- 保存后，重新打开此 `README` 或在 Git 工具中查看即可显示图片。