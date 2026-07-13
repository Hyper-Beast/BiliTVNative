package com.kirin.bilitv.core.player

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kirin.bilitv.core.storage.biliDataStore
import kotlinx.coroutines.flow.first

data class PlaybackProgress(
  val cid: Long,
  val positionMs: Long,
  val durationMs: Long,
  val updatedAtMs: Long,
)

class PlaybackProgressStore(private val context: Context) {
  suspend fun getProgress(bvid: String, cid: Long): PlaybackProgress? {
    val keyPrefix = keyPrefix(bvid, cid)
    val preferences = context.biliDataStore.data.first()
    val positionMs = preferences[longPreferencesKey("${keyPrefix}_position_ms")] ?: return null
    val durationMs = preferences[longPreferencesKey("${keyPrefix}_duration_ms")] ?: 0L
    val updatedAtMs = preferences[longPreferencesKey("${keyPrefix}_updated_at_ms")] ?: 0L
    return PlaybackProgress(
      cid = cid,
      positionMs = positionMs,
      durationMs = durationMs,
      updatedAtMs = updatedAtMs,
    )
  }

  suspend fun getLatestProgress(bvid: String): PlaybackProgress? {
    if (bvid.isBlank()) {
      return null
    }
    val keyPrefix = latestKeyPrefix(bvid)
    val preferences = context.biliDataStore.data.first()
    val cid = preferences[longPreferencesKey("${keyPrefix}_cid")]?.takeIf { it > 0L } ?: return null
    val positionMs = preferences[longPreferencesKey("${keyPrefix}_position_ms")] ?: return null
    val durationMs = preferences[longPreferencesKey("${keyPrefix}_duration_ms")] ?: 0L
    val updatedAtMs = preferences[longPreferencesKey("${keyPrefix}_updated_at_ms")] ?: 0L
    return PlaybackProgress(
      cid = cid,
      positionMs = positionMs,
      durationMs = durationMs,
      updatedAtMs = updatedAtMs,
    )
  }

  suspend fun saveProgress(
    bvid: String,
    cid: Long,
    positionMs: Long,
    durationMs: Long,
  ) {
    if (bvid.isBlank() || cid <= 0L || positionMs < 0L) {
      return
    }
    val keyPrefix = keyPrefix(bvid, cid)
    val latestKeyPrefix = latestKeyPrefix(bvid)
    val updatedAtMs = System.currentTimeMillis()
    context.biliDataStore.edit { preferences ->
      preferences[longPreferencesKey("${keyPrefix}_position_ms")] = positionMs
      preferences[longPreferencesKey("${keyPrefix}_duration_ms")] = durationMs.coerceAtLeast(0L)
      preferences[longPreferencesKey("${keyPrefix}_updated_at_ms")] = updatedAtMs
      preferences[longPreferencesKey("${latestKeyPrefix}_cid")] = cid
      preferences[longPreferencesKey("${latestKeyPrefix}_position_ms")] = positionMs
      preferences[longPreferencesKey("${latestKeyPrefix}_duration_ms")] = durationMs.coerceAtLeast(0L)
      preferences[longPreferencesKey("${latestKeyPrefix}_updated_at_ms")] = updatedAtMs

      val previousIndex = preferences[Keys.ProgressIndex]
        ?.let(::decodeProgressIndex)
        .orEmpty()
      val nextIndex = (previousIndex.filterNot { entry -> entry.keyPrefix == keyPrefix } +
        ProgressIndexEntry(
          keyPrefix = keyPrefix,
          latestKeyPrefix = latestKeyPrefix,
          updatedAtMs = updatedAtMs,
        ))
        .sortedBy { entry -> entry.updatedAtMs }
      val keptIndex = nextIndex.takeLast(MaxProgressEntries)
      val keptKeyPrefixes = keptIndex.mapTo(mutableSetOf()) { entry -> entry.keyPrefix }
      val keptLatestKeyPrefixes = keptIndex.mapTo(mutableSetOf()) { entry -> entry.latestKeyPrefix }

      nextIndex.asSequence()
        .filterNot { entry -> entry.keyPrefix in keptKeyPrefixes }
        .forEach { entry ->
          preferences.remove(longPreferencesKey("${entry.keyPrefix}_position_ms"))
          preferences.remove(longPreferencesKey("${entry.keyPrefix}_duration_ms"))
          preferences.remove(longPreferencesKey("${entry.keyPrefix}_updated_at_ms"))
          if (entry.latestKeyPrefix !in keptLatestKeyPrefixes) {
            preferences.remove(longPreferencesKey("${entry.latestKeyPrefix}_cid"))
            preferences.remove(longPreferencesKey("${entry.latestKeyPrefix}_position_ms"))
            preferences.remove(longPreferencesKey("${entry.latestKeyPrefix}_duration_ms"))
            preferences.remove(longPreferencesKey("${entry.latestKeyPrefix}_updated_at_ms"))
          }
        }
      preferences[Keys.ProgressIndex] = encodeProgressIndex(keptIndex)
    }
  }

  private data class ProgressIndexEntry(
    val keyPrefix: String,
    val latestKeyPrefix: String,
    val updatedAtMs: Long,
  )

  private fun decodeProgressIndex(raw: String): List<ProgressIndexEntry> {
    return raw.lineSequence()
      .mapNotNull { line ->
        val parts = line.split(IndexSeparator)
        if (parts.size != 3) return@mapNotNull null
        ProgressIndexEntry(
          keyPrefix = parts[0],
          latestKeyPrefix = parts[1],
          updatedAtMs = parts[2].toLongOrNull() ?: return@mapNotNull null,
        )
      }
      .toList()
  }

  private fun encodeProgressIndex(entries: List<ProgressIndexEntry>): String {
    return entries.joinToString("\n") { entry ->
      "${entry.keyPrefix}$IndexSeparator${entry.latestKeyPrefix}$IndexSeparator${entry.updatedAtMs}"
    }
  }

  private fun keyPrefix(bvid: String, cid: Long): String {
    return "playback_${bvid}_${cid}"
  }

  private fun latestKeyPrefix(bvid: String): String {
    return "playback_${bvid}_latest"
  }

  private object Keys {
    val ProgressIndex = stringPreferencesKey("playback_progress_index")
  }

  private companion object {
    const val MaxProgressEntries = 200
    const val IndexSeparator = "\t"
  }
}
