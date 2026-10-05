package com.mutantcat.dailydiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mutantcat.dailydiet.ui.DailyDietRoot
import com.mutantcat.dailydiet.ui.theme.DailyDietTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as DailyDietApplication).container
        setContent {
            DailyDietTheme {
                DailyDietRoot(container)
            }
        }
    }
}

