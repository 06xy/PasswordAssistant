package com.passwordassistant.app.ui

import android.view.WindowManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.passwordassistant.app.ui.screens.EntryEditScreen
import com.passwordassistant.app.ui.screens.GroupDetailScreen
import com.passwordassistant.app.ui.screens.GroupEditScreen
import com.passwordassistant.app.ui.screens.HomeScreen
import com.passwordassistant.app.ui.screens.LockScreen
import com.passwordassistant.app.ui.screens.SettingsScreen
import com.passwordassistant.app.ui.screens.SetupScreen
import com.passwordassistant.app.ui.screens.VaultViewModel
import com.passwordassistant.app.data.LockState
import com.passwordassistant.app.ui.theme.PasswordAssistantTheme

@Composable
fun PasswordAssistantApp(
    mainViewModel: MainViewModel = viewModel(),
    vaultViewModel: VaultViewModel = viewModel(),
) {
    val themeMode by mainViewModel.themeMode.collectAsState()
    val lockState by vaultViewModel.lockState.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(lockState) {
        if (lockState == LockState.Locked || lockState == LockState.NoPassword) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    PasswordAssistantTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(animationSpec = tween(260)) +
                        slideInHorizontally(animationSpec = tween(260)) { it / 8 }
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(260))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180)) +
                        slideOutHorizontally(animationSpec = tween(180)) { it / 8 }
                },
            ) {
                composable(Route.Home) {
                    HomeScreen(navController = navController)
                }
                composable(Route.Settings) {
                    SettingsScreen(
                        navController = navController,
                        vaultViewModel = vaultViewModel,
                    )
                }
                composable(
                    route = Route.GroupDetail,
                    arguments = listOf(navArgument(Route.ArgGroupId) { type = NavType.LongType }),
                ) { entry ->
                    GroupDetailScreen(
                        navController = navController,
                        groupId = entry.arguments?.getLong(Route.ArgGroupId) ?: 0L,
                    )
                }
                composable(
                    route = Route.GroupEdit,
                    arguments = listOf(navArgument(Route.ArgGroupId) { type = NavType.LongType }),
                ) { entry ->
                    GroupEditScreen(
                        navController = navController,
                        groupId = entry.arguments?.getLong(Route.ArgGroupId) ?: -1L,
                    )
                }
                composable(
                    route = Route.EntryEdit,
                    arguments = listOf(
                        navArgument(Route.ArgGroupId) { type = NavType.LongType },
                        navArgument(Route.ArgEntryId) { type = NavType.LongType },
                    ),
                ) { entry ->
                    EntryEditScreen(
                        navController = navController,
                        groupId = entry.arguments?.getLong(Route.ArgGroupId) ?: 0L,
                        entryId = entry.arguments?.getLong(Route.ArgEntryId) ?: -1L,
                    )
                }
            }

            when (lockState) {
                LockState.NoPassword -> SetupScreen(vaultViewModel = vaultViewModel)
                LockState.Locked -> LockScreen(vaultViewModel = vaultViewModel)
                LockState.Unlocked -> Unit
            }
        }
    }
}
