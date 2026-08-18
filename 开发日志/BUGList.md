# BUG List

> 状态标记：🐛 待修复 | 🔧 修复中 | ✅ 已修复 | ❌ 无法复现

---

## 🐛 待修复

> 暂无

---

## 🔧 修复中

> 暂无

---

## ✅ 已修复

1. **系统返回键在 Tab 页行为错误** — `BackHandler(enabled = isMainTab)` 拦截 → `activity.finish()`
2. **训练完成界面按钮** — 历史记录中 `showHomeButton = false`；训练完成 `showHomeButton = true`，按钮点击 → PLAN_LIST
3. **日历绿点方向错误** — Compose Canvas y↓ 坐标系修正：`PI/2 - PI/6*i`（6 点钟方向逆时针排列）
4. **历史页面当天记录未自动加载** — `init` 中预选当天日期 + `loadSessionsForDate(today)`
5. **计划列表"还未训练"删除** — 移除 `lastDurationSec` 和 `formatDurationBrief`，名称单独一列上下居中
6. **训练完成页曲线图数据为空** — 每个动作的趋势曲线图未显示，trendPoints.size < 2 不渲染图表 → 修复数据过滤逻辑
7. **查看计划页点击动作** — 添加 `FrostedBottomSheet` + `ExerciseDetailSheetContent`，查看模式下点击动作卡片弹出详情
8. **训练完成页关闭后仍停留在训练页** — 关闭完成弹窗（遮罩点击/下滑/返回键）仅关闭面板，训练页残留且底部按钮回到"开始"态 → 统一 `exitWorkout`：关闭即导航回「我的计划」；`DisposableEffect` 离开训练页时恢复底部导航栏

---

## ❌ 无法复现

> 暂无
