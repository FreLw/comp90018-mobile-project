package com.unimelb.losttreasures

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.unimelb.losttreasures.ui.LostTreasuresApp
import com.unimelb.losttreasures.ui.theme.LostTreasuresTheme

@Preview(
    name = "Main App",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun LostTreasuresAppPreview() {
    LostTreasuresTheme {
        LostTreasuresApp()
    }
}
