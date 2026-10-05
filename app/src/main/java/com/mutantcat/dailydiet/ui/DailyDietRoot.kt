package com.mutantcat.dailydiet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mutantcat.dailydiet.di.AppContainer
import com.mutantcat.dailydiet.ui.exercise.ExerciseScreen
import com.mutantcat.dailydiet.ui.food.FoodScreen
import com.mutantcat.dailydiet.ui.goal.GoalScreen
import com.mutantcat.dailydiet.ui.profile.ProfileScreen
import com.mutantcat.dailydiet.ui.settings.SettingsScreen
import com.mutantcat.dailydiet.ui.today.TodayScreen

private object Routes {
    const val TODAY = "today"
    const val FOOD = "food"
    const val EXERCISE = "exercise"
    const val PROFILE = "profile"
    const val GOAL = "goal"
    const val SETTINGS = "settings"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.TODAY, "今天", Icons.Filled.Home),
    BottomNavItem(Routes.FOOD, "饮食", Icons.Filled.Restaurant),
    BottomNavItem(Routes.EXERCISE, "运动", Icons.Filled.FitnessCenter),
    BottomNavItem(Routes.PROFILE, "我的", Icons.Filled.Person),
)

@Composable
fun DailyDietRoot(container: AppContainer) {
    val viewModel: DailyDietViewModel = viewModel(
        factory = DailyDietViewModel.factory(container),
    )
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val configured by viewModel.profileConfigured.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val summary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val calorieTarget by viewModel.calorieTarget.collectAsStateWithLifecycle()
    val energyUnit by viewModel.energyUnit.collectAsStateWithLifecycle()
    val massUnit by viewModel.massUnit.collectAsStateWithLifecycle()
    val foodLogs by viewModel.todayFoodLogs.collectAsStateWithLifecycle()
    val exerciseLogs by viewModel.todayExerciseLogs.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        message?.let { text ->
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    if (configured == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = if (configured == true) Routes.TODAY else Routes.GOAL,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    summary = summary,
                    energyUnit = energyUnit,
                    foodLogs = foodLogs,
                    exerciseLogs = exerciseLogs,
                    onAddFood = { meal ->
                        viewModel.openFoodForMeal(meal)
                        navController.navigate(Routes.FOOD)
                    },
                    onAddExercise = { navController.navigate(Routes.EXERCISE) },
                    onDeleteFoodLog = viewModel::deleteFoodLog,
                    onDeleteExerciseLog = viewModel::deleteExerciseLog,
                )
            }

            composable(Routes.FOOD) {
                FoodScreen(
                    viewModel = viewModel,
                    onSaved = {
                        navController.navigate(Routes.TODAY) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable(Routes.EXERCISE) {
                ExerciseScreen(
                    weightKg = profile.currentWeightKg,
                    exerciseLogs = exerciseLogs,
                    onAdd = { preset, minutes ->
                        viewModel.addExercise(
                            name = preset.name,
                            category = preset.category,
                            met = preset.met,
                            minutes = minutes,
                        )
                    },
                    onDelete = viewModel::deleteExerciseLog,
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    profile = profile,
                    calorieTarget = calorieTarget,
                    energyUnit = energyUnit,
                    massUnit = massUnit,
                    weightLogs = weightLogs,
                    onEditGoal = { navController.navigate(Routes.GOAL) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onChangeEnergyUnit = viewModel::saveEnergyUnit,
                    onChangeMassUnit = viewModel::saveMassUnit,
                    onAddWeight = viewModel::addWeight,
                    onDeleteWeight = viewModel::deleteWeightLog,
                )
            }

            composable(Routes.GOAL) {
                GoalScreen(
                    initialProfile = profile,
                    showBack = configured == true,
                    onBack = { navController.popBackStack() },
                    onSave = { newProfile ->
                        viewModel.saveProfile(newProfile)
                        navController.navigate(Routes.TODAY) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = aiSettings,
                    onSave = { settings -> viewModel.saveAiSettings(settings) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

