package com.gym.app

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import com.gym.app.data.database.AppDatabase
import com.gym.app.data.importer.ExerciseImporter
import com.gym.app.data.repository.ExerciseRepository
import com.gym.app.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
                // coil-gif 2.6.0+ auto-registers its GifDecoder for API < 28
            }
            .memoryCache {
                MemoryCache.Builder(this@GymApplication)
                    .maxSizePercent(0.25)
                    .build()
            }
            .build()
    }

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
