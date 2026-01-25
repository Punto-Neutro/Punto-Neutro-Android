package utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.sprint_2_kotlin.R

/**
 * Maps a category name from the database to a translatable string resource.
 *
 * @param categoryName The English category name from Supabase (e.g., "Technology").
 * @return The translated string from your string resources.
 */
@Composable
fun getTranslatedCategoryName(categoryName: String): String {
    val resourceId = when (categoryName.lowercase()) {
        "economics" -> R.string.category_economics
        "politics" -> R.string.category_politics
        "science" -> R.string.category_science
        "health" -> R.string.category_health
        "sports" -> R.string.category_sports
        "climate" -> R.string.category_climate
        "business" -> R.string.category_business
        "conflict" -> R.string.category_conflict
        // Add other categories here
        else -> -1 // Default case for unknown categories
    }

    // If a mapping is found, return the string resource. Otherwise, return the original name.
    return if (resourceId != -1) {
        stringResource(id = resourceId)
    } else {
        categoryName
    }
}

