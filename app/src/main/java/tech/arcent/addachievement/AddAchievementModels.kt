package tech.arcent.addachievement

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.Calendar

/* Utiliyt functions to provide default hour/minute compatible with minSdk 24 */
private fun defaultHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
private fun defaultMinute(): Int = Calendar.getInstance().get(Calendar.MINUTE)

/* UI state for creating a new achievement */
@Immutable
data class AddAchievementUiState(
    val title: String = "",
    val details: String = "",
    val selectedCategory: String? = null,
    val selectedTags: Set<String> = emptySet(),
    val userCategories: List<String> = emptyList(),
    val userTags: List<String> = emptyList(),
    val dateMillis: Long = System.currentTimeMillis(),
    val hour: Int = defaultHour(),
    val minute: Int = defaultMinute(),
    val imageUri: Uri? = null,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showTipsSheet: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val showAddTagDialog: Boolean = false,
    val isSaving: Boolean = false
)

/* events that chanag the Add Achievement state */
sealed interface AddAchievementEvent {
    data class TitleChanged(val value: String): AddAchievementEvent
    data class DetailsChanged(val value: String): AddAchievementEvent
    data class CategorySelected(val category: String): AddAchievementEvent
    data class TagToggled(val tag: String): AddAchievementEvent
    data class DateChanged(val millis: Long): AddAchievementEvent
    data class TimeChanged(val hour: Int, val minute: Int): AddAchievementEvent
    data class ImagePicked(val uri: Uri?): AddAchievementEvent
    data class AddCategory(val name: String): AddAchievementEvent
    data class AddTag(val name: String): AddAchievementEvent
    object ToggleDatePicker: AddAchievementEvent
    object ToggleTimePicker: AddAchievementEvent
    object ToggleTips: AddAchievementEvent
    object ToggleAddCategoryDialog: AddAchievementEvent
    object ToggleAddTagDialog: AddAchievementEvent
    object Save: AddAchievementEvent
    object Clear: AddAchievementEvent
}
