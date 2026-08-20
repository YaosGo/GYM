package com.gym.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        private const val DATABASE_NAME = "gym_database"

        /** v4 → v5：exercises 表新增 is_custom 列（自定义动作标记）。 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE exercises ADD COLUMN is_custom INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * v5 → v6：助力/负重重量语义。
         * - exercises.weight_mode：重量模式标记，并按精选清单自动标记已有动作
         * - workout_sessions.body_weight_kg：训练开始时的体重快照
         * - exercise_records.effective_weight：有效对抗重量（与 weight 一一对应）
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE exercises ADD COLUMN weight_mode INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN body_weight_kg REAL NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE exercise_records ADD COLUMN effective_weight TEXT NOT NULL DEFAULT '[]'"
                )
                db.execSQL(
                    "UPDATE exercises SET weight_mode = 1 WHERE name IN " +
                        sqlNameList(Exercise.ASSISTED_EXERCISE_NAMES)
                )
                db.execSQL(
                    "UPDATE exercises SET weight_mode = 2 WHERE name IN " +
                        sqlNameList(Exercise.WEIGHTED_EXERCISE_NAMES)
                )
            }

            private fun sqlNameList(names: Set<String>): String =
                names.joinToString(prefix = "(", postfix = ")") { "'$it'" }
        }

        /** v6 → v7：workout_plan_exercises 新增 target_weight 列（计划目标重量）。 */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_plan_exercises ADD COLUMN target_weight REAL NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
