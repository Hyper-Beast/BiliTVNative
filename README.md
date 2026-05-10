# BiliTVNative

BiliTVNative 是一个面向 Android TV 的原生 B 站客户端实验项目，使用 Kotlin、Jetpack Compose、Compose for TV 和 Media3 重写电视端观看体验。

项目重点不是做一个极简壳，而是在电视设备上尽量平衡几个实际问题：播放稳定性、遥控器焦点可控性、弹幕性能、主页视觉质感，以及不同硬件档位下的流畅度。

直播暂缓，不在当前版本范围内。

## 截图

<img width="2048" height="1104" alt="image" src="https://github.com/user-attachments/assets/dd043188-5cb2-422b-8905-320ddac69473" />

<img width="2238" height="1218" alt="image" src="https://github.com/user-attachments/assets/8bc33ebd-013f-4041-b64b-2e377ee685ee" />

<img width="2248" height="1220" alt="image" src="https://github.com/user-attachments/assets/e2af9862-f306-413b-bf07-0b8a5e8686c8" />

<img width="2248" height="1212" alt="image" src="https://github.com/user-attachments/assets/758dd11d-4c6f-485a-ac15-a3b0811ee122" />



## 主要功能

- 首页推荐、热门、分区内容流。
- 搜索键盘、搜索建议、搜索历史、搜索结果排序和分页。
- 动态、历史记录和账号二维码登录。
- Media3 点播播放器，支持 DASH 播放、进度保存和返回焦点恢复。
- 默认画质、解码器偏好、倍速、弹幕、快进预览雪碧图。
- 字节跳动 DanmakuRenderEngine 原生弹幕渲染，避免把高频弹幕做成 Compose 节点。
- 空降助手，支持跳过片段提示并在进度条上标出跳过范围。
- 自动播放下一集、自动播放相关推荐、播放完成后自动退出。
- 播放退出二次确认、应用退出二次确认。
- 简体中文、香港繁体、台湾繁体界面和动态标题转换。
- Android TV launcher 图标和 TV 横幅。

## UI 与视觉

应用提供 4 种主页主题：

- 默认粉
- 深黑
- 高级灰
- 蓝灰

视觉性能模式分为 3 档：

- 流畅：面向低端电视，关闭重动画、流光、阴影、封面预取和图片内存缓存，缩略图使用较低尺寸与 RGB_565。
- 标准：默认推荐档，保留主题色、仿玻璃表面、轻缩放、边框、文字颜色过渡、封面轻提亮和平滑滚动。
- 高级：手动开启的高视觉档，增加更强玻璃氛围、环境高光、更高质量缩略图、主题色斜向流光、液态玻璃感边缘、卡片轻微放大和上浮。

Android 13 及以上设备可以在高级档中单独开启实验液态玻璃控件。开启后，侧边栏、首页分区胶囊、视频卡片、设置行和播放器控制面板会使用真实液态玻璃表面；关闭或不支持时自动回落到自绘半透明玻璃、边框和高光。

主页主题只作用于主页、搜索、动态、历史、设置、侧边栏和标签栏。播放器继续使用独立稳定配色，避免主题化影响播放性能和兼容性；播放器控制、面板和弹窗会按视觉性能策略使用液态玻璃或 fallback 表面。

## 播放器体验

播放器使用系统硬解优先的 Media3 ExoPlayer，并保留 SurfaceView 路径以优先保证兼容性和性能。

播放器 UI 包括：

- 顶部标题、UP、发布时间、播放量、当前时间。
- 底部大进度条、控制按钮、画质和弹幕状态。
- 控制层隐藏时的迷你进度条，可在设置中关闭，默认开启。
- 右侧设置、选集、UP 主更多视频、相关推荐面板。
- 画质、弹幕、倍速子面板，长列表使用滚动而不是压缩字号。
- 推荐视频和发布者更多视频使用更宽面板和更大封面，播放数、弹幕数、时长贴近主页卡片展示，避免小时级时长和万级弹幕数挤在一起。
- 进入和退出播放使用短黑屏遮罩过渡，避免 Surface 切换时出现闪烁。

弹幕层由原生 DanmakuView 承载，弹幕 XML 解码和解析放在后台线程，应用层不使用固定 delay 驱动弹幕重绘。

## 设置分组

设置页按使用语义分成三组：

- 播放设置：默认画质、解码器、快进预览、空降助手、退出确认、自动连播、自动推荐、播放完成退出、显示时间、迷你进度条。
- UI/UX：效果档位、液态玻璃、主页主题、切换时自动确认、切换时自动刷新。
- 系统设置：清理缓存、语言。

首页分区开关独立显示在右侧，至少保留一个分区。

## 构建与安装

常用 release 构建命令：

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:assembleRelease -PtargetAbi=armeabi-v7a
```

生成 APK：

```text
C:\Users\Kirin\.gradle\bilitv-native-build\app\outputs\apk\release\app-release.apk
```

安装到指定电视：

```powershell
adb -s 192.168.1.131:5555 install -r -d "$env:USERPROFILE\.gradle\bilitv-native-build\app\outputs\apk\release\app-release.apk"
```

## 技术栈

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
| AndroidLiquidGlass / Backdrop | Android 13+ 实验液态玻璃控件 | https://github.com/Kyant0/AndroidLiquidGlass |
| ZXing | 二维码生成 | https://github.com/zxing/zxing |

第三方库遵循其各自许可证。

## 开发说明

本项目主要由 AI 辅助完成。根目录文档用于保留上下文和约束：

- `AGENTS.md`：开发约束和项目规则。
- `DEVELOPMENT_PLAN.md`：产品、架构和技术路线。
- `DEVELOPMENT_PROGRESS.md`：阶段进度和历史决策。

继续开发时建议按小步修改、编译、安装、实机验证的节奏推进，不要一次性重写大模块。播放器、弹幕、焦点路径和液态玻璃开关尤其需要同时考虑性能档位和电视端遥控器操作。

## 免责声明

本项目不是哔哩哔哩官方项目，也不与哔哩哔哩存在任何官方关联。

项目只作为个人学习、研究和自用客户端实现参考。使用者需要自行承担账号、接口、播放兼容性和后续维护风险。

## License

本项目代码使用 MIT License。详见 [LICENSE](LICENSE)。
