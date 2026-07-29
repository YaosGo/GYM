package com.gym.app

import android.app.Application
import com.gym.app.data.database.AppDatabase
import com.gym.app.data.importer.ExerciseImporter
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymApplication : Application() {

    /** 应用级别的数据库实例 */
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    /** 仓库层 */
    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao())
    }
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutPlanDao(), database.workoutSessionDao())
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 首次启动：导入 exercises.json 到 Room
        applicationScope.launch {
            ExerciseImporter.importIfNeeded(this@GymApplication, database.exerciseDao())
        }
    }

    companion object {
        lateinit var instance: GymApplication
            private set
    }
}
