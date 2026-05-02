# BiliTVNative 开发代理规则

## 项目目标

使用 Kotlin、Jetpack Compose、Compose for TV 和 Media3，将 `BiliTVNative` 构建为原生 Android TV 应用。

现有 Flutter 项目 `../BiliTV` 是行为参考。除非用户明确要求，否则不要修改 Flutter 项目。

## 阶段纪律

- 只处理用户明确要求的 Phase。
- 除非当前 Phase 编译或运行必需，否则不要生成后续 Phase 的代码。
- 不要为未来功能创建当前没有实际用途的占位抽象。
- 变更范围要小，便于审查和运行。
- 对复杂模块，先定义接口和数据结构；只有在用户确认或当前任务明确需要时，再实现具体行为。
- 保持 `DEVELOPMENT_PROGRESS.md` 最新。每完成一个可验证任务，都要在同一次变更中更新状态和验证备注。

## 当前产品决策

- Android 包名：`com.kirin.bilitv`。
- 最低 SDK 目标：从 `minSdk 23` 开始；只有在不损害 UI 质量、性能、Compose 或 Media3 的前提下，才评估 `minSdk 21`。
- 首发 ABI：`armeabi-v7a`。
- 保留 `arm64-v8a` 构建能力，但不要把它作为首个主发布包。
- 不做应用内更新检查。
- 不做可扩展插件系统。
- 只保留空降助手作为内置设置开关。
- 直播播放暂缓，直到用户明确要求恢复。
- 弹幕渲染可以使用已内置的字节跳动 `danmaku-render-engine` `DanmakuView`；除非性能分析或产品需求证明有必要，否则不要替换为自定义渲染器。

## 设计系统规则

- 在 Material 3 for TV / Compose for TV 之上使用 `BiliTV 设计系统`。
- 所有颜色、间距、圆角、焦点缩放、动画时长和缓动曲线必须来自 `BiliTokens.kt`。
- `BiliPink` 必须是 `#FB7299`。
- 不要在 Composable 内硬编码 `Color(0x...)`、`#FF...`、随机 `dp` 或随机动画时长。
- 如果需要新的视觉值，先把它加入 tokens。
- 焦点视觉语言应使用缩放、边框、阴影和 elevation，不要只依赖颜色。
- 优先保证电视远距离可读性和 D-pad 操作清晰度，而不是手机端的信息密度。

## TV 焦点规则

- D-pad 焦点必须确定。
- 复杂网格不要依赖默认最近邻焦点搜索。
- 侧边栏到内容区、内容区到侧边栏的切换必须使用 `FocusRequester` / 焦点属性。
- 从播放器、对话框、面板或设置页返回时必须恢复焦点。
- 从播放器返回首页时，尽量聚焦之前选中的视频卡片。
- 长按 D-pad 不得丢焦点，也不能跳到隐藏项。

## 播放器规则

- 使用 Media3 ExoPlayer。
- 默认视频渲染使用 `SurfaceView`。
- 不要对视频 surface 本身应用圆角裁切、透明度动画、缩放变换或复杂 Compose 特效。
- 控制层、弹幕、面板和叠加层作为独立 UI 层叠在播放器上方。
- 播放请求使用专用的 Media3 `OkHttpDataSource.Factory` 包装器。
- 播放分片请求必须稳定携带 B 站请求头和 Cookie。
- 在选择播放编码偏好或 B 站播放参数前，先探测设备 MediaCodec 能力。
- 编码探测只能作为参考，不能当作绝对事实。播放仍必须支持 H.264 回退和设备级降级规则。
- 开发构建保留隐藏播放器诊断叠加层。可用时显示编码、分辨率、已选画质、缓冲状态、掉帧和 CDN/host 信息。
- 诊断信息绝不能显示 Cookie、SESSDATA、tokens 或其他敏感值。
- 播放器状态必须与普通页面状态隔离，避免播放期间大范围重组。
- 在 `onPause` 保存播放进度，不要等到 `onStop` 或 `onDestroy`。

## 网络与 Bilibili 规则

- OkHttp 必须在需要时支持 Bilibili API 的 Brotli 响应。
- 直播 WebSocket 包体的 Brotli 解码必须显式处理。
- 需要 WBI 签名时，使用纯 Kotlin 实现。
- 除非有测量依据，否则不要为了 WBI 引入原生加密库或额外底层库。
- 请求头和签名行为要尽量贴近 Flutter 参考实现。
- 二维码登录轮询必须感知生命周期，应用进入后台时暂停。

## 图片规则

- 使用 Coil 加载图片。
- 海报和头像请求必须始终限制尺寸。
- 明确设置内存缓存和磁盘缓存上限。
- 低内存路径中的列表海报缩略图可以使用 `Bitmap.Config.RGB_565`。
- 不要对详情图、头像、透明图、渐变图或快进预览雪碧图全局强制 `RGB_565`。
- 不要预加载整页或全部分类。只预取可见窗口附近的内容。

## 弹幕规则

- 不要把每条弹幕渲染成一个 Compose 节点。
- 使用已批准的字节跳动 `danmaku-render-engine` 原生 `DanmakuView` 叠加层；如果以后移除该引擎，再使用基于 Canvas 的自定义叠加层。
- XML 获取、解码和解析不能运行在 UI 线程。
- 使用字节跳动 `DanmakuView` 时，轨道分配、碰撞计算和帧节奏交给引擎处理；应用代码不要在外层增加 coroutine `delay()` 重绘循环。
- 如果替换为自定义渲染器，轨道分配和碰撞计算必须在 UI 线程外执行，绘制循环必须用 `Choreographer` 或 `withFrameNanos` 与帧同步。
- 只有在性能分析证明 Kotlin 或已批准引擎是瓶颈后，才考虑用 C/C++/so 做弹幕轨道分配或碰撞计算。

## 构建规则

- 从一开始就使用 Gradle 版本目录 (`libs.versions.toml`)。
- 依赖必须集中管理。
- 发布构建应开启 R8 和资源裁剪。
- 即使首发 `armeabi-v7a`，也要保留 `arm64-v8a` 构建支持。
- 尽早配置 Baseline Profile 支持，但只有在启动、首页、搜索和播放器主路径存在后，才生成有意义的 profile。

## 编码风格

- 优先使用 Kotlin、coroutines、Flow、ViewModel、DataStore、Room、Coil、OkHttp 和 Media3。
- 避免为核心服务使用临时全局单例。
- 对 Network、Repository、Player、Storage、UseCase 和 ViewModel 依赖，使用轻量依赖注入或显式 `AppContainer`。
- 如果引入 Koin，把作用域限制在核心应用服务和 ViewModels。不要注入简单 UI helper 或 token 对象。
- 在真正出现第二个使用场景前，不要过度抽象。
- 文件尺寸保持合理。在文件变得难以审查前按职责拆分。
- 不要一次性输出几千行复杂模块代码。
- 静态 UI 文案必须放在 `res/values/strings.xml`，并在 Compose 中通过 `stringResource()` 访问。
- 不要在 Composable 内硬编码面向用户的中文或英文文案。服务端内容、日志和临时 debug-only 标签除外。
- 只为不明显的决策或复杂 TV/播放器行为添加注释。
- 与 `DEVELOPMENT_PLAN.md` 中的既定项目决策保持一致。

## 验证

每个已实现切片都运行最小但有效的验证：

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r $env:USERPROFILE\.gradle\bilitv-native-build\app\outputs\apk\debug\app-debug.apk
adb shell dumpsys meminfo com.kirin.bilitv
adb shell dumpsys gfxinfo com.kirin.bilitv
```

涉及播放时，使用相同的 BVID、CID、画质、编码、请求头和弹幕时间，对比 Flutter 参考应用行为。
