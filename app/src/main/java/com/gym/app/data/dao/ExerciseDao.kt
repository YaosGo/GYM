package com.gym.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.gym.app.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE body_part = :bodyPart ORDER BY name ASC")
    fun getExercisesByBodyPart(bodyPart: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    fun getExercisesByEquipment(equipment: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR name_zh LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<Exercise>>

    /**
     * 模糊搜索：将用户输入按空格/字符拆分，匹配名称中包含所有分词的动作。
     * 支持中文单字匹配，如搜索"卧推"会匹配"杠铃卧推"、"上斜哑铃卧推"等。
     */
    @RawQuery(observedEntities = [Exercise::class])
    fun fuzzySearchExercises(query: SupportSQLiteQuery): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): Exercise?

    @Query("SELECT DISTINCT body_part FROM exercises ORDER BY body_part ASC")
    fun getAllBodyParts(): Flow<List<String>>

    @Query("SELECT DISTINCT equipment FROM exercises ORDER BY equipment ASC")
    fun getAllEquipments(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int

    /**
     * 构建模糊搜索 SQL 语句。
     * 将搜索词按空格拆分为多个 token，每个 token 再拆分为单个字符，
     * 要求名称中同时包含所有 token 以及 token 中的所有字符。
     *
     * 示例：
     * - "卧推" → WHERE name LIKE '%卧%' AND name LIKE '%推%'
     * - "哑铃 胸" → WHERE name LIKE '%哑%' AND name LIKE '%铃%' AND name LIKE '%胸%'
     */
    companion object {
        fun buildFuzzySearchQuery(query: String): SupportSQLiteQuery {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                return SimpleSQLiteQuery("SELECT * FROM exercises ORDER BY name ASC")
            }

            val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }

            val conditions = mutableListOf<String>()
            for (token in tokens) {
                // 完整 token 匹配（英文单词 + 中文词）
                conditions.add("(name LIKE '%' || ? || '%' OR name_zh LIKE '%' || ? || '%')")
                // 单字符匹配（中文单字）
                if (token.length > 1) {
                    token.forEach { char ->
                        conditions.add("(name LIKE '%' || ? || '%' OR name_zh LIKE '%' || ? || '%')")
                    }
                }
            }

            val whereClause = conditions.joinToString(" AND ")
            val sql = "SELECT * FROM exercises WHERE $whereClause ORDER BY name ASC"

            val args = mutableListOf<Any>()
            for (token in tokens) {
                args.add(token) // name
                args.add(token) // name_zh
                if (token.length > 1) {
                    token.forEach { char ->
                        args.add(char.toString()) // name
                        args.add(char.toString()) // name_zh
                    }
                }
            }

            return SimpleSQLiteQuery(sql, args.toTypedArray())
        }
    }
}
