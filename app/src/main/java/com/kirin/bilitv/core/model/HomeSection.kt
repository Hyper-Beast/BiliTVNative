package com.kirin.bilitv.core.model

enum class HomeSection(
  val key: String,
  val regionTid: Int?,
) {
  Recommend("recommend", null),
  Popular("popular", null),
  Anime("anime", 13),
  Movie("movie", 181),
  Game("game", 4),
  Knowledge("knowledge", 36),
  Tech("tech", 188),
  Music("music", 3),
  Dance("dance", 129),
  Life("life", 160),
  Food("food", 211),
  Douga("douga", 1);

  companion object {
    val DefaultOrder = entries.toList()

    fun fromKey(key: String): HomeSection? {
      return entries.firstOrNull { section -> section.key == key }
    }
  }
}
