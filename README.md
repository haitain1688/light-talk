# 灯语助手

一个运行在安卓手机上的闪光灯控制 App，目前维护两条版本线：

- `classic-v0.1.0`：稳定版，包含延时关灯、闪灯和灯语发送
- `feature/read-light`：读灯版，在稳定版基础上新增摄像头读取灯语

当前工作分支说明：

- 默认稳定能力：延时关灯、闪灯、灯语发送
- 读灯版新增：摄像头取景框、实时光强分析、摩斯码尝试识别、自动校准阈值

## 功能特性

- 手动常亮开关
- 闪灯模式，支持调节闪灯间隔
- 延时关灯，支持小时/分钟设置与倒计时显示
- 灯语模式：
  - 国际摩斯码输入，自动转换闪灯顺序
  - 自定义 `.` `-` 灯语节奏
  - 短闪、长闪、符号间隔、词间隔调节
  - 循环播放次数设置
  - 手动停止发送
- 读灯模式（`feature/read-light`）：
  - 用摄像头对准光源中心区域
  - 实时显示中心亮度、周围亮度、信号强度、噪声水平
  - 根据亮灭时长解码为 `.` `-`
  - 尝试翻译为摩斯码文本
  - 支持建议阈值与自动校准

## 应用截图

当前仓库还未加入最终截图文件；建议把手机实拍或截图文件放到 `docs/images/` 后，再更新这里的展示图。

预留展示位：

- 首页总览：`docs/images/home.png`
- 延时关灯：`docs/images/timer.png`
- 灯语模式：`docs/images/message.png`

## 技术栈

- Android 原生应用
- Kotlin
- Jetpack Compose
- Material 3
- `CameraManager.setTorchMode()` 控制闪光灯
- 前台服务维持闪灯与灯语任务
- `AlarmManager` 处理自动关灯

## 页面结构

- `延时关灯`
  - 设置小时和分钟
  - 点击确认开灯
  - 显示自动关灯倒计时
- `闪灯与手动控制`
  - 常亮开关
  - 闪灯间隔滑块
  - 开始闪灯 / 停止任务
- `灯语模式`
  - 摩斯码输入模式
  - 自定义节奏模式
  - 循环次数
  - 发送灯语 / 停止发送
- `读灯功能`（`feature/read-light`）
  - 摄像头预览与中心取景框
  - 建议阈值、自动校准
  - 已读取灯语与摩斯码翻译

## 运行方式

1. 用 Android Studio 打开项目目录 `light-tool`
2. 确保安装 `JDK 17`、Android SDK 和 Build Tools
3. 等待 Gradle 同步完成
4. 连接安卓真机
5. 运行 `app` 模块
6. 首次启动时允许相机权限；Android 13 及以上还需要通知权限

## 构建产物

调试构建：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`

发布构建：

- Release AAB：`app/build/outputs/bundle/release/app-release.aab`
- Release APK：`app/build/outputs/apk/release/app-release-unsigned.apk`

说明：

- 当前生成的 Release APK 是未签名包，不能直接作为正式发布安装包
- 如果要对外分发或上架商店，需要先配置签名

## 分支与版本

- 稳定版分支：`classic-v0.1.0`
- 读灯版分支：`feature/read-light`
- 稳定版版本：`v0.1.0`
- 读灯版版本：`v0.2.0-reader`
- 稳定版发布说明：`docs/release-notes/v0.1.0.md`

## 注意事项

- 需要手机本身带闪光灯
- 高频或长时间运行会带来明显发热
- 某些安卓系统会限制后台服务与精确定时
- 不同品牌手机对闪光灯后台行为可能存在差异

## 后续计划

- 增加应用图标和启动页
- 增加常用摩斯码预设
- 优化读灯版在强环境光下的识别稳定性
- 增加截图与演示说明
- 增加正式签名与发布流程文档
