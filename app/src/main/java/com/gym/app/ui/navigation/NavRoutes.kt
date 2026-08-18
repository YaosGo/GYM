package com.gym.app.ui.navigation

/**
 * 应用导航路由常量。
 */
object NavRoutes {
    const val EXERCISE_LIST = "exercise_list"
    const val PLAN_LIST = "plan_list"
    const val CREATE_PLAN = "create_plan"
    const val EDIT_PLAN = "edit_plan/{planId}"
    const val WORKOUT = "workout/{planId}"
    const val WORKOUT_COMPLETE = "workout_complete/{sessionId}"
    const val HISTORY_TAB = "history_tab"
    const val HISTORY = "history"
    const val SESSION_DETAIL = "session_detail/{sessionId}"
    const val SETTINGS = "settings"
    const val IMPORT_IMAGE = "import_image"

    fun editPlan(planId: Long) = "edit_plan/$planId"
    fun workout(planId: Long) = "workout/$planId"
    fun workoutComplete(sessionId: Long) = "workout_complete/$sessionId"
    fun sessionDetail(sessionId: Long) = "session_detail/$sessionId"
}
