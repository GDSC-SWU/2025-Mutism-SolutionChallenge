package com.example.mutism.manager
import android.content.Context
import androidx.core.content.edit

class TagSelectionManager(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSelectedTags(tags: Set<String>) {
        prefs.edit { putStringSet(KEY_SELECTED_NOISE_TAGS, tags) }
    }

    fun loadSelectedTags(): Set<String> = prefs.getStringSet(KEY_SELECTED_NOISE_TAGS, emptySet()) ?: emptySet()

    companion object {
        private const val PREFS_NAME = "NoiseSelectPrefs"
        private const val KEY_SELECTED_NOISE_TAGS = "selected_noise_tags"
    }
}
