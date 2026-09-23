package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.AnalyzerScreen
import com.example.ui.screens.ChartsScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PhViewModel

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ANALYZE("Analyze", Icons.Filled.Science, Icons.Outlined.Science, "tab_analyze"),
    CHARTS("Charts", Icons.Filled.Palette, Icons.Outlined.Palette, "tab_charts"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "tab_history"),
    GUIDE("Guide", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "tab_guide")
}

class MainActivity : ComponentActivity() {
    private val viewModel: PhViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: PhViewModel) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = MainTab.values()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("main_navigation_bar")) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tabs[selectedTabIndex]) {
                MainTab.ANALYZE -> AnalyzerScreen(viewModel = viewModel)
                MainTab.CHARTS -> ChartsScreen(viewModel = viewModel)
                MainTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                MainTab.GUIDE -> GuideScreen()
            }
        }
    }
}
