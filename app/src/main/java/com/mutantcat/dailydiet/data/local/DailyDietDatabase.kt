package com.mutantcat.dailydiet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FoodEntity::class,
        FoodLogEntity::class,
        ExerciseLogEntity::class,
        WeightLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class DailyDietDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    abstract fun foodLogDao(): FoodLogDao

    abstract fun exerciseLogDao(): ExerciseLogDao

    abstract fun weightLogDao(): WeightLogDao

    companion object {
        fun build(context: Context): DailyDietDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DailyDietDatabase::class.java,
                "daily_diet.db",
            ).fallbackToDestructiveMigration().build()
    }
}

