package com.gym.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gym.app.data.dao.ExerciseDao
import com.gym.app.data.dao.WorkoutPlanDao
import com.gym.app.data.dao.WorkoutSessionDao
import com.gym.app.data.entity.Exercise
import com.gym.app.data.entity.ExerciseRecord
import com.gym.app.data.entity.WorkoutPlan
import com.gym.app.data.entity.WorkoutPlanExercise
import com.gym.app.data.entity.WorkoutSession

@Database(
    entities = [
        Exercise::class,
        WorkoutPlan::class,
        WorkoutPlanExercise::class,
        WorkoutSession::class,
        ExerciseRecord::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        private const val DATABASE_NAME = "gym_database"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
