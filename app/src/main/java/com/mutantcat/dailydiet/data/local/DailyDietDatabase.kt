package com.mutantcat.dailydiet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        FoodEntity::class,
        FoodLogEntity::class,
        ExerciseLogEntity::class,
        WeightLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
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
            )
                // 只允许 v1 -> v1（即没有 schema 变更）时重建；一旦版本号上调而忘记写迁移，
                // 直接抛异常而不是静默清空用户的饮食记录。新增字段时请在 MIGRATIONS 中补对应迁移。
                .addMigrations(*MIGRATIONS)
                .build()
    }
}

/**
 * 数据库迁移按版本号从小到大排列。
 *
 * 修改表结构时的步骤：
 * 1. 上调 @Database version；
 * 2. 在下方加一条 Migration(旧版本, 新版本)；
 * 3. 提交 app/schemas/ 下的 schema JSON 供 review。
 */
private val MIGRATIONS: Array<Migration> = emptyArray()

