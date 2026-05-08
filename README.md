# 灯语助手

一个可在安卓手机上运行的闪光灯控制 App，支持：

- 手动开启/关闭闪光灯常亮
- 设置闪灯间隔，进入循环闪灯模式
- 设置几分钟后开灯、几分钟后关灯
- 自定义短闪 `.`、长闪 `-` 和间隔，发送灯语

## 当前实现

- 原生 Android
- Kotlin
- Jetpack Compose
- `CameraManager.setTorchMode()` 控制闪光灯
- 前台服务维持闪灯/灯语任务
- `AlarmManager` 处理定时开关灯

## 页面说明

- `实时灯光控制`
  - 常亮开关
  - 闪灯间隔滑块
  - 开始闪灯 / 停止任务
- `定时开关灯`
  - 几分钟后开灯
  - 几分钟后关灯
- `自定义灯语`
  - 输入 `.` `-` 和空格
  - 自定义短亮、长亮、灯间隔、词间隔

## 使用方式

1. 用 Android Studio 打开项目根目录 `light-tool`
2. 安装 Android SDK 与 JDK 17
3. 等待 Gradle 同步完成
4. 连接安卓真机运行
5. 首次启动时允许相机权限和通知权限

## 打包 APK

1. 在 Android Studio 中点击 `Build` -> `Build APK(s)`
2. 或配置签名后点击 `Build` -> `Generate Signed Bundle / APK`

## 注意事项

- 需要手机本身支持闪光灯
- 某些系统在后台会限制定时精度
- Android 12 及以上系统可能限制精确定时，当前已做降级兼容
- 长时间开启闪光灯会明显发热，请避免连续高频运行太久

## 本地环境缺失

当前这台机器的命令行环境里没有检测到：

- `java`
- `gradle`
- `ANDROID_*` / `JAVA_*` 环境变量

因此我已经把工程代码搭好，但还不能在这里直接完成本地编译出 APK。只要你在 Android Studio 安装好 JDK 和 Android SDK，就可以继续同步并打包。
