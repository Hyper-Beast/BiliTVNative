package com.kirin.bilitv.core.model

import java.util.Calendar
import java.util.Locale
import kotlin.math.max

data class VideoCardRelativeText(
  val viewedSuffixFormat: String,
  val minutesAgoFormat: String,
  val hoursAgoFormat: String,
  val yesterday: String,
  val daysAgoFormat: String,
)

fun VideoSummary.durationText(): String {
  return duration.formatDurationSeconds()
}

fun VideoSummary.pubdateText(
  relativeText: VideoCardRelativeText,
  nowMillis: Long = System.currentTimeMillis(),
): String {
  return pubdate.formatRelativePublishTime(relativeText, nowMillis)
}

fun VideoSummary.viewAtText(
  relativeText: VideoCardRelativeText,
  nowMillis: Long = System.currentTimeMillis(),
): String {
  val viewedAt = viewAt.formatRelativePublishTime(relativeText, nowMillis)
  return if (viewedAt.isBlank()) "" else relativeText.viewedSuffixFormat.formatWithLocale(viewedAt)
}

fun VideoSummary.watchProgressText(completedText: String): String {
  return if (isWatchCompleted()) {
    completedText
  } else {
    val watchedSeconds = if (isCompletedHistoryPart()) {
      duration
    } else {
      max(progress, 0)
    }
    val watched = watchedSeconds.formatDurationSeconds()
    val total = duration.formatDurationSeconds()
    "$watched / $total"
  }
}

fun VideoSummary.watchProgressRatio(): Float {
  return when {
    isWatchCompleted() || isCompletedHistoryPart() -> 1f
    duration <= 0 || progress <= 0 -> 0f
    else -> (progress.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
  }
}

fun VideoSummary.isWatchCompleted(): Boolean {
  return isCompletedHistoryPart() && !hasFollowingHistoryEpisode()
}

fun VideoSummary.shouldAdvanceToNextHistoryEpisode(): Boolean {
  return isCompletedHistoryPart() && hasFollowingHistoryEpisode()
}

fun VideoSummary.isCompletedHistoryPart(): Boolean {
  return progress == ProgressUnset || (duration > 0 && progress >= duration - 1)
}

private fun VideoSummary.hasFollowingHistoryEpisode(): Boolean {
  return historyVideos > 1 && historyPage > 0 && historyPage < historyVideos
}

fun Int.formatDurationSeconds(): String {
  val safeSeconds = coerceAtLeast(0)
  val hours = safeSeconds / 3600
  val minutes = (safeSeconds % 3600) / 60
  val seconds = safeSeconds % 60
  return if (hours > 0) {
    "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
  } else {
    "%02d:%02d".format(Locale.US, minutes, seconds)
  }
}

private fun Long.formatRelativePublishTime(
  relativeText: VideoCardRelativeText,
  nowMillis: Long,
): String {
  if (this <= 0L) return ""

  val eventMillis = this * 1000L
  val diffMillis = (nowMillis - eventMillis).coerceAtLeast(0L)
  val minutes = diffMillis / MinuteMillis
  val hours = diffMillis / HourMillis
  val days = diffMillis / DayMillis

  return when {
    hours < 1 -> relativeText.minutesAgoFormat.formatWithLocale(minutes.coerceAtLeast(1))
    days < 1 -> relativeText.hoursAgoFormat.formatWithLocale(hours)
    days == 1L -> relativeText.yesterday
    days < RelativeDayLimit -> relativeText.daysAgoFormat.formatWithLocale(days)
    else -> formatCalendarDate(eventMillis, nowMillis)
  }
}

private fun String.formatWithLocale(vararg args: Any): String {
  return format(Locale.US, *args)
}

private fun formatCalendarDate(eventMillis: Long, nowMillis: Long): String {
  val eventCalendar = Calendar.getInstance().apply {
    timeInMillis = eventMillis
  }
  val nowCalendar = Calendar.getInstance().apply {
    timeInMillis = nowMillis
  }
  val eventYear = eventCalendar.get(Calendar.YEAR)
  val nowYear = nowCalendar.get(Calendar.YEAR)
  val month = eventCalendar.get(Calendar.MONTH) + 1
  val day = eventCalendar.get(Calendar.DAY_OF_MONTH)

  return if (eventYear == nowYear) {
    "%02d-%02d".format(Locale.US, month, day)
  } else {
    "%02d-%02d".format(Locale.US, eventYear % 100, month)
  }
}

private const val MinuteMillis = 60_000L
private const val HourMillis = 60L * MinuteMillis
private const val DayMillis = 24L * HourMillis
private const val RelativeDayLimit = 7L
