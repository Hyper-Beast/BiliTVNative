# BiliTVNative 开发进度

最后更新：2026-05-03

## 更新规则

- 每完成一个可验证任务，都要更新本文件。
- 状态只使用：`Done`、`In Progress`、`Pending`、`Blocked`。
- 新任务必须插入到合理阶段，不要只追加到末尾。
- 完成任务时补充验收结果，例如编译、安装、接口验证或用户确认。
- UI/UX 交互体验由用户手动测试，开发侧只负责编译、安装和必要的日志验证。

## 当前状态

当前阶段：真实二维码登录、首页、搜索、动态、历史、设置、点播播放器、字节跳动弹幕叠加层、空降助手、发布构建和 TV 图标/横幅均已接入；直播播放暂缓，后续单独评估。

推荐下一项：继续收尾 `P6` 体验和稳定性，优先处理首页/动态/历史切换卡顿、设置/焦点回归边界、文档一致性和本地化遗漏；Room 迁移仅在播放进度/历史需要结构化查询、清理或同步时再启动。

## P0 项目决策与规则

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P0-01 | 确定原生重写方案：Kotlin + Jetpack Compose + Android TV | Done | 已写入 `DEVELOPMENT_PLAN.md` |
| P0-02 | 确定包名 `com.kirin.bilitv` | Done | Gradle `applicationId` 已使用 |
| P0-03 | 确定首发 ABI 策略：主发 `armeabi-v7a`，保留 `arm64-v8a` 能力 | Done | `targetAbi` Gradle 参数已支持 |
| P0-04 | 确定不做应用内更新检查 | Done | 计划中已明确 |
| P0-05 | 确定不复刻完整插件系统，只保留内置空降助手开关 | Done | 计划中已明确 |
| P0-06 | 建立 `AGENTS.md` 开发约束 | Done | 包含 tokens、焦点、播放器、图片、低配置模式等规则 |
| P0-07 | 建立进度跟踪文件 | Done | 本文件 |

## P1 基础工程与首页/搜索

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P1-01 | 创建原生 Android 项目骨架 | Done | 单 app 模块，Gradle 可编译 |
| P1-02 | 接入 Gradle 版本目录 | Done | `gradle/libs.versions.toml` 已使用 |
| P1-03 | 接入 Compose、Material3、TV Material、DataStore、OkHttp、Coil、Brotli | Done | 依赖集中管理 |
| P1-04 | 建立 `AppContainer` | Done | Network、Repository、Storage 等核心对象集中创建 |
| P1-05 | 建立设计令牌 | Done | `BiliTokens.kt` 包含颜色、字号、间距、圆角、焦点、骨架屏常量 |
| P1-06 | 建立 D-pad 焦点基础组件 | Done | `BiliFocusableSurface` 支持粉色焦点框、低配置关闭动画 |
| P1-07 | 接入推荐、热门、分区接口 | Done | 首页可加载卡片 |
| P1-08 | 接入 WBI 签名与 Brotli | Done | 推荐和搜索 API 可用 |
| P1-09 | 实现首页分区标签 | Done | 支持分区切换和确认刷新 |
| P1-10 | 实现首页卡片网格 | Done | 16:9 封面、UP 主头像、固定底部作者行 |
| P1-11 | 修复首页焦点滚动与卡片完整显示 | Done | 选中卡片保持完整可见 |
| P1-12 | 实现首页分页加载 | Done | 接近末尾自动加载更多 |
| P1-13 | 实现搜索键盘 | Done | 参考 Flutter 版布局 |
| P1-14 | 实现搜索建议与搜索历史 | Done | 支持历史记录和清除历史 |
| P1-15 | 实现搜索结果列表 | Done | 支持卡片、分页、焦点回退 |
| P1-16 | 实现搜索排序 | Done | 综合排序、最多播放、最新发布、最多弹幕 |
| P1-17 | 修复搜索结果头像加载 | Done | 头像 URL 解析已处理 |
| P1-18 | 修复启动初始焦点 | Done | 启动后焦点进入首页首个卡片 |
| P1-19 | 调整侧边栏导航顺序 | Done | 搜索、主页、动态、历史、设置 |
| P1-20 | 添加动态入口图标 | Done | 使用风车样式图标 |
| P1-21 | 修复首页标签左键回导航栏焦点错误 | Done | 第一个标签按左回当前激活导航项 |

## P1.5 设置、低配置模式与图片治理

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P1.5-01 | 实现设置页基础布局 | Done | 性能、交互、首页分区 |
| P1.5-02 | 实现低配置模式开关 | Done | 开关使用 BiliPink，操作时不放大 |
| P1.5-03 | 实现切换时自动确认开关 | Done | 设置页交互区显示“切换时自动确认”；默认关闭；关闭后焦点移到导航栏或首页分区标签不会自动进入，首次访问或无内容时仍会自动进入加载 |
| P1.5-04 | 实现切换时自动刷新开关 | Done | 设置页交互区新增独立“切换时自动刷新”；依赖“切换时自动确认”，自动确认关闭时不可打开；关闭后自动切回已有内容会保留原内容，按确认键切换/确认时刷新；主页加载请求完成后清空触发器，避免重复消费旧请求，同时保留冷启动首次加载；`assembleDebug` 通过 |
| P1.5-05 | 实现首页分区显示开关 | Done | 至少保留一个分区 |
| P1.5-06 | 低配置模式关闭焦点动画和平滑滚动 | Done | 已接入 `AppPerformancePolicy` |
| P1.5-07 | 低配置模式降低封面和头像请求尺寸 | Done | 图片请求按策略传入尺寸 |
| P1.5-08 | 低配置模式减少骨架屏数量 | Done | 标准 12，低配置 8 |
| P1.5-09 | 低配置模式预留强制 H.264 | Done | 字段已存在，播放器阶段接入 |
| P1.5-10 | 封面使用 CDN 尺寸参数 | Done | `@widthw_heighth_1c.webp` |
| P1.5-11 | 头像使用 CDN 尺寸参数 | Done | 小头像不拉原图 |
| P1.5-12 | Coil 全局缓存限制 | Done | 全局内存上限 20%，磁盘 128MB；低配置模式通过请求级策略禁用图片内存缓存并清空已热缓存 |
| P1.5-13 | `RGB_565` 不再全局强制 | Done | 低配置策略控制 |
| P1.5-14 | 清理局部视觉硬编码 | Done | 搜索输入字号、设置列数、骨架屏参数移入 tokens |

## P2 账号与登录

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P2-00 | 独立登录入口到侧边栏顶部 | Done | 未登录显示账号图标，设置页账号卡片已移除 |
| P2-00A | 建立用户登录态结构 | Done | `UserSession` 支持 `sessData`、`biliJct`、`mid`、`face`、`uname`、`isVip` |
| P2-00B | 侧边栏头像和大会员角标结构 | Done | 登录后显示头像，大会员显示右下角“大”；登录后头像只展示不参与焦点 |
| P2-01 | 实现 TV 二维码生成 | Done | 使用 TV 登录接口生成 `auth_code`，ZXing 本地渲染二维码，`assembleDebug` 通过 |
| P2-02 | 实现二维码状态轮询 | Done | 每 2 秒轮询，使用 `repeatOnLifecycle(RESUMED)` 暂停后台轮询 |
| P2-03 | 处理二维码过期和刷新 | Done | 过期/失败显示刷新二维码按钮 |
| P2-04 | 登录成功保存 Cookie | Done | 从 `cookie_info.cookies` 保存 `SESSDATA`、`bili_jct` |
| P2-05 | 登录成功拉取用户信息 | Done | 调用 `/x/web-interface/nav` 保存头像、昵称、UID、VIP 状态；已补完整 Cookie 和登录后资料补刷 |
| P2-06 | 登录成功刷新侧边栏头像 | Done | `SessionStore.session` 驱动 Compose 自动刷新；Shell 全局补刷用户资料，头像使用限定尺寸请求、HTTPS 规范化和 B 站图片请求头 |
| P2-07 | 账号页显示登录态 | Done | 退出登录入口暂时移除，后续整体完成后再决定位置；`assembleDebug` 通过 |

## P3 登录态页面

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P3-00 | 补齐动态/历史接入前的视频卡片数据与显示规则 | Done | `VideoSummary` 已加入历史进度、观看时间、多 P、角标、直播标记等字段；`VideoCard` 支持普通、动态、历史模式；发布时间格式为分钟/小时/昨天/2-6 天前/同年 MM-DD/跨年 YY-MM；`assembleDebug` 通过，并使用外部 build 目录 APK 安装到 `127.0.0.1:16384` |
| P3-01 | 实现动态页接口 | Done | 使用 `/x/polymer/web-dynamic/v1/feed/all`，解析视频动态、offset 和 has_more；依赖登录 Cookie |
| P3-02 | 实现动态页卡片列表 | Done | 复用视频卡片与分页焦点逻辑，使用 `VideoCardMode.Dynamic`；接近末尾自动加载更多 |
| P3-03 | 实现历史页接口 | Done | 使用 `/x/web-interface/history/cursor`，解析 view_at/max 游标、观看进度、多 P、直播标记和角标 |
| P3-04 | 实现历史页卡片列表 | Done | 使用 `VideoCardMode.History` 显示已看时长/总时长、进度条、最后观看时间和多 P 提示 |
| P3-05 | 未登录页面提示与跳转 | Done | 动态/历史未登录时显示居中登录提示；暂不自动跳转账号入口，避免焦点链路不确定 |
| P3-06 | 视频卡片播放量图标 | Done | 封面左下角播放数前新增播放 icon；`assembleDebug` 通过并安装到 `127.0.0.1:16384` |

## P4 播放器

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P4-01 | 接入 Media3 ExoPlayer | Done | `PlayerScreen` 使用 Media3 `ExoPlayer` + `PlayerView`，保持默认 SurfaceView；`assembleDebug` 通过；`127.0.0.1:16384` 烟测进入播放器 |
| P4-02 | 实现播放地址获取 | Done | `PlaybackRepository` 接入 `/x/player/wbi/playurl`，支持 BVID/CID/quality/fnval；CID 缺失时通过 `/x/web-interface/view` 解析；烟测已创建音视频解码器 |
| P4-03 | 播放请求头/Cookie 封装 | Done | `BiliMediaDataSourceFactory` 使用 Media3 OkHttp DataSource 统一注入 User-Agent/Referer/Origin/Cookie；`assembleDebug` 通过，烟测未见 403/source error |
| P4-04 | 编码探测与 H.264 回退 | Done | `CodecCapabilityProbe` 探测 H.264/H.265/AV1；低配置模式强制 H.264 并调整 `fnval`；`127.0.0.1:16384` 烟测选择 HEVC 解码器 |
| P4-05 | 点击卡片直接播放 | Done | 首页、搜索、动态、历史卡片确认键直接进入播放器，未引入详情页前置 |
| P4-06 | 播放控制栏 | Done | 遥控器确认键播放/暂停，左右键 10 秒 seek，控制层自动隐藏；画质切换面板待后续细化 |
| P4-07 | 播放进度保存 | Done | `PlaybackProgressStore` 使用 DataStore 保存进度，播放器 `onPause` 和返回时保存 |
| P4-08 | 返回后恢复原卡片焦点 | Done | 退出播放器后按当前页面 FocusRequester 重试恢复：首页/搜索/动态/历史共用入口；真机焦点待手测 |
| P4-09 | 开发诊断面板 | Done | 开发诊断叠加层显示编码、分辨率、画质、缓冲、掉帧、host、time；不显示 Cookie/token |
| P4-10 | Flutter 播放器 UI 对照细化 | Done | 播放器改为顶部标题/UP/发布时间/播放量/时钟、底部大进度条/icon 控制行/画质弹幕状态、自动隐藏小进度条、右侧设置面板、画质/弹幕/倍速子面板和 seek 预览；`assembleDebug` 通过，已安装 `127.0.0.1:16384` 并烟测无 `FATAL EXCEPTION`/`ParserException`/source error |
| P4-11 | 播放器内容面板和状态数据 | Done | 剧集、UP 主视频和相关推荐面板已接入真实 API；移除点赞控制；在线人数使用 `/x/player/online/total`；画质状态追加 H.264/H.265/AV1；`assembleDebug` 通过 |
| P4-12 | UP 主面板排序与关注操作 | Done | UP 主面板默认最新投稿，新增最新/热门排序和关注/已关注操作；取消关注显示确认弹窗；打开前先解析元数据 owner mid，并忽略过期面板请求，降低首次空列表风险；画质状态现在格式化为 `高清 1080P(H.265)`；`assembleDebug` 通过 |
| P4-13 | UP 主面板加载和滚动打磨 | Done | UP 主面板现在匹配 Flutter 布局，头部使用一个排序切换按钮加关注按钮；重复打开时保留已缓存视频并刷新，WBI 失败时回退到未签名空间接口；列表只在焦点视频离开可见范围时滚动；`assembleDebug` 通过 |
| P4-14 | UP 主面板行元数据与 Cookie 补充 | Done | UP 主视频行隐藏重复 owner 名称，使用首页卡片播放图标显示播放数，头部焦点可在排序和关注按钮之间移动，空间投稿请求同时携带 `SESSDATA` 和 `bili_jct` Cookie；`assembleDebug` 通过 |
| P4-15 | 播放器面板焦点和行样式打磨 | Done | 播放默认隐藏控制层，控制栏默认进入剧集；UP 主/相关推荐视频面板在数据加载后聚焦第一个视频；UP 主列表过滤当前视频；视频列表滚动会露出被遮挡的焦点行；行播放数移到缩略图左下角；`assembleDebug` 通过 |
| P4-16 | 播放器按键处理和调试清理 | Done | 移除画质/倍速待确认提示，控制层激活改为 Menu 键，OK/Enter 切换播放/暂停，新增居中暂停指示，并移除播放诊断叠加层；`assembleDebug` 通过 |
| P4-17 | 播放器按键修正和 UP 主重试诊断 | Done | OK/Enter 仅在控制层隐藏时切换播放/暂停，控制层可见时激活当前聚焦控件/面板；Menu 只显示控制层；隐藏状态 seek 不再显示控制层；UP 主投稿请求在签名失败后刷新 WBI key 并重试，同时记录脱敏失败码；`assembleDebug` 通过 |
| P4-18 | 快进预览雪碧图和选中设置颜色 | Done | 画质/倍速当前行使用 BiliPink 显示选中文本；设置页新增快进预览雪碧图开关；播放器加载 `/x/player/videoshot` 和 pvdata，并在 seek 预览时裁剪雪碧图帧，关闭或不可用时回退到纯时间预览；`assembleDebug` 通过 |
| P4-19 | 快进预览雪碧图确认模式 | Done | 快进预览雪碧图默认开启，雪碧图模式等待 OK/Enter 后再 seek，Back 取消预览；雪碧图渲染改用 Canvas source-rect 裁剪，不再用超大偏移图片渲染，避免黑色预览帧；`assembleDebug` 通过 |
| P4-20 | 快进预览雪碧图加载路径 | Done | 雪碧图预览图片现在通过播放仓库下载并携带 Bilibili 请求头，在 `PlayerScreen` 解码为 `ImageBitmap` 后传入叠加层直接进行 Canvas 裁剪；缺失图片时回退到纯时间预览，不再无限加载；`assembleDebug` 通过 |
| P4-21 | 快进预览时间戳对齐 | Done | 原生快进预览模式现在对齐 Flutter 的 `getClosestTimestamp` 行为：左右预览目标在渲染前和 OK 确认 seek 前都会吸附到 pvdata 时间戳，减少预览图、显示时间和最终跳转目标不一致；`assembleDebug` 通过 |
| P4-22 | 播放和应用退出确认 | Done | 设置页新增默认开启的播放退出确认开关；开启后播放器 Back 显示 `再按一次返回键退出播放`，并且只在 3 秒内第二次 Back 时退出；应用级 Back 始终使用 `再按一次返回键退出APP` 双确认；`assembleDebug` 通过 |
| P4-23 | 播放页防息屏 | Done | 播放页通过多层兼容方式防止 TV 息屏：`FLAG_KEEP_SCREEN_ON`、根 View 和 `PlayerView.keepScreenOn`，以及在 pause/dispose 时释放的 `SCREEN_BRIGHT_WAKE_LOCK` 回退方案；manifest 声明 `WAKE_LOCK`；`assembleDebug` 通过 |
| P4-24 | 设置页溢出修复 | Done | 设置行为列改为由焦点驱动的 `LazyColumn`，新增播放开关后底部交互设置会滚入视图，不再被裁剪；`assembleDebug` 通过 |
| P4-25 | 设置页双列焦点返回 | Done | 设置页记住上次聚焦的行为开关，并给每个 lazy row 提供稳定 `FocusRequester`；从首页分区网格向左移动时会滚回并聚焦之前的设置行，不再落到被复用的顶部项；`assembleDebug` 通过 |
| P4-26 | 播放心跳和历史续播打磨 | Done | 在暂停、完成、退出、后台和切换当前视频时上报 `/x/click-interface/web/heartbeat`；完成时上报 `played_time=-1`；历史播放强制使用卡片 cid/progress，不再被本地缓存覆盖；历史卡片用粉色 `P1/P2` 角标显示多 P，用粉色 `已看完` 角标显示已完成；`assembleDebug` 通过 |
| P4-27 | 播放器顶部元数据图标 | Done | 播放器顶部元数据现在使用图标显示 owner、发布时间和播放数，不再使用纯分隔符字符串；`assembleDebug` 通过 |
| P4-28 | 播放编码偏好设置 | Done | 设置页新增解码器选项，默认 Auto，优先级为 AV1 > H.265 > H.264；手动选项只显示 `MediaCodecList` 报告的硬件加速编码；不支持的已保存选项回退到 Auto；播放 URL 请求和 DASH 轨道选择尊重 Auto/H.264/H.265/AV1 偏好，同时保留对受支持轨道的自动回退；低配置模式不再强制覆盖编码偏好；`assembleDebug` 通过，`192.168.1.131:5555` 探测结果为支持 H.264/H.265，不支持 AV1 |
| P4-29 | 默认播放画质设置 | Done | 设置页新增默认画质选项，包含最高/1080P/720P/480P，默认最高。播放 URL 请求优先使用播放器已选画质，仅在初始播放时回退到全局默认；播放器画质面板仍显示 Bilibili 返回的全部画质；`assembleDebug` 通过 |
| P4-30 | 播放完成动作 | Done | 设置页新增默认关闭的完成后自动播下一集、自动播相关推荐和自动退出播放。播放完成时会上报进度，显示可取消 toast 并标出下一个目标，然后执行已启用动作中优先级最高的一项；自动退出复用手动退出播放器路径，确保焦点回到原视频；AirJump 直接跳到结尾附近时会抑制已跳过 toast，避免与完成提示冲突；`assembleDebug` 通过 |
| P4-31 | 缓存清理操作 | Done | 设置页在性能区域新增清理缓存操作，并在行内显示当前磁盘/临时缓存大小。当前缓存策略为 Coil 内存缓存占可用内存 20%，图片磁盘缓存位于 `cacheDir/image_cache` 且上限 128MB；OkHttp 不使用磁盘缓存，推荐/播放器侧缓存仅在内存中。清理缓存会删除 Coil 图片缓存和 app cache 临时文件，同时保留登录、设置、搜索历史、WBI key 和播放进度；`assembleDebug` 通过 |
| P4-32 | 繁体中文语言支持 | Done | 新增 OpenCC4J 和简体/香港繁体/台湾繁体语言设置循环。静态字符串已有 `values-zh-rHK` 和 `values-zh-rTW`；动态标题、角标、弹幕文本、UP 主名称、分集标题、播放器侧边面板、账号名称和完成提示目标名称在显示时转换，请求和缓存 key 保留原文；`assembleDebug` 通过，并已在 `192.168.1.131:5555` 安装/启动 |

## P5 弹幕与空降助手

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P5-00 | 弹幕获取/渲染首版 | Done | 新增 `x/v1/dm/list.so?type=1` 和 `comment.bilibili.com/{cid}.xml` 回退路径，在 UI 线程外完成 gzip/zlib/raw-deflate XML 解码，补充播放器弹幕加载日志，打磨暂停/播放控制层可见性，并为弹幕数值设置加入适合 TV 的左右键调节；`assembleDebug` 通过 |
| P5-01 | 弹幕 XML 获取与解析 | Done | `PlaybackRepository.getDanmaku()` 使用 `x/v1/dm/list.so?type=1` 和 `comment.bilibili.com/{cid}.xml` 回退路径，gzip/zlib/raw-deflate 解码和 XML 解析放在 `Dispatchers.IO` |
| P5-02 | 弹幕排布与碰撞处理 | Done | 当前决策为使用字节跳动 `danmaku-render-engine`，轨道分配和碰撞由 `DanmakuView` 引擎处理；不再要求首版自研 Kotlin 轨道预计算 |
| P5-03 | 弹幕原生叠加层渲染 | Done | `PlayerDanmakuLayer` 通过 Compose `AndroidView` 承载字节跳动 `DanmakuView`，不把每条弹幕渲染为 Compose 节点；应用层不使用 `delay()` 驱动弹幕重绘 |
| P5-04 | 弹幕显示开关与样式设置 | Done | 弹幕开关、透明度、字号、占屏比、速度、顶部/底部悬停全部接入独立 DataStore 持久化；透明度按 0.1 调节，字号最小 16 且按 2 调节；`assembleDebug` 通过 |
| P5-05 | 空降助手内置开关 | Done | 设置页新增默认开启的空降助手开关，使用 AppSettings/DataStore 持久化；不做插件标签页；`assembleDebug` 通过 |
| P5-06 | 空降助手播放跳转逻辑 | Done | 播放器按 BVID 请求 `bsbsb.top/api/skipSegments` 的 sponsor/intro/outro/interaction/selfpromo 片段，进度条和迷你进度条用绿色标出跳过范围；跳过前 3.5 秒显示 `Toast.LENGTH_LONG` 的即将跳过提示，跳过后保持 `Toast.LENGTH_SHORT` 的已跳过提示；回退到片段前会重置触发状态；`assembleDebug` 通过，已安装并启动到 `192.168.1.131:5555` |

## P6 收尾与发布

| ID | 任务 | 状态 | 验收/备注 |
| --- | --- | --- | --- |
| P6-00 | 卡片焦点移动流畅度优化 | Done | 视频网格覆盖默认 TV pivot bring-into-view 策略，左右切换焦点时不再触发纵向支点滚动；上下切换焦点改为目标行稳定定位，避免第三行后焦点继续移动但画面不滚；视频卡片关闭焦点阴影动画，保留边框/底色反馈以降低重绘成本；视频行声明稳定 `contentType` 便于 LazyColumn 复用；`assembleDebug` 通过 |
| P6-00A | 设置页焦点和导航反馈修正 | Done | 设置页上下键改为显式计算目标设置项，目标项已完整可见时不滚动，贴边/半遮挡时只做最小像素补偿，并覆盖默认 TV pivot bring-into-view，避免每移动一项整列大幅跳动；侧边栏图标获得焦点立即变粉，降低导航切换慢一拍的体感；`assembleDebug` 通过，已安装 `192.168.1.195:5555` 并验证设置项上下移动 |
| P6-00B | 画质轨道与主页焦点恢复修正 | Done | playurl 返回后只向 ExoPlayer 暴露接口实际 `quality` 对应的视频轨道，避免 480P 设置被高分辨率轨道覆盖；播放日志输出 requested/returned qn 和实际轨道分辨率；侧栏右键进入首页/动态/历史时读取 `requestFocus()` 真实返回值，失败则触发网格滚动恢复焦点，修复低配置模式切回主页后必须确认刷新才能进内容；封面预取降为标准 12/低配 6，降低导航切页时的解码压力；`assembleDebug` 通过，已安装 `192.168.1.195:5555` 并验证侧栏右键进首页内容 |
| P6-00C | 卡片聚焦特效恢复 | Done | 视频卡片标准模式恢复聚焦 `scale` 放大；低配置模式通过 `motionEnabled=false` 关闭卡片放大和标题跑马灯，保留无动画边框/底色焦点反馈；焦点阴影仍关闭以避免额外合成负担；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` |
| P6-00D | 卡片轻量焦点视觉增强 | Done | 标准模式下新增封面轻微提亮、标题颜色过渡、UP 主/日期元信息颜色过渡；边框颜色/粗细继续复用 `BiliFocusableSurface` 的轻量动画；低配置模式关闭这些动画效果，仅保留静态焦点可见性；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` |
| P6-00E | 首页标签栏与时钟避让 | Done | 首页分类栏改为轻量文字标签/选中小胶囊样式，移除整条深色胶囊背景；右侧预留 `176dp` 时钟避让区，保持时钟位置不动；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` 并截图确认顶部不冲突 |
| P6-00F | 启动图标与繁体切换崩溃修复 | Done | 复用 Flutter 版 `BiliTV` 的启动器图标和 TV 横幅资源；manifest 声明 `android:icon` 和 application/activity `android:banner`，`aapt dump badging` 确认手机图标与 leanback 横幅均存在；发布构建 R8 保留 OpenCC4J 反射构造器，修复切换香港/台湾繁体后 `FastForwardSegment` 初始化崩溃；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已保留数据安装到 `192.168.1.195:5555` 并启动无 BiliTV 崩溃 |
| P6-01 | Baseline Profile 配置 | Done | 新增保守 baseline profile；发布构建已产出 `assets/dexopt/baseline.prof` 和 `assets/dexopt/baseline.profm` |
| P6-02 | R8/资源裁剪检查 | Done | `assembleRelease -PtargetAbi=armeabi-v7a` 通过；发布构建已执行 R8 代码压缩和资源裁剪，输出 `mapping.txt`、`usage.txt`、`resources.txt`、`seeds.txt`、`configuration.txt` |
| P6-03 | v7a 包体检查 | Done | 电视 `192.168.1.195:5555` ABI 为 `armeabi-v7a,armeabi`；当前 v7a 发布 APK `5,719,171` bytes，仅包含 `lib/armeabi-v7a/*` native 库 |
| P6-04 | 内存检查 | Done | 电视首页空闲 PSS 约 40.7 MB；导航/焦点操作后 PSS 约 55.0 MB，Native heap 约 20.1 MB，Java heap 约 12.7 MB |
| P6-05 | 帧耗时检查 | Done | 电视端焦点导航后 `dumpsys gfxinfo`：20721 frames，jank 123（0.59%），P50 9ms，P90 10ms，P95 11ms，P99 14ms，GPU memory 约 26 MB |
| P6-06 | 真机/模拟器安装验证 | Done | v7a 发布包已安装并启动到电视 `192.168.1.195:5555` 和模拟器 `192.168.1.131:5555`；本轮文档/字符串清理后重新通过 `assembleRelease -PtargetAbi=armeabi-v7a`，如需重新真机手测再安装最新 APK |
| P6-07 | 清理临时代码和调试页面 | Done | 删除未使用的 `NetworkProbeScreen` 和 `network_probe_*`、`home_shell_title` 字符串；`rg` 确认无 `NetworkProbeScreen`、`network_probe`、`home_shell_title`、`ui.debug` 残留引用 |
| P6-08 | 文档一致性与视频卡片本地化清理 | Done | `AGENTS.md`/`DEVELOPMENT_PLAN.md`/本文已记录字节跳动弹幕引擎、直播暂缓和 DataStore/Room 取舍；视频卡片相对时间、历史“看过”和播放/弹幕数量单位已移入 strings 资源；`assembleRelease -PtargetAbi=armeabi-v7a` 通过 |
| P6-09 | 内存和缓存收口复查 | Done | 推荐、动态、历史和搜索结果恢复按接口无限分页，不再用高低配置条数上限提前停止；封面预取记录随列表和图片尺寸重置；UP 主投稿缓存限制为最近 4 个 key、每个 50 条；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555`，图片列表实测 PSS 约 44 MB、CPU 0%、jank 0.73%、P99 13ms |
| P6-10 | 高低配置策略分层 | Done | 标准模式启用卡片放大、阴影、封面提亮、焦点封面模糊、标题/元信息颜色动画、平滑滚动、16 张封面预取和图片内存缓存；低配置模式关闭这些动画/模糊/阴影/平滑滚动/封面预取，封面 320x180 RGB_565，头像低尺寸 RGB_565，请求级禁用图片内存缓存并切换时清空 Coil 内存缓存；列表分页不按模式限条，避免影响连续浏览体验；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` |
| P6-11 | 导航栏进入网格焦点一致性 | Done | `TvVideoGrid` 将导航入口焦点和播放返回恢复焦点拆分：侧边栏右键进入首页/动态/历史时请求第一个卡片；播放返回或显式内容恢复仍请求上次记录的卡片；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` |
| P6-12 | 首页/搜索标签栏紧凑化 | Done | 首页分类和搜索排序同步改为轻量文字标签，选中项改为粉色文字且无背景色；遥控焦点落到标签时使用与导航栏/卡片一致的粉色边框反馈；视频卡片未改动；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` 并启动无崩溃 |
| P6-13 | 低配置播放策略与首次启动内存检测 | Done | 低配置模式下播放器有效解码偏好强制为 H.264，设置页解码器显示同步为 H.264，并新增 playurl codec 日志输出 requested/effective/fnval；首次读取设置时若设备总内存低于 1GB 且用户未手动设置过低配置开关，则默认启用低配置模式；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` 并启动无崩溃，电视 `MemTotal` 约 2.26GB 因此不会自动默认低配 |
| P6-14 | 网络权限兼容性补齐 | Done | Manifest 原本已声明 `android.permission.INTERNET`；为兼容国产系统和连通性判断补充 `android.permission.ACCESS_NETWORK_STATE`；`aapt2 dump permissions` 和电视 `dumpsys package` 均确认 `INTERNET`、`ACCESS_NETWORK_STATE`、`WAKE_LOCK` 存在且已授予；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` 并启动无崩溃 |
| P6-15 | 发布包体编译压缩 | Done | 发布构建增加 `androidResources.localeFilters`，仅保留默认、简中、香港繁中和台湾繁中资源，并排除依赖嵌套 LICENSE 文本；APK 从 `6,186,139` bytes 降至 `5,719,171` bytes，主要减少 `resources.arsc`；`assembleRelease -PtargetAbi=armeabi-v7a` 通过，已安装 `192.168.1.195:5555` 并启动无崩溃 |
| P6-16 | 根目录文档英文残留中文化 | Done | 已将 `AGENTS.md`、`DEVELOPMENT_PLAN.md`、`DEVELOPMENT_PROGRESS.md` 中面向读者的英文说明翻译为中文；保留命令、API、类名、构建类型和状态值；`rg` 检查后剩余英文主要为技术标识 |
| P6-17 | GitHub 上传前无用文件清理 | Done | 删除本机生成目录 `.gradle/`、`.kotlin/`、`build/`、`app/build/` 和本机 SDK 配置 `local.properties`；补充 `.gitignore` 忽略 `.kotlin/`、NDK/外部构建目录和 APK/AAB 产物；删除 0 引用资源 `ic_player_like.xml`、`ic_banner.png`；`rg --files --hidden` 确认剩余文件为源码、资源、Gradle wrapper 和文档 |
