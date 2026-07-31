package com.gym.app

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gym.app.ui.components.AnimationConfig
import com.gym.app.ui.components.BottomTab
import com.gym.app.ui.components.GymBottomNavBar
import com.gym.app.ui.components.LocaleStrings
import com.gym.app.ui.exercise.ExerciseDetailScreen
import com.gym.app.ui.exercise.ExerciseListScreen
import com.gym.app.ui.exercise.ExerciseListViewModel
import com.gym.app.ui.history.HistoryScreen
import com.gym.app.ui.history.HistoryViewModel
import com.gym.app.ui.history.SessionDetailScreen
import com.gym.app.ui.navigation.NavRoutes
import com.gym.app.ui.plan.CreatePlanScreen
import com.gym.app.ui.plan.CreatePlanViewModel
import com.gym.app.ui.plan.PlanDetailScreen
import com.gym.app.ui.plan.PlanDetailViewModel
import com.gym.app.ui.plan.PlanListScreen
import com.gym.app.ui.plan.PlanListViewModel
import com.gym.app.ui.settings.AppSettings
import com.gym.app.ui.settings.SettingsScreen
import com.gym.app.ui.theme.GymTheme
import com.gym.app.ui.workout.WorkoutScreen
import com.gym.app.ui.workout.WorkoutViewModel

private val DarkBg = Color(0xFF1A1A2E)
private val LightGray = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF9E9E9E)
private val WhiteColor = Color.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GymApplication
        setContent {
            GymTheme {
                MainScreen(
                    exerciseRepository = app.exerciseRepository,
                    workoutRepository = app.workoutRepository,
                    settings = AppSettings(this),
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    exerciseRepository: com.gym.app.data.repository.ExerciseRepository,
    workoutRepository: com.gym.app.data.repository.WorkoutRepository,
    settings: AppSettings,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 收集设置
    val weightUnit by settings.weightUnit.collectAsState(initial = "kg")

    // 当前底部 Tab（计划/历史/动作）
    var selectedTab by rememberSaveable { mutableStateOf(NavRoutes.PLAN_LIST) }

    val bottomTabs = listOf(
        BottomTab(LocaleStrings.tabPlan, NavRoutes.PLAN_LIST),
        BottomTab(LocaleStrings.tabHistory, NavRoutes.HISTORY_TAB),
        BottomTab(LocaleStrings.tabExercise, NavRoutes.EXERCISE_LIST),
    )

    // 判断当前是否在主 Tab 页面
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isMainTab = bottomTabs.any { it.route == currentRoute }

    // 主 Tab 页面按返回键 → 退出 App
    BackHandler(enabled = isMainTab) {
        (context as? Activity)?.finish()
    }

    // 共享状态：面板显示时通知导航栏收起
    val sheetShowing = remember { mutableStateOf(false) }

    val bottomNavHeight by animateDpAsState(
        targetValue = if (sheetShowing.value || !isMainTab) 0.dp else 64.dp,
        animationSpec = tween(300),
        label = "bottomNavH",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区（全屏）
        NavHost(
            navController = navController,
            startDestination = NavRoutes.PLAN_LIST,
            modifier = Modifier.fillMaxSize(),
        ) {
                // === 计划列表（主页）===
                composable(
                    route = NavRoutes.PLAN_LIST,
                    enterTransition = { fadeIn(tween(AnimationConfig.Page.tabFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { fadeOut(tween(AnimationConfig.Page.tabFadeMs, easing = AnimationConfig.Page.easing)) },
                ) {
                    val planListVM: PlanListViewModel = viewModel(
                        factory = PlanListViewModel.Factory(workoutRepository)
                    )
                    PlanListScreen(
                        viewModel = planListVM,
                        onCreatePlan = { navController.navigate(NavRoutes.CREATE_PLAN) },
                        onPlanClick = { navController.navigate(NavRoutes.workout(it)) },
                        onPlanView = { navController.navigate(NavRoutes.editPlan(it)) },
                    )
                }

                // === 计划详情/编辑 ===
                composable(
                    route = NavRoutes.EDIT_PLAN,
                    arguments = listOf(navArgument("planId") { type = NavType.LongType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                ) { entry ->
                    val planId = entry.arguments?.getLong("planId") ?: return@composable
                    val vm: PlanDetailViewModel = viewModel(
                        factory = PlanDetailViewModel.Factory(planId, workoutRepository, exerciseRepository)
                    )
                    PlanDetailScreen(vm, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
                }

                // === 创建计划 ===
                composable(
                    route = NavRoutes.CREATE_PLAN,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                ) {
                    val vm: CreatePlanViewModel = viewModel(
                        factory = CreatePlanViewModel.Factory(workoutRepository, exerciseRepository)
                    )
                    CreatePlanScreen(
                        viewModel = vm,
                        onBack = {
                            navController.navigate(NavRoutes.PLAN_LIST) {
                                popUpTo(NavRoutes.PLAN_LIST) { inclusive = true }
                            }
                        },
                        onSaved = {
                            navController.navigate(NavRoutes.PLAN_LIST) {
                                popUpTo(NavRoutes.PLAN_LIST) { inclusive = true }
                            }
                        },
                    )
                }

                // === 训练执行 ===
                composable(
                    route = NavRoutes.WORKOUT,
                    arguments = listOf(navArgument("planId") { type = NavType.LongType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                ) { entry ->
                    val planId = entry.arguments?.getLong("planId") ?: return@composable
                    val vm: WorkoutViewModel = viewModel(
                        factory = WorkoutViewModel.Factory(planId, workoutRepository, exerciseRepository)
                    )
                    WorkoutScreen(
                        viewModel = vm,
                        workoutRepository = workoutRepository,
                        exerciseRepository = exerciseRepository,
                        weightUnit = weightUnit,
                        onBack = { navController.popBackStack() },
                        onComplete = {
                            navController.navigate(NavRoutes.PLAN_LIST) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        sheetShowing = sheetShowing,
                    )
                }

                // === 历史 ===
                composable(
                    route = NavRoutes.HISTORY_TAB,
                    enterTransition = { fadeIn(tween(AnimationConfig.Page.tabFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { fadeOut(tween(AnimationConfig.Page.tabFadeMs, easing = AnimationConfig.Page.easing)) },
                ) {
                    val vm: HistoryViewModel = viewModel(
                        factory = HistoryViewModel.Factory(workoutRepository)
                    )
                    HistoryScreen(
                        viewModel = vm,
                        onSessionClick = { navController.navigate(NavRoutes.sessionDetail(it)) },
                        workoutRepository = workoutRepository,
                        exerciseRepository = exerciseRepository,
                        sheetShowing = sheetShowing,
                    )
                }

                // === 历史训练详情 ===
                composable(
                    route = NavRoutes.SESSION_DETAIL,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                ) { entry ->
                    val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
                    SessionDetailScreen(
                        sessionId = sessionId,
                        workoutRepository = workoutRepository,
                        exerciseRepository = exerciseRepository,
                        onBack = { navController.popBackStack() },
                    )
                }

                // === 动作库 ===
                composable(
                    route = NavRoutes.EXERCISE_LIST,
                    enterTransition = { fadeIn(tween(AnimationConfig.Page.tabFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { fadeOut(tween(AnimationConfig.Page.tabFadeMs, easing = AnimationConfig.Page.easing)) },
                ) {
                    val vm: ExerciseListViewModel = viewModel(
                        factory = ExerciseListViewModel.Factory(exerciseRepository)
                    )
                    ExerciseListScreen(
                        viewModel = vm,
                        onExerciseClick = { navController.navigate(NavRoutes.exerciseDetail(it)) },
                        exerciseRepository = exerciseRepository,
                        sheetShowing = sheetShowing,
                    )
                }

                // === 动作详情 ===
                composable(
                    route = NavRoutes.EXERCISE_DETAIL,
                    arguments = listOf(navArgument("exerciseId") { type = NavType.StringType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                ) { entry ->
                    val exerciseId = entry.arguments?.getString("exerciseId") ?: return@composable
                    ExerciseDetailScreen(
                        exerciseId = exerciseId,
                        repository = exerciseRepository,
                        onBack = { navController.popBackStack() },
                    )
                }

                // === 设置 ===
                composable(
                    route = NavRoutes.SETTINGS,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeIn(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(AnimationConfig.Page.pushSlideMs, easing = AnimationConfig.Page.easing)) + fadeOut(tween(AnimationConfig.Page.pushFadeMs, easing = AnimationConfig.Page.easing)) },
                ) {
                    SettingsScreen(settings = settings, scope = scope, onBack = { navController.popBackStack() })
                }
            }

        // 浮动导航栏（覆盖在内容上方）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomNavHeight)
                .clipToBounds()
        ) {
            GymBottomNavBar(
                tabs = bottomTabs,
                selectedRoute = selectedTab,
                onTabSelected = { route ->
                    selectedTab = route
                    navController.navigate(route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
            )
        }
    } // Box end
}
