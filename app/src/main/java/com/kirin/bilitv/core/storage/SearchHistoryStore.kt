package com.kirin.bilitv.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class SearchHistoryStore(private val context: Context) {
  private val stringListSerializer = ListSerializer(String.serializer())

  val history: Flow<List<String>> = context.biliDataStore.data.map { preferences ->
    preferences[Keys.SearchHistory]
      ?.let(::decodeHistory)
      ?: emptyList()
  }

  suspend fun add(query: String) {
    val normalized = query.trim()
    if (normalized.isBlank()) return

    context.biliDataStore.edit { preferences ->
      val current = preferences[Keys.SearchHistory]
        ?.let(::decodeHistory)
        .orEmpty()
      val next = (listOf(normalized) + current.filterNot { item -> item == normalized })
        .take(MaxHistorySize)

      preferences[Keys.SearchHistory] = Json.encodeToString(stringListSerializer, next)
    }
  }

  suspend fun clear() {
    context.biliDataStore.edit { preferences ->
      preferences.remove(Keys.SearchHistory)
    }
  }

  private fun decodeHistory(raw: String): List<String> {
    return runCatching {
      Json.decodeFromString(stringListSerializer, raw)
    }.getOrDefault(emptyList())
  }

  private object Keys {
    val SearchHistory = stringPreferencesKey("search_history")
  }

  private companion object {
    const val MaxHistorySize = 10
  }
}
