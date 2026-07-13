# BiliTVNative

BiliTVNative 是一个面向 Android TV 与 Android 平板的原生 B 站客户端实验项目，使用 Kotlin、Jetpack Compose、Compose for TV 和 Media3 构建大屏观看体验。

项目最初以电视遥控器交互为核心，目前已经增加平板触控界面，并完成一轮渐进式 MVVM 改造。TV 与平板共享 Repository、ViewModel、播放器和业务模型，导航外壳、视频网格、搜索入口和播放器控制层则根据设备与输入方式分别适配，避免两端交互互相干扰。

项目重点不是做一个极简壳，而是在大屏设备上尽量平衡几个实际问题：播放稳定性、TV 遥控器焦点可控性、平板触控效率、弹幕性能、主页视觉质感，以及不同硬件档位下的流畅度。

直播暂缓，不在当前版本范围内。

## 截图

> 当前截图主要展示 TV / Remote 界面；平板会自动切换为顶部导航、Touch 网格、系统搜索输入和触控播放器控制层。

<img width="2048" height="1104" alt="image" src="https://github.com/user-attachments/assets/dd043188-5cb2-422b-8905-320ddac69473" />

<img width="2244" height="1216" alt="image" src="https://github.com/user-attachments/assets/9ecc0460-011f-4618-880c-46b7ecb368c8" />

<img width="2238" height="1218" alt="image" src="https://github.com/user-attachments/assets/8bc33ebd-013f-4041-b64b-2e377ee685ee" />

<img width="2248" height="1220" alt="image" src="https://github.com/user-attachments/assets/e2af9862-f306-413b-bf07-0b8a5e8686c8" />

<img width="2248" height="1212" alt="image" src="https://github.com/user-attachments/assets/758dd11d-4c6f-485a-ac15-a3b0811ee122" />

<img width="2242" height="1216" alt="image" src="https://github.com/user-attachments/assets/2b9832f6-948b-4bbf-a14e-bddd7679b388" />

<img width="2230" height="1232" alt="image" src="https://github.com/user-attachments/assets/1589bc8c-6427-43b7-9f56-3e963f6486fb" />

## 主要功能

- 首页推荐、热门、分区内容流。
- TV 搜索键盘，以及平板系统输入框；支持搜索建议、搜索历史、结果排序和分页。
- 动态、历史记录和账号二维码登录。
- Media3 点播播放器，支持 DASH 播放、进度保存和返回焦点恢复。
- 默认画质、解码器偏好、倍速、弹幕、快进预览雪碧图。
- 字节跳动 DanmakuRenderEngine 原生弹幕渲染，避免把高频弹幕做成 Compose 节点。
- 空降助手，支持跳过片段提示并在进度条上标出跳过范围。
- 自动播放下一集、自动播放相关推荐、播放完成后自动退出。
- 播放退出二次确认、应用退出二次确认。
- 简体中文、香港繁体、台湾繁体界面和动态标题转换。
- Android TV launcher 图标和 TV 横幅。

## TV 与平板支持

应用会综合设备形态、TV `uiMode`、触摸屏和输入方式生成 `InteractionProfile`，再选择 Remote 或 Touch 交互路径。

TV / Remote 路径保留：

- 左侧电视导航栏、D-pad 确定性焦点移动和跨行滚动。
- 从侧栏进入内容区、从播放器返回来源卡片等焦点恢复逻辑。
- TV 专用播放器控制栏、遥控器面板和退出确认。

平板 / Touch 路径提供：

- 独立顶部导航栏，不渲染 TV 左侧栏。
- 根据可用宽度切换 2 / 3 / 4 列的视频网格。
- 触底分页、下拉刷新、系统搜索输入框和可触摸的历史/建议/排序界面。
- 播放器单击显隐控制、双击播放暂停、长按临时 2 倍速、横滑快进/后退、左右区域亮度/音量调节、可拖动进度条和触摸侧栏面板。

两端共享同一套数据与业务状态，不复制推荐、搜索、动态、历史或播放请求逻辑；TV 焦点代码不会进入 Touch 网格，Touch 手势和下拉刷新也不会进入 TV 网格。

## 架构与 MVVM

项目采用渐进式 MVVM，而不是为了形式追求“所有状态都必须进入 ViewModel”。当前边界如下：

- `RecommendViewModel`、`SearchViewModel`、`DynamicFeedViewModel`、`HistoryFeedViewModel` 负责页面加载、刷新、分页、筛选和可恢复的业务状态。
- `PlaybackSessionViewModel` 通过 `SavedStateHandle` 保存当前 `PlaybackRequest`，Activity 重建后可以恢复播放会话。
- `AppShellNavigationViewModel` 管理应用级目的地、访问状态和播放来源卡片信息，冷启动仍默认进入推荐主页。
- Repository 负责网络、播放信息、进度、评论和账号数据；DataStore 负责设置、会话、搜索历史和本地播放进度。
- TV 焦点请求、LazyGrid/LazyColumn 滚动、Touch 手势、`ExoPlayer`、`PlayerView`、`SurfaceView`、WakeLock 和高频播放位置仍保留在 UI runtime，因为这些状态与 Compose/Android 生命周期和真实控件直接绑定。

这种拆分让数据逻辑可以独立测试和跨 TV/平板复用，同时避免把高频播放器状态塞进宽 `StateFlow`，导致不必要的大范围重组。

当前工程没有引入 Room、Koin 或 Compose Navigation。现阶段继续使用显式 `AppContainer`、Repository、Flow、DataStore 和轻量 ViewModel；只有出现明确需求时才会评估新增基础设施。

## UI 与视觉

应用提供 4 种主页主题：

- 默认粉
- 深黑
- 高级灰
- 蓝灰

视觉性能模式分为 3 档：

- 流畅：面向低配置电视和平板，关闭重动画、流光、阴影、封面预取和图片内存缓存，缩略图使用较低尺寸与 RGB_565。
- 均衡：默认推荐档，保留主题色、仿玻璃表面、轻缩放、边框、文字颜色过渡、封面轻提亮和平滑滚动。
- 精致：手动开启的高视觉档，增加更强玻璃氛围、环境高光、更高质量缩略图、主题色斜向流光、液态玻璃感边缘、卡片轻微放大和上浮。

Android 13 及以上设备可以在精致档中单独开启实验液态玻璃控件。开启后，侧边栏、首页分区胶囊、视频卡片、设置行和播放器控制面板会使用真实液态玻璃表面；关闭或不支持时自动回落到自绘半透明玻璃、边框和高光。

主页主题只作用于主页、搜索、动态、历史、设置、侧边栏和标签栏。播放器继续使用独立稳定配色，避免主题化影响播放性能和兼容性；播放器控制、面板和弹窗会按视觉性能策略使用液态玻璃或 fallback 表面。

## 播放器体验

播放器使用系统硬解优先的 Media3 ExoPlayer，并保留 SurfaceView 路径以优先保证兼容性和性能。

播放器 UI 包括：

- 顶部标题、UP、发布时间、播放量、当前时间。
- TV 底部大进度条、控制按钮、画质和弹幕状态，以及平板专用 Touch 控制层。
- 控制层隐藏时的迷你进度条，可在设置中关闭，默认开启。
- 右侧设置、选集、UP 主更多视频、相关推荐面板。
- 画质、弹幕、倍速子面板，长列表使用滚动而不是压缩字号。
- 推荐视频和发布者更多视频使用更宽面板和更大封面，播放数、弹幕数、时长贴近主页卡片展示，避免小时级时长和万级弹幕数挤在一起。
- 进入播放时由来源卡片封面放大到全屏；退出时抓取 `SurfaceView` 当前有效画面并沿同一 shared key 缩回来源卡片。流畅档会关闭这组动画和抓帧，直接切换页面。

弹幕层由原生 DanmakuView 承载，弹幕 XML 解码和解析放在后台线程，应用层不使用固定 delay 驱动弹幕重绘。

## 设置分组

设置页按使用语义分成三组：

- 播放设置：默认画质、解码器、快进预览、空降助手、退出确认、自动连播、自动推荐、播放完成退出、显示时间、迷你进度条。
- UI/UX：效果档位、液态玻璃、主页主题、切换时自动确认、切换时自动刷新。
- 系统设置：清理缓存、语言。

首页分区开关独立显示在右侧，至少保留一个分区。

## 构建

构建环境：

- JDK 17
- Android SDK 36
- Android 6.0（API 23）及以上设备

Debug 构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

同时构建 TV 常用的 `armeabi-v7a` 和现代平板/电视常用的 `arm64-v8a` Release：

```powershell
.\build-release.bat
```

只构建单个 ABI：

```powershell
.\build-release.bat armeabi-v7a
.\build-release.bat arm64-v8a
```

Release APK 会保存在：

```text
%USERPROFILE%\.gradle\bilitv-native-build\release-apks\
```

对应文件名为 `BiliTVNative-armeabi-v7a-release.apk` 和 `BiliTVNative-arm64-v8a-release.apk`。Release 已启用 R8、资源裁剪、语言资源过滤和保守 Baseline Profile。

## 技术栈

| 名称 | 用途 | 链接 |
| --- | --- | --- |
| Kotlin | 主要开发语言 | https://kotlinlang.org/ |
| Gradle / Android Gradle Plugin | 构建系统 | https://gradle.org/ |
| AndroidX / Jetpack | Activity、Lifecycle、ViewModel、SavedStateHandle、DataStore 等 | https://developer.android.com/jetpack/androidx |
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

本项目全部由 AI 辅助完成。根目录文档用于保留上下文和约束：

- `AGENTS.md`：开发约束和项目规则。
- `DEVELOPMENT_PLAN.md`：产品、架构和技术路线。
- `DEVELOPMENT_PROGRESS.md`：阶段进度和历史决策。

继续开发时建议按小步修改、编译、安装、实机验证的节奏推进，不要一次性重写大模块。播放器、弹幕、TV 焦点、平板 Touch 手势和液态玻璃开关尤其需要同时考虑性能档位与双端隔离。

## 免责声明

本项目不是哔哩哔哩官方项目，也不与哔哩哔哩存在任何官方关联。

项目只作为个人学习、研究和自用客户端实现参考。使用者需要自行承担账号、接口、播放兼容性和后续维护风险。

## License

本项目代码使用 MIT License。详见 [LICENSE](LICENSE)。
