# BiliTVNative 开发计划

## 目标

将 BiliTV 重写为原生 Android TV 应用，技术栈使用 Kotlin 和 Jetpack Compose，同时保留当前 Flutter 项目作为行为参考和回退版本。

这次重写的目标是改善电视遥控器操作响应、降低安装包体积和空闲内存，并保持现有播放行为兼容。

## 目标成果

- 使用 Kotlin 和 Jetpack Compose 构建原生 Android TV 应用。
- 与当前 Flutter 版 BiliTV 达成功能等价。
- D-pad 遥控器焦点导航更快、更可预测。
- 第一版主发 `armeabi-v7a` 发布 APK，体积小于当前 Flutter 构建。
- 低端 Android TV 设备上的空闲内存更低。
- B 站播放、登录、历史、搜索、弹幕和设置流程稳定可用；直播后续单独评估。

## 当前边界与暂缓内容

- 跨平台支持。
- 复刻完整插件系统。
- 应用内更新检查和更新流程。
- 复用 Flutter UI 代码。
- 直播播放和直播弹幕，直到用户明确要求恢复。
- 在当前单 app 模块稳定前拆分多 Gradle 模块。
- 引入 Room、Koin 或 Compose Navigation；当前代码没有使用这些依赖，只有出现明确需求时再评估。

## 项目布局

原生项目放在 Flutter 项目旁边：

```text
C:\Users\Kirin\OneDrive\Code\
  BiliTV\             # 现有 Flutter 实现，保留作为参考
  BiliTVNative\       # 新 Kotlin + Compose 实现
```

当前 Android 包名：

```text
com.kirin.bilitv
```

当前源码布局：

```text
BiliTVNative/
  app/
    src/main/java/com/kirin/bilitv/
      BiliTvApplication.kt
      MainActivity.kt
      core/
        app/
        auth/
        cache/
        i18n/
        image/
        model/
        network/
        player/
        settings/
        storage/
      ui/
        account/
        common/
        feed/
        focus/
        glass/
        home/
        i18n/
        login/
        player/
        search/
        settings/
        shell/
        theme/
```

初期使用单 Android app 模块。等主流程稳定后，再考虑拆分多模块。

## 技术栈

核心：

- Kotlin 2.x
- JDK 17
- Android Gradle Plugin 9.x
- Gradle 版本目录 / `libs.versions.toml` 统一管理依赖版本
- minSdk 当前为 23；如果 Compose、Media3 和目标设备测试都稳定，再评估降到 21
- compileSdk / targetSdk 当前为 36

UI：

- Jetpack Compose
- Compose for TV / `androidx.tv:tv-material`
- 当前不使用 Compose Navigation；页面级路由由 `AppShell`、`AppDestination` 和显式焦点恢复状态管理
- 自定义 TV 焦点工具，覆盖 D-pad 网格、侧边栏、播放器浮层和弹窗面板

播放：

- AndroidX Media3 ExoPlayer
- Media3 DASH 支持 B 站点播
- 需要时使用 Media3 HLS 支持直播流
- Media3 专用 `OkHttpDataSource.Factory`，统一注入 B 站播放请求头和 Cookie
- `PlayerView` 通过 Compose `AndroidView` 承载
- 控制栏、画质选择、面板和播放状态使用 Compose 叠加层

网络：

- OkHttp 负责 HTTP 和 WebSocket
- Kotlin coroutines 负责异步任务
- Kotlin Flow 负责状态更新
- kotlinx.serialization 负责 JSON 解析

存储：

- DataStore Preferences 存设置、auth/session、搜索历史、WBI key、播放进度和弹幕设置等小型状态
- 当前不使用 Room；只有播放进度、历史、本地元数据或缓存索引需要复杂查询/清理/同步时再迁移
- Android 文件缓存存图片和雪碧图缓存

图片：

- Coil 2.x / Coil Compose 负责图片加载
- 明确限制内存缓存和磁盘缓存
- 海报、头像请求必须约束目标尺寸
- TV 卡片离屏后懒加载，避免一次性解码太多图
- 列表海报缩略图允许使用 `Bitmap.Config.RGB_565` 降低内存
- 详情大图、头像、透明图和需要高质量显示的图片默认保留高质量配置

弹幕：

- 使用 XML pull parser 解析 B 站弹幕 XML
- 使用已确认的字节跳动 `danmaku-render-engine` `DanmakuView` 作为原生叠加层渲染高频弹幕
- 避免把每条弹幕渲染成一个 Compose 节点
- 弹幕 XML 解析、轨道分配和碰撞计算不能放在 UI 线程
- 第一版将轨道分配、碰撞和帧节奏委托给字节跳动弹幕引擎；若后续替换为自研 Canvas，再使用 Kotlin 预计算轨道和时间窗口缓存
- 恢复直播后，直播弹幕使用队列上限和帧预算控制 drain 频率

构建：

- 第一版主发 `armeabi-v7a` 包；工程保留 `arm64-v8a` 构建能力
- 发布构建开启 R8 代码压缩
- 发布构建开启资源裁剪
- 当前已有保守 Baseline Profile，发布构建已包含 `baseline.prof` / `baseline.profm`
- 发布签名前先沿用 debug 签名

## 设计语言

采用自定义 `BiliTV 设计系统`，底座使用 Material 3 for TV / Compose for TV，视觉层使用 B 站品牌风格。

原则：

- 使用 MD3 和 TV Material 的组件能力，但不照搬默认 Material 长相。
- 固定品牌色，不使用 Material You 动态取色。
- 面向电视远距离观看，优先保证焦点清晰、字号可读、层级明确。
- 视觉方向更接近媒体电视客户端，而不是普通 Android 设置应用。
- 焦点状态是核心视觉语言，不只改变颜色，还要包含缩放、边框、阴影和内容抬升。
- 动效可以比手机端更积极，但必须短、稳、可预测。

建议视觉基准：

- 左侧导航 + 横向内容轨道 + 大海报/沉浸式选中预览。
- 播放器控制层使用半透明暗色底，减少遮挡视频内容。
- 主要品牌色使用 B 站粉。
- 背景以深灰黑为主，避免大面积纯黑和大面积纯粉。
- 卡片、面板和按钮圆角统一从 token 读取。
- 焦点动画建议控制在 120-180ms。

## 首页主题与玻璃视觉专项

本专项覆盖首页、搜索、动态、历史、设置、侧边导航、主页标签栏和共享焦点控件。播放器视频内容和 `SurfaceView` 本体不参与主页主题、液态玻璃采样、裁切或变换；播放器覆盖层控件可以在液态玻璃开启时使用同一套单层玻璃 fallback，但不能影响视频 surface。

目标视觉方向：

- 做接近电视媒体客户端的深色玻璃质感，而不是普通 Material 卡片。
- 使用“假玻璃”为基础：半透明深色面板、细边框、渐变高光、轻阴影和背景氛围色。
- 高配置设备再启用环境高光、焦点流光和更强玻璃层次；不把实时模糊作为基础能力。
- Android 9-11 设备必须能显示合理 fallback，不依赖 `RenderEffect` 或系统级 Liquid Glass。
- Android 13+ 设备可以通过独立开关试验 AndroidLiquidGlass；当前仅在精致档允许启用，Android 13 以下持久化读取和写入都会强制关闭，并保留自绘玻璃边缘作为 fallback。
- 卡片列表禁止每张卡片持续实时模糊，避免滚动和换行焦点掉帧。
- 焦点缩放保持克制，建议不超过 `1.055`；主要反馈来自边框、提亮、文字颜色和一次性高光。

主页主题预设固定为 4 种：

- 默认粉：保留 B 站粉作为焦点色和主要强调色。
- 深黑：更低亮度背景，适合 OLED、夜间和高对比电视。
- 高级灰：接近参考图的灰蓝玻璃氛围，突出半透明层次。
- 蓝灰：偏电视系统 UI，降低粉色面积，适合长期观看。

视觉性能模式固定为 3 档：

- 流畅：保留当前低配置策略。关闭动画、模糊、流光、阴影和平滑滚动；缩略图使用低尺寸和 RGB_565；禁用图片内存缓存和预取。
- 均衡：默认推荐档。启用主题色、假玻璃、轻缩放、边框和文字颜色过渡、封面轻提亮；不启用实时模糊，流光默认关闭或极轻。
- 精致：手动开启的高视觉档。在均衡基础上启用更强玻璃氛围、环境高光、跟随主题色的焦点卡片斜向流光、液态玻璃感边缘、轻微放大、卡片上浮、更高质量缩略图和更大的图片缓存。

首次启动默认策略：

- 设备总内存低于 1GB：默认流畅。
- 设备总内存 1GB-2GB：默认均衡。
- 设备总内存高于 2GB：默认均衡，不自动打开精致。
- 精致模式必须由用户手动开启，避免电视系统虚标内存或 GPU 较弱导致卡顿。

当前已实现要点：

1. 主页主题、视觉性能模式、液态玻璃开关均通过 DataStore 持久化。
2. `HomeColorScheme` / `LocalHomeColors` 负责主页域主题，播放器视频内容不纳入主题。
3. 设置页按 `播放设置`、`UI/UX`、`系统设置` 分组；视觉效果档位、液态玻璃控件和主页主题属于 `UI/UX`。
4. 首页背景、侧边栏、分区胶囊、视频卡片、设置/搜索共享控件均支持液态玻璃开启/关闭两条路径。
5. 精致模式包含焦点卡片流光、上浮、轻微放大和液态玻璃感边缘；流畅模式关闭高成本动画、预取、阴影和内存缓存。
6. 播放器覆盖层控件可使用液态玻璃表面，视频 `SurfaceView`、弹幕引擎和播放链路不参与视觉采样。

## 设计令牌

从第一版开始建立统一 token 文件，所有颜色、尺寸、圆角、动画时长、焦点缩放和阴影都从 token 读取，页面内不散写魔法值。

建议文件：

```text
app/src/main/java/com/kirin/bilitv/ui/theme/BiliTokens.kt
```

建议初始内容：

```kotlin
object BiliColors {
    val BiliPink = Color(0xFFFB7299)
    val Background = Color(0xFF101014)
    val Surface = Color(0xFF1A1A20)
    val SurfaceElevated = Color(0xFF24242C)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xB3FFFFFF)
}

object BiliSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
}

object BiliRadius {
    val Card = 12.dp
    val Panel = 16.dp
    val Pill = 999.dp
}

object BiliMotion {
    const val FocusMs = 140
    const val PanelMs = 180
    const val OverlayMs = 160
    val FocusEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}

object BiliFocus {
    const val CardScale = 1.06f
    val BorderWidth = 2.dp
}
```

命名规则：

- 品牌粉统一命名为 `BiliPink`，值为 `#FB7299`。
- 新增颜色必须先进入 token 文件，再被页面引用。
- 组件默认尺寸、间距、圆角、动画时长必须优先使用 token。
- 焦点缩放、焦点动画和面板动画必须使用 `BiliMotion` 中的时长和 easing。
- 特殊页面可以新增局部 token，但不能在 Composable 内直接散写关键视觉值。

## 架构

采用渐进式 MVVM 与轻量状态持有器分层：

```text
UI 页面 / AppShell / UI-runtime 状态持有器
  -> Feature ViewModel / Session ViewModel
  -> Repository / Store
  -> API / DataStore / 文件缓存
```

规则：

- Screen 只渲染 ViewModel、上层参数或本地 UI 状态持有器暴露的状态对象，避免在 UI 内散落跨页面全局状态。
- 推荐、搜索、动态、历史和播放会话已使用 ViewModel / `StateFlow`；后续只在业务数据需要跨配置变化持有、复用或测试时继续定点增加 ViewModel，不追求形式上的严格 MVVM。
- `FocusRequester`、列表滚动、动画、手势、`ExoPlayer`、`PlayerView`、`SurfaceView`、`DanmakuView` 和 `WakeLock` 等 UI-runtime 对象继续由 Compose/UI 生命周期持有，不移入 ViewModel。
- Repository 负责数据加载、缓存策略和请求重试。
- Store 负责 DataStore 持久化和小型状态读写。
- API / Repository 行为尽量贴近现有 Flutter service，方便做等价校验。
- 播放器状态必须和普通页面状态隔离，避免播放中频繁大范围重组。

## AI 实施约束

后续开发会长期由 AI 辅助执行，因此必须把实现边界写清楚，避免过度发散、提前抽象和一次性生成不可维护的大文件。

约束：

- 实际开发时只执行用户指定的任务。
- 未要求进入下一项功能前，不生成后续功能代码。
- 不为了未来功能提前创建无实际调用的抽象层。
- 复杂模块先定义接口、数据结构和调用边界，再实现具体逻辑。
- 不一次性输出几千行复杂文件。
- Compose UI 必须优先使用 `BiliTokens.kt` 中的 token。
- 禁止在 Composable 中散写十六进制颜色、随机 `dp`、随机动画时长和临时视觉常量。
- 必要的新视觉值必须先进入 token 文件，再被页面引用。
- 每个阶段结束时优先保证可构建、可运行、可验证，而不是追求一次性完整。

项目根目录应保留 `AGENTS.md`，用于约束后续 AI 开发行为。`DEVELOPMENT_PLAN.md` 负责描述产品和架构计划，`AGENTS.md` 负责描述执行规则和代码约束。

## 确定性焦点系统

TV 焦点不能依赖默认最近邻搜索。复杂页面必须显式定义焦点路径，避免长按 D-pad 或从弹窗返回时跳到不可预测位置。

焦点系统要求：

- 在 `ui/focus` 下建立统一焦点工具。
- 侧边栏进入内容区必须使用 `FocusRequester` 指定进入点。
- 内容区返回侧边栏必须回到当前 tab 对应的侧边栏项。
- LazyRow、LazyGrid、播放器面板和设置弹窗必须定义边界行为。
- 弹窗关闭后必须恢复到打开弹窗前的焦点。
- 不在复杂网格中依赖系统默认 `FocusDirection.Right/Left/Up/Down` 最近邻搜索。
- 焦点移动逻辑和焦点视觉状态分离，避免为了视觉动画破坏导航确定性。

当前工具：

```text
ui/focus/BiliFocusableSurface.kt
ui/home/TvVideoGrid.kt
ui/shell/AppShell.kt
ui/player/PlayerOverlay.kt
ui/settings/SettingsScreen.kt
```

验收：

- 长按方向键不丢焦点。
- 从侧边栏进入内容区时总是进入当前页面的首个合理内容项。
- 从播放器面板、设置弹窗、画质选择弹窗返回后焦点可恢复。
- 焦点不跳到屏幕外、不可见项或错误 tab。

## Media3 播放数据源规范

B 站播放请求对请求头和 Cookie 敏感。播放器的数据源必须独立封装，不能散落在各个播放器调用处。

要求：

- 在 `core/player` 中建立 `BiliMediaDataSourceFactory`。
- 使用 Media3 `OkHttpDataSource.Factory`。
- 所有播放分片请求必须统一注入 `User-Agent`、`Referer`、`Origin` 和 Cookie。
- 播放请求头应尽量和 Flutter 版当前行为保持一致，便于排查 403。
- 点播和直播可以共享底层 OkHttpClient，但播放数据源配置必须显式。
- 画质切换、分 P 切换和重试必须复用同一套 header 策略。

建议初始 header：

```text
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
Referer: https://www.bilibili.com/
Origin: https://www.bilibili.com
Cookie: SESSDATA=...
```

播放器缓冲策略：

- 电视端允许用适度内存换取弱网流畅。
- 第一版建立 `TvPlaybackLoadControl`，比 Media3 默认值略积极。
- 不无脑放大 buffer，避免高码率 DASH 播放、画质切换和低内存设备出现反效果。
- buffer 参数必须通过实际设备播放测试再调。

## PlayerView Surface 策略

Android TV 播放器默认使用 `SurfaceView`，优先保证硬解性能、稳定性和兼容性。

约束：

- 默认视频渲染使用 `SurfaceView`。
- 不追求视频本体的圆角裁切、透明度动画、缩放变换或复杂 Compose 特效。
- 控制栏、弹幕、画质面板和提示层必须作为独立叠加层位于 `PlayerView` 上方。
- 不对视频 View 本身做复杂变换，避免 SurfaceView 在 Compose `AndroidView` 中出现黑屏、穿透或层级异常。
- 如未来必须做复杂视频变换，再单独评估 `TextureView`，不能默认切换。

验收：

- 播放器控制层和弹幕能稳定覆盖在视频上方。
- 退出播放器后 Surface 和 player 资源能释放。
- 播放器页面不因圆角、透明或层级效果导致黑屏。

## 图片解码和缓存策略

图片内存治理不只限制尺寸，也要控制解码格式、缓存窗口和预取策略。

策略：

- 首页和列表海报缩略图优先约束宽高，并可在低内存设备上使用 `RGB_565`。
- 详情大图、头像、透明图、渐变图和快进预览图默认不强制 `RGB_565`。
- Coil 内存缓存和磁盘缓存必须设置上限。
- 首页只预取当前可见窗口附近内容，不预取全部分类。
- 进入播放器时释放非必要首页图片引用。
- 低内存模式可以进一步降低图片缓存、预取数量和海报解码质量。

验收：

- 首页滚动不会因为图片解码出现明显卡顿。
- 低端设备上图片缓存不会持续增长。
- 海报远距离观看无明显不可接受的色带或锯齿。

## 弹幕帧循环规范

弹幕刷新必须由字节跳动弹幕引擎或系统帧时钟驱动，避免用固定 `delay()` 驱动重绘导致 TV 上卡顿、撕裂或节奏不稳。

约束：

- 使用字节跳动 `DanmakuView` 时由引擎负责绘制循环；自研 Canvas 时必须使用 Android `Choreographer` 或 Compose `withFrameNanos`。
- 不使用 coroutine `delay(16)`、`Timer` 或固定 sleep 作为主绘制节拍。
- UI 线程只承载已解析数据到渲染引擎的更新，不做 XML 解码、解析或额外重型排布计算。
- XML 解析、轨道分配、碰撞计算和过滤规则在后台线程完成。
- 播放暂停、seek、倍速切换后必须重置弹幕时间窗口。

验收：

- 弹幕移动节奏和屏幕刷新率同步。
- seek 后弹幕能快速重新定位。
- 高弹幕量下 UI 线程没有持续重型计算。

## 生命周期保存策略

Android TV 设备内存较小，应用进入后台后容易被系统杀掉。核心状态不能只依赖 `onStop` 或 `onDestroy` 保存。

约束：

- 播放进度必须在 `onPause` 触发保存。
- 当前 BVID、CID、播放位置、画质、倍速和弹幕开关等播放器关键状态应尽早持久化或缓存。
- auth/session/settings 变更后立即写入 DataStore；当前未使用数据库。
- 二维码登录轮询必须感知生命周期，应用进入后台时暂停轮询，回到前台后重新判断二维码状态。
- WebSocket、播放器和后台任务必须在对应生命周期中停止或降级，避免回前台后状态错乱。

验收：

- 播放时切到系统设置或主页，再回到 app 后不丢关键状态。
- App 后台被杀后重新进入，登录态和主要设置仍存在。
- 二维码登录页后台暂停后不会继续无意义轮询。

## B 站网络兼容规范

B 站 API 和播放链路对压缩格式、签名和请求头敏感，原生实现必须从第一版就保留兼容策略。

要求：

- OkHttp API client 必须支持 Brotli 响应，必要时引入 `okhttp-brotli`。
- 恢复直播后，直播 WebSocket 包体 Brotli 解码需要显式处理。
- WBI 签名使用纯 Kotlin 实现 MD5、参数过滤、排序和 URL encode。
- 不为了 WBI 签名引入原生加密库或 C/C++。
- WBI key 缓存策略应参考 Flutter 版行为，避免每次启动重复请求。
- TV 登录签名和 WBI 签名分别实现，不混用。
- 播放请求头与普通 API 请求头分开管理，播放走 `BiliMediaDataSourceFactory`。

验收：

- 推荐、搜索、空间投稿等 WBI API 能稳定返回。
- 遇到 `Content-Encoding: br` 时 JSON 解析不乱码。
- 播放分片请求不因缺少请求头/Cookie 出现 403。

## Baseline Profile 策略

Compose 项目冷启动和首屏性能受类加载、Compose 运行时和主路径编译状态影响。当前发布构建已包含保守 Baseline Profile；后续只有在主路径明显变化或实测启动性能回退时再更新。

策略：

- 保留当前 profile 覆盖启动、首页、搜索、设置和播放器主路径。
- 发布构建必须继续产出 `assets/dexopt/baseline.prof` 和 `assets/dexopt/baseline.profm`。
- UI 频繁微调时不反复生成无效 profile；等交互稳定后再补。
- 直播路径在恢复直播阶段再补。

目标路径：

- 冷启动到首页。
- 侧边栏切换到首页内容。
- 首页首屏海报渲染。
- 搜索进入结果页。
- 视频卡片进入播放器。
- 播放器控制层显示和隐藏。

## 从 Flutter 到原生的功能映射

现有 Flutter 区域迁移关系：

- `AuthService` -> `core/auth`
- `BilibiliApi` 和 API service 文件 -> `core/network` 和 repositories
- `SettingsService` -> `core/storage`，使用基于 DataStore 的设置仓库
- `LocalServer` 和 `MpdGenerator` -> 优先尝试 Media3 原生 DASH 处理，只在必要时保留本地 MPD 回退方案
- `PlaybackProgressCache` -> 首个发布版使用 DataStore；如果播放进度/历史需要查询、清理或同步，再迁移到 Room。
- `canvas_danmaku` 用法 -> 字节跳动 `DanmakuView` 原生叠加层
- `HomeScreen` 和标签页 -> `AppShell` / `AppDestination` 状态路由和 Lazy 布局
- `PlayerScreen` 和播放器组件 -> Media3 播放器宿主 + Compose 叠加层
- 直播 socket service -> 暂缓；恢复直播时使用 OkHttp WebSocket 服务

## 里程碑

### Phase 1：原生工程骨架

预计时间：1-2 天

交付物：

- 在 `BiliTVNative` 下新建 Gradle Android 项目。
- 引入 `libs.versions.toml` 管理依赖版本。
- 配置 Compose 和 TV Material。
- 预留 Baseline Profile 配置。
- 应用能在 Android TV 上启动。
- 基础启动页。
- 侧边栏壳：搜索、首页、动态、历史、用户/设置；直播暂缓，不放入首个发布版导航。
- D-pad 焦点高亮可用。
- 每个页面有空状态内容。

验收：

- debug 构建可安装并启动。
- 遥控器上/下键能在侧边栏移动。
- 返回键行为可退出应用。
- 依赖版本集中在 Version Catalog 中。

### Phase 2：核心 API 和 Session 层

预计时间：3-5 天

交付物：

- 带 B 站请求头和 Cookie 处理的 OkHttp client。
- 推荐、视频信息、播放地址、历史、搜索、动态、登录相关 JSON models；直播 models 后续恢复直播阶段再补。
- 二维码登录后端逻辑。
- 基于 DataStore 的 auth/session 持久化。
- 带基础错误处理的 Repository 封装。

验收：

- 能获取推荐列表。
- 能搜索视频。
- 能获取视频详情和播放地址。
- 能完成二维码登录，或至少能轮询二维码状态。
- Flutter 和原生 API 结果的关键字段一致。

### Phase 3：首页和 TV 导航

预计时间：4-7 天

交付物：

- 首页推荐页。
- 分区横向列表和视频卡片。
- 尺寸稳定的可聚焦 TV 卡片。
- LazyRow/LazyGrid 渲染。
- 通过 Coil 加载图片，并限制目标尺寸。
- 列表缩略图支持低内存解码策略。
- 侧边栏到内容区、内容区回侧边栏的焦点切换。
- 复杂网格使用确定性焦点路径，不依赖默认最近邻搜索。
- 基础视频卡片进入播放器。

验收：

- 首页可完全用遥控器操作。
- 长按 D-pad 不丢焦点。
- 侧边栏、内容区、弹窗返回焦点可预测。
- 目标 TV 硬件上滚动顺滑。
- 图片缓存不会无限增长。

### Phase 4：点播播放器

预计时间：7-10 天

交付物：

- Media3 ExoPlayer 集成。
- `BiliMediaDataSourceFactory` 集成。
- B 站 DASH 视频/音频播放。
- 播放请求支持 header 和 cookie。
- `TvPlaybackLoadControl` 初版缓冲策略。
- 编码偏好和回退。
- 画质列表和画质切换。
- 播放/暂停、seek、快进、快退。
- 控制层自动隐藏。
- 续播进度。
- 上报播放进度。
- 分 P 切换。

验收：

- H.264 播放正常。
- 设备支持时 H.265 播放正常。
- AV1 回退行为正确。
- 播放分片请求稳定携带 B 站所需请求头和 Cookie。
- DASH 音视频保持同步。
- 画质切换后能从接近原位置继续播放。
- 返回退出播放器后能释放内存。

### Phase 5：弹幕和快进预览

预计时间：5-8 天

交付物：

- 弹幕 XML 获取和解析。
- 弹幕排布和碰撞由字节跳动弹幕引擎处理，应用层负责数据转换和配置。
- 字节跳动 `DanmakuView` 原生叠加层。
- 弹幕设置：开关、透明度、字号、显示区域、速度、顶部/底部过滤。
- 弹幕和播放进度时间同步。
- Videoshot API 迁移。
- 雪碧图加载和滑动淘汰窗口。
- 快进预览 UI。

验收：

- seek 后弹幕仍能同步。
- 高弹幕量下 UI 没有明显卡顿。
- UI 线程不做 XML 解码、解析或额外重型排布计算。
- 快进预览不会一次性预加载所有雪碧图。
- 离开播放器后内存可释放。

### Phase 6：收尾与发布

预计时间：5-8 天

交付物：

- 发布构建配置。
- 按 ABI 过滤的 APK 任务。
- R8 和资源裁剪规则。
- Baseline Profile 配置和保守 profile。
- 图标、TV 横幅、语言切换和发布前稳定性修复。
- 直播播放暂缓，后续单独开启时再补分区页、房间列表、HLS/WebSocket 和直播弹幕。

验收：

- `armeabi-v7a` 发布 APK 可构建并安装到目标电视。
- `arm64-v8a` 发布 APK 保持可构建。
- 发布构建启动无 R8 反射崩溃。
- 长按 D-pad、首页/设置/播放器主路径保持可用。

### Phase 7：剩余页面和设置

预计时间：8-12 天

交付物：

- 搜索 UI 和搜索历史。
- 历史页面。
- 动态页面。
- 登录/用户页面。
- 设置页面。
- 播放设置。
- 界面设置。
- 缓存清理。
- 空降助手设置开关。

验收：

- 主流程达到足够替代日常使用的功能等价。
- 设置重启后仍能持久化。
- 缓存清理可用。
- 登录/退出不会破坏已存 session。

### Phase 8：构建、体积和性能收尾

预计时间：3-5 天

交付物：

- 发布构建配置。
- 按 ABI 过滤的 APK 任务。
- R8 和资源裁剪规则。
- 首个可用版本稳定后，如有必要加入 Baseline Profile。
- 与 Flutter 版本的性能对比报告。

验收目标：

- `armeabi-v7a` APK：目标 14-17 MB。
- `arm64-v8a` APK：暂不作为第一版主发包；保留构建能力，后续如遇到 64-bit-only 设备再发布。
- 首页空闲 PSS 至少比 Flutter 目标构建低 20 MB。
- 播放态 PSS 更低或接近，同时保持稳定。
- 目标 TV 上冷启动到首页可操作低于 2 秒。
- 长按 D-pad 时焦点移动稳定。

## 当前可用范围

当前原生版本已包含：

- 侧边栏、首页推荐/分区、搜索结果、动态、历史、设置和账号登录入口。
- TV 二维码登录、session 持久化、搜索历史、WBI key、播放进度、弹幕设置和应用设置。
- 点播 DASH 播放、画质/编码偏好、分 P、UP 主更多视频、相关推荐、播放完成动作、播放心跳和续播。
- 字节跳动 `DanmakuView` 弹幕叠加层、弹幕样式设置、快进预览雪碧图和空降助手。
- 三档视觉性能模式、四种主页主题、Android 13+ 液态玻璃控件、迷你进度条开关、关于展示面板和缓存清理。
- `armeabi-v7a` / `arm64-v8a` 发布构建能力、R8、资源裁剪和保守 Baseline Profile。

当前仍不包含：

- 直播播放、直播房间列表和直播弹幕。
- 独立插件中心。
- 应用内更新检查和更新流程。
- Room 数据库、Koin 依赖注入和 Compose Navigation。

## 测试计划

手工设备检查：

- 冷启动。
- 遥控器焦点导航。
- 侧边栏 tab 切换。
- 搜索。
- 视频播放。
- 画质切换。
- seek。
- 续播进度。
- 弹幕开关。
- 从播放器返回。
- 直播播放（当前暂缓，恢复直播阶段再测）。
- 登录态重启后保持。

命令行检查：

```powershell
.\gradlew.bat :app:assembleDebug
.\build-release.bat
# 默认分别输出 armeabi-v7a / arm64-v8a 到 ~/.gradle/bilitv-native-build/release-apks/
adb install -r $env:USERPROFILE\.gradle\bilitv-native-build\app\outputs\apk\debug\app-debug.apk
adb shell dumpsys meminfo com.kirin.bilitv
adb shell dumpsys gfxinfo com.kirin.bilitv
```

回归对比：

- Flutter 和原生版本使用同一个 BVID 对比。
- 对比视频 URL 选择、CID、画质列表、编码和播放请求头。
- 对比弹幕数量和时间点。
- 对比历史/进度上报行为。

## 发布策略

初期原生 app 包名定为：

```text
com.kirin.bilitv
```

Flutter 参考 app 继续保留在原项目中，用于行为对照和回退参考。

原生功能等价足够后：

1. 如有需要，加入 auth/session/settings 迁移逻辑。
2. 第一版主发 `armeabi-v7a` 发布包。
3. 保留 `arm64-v8a` 构建能力，遇到 64-bit-only 设备需求再发布。
4. 不做应用内更新检查。
5. 在原生播放稳定前，继续保留 Flutter app 作为回退参考。

## 风险清单

高风险：

- B 站 DASH 播放在不同 Android TV 设备上的兼容性。
- 播放请求头和 Cookie 行为正确性。
- 弹幕如果用太多 Compose 节点实现，会导致性能问题。
- 嵌套 LazyRows、网格和播放器面板中的 TV 焦点回归。

中风险：

- 二维码登录流程等价。
- 直播 WebSocket 包解析和心跳行为（后续恢复直播时处理）。
- 画质切换后没有 player 泄漏。
- 低内存设备上的图片缓存调优。

低风险：

- 静态设置页面。
- 基础搜索和历史页面。
- 应用壳和侧边栏 UI。

## 实现原则

- 先匹配 Flutter 行为，再改善架构。
- 播放器状态和页面状态隔离。
- 优先使用 Media3 原生能力，再考虑本地服务器回退方案。
- 从第一版就限制图片尺寸。
- D-pad 焦点必须确定性，不在复杂网格中依赖默认焦点搜索。
- 播放中避免大范围重组。
- 先测量，再优化包体或内存。
- 电视端优先级为流畅、美观、内存合理，功耗不作为主要约束。
- 不为了省电牺牲焦点动画、图片预取和播放器体验。
- 资源使用仍然必须有上限，避免缓存、弹幕和图片解码导致 UI 卡顿。

## NDK / so 引入原则

第一版默认不使用 C/C++ 或自定义 so。优先使用 Kotlin、Media3、OkHttp、Coil 和系统硬解能力完成重写。

不建议第一版引入 NDK 的原因：

- 增加 ABI 包体。
- 增加 Gradle 和 CMake 构建复杂度。
- JNI 边界会增加调试成本。
- 原生崩溃排查成本高于 Kotlin 崩溃。
- 大多数业务逻辑和 UI 性能瓶颈不需要 C/C++ 解决。

只有满足以下条件之一，才考虑引入 C/C++：

- 弹幕轨道分配、碰撞计算等纯计算热点在 Kotlin 优化后仍然明显卡顿。
- 需要复刻某些 native 签名、加密、压缩或二进制协议算法。
- 某个纯计算模块稳定占用过多 CPU，并且能被清晰隔离。
- 必须接入现成 native 库，例如特殊解码、特殊压缩或协议解析。

播放器不自研原生解码，优先交给 Media3 ExoPlayer 和系统硬解。弹幕第一版使用字节跳动 `danmaku-render-engine` 原生叠加层；如后续压测仍有瓶颈，再评估是否替换为自研 Canvas，或只把轨道分配、碰撞计算抽成原生 so。

## 已定决策

- 最终包名：`com.kirin.bilitv`。
- 最低 Android 版本：初步定为 minSdk 23；可以为覆盖更老电视评估 minSdk 21，但不能因此牺牲美观、流畅度和核心库选择。
- ABI 策略：第一版主发 `armeabi-v7a`；工程保留 `arm64-v8a` 构建能力，不把实现锁死在 32 位。
- 更新检查：第一版不做更新检查，也不做应用内更新流程。
- 插件策略：不做可扩展插件系统，不保留独立插件标签页；只保留空降助手，在设置页提供开关。
