# BiliTVNative

BiliTVNative 是一个面向 Android TV 的原生 B 站客户端实验项目，使用 Kotlin、Jetpack Compose、Compose for TV 和 Media3 重写原 Flutter 版 BiliTV 的主要体验。

项目目标很简单：在电视设备上获得更轻的安装包、更低的内存占用，以及更可控的遥控器焦点体验。

## 当前状态

已接入的主要能力：

- 首页推荐、搜索、动态、历史
- TV 二维码登录
- Media3 点播播放
- 画质、编码、倍速、弹幕设置
- 字节跳动弹幕渲染引擎
- 快进预览雪碧图
- 空降助手
- 低配置模式
- 简体中文、香港繁体、台湾繁体
- Android TV launcher 图标和 TV 横幅

直播暂缓，不在当前版本范围内。

## 维护说明

这个项目基本已经完成我自己的使用目标。

除非是严重 bug，否则不会继续修。这里的严重 bug 指无法启动、核心播放崩溃、登录完全不可用、关键功能大面积失效这类问题。

任何新功能都没有计划加入，包括但不限于直播、插件系统、更多页面、更多设置项、更多 UI 风格、更多设备专项适配。需要新功能的话，建议直接 fork 后自己改。

## 关于 AI 开发

本项目主要由 AI 辅助完成，根目录的文档就是为了让 AI 能继续接手上下文：

- `AGENTS.md`：开发约束和项目规则
- `DEVELOPMENT_PLAN.md`：产品、架构和技术路线
- `DEVELOPMENT_PROGRESS.md`：阶段进度和历史决策

如果你想加功能，推荐做法是 fork 项目，然后把这些文档连同需求一起交给 AI，让 AI 先读项目结构，再按小步提交的方式改。不要直接让 AI 一次性重写大模块。

## 构建

需要本机已安装 Android SDK，并配置 `ANDROID_HOME` 或本地 `local.properties`。

PowerShell 示例：

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease -PtargetAbi=armeabi-v7a
```

当前首发目标是 `armeabi-v7a`，同时保留 `arm64-v8a` 构建能力。

发布构建目前仍使用 debug signing。如需正式发布，请自行配置签名。

## 开源库

项目中主要引用了这些开源库和工具：

| 名称 | 用途 | 链接 |
| --- | --- | --- |
| Kotlin | 主要开发语言 | https://kotlinlang.org/ |
| Gradle / Android Gradle Plugin | 构建系统 | https://gradle.org/ |
| AndroidX / Jetpack | Android 基础库、Activity、Lifecycle、DataStore 等 | https://developer.android.com/jetpack/androidx |
| Jetpack Compose | 声明式 UI | https://developer.android.com/develop/ui/compose |
| Compose for TV | TV UI 和遥控器焦点基础能力 | https://developer.android.com/develop/ui/compose/tv |
| Media3 | ExoPlayer 播放器和 DASH 播放 | https://developer.android.com/media/media3 |
| OkHttp | HTTP、WebSocket、播放数据源请求 | https://square.github.io/okhttp/ |
| Coil | 图片加载 | https://coil-kt.github.io/coil/ |
| Kotlin Coroutines | 异步任务 | https://github.com/Kotlin/kotlinx.coroutines |
| kotlinx.serialization | JSON 解析 | https://github.com/Kotlin/kotlinx.serialization |
| DanmakuRenderEngine | 原生弹幕渲染 | https://github.com/bytedance/DanmakuRenderEngine |
| OpenCC4J | 简繁转换 | https://github.com/houbb/opencc4j |
| ZXing | 二维码生成 | https://github.com/zxing/zxing |

第三方库遵循其各自许可证。

## 免责声明

本项目不是哔哩哔哩官方项目，也不与哔哩哔哩存在任何官方关联。

项目只作为个人学习、研究和自用客户端实现参考。使用者需要自行承担账号、接口、播放兼容性和后续维护风险。

## License

本项目代码使用 MIT License。详见 [LICENSE](LICENSE)。
