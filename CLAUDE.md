# GYM 健身App 开发指引

## 项目概述
Android 日常健身记录 App，帮助用户创建健身计划、记录训练数据、追踪进步趋势。

## 开发规则

### 1. 需求确认
- 每次写代码前需要确定需求
- 如果有成熟方案需要和用户确认再实施
- 每天开发前需要查看「开发日志/待完成项.md」和「开发日志/BUGList.md」
- 和用户确认当天优先做什么之后才开始编码

### 2. 开发日志
- 每天开发都需要在「开发日志/开发日志.md」中记录
- 日志格式：日期、今日完成、遇到的问题、明日计划

### 3. 代码风格
- 使用 Google Style（Google Java Style Guide / Google Kotlin Style Guide）
- 命名规范遵循对应语言标准

### 4. 健身动作数据来源
- 参考 `exercises-dataset-main/data/exercises.json`
- 1324 个动作，含中文说明（zh）、GIF 动图、分步指导
- 数据结构参见 `exercises-dataset-main/data/exercises.schema.json`

### 5. 本地存储
- 所有数据存储在本地（Room 数据库 / DataStore）
- 包括：健身记录、健身计划、训练日期

## 已确认技术决策

| 决策项 | 选择 | 日期 |
|--------|------|------|
| 技术栈 | Kotlin + Jetpack Compose | 2026-07-29 |
| 最小 SDK | Android 8.0 (API 26) | 2026-07-29 |
| 编译 SDK | API 34 | 2026-07-29 |
| UI 框架 | Jetpack Compose + Material 3 | 2026-07-29 |
| 数据库 | Room | 2026-07-29 |
| 架构 | MVVM + Repository | 2026-07-29 |
| 图表库 | ~~Vico (Compose 原生)~~ → Canvas 手绘（Vico beta.2 API 不兼容已移除） | 2026-07-29 / 2026-07-30 |
| 图片加载 | Coil (Compose 原生) | 2026-07-29 |
| JSON 解析 | Moshi | 2026-07-29 |
| GIF 策略 | 打包到 assets/，首次启动导入 Room | 2026-07-29 |

## 已确认 (全部)

## 数据集资产统计

| 资产 | 数量 | 大小 | 状态 |
|------|------|------|------|
| exercises.json | 1 | 17.4 MB | ✅ 已导入 assets/ |
| images (JPG) | 1324 | 8.46 MB | ✅ 已拷贝 assets/images/ |
| videos (GIF) | 1324 | 122.78 MB | ✅ 已拷贝 assets/videos/ |
| **总计** | | **~148 MB** | APK 预计较大，用户已确认接受 |

## 编码规范 & 已知陷阱（2026-07-30 首次打包教训总结）

### 问题分类与根因

本次打包共修复 **12 类编译错误**，根因分为 5 大类：

#### A. 项目脚手架不完整
| 问题 | 表现 | 根因 |
|------|------|------|
| 缺少启动图标 | `mipmap/ic_launcher not found` | 项目未包含 adaptive icon 资源 |

#### B. 第三方库 API 版本不匹配（Vico 图表库）
| 问题 | 表现 | 根因 |
|------|------|------|
| API 不存在 | `rememberBottomAxis` / `rememberStartAxis` / `rememberCartesianChartModelProducer` / `lineSeries` 全部 unresolved | Vico `2.0.0-beta.2` 的 API 与后续 beta 完全不同（mangled names、无 DSL builder） |
| 传递依赖冲突 | Vico beta.5 → core-ktx 1.15.0 → 要求 compileSdk 35 | 升级 beta 版本引入新问题 |
| **最终方案** | **移除 Vico，使用 Compose Canvas 手绘折线图** | 避免第三方图表库的 API 不稳定问题 |

#### C. 网络/仓库访问受限
| 问题 | 表现 | 根因 |
|------|------|------|
| AGP 8.6.2 无法解析 | `plugin not found in Google / MavenRepo` | 网络无法访问完整的 Google Maven 仓库 |
| Gradle 下载超时 | `timeout (10000ms)` 从 services.gradle.org | 需手动下载 gradle-8.7-bin.zip 到 `~/.gradle/wrapper/dists/` |

#### D. 缺失 import（6 处）
| 问题 | 文件 | 根因 |
|------|------|------|
| `Button` 未导入 | `SessionDetailScreen.kt` | Material3 组件未被 IDE 自动 import |
| `ButtonDefaults` 未导入 | `SessionDetailScreen.kt`, `PlanDetailScreen.kt` | 同上 |
| `items` 未导入 | `CreatePlanScreen.kt` | 缺少 `import foundation.lazy.items`，编译器误解析到 `items(count: Int)` |
| `ExerciseListViewModel` 未导入 | `CreatePlanScreen.kt`, `PlanDetailScreen.kt` | 跨包引用无 import |

#### E. Kotlin/Compose 惯用法错误（8 处）
| 问题 | 影响文件 | 根因 |
|------|------|------|
| `StateFlow` 直接 `by` 委托 | 6 个 Screen 文件 | `StateFlow` 不是 Compose `State`，必须 `.collectAsState()` |
| 手动实现 `StateFlow` 接口 | `CreatePlanViewModel.kt`, `PlanDetailViewModel.kt` | `object : StateFlow<T>` + 重写 `collect` 在 Kotlin 2.0+ 下有返回类型不匹配 |
| `combine` 6 个 flow 超限 | 同上两个 ViewModel | `combine(f1,f2,f3,f4,f5,transform)` 最多 5 个，6个需用 `combine(listOf(...)) { values -> }` |

---

### 后续开发避免规则

#### 规则 1：第三方库使用前必须验证 API
- **引入新库或升级版本前**，先查该版本的 API 文档 / 源码，确认使用的函数确实存在
- 优先使用稳定版（非 beta/alpha），除非确实需要预览特性
- 如果库的 API 不稳定，优先考虑自己实现（如本次 Canvas 手绘替代 Vico）

#### 规则 2：Compose + StateFlow 固定模式
- ❌ `val state by viewModel.someStateFlow`
- ✅ `val state by viewModel.someStateFlow.collectAsState()`
- 每个使用 `StateFlow` 的 Screen 必须导入 `import androidx.compose.runtime.collectAsState`

#### 规则 3：组合 StateFlow 使用标准 API
- ❌ 手动 `object : StateFlow<T> { ... }`
- ✅ `combine(...).stateIn(viewModelScope, WhileSubscribed(5000), initialValue)`
- 超过 5 个 flow 组合时使用 `combine(listOf<Flow<Any?>>(...)) { values -> ... }`

#### 规则 4：Import 管理
- 使用 Material3 组件前确认 import 是否存在
- `LazyColumn` 的 `items` 有两个重载：`items(count: Int)` 和 `items(list: List<T>)`，必须显式 import `foundation.lazy.items`
- 跨包引用类必须 import

#### 规则 5：每次写完代码立即编译验证
- 每完成一个功能模块就 `Build → Make Project`，不要等全部写完再编译
- 网络受限环境下，优先保持依赖版本不变，避免因下载失败阻塞

#### 规则 6：资源文件完整性
- 新建 Android 项目必须包含 `mipmap-anydpi-v26/ic_launcher.xml` 等图标资源
- 使用 `@mipmap/ic_launcher` 前确认资源文件存在
