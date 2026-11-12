package com.mutantcat.dailydiet

import android.app.Application
import android.util.Log
import com.mutantcat.dailydiet.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyDietApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 提前在后台播种内置食物库，让首次进入饮食页时列表已经有数据。
        applicationScope.launch {
            runCatching { container.initialize() }
                .onFailure { Log.e(TAG, "Failed to initialise app container", it) }
        }
    }

    private companion object {
        const val TAG = "DailyDietApplication"
    }
}

