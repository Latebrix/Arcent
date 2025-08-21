package tech.arcent.home

import androidx.annotation.DrawableRes

data class Achievement(
    val title: String,
    val timestamp: String,
    val tags: List<String>,
    @DrawableRes val imageRes: Int
)

data class HomeUiState(
    val achievements: List<Achievement> = emptyList(),
    val totalWins: Int = achievements.size,
    val streakDays: Int = 7
)

internal fun sampleAchievements(): List<Achievement> = listOf(
    Achievement("Cooked a Healthy Meal", "Yesterday • 7:30 PM", listOf("Wellness", "Habit"), tech.arcent.R.drawable.ic_splash),
    Achievement("Studied 30 Minutes", "Tue • 8:00 PM", listOf("Learning", "Consistency"), tech.arcent.R.drawable.ic_splash),
    Achievement("Decluttered Workspace", "Mon • 6:15 PM", listOf("Productivity", "Home"), tech.arcent.R.drawable.ic_splash)
)
