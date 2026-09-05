package com.unimelb.losttreasures

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.unimelb.losttreasures.ui.LostTreasuresApp
import com.unimelb.losttreasures.ui.theme.LostTreasuresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Keep the Activity thin so screens can be changed and tested independently.
            LostTreasuresTheme {
                LostTreasuresApp()
            }
        }
    }
}
