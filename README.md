# BiliTVNative

BiliTVNative 是一个面向 Android TV 的原生 B 站客户端实验项目，使用 Kotlin、Jetpack Compose、Compose for TV 和 Media3 迁移并重写原 Flutter 版 BiliTV 的主要体验。

项目目标不是单纯追求轻量，而是在电视设备上尽量平衡播放稳定性、遥控器焦点可控性、主页视觉质感，以及不同硬件档位下的流畅度。


#部分截图
<img width="2242" height="1216" alt="image" src="https://github.com/user-attachments/assets/2ffedb60-189c-48c7-b3e7-f84968a0ab97" />

<img width="2018" height="1180" alt="image" src="https://github.com/user-attachments/assets/f8a0330a-8fab-4cae-9590-c02aacd9d6a7" />

<img width="1012" height="552" alt="image" src="https://github.com/user-attachments/assets/1c1e0ce1-af22-408f-9716-fefa057cc30a" />

<img width="986" height="1012" alt="image" src="https://github.com/user-attachments/assets/d20d50b3-de60-4c06-9d70-680903175f7a" />

<img width="2156" height="1170" alt="image" src="https://github.com/user-attachments/assets/bba255c2-681b-45c4-81b8-a92484042104" />


## 当前状态

已接入的主要能力：

- 首页推荐、搜索、动态、历史
- 4 种主页主题：默认粉、深黑、高级灰、蓝灰
- 3 档视觉性能模式：流畅、均衡、精致
- TV 二维码登录
- Media3 点播播放
- 画质、编码、倍速、弹幕设置
- 字节跳动弹幕渲染引擎
- 快进预览雪碧图
- 空降助手
- 简体中文、香港繁体、台湾繁体
- Android TV launcher 图标和 TV 横幅

视觉性能模式说明：

- 流畅：面向低端电视，关闭重动画、流光、阴影、封面预取和图片内存缓存，缩略图使用较低尺寸与 RGB_565。
- 均衡：默认推荐档，保留主题色、假玻璃、轻缩放、边框、文字颜色过渡、封面轻提亮和平滑滚动。
- 精致：手动开启的高视觉档，增加更强玻璃氛围、环境高光、更高质量缩略图和主题色斜向流光。

主页主题只作用于主页、搜索、动态、历史、设置、侧边栏和标签栏。播放器继续使用独立稳定样式，避免主题化影响播放性能和兼容性。

直播暂缓，不在当前版本范围内。

## 维护说明

这个项目基本已经完成我自己的使用目标。

想到什么做什么如果你有什么需要的功能我没做，建议直接 fork 后自己改。

## 关于 AI 开发

本项目主要由 AI 辅助完成，根目录的文档就是为了让 AI 能继续接手上下文：

- `AGENTS.md`：开发约束和项目规则
- `DEVELOPMENT_PLAN.md`：产品、架构和技术路线
- `DEVELOPMENT_PROGRESS.md`：阶段进度和历史决策

如果你想加功能，推荐做法是 fork 项目，然后把这些文档连同需求一起交给 AI，让 AI 先读项目结构，再按小步提交的方式改。不要直接让 AI 一次性重写大模块。

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
