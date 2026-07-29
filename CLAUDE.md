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
| 图表库 | Vico (Compose 原生) | 2026-07-29 |
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
