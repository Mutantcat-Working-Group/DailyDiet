package com.mutantcat.dailydiet.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Insert
    suspend fun insert(food: FoodEntity): Long

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FoodEntity?

    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%' OR aliases LIKE '%' || :query || '%'
        ORDER BY
            CASE
                WHEN name = :query THEN 0
                WHEN name LIKE :query || '%' THEN 1
                ELSE 2
            END,
            name
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, limit: Int = 40): List<FoodEntity>

    @Query("SELECT * FROM foods ORDER BY id LIMIT :limit")
    suspend fun first(limit: Int = 40): List<FoodEntity>
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE loggedAt BETWEEN :start AND :end ORDER BY loggedAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<FoodLogEntity>>

    @Insert
    suspend fun insert(log: FoodLogEntity): Long

    @Delete
    suspend fun delete(log: FoodLogEntity)
}

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM exercise_logs WHERE loggedAt BETWEEN :start AND :end ORDER BY loggedAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<ExerciseLogEntity>>

    @Insert
    suspend fun insert(log: ExerciseLogEntity): Long

    @Delete
    suspend fun delete(log: ExerciseLogEntity)
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY loggedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 180): Flow<List<WeightLogEntity>>

    @Insert
    suspend fun insert(log: WeightLogEntity): Long

    @Delete
    suspend fun delete(log: WeightLogEntity)
}

