package com.gym.app.ui.components

/**
 * 应用中文语言包。所有 UI 文字统一从此处获取，
 * 未来可通过 LocaleStrings.en 或 LocaleStrings.from(lang) 扩展多语言。
 */
object LocaleStrings {
    // 通用
    const val appName = "GYM 健身"
    const val save = "保存"
    const val cancel = "取消"
    const val delete = "删除"
    const val confirm = "确定"
    const val back = "返回"
    const val home = "首页"

    // 底部导航
    const val tabPlan = "计划"
    const val tabHistory = "历史"
    const val tabExercise = "动作"

    // 搜索
    const val searchHint = "搜索动作名称（支持模糊匹配）"
    const val searchAddHint = "搜索并添加动作..."
    const val noResults = "未找到匹配的动作"

    // 动作库
    const val exerciseTitle = "动作库"
    const val exerciseAll = "全部"

    // 动作详情
    const val detailTitle = "动作详情"
    const val instructions = "动作要点"
    const val videoTutorial = "视频教程"
    const val musclesWorked = "训练肌群"

    // 计划
    const val myPlan = "我的计划"
    const val createdByYou = "我创建的"
    const val addNewPlan = "添加新计划"
    const val createPlan = "创建计划"
    const val editPlan = "编辑计划"
    const val viewPlan = "查看"
    const val planNameHint = "例如：三分化-推日"
    const val planName = "计划名称"
    const val noPlans = "还没有健身计划"
    const val noPlansHint = "点击下方 + 创建你的第一个计划"

    // 编辑计划
    const val selectedExercises = "已选动作"
    const val setsLabel = "组"
    const val repsLabel = "次"
    const val addExercise = "添加动作"
    const val removeExercise = "删除动作"
    const val moveUp = "上移"
    const val moveDown = "下移"
    const val alreadyAdded = "已添加"

    // 训练
    const val workoutTitle = "训练"
    const val completedWeight = "完成重量"
    const val targetWeight = "目标重量"
    const val totalWorkout = "训练时长"
    const val trainingPlan = "训练计划"
    const val previous = "上次"
    const val bestSet = "最佳"
    const val target = "目标"
    const val addSet = "添加一组"
    const val finishWorkout = "完成训练"
    const val savingWorkout = "保存中..."
    const val cancelWorkout = "取消"

    // 训练完成
    const val workoutComplete = "训练完成"
    const val returnHome = "返回首页"
    const val duration = "时长"
    const val exerciseCount = "动作数"
    const val totalSets = "总组数"
    const val exerciseDetail = "动作详情"
    const val setNumber = "第{0}组"

    // 历史
    const val historyTitle = "历史记录"
    const val totalWorkouts = "总训练次数"
    const val thisMonth = "本月"
    const val selectDateHint = "选择日期查看训练记录"
    const val noWorkoutsOnDay = "当天没有训练记录"

    // 趋势/对比
    const val weight = "重量"
    const val reps = "次数"
    const val volume = "总容量"
    const val trendUp = "进步"
    const val trendDown = "退步"
    const val trendSame = "持平"
    const val firstTime = "首次训练"
    const val needMoreData = "完成更多训练后显示趋势"

    // 图片导入
    const val importImageTitle = "从图片导入"
    const val importSelectImage = "选择截图"
    const val importChangeImage = "更换图片"
    const val importEmptyDesc = "选择一张旧训练记录截图，自动识别动作与重量次数，创建计划并导入一次训练记录"
    const val importRecognizing = "正在识别…"
    const val importErrorTitle = "识别失败"
    const val importRetry = "重试"
    const val importDateLabel = "训练日期"
    const val importSummary = "识别出 {0} 个动作 · {1} 组"
    const val importUnmatchedTitle = "未匹配到动作库"
    const val importUnmatchedHint = "以下条目导入时将跳过，可点击删除"
    const val importUnmatchedBadge = "未匹配，导入时跳过"
    const val importButton = "导入训练记录"
    const val importSaving = "导入中…"
    const val importRawLines = "识别原文"
    const val importNoText = "没有识别到任何文字，请换一张更清晰的截图再试"
    const val importParsedNothing = "识别到文字但未解析出动作，展开「识别原文」查看"

    // 设置
    const val settingsTitle = "设置"
    const val language = "语言"
    const val weightUnit = "重量单位"
    const val bodyInfo = "身体数据"
    const val gender = "性别"
    const val height = "身高"
    const val weightInfo = "体重"
    const val comingSoon = "即将推出"
    const val unitKg = "公斤 (kg)"
    const val unitLb = "磅 (lb)"
    const val langZh = "中文"
    const val langEn = "English"

    // 部位
    val bodyParts: Map<String, String> = mapOf(
        "back" to "背部",
        "cardio" to "有氧",
        "chest" to "胸部",
        "lower arms" to "前臂",
        "lower legs" to "小腿",
        "neck" to "颈部",
        "shoulders" to "肩部",
        "upper arms" to "上臂",
        "upper legs" to "大腿",
        "waist" to "腰部",
    )

    fun bodyPartLabel(en: String): String = bodyParts[en] ?: en
}
