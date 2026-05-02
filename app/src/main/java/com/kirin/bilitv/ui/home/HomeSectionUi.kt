package com.kirin.bilitv.ui.home

import androidx.annotation.StringRes
import com.kirin.bilitv.R
import com.kirin.bilitv.core.model.HomeSection

@StringRes
fun HomeSection.titleRes(): Int {
  return when (this) {
    HomeSection.Recommend -> R.string.home_section_recommend
    HomeSection.Popular -> R.string.home_section_popular
    HomeSection.Anime -> R.string.home_section_anime
    HomeSection.Movie -> R.string.home_section_movie
    HomeSection.Game -> R.string.home_section_game
    HomeSection.Knowledge -> R.string.home_section_knowledge
    HomeSection.Tech -> R.string.home_section_tech
    HomeSection.Music -> R.string.home_section_music
    HomeSection.Dance -> R.string.home_section_dance
    HomeSection.Life -> R.string.home_section_life
    HomeSection.Food -> R.string.home_section_food
    HomeSection.Douga -> R.string.home_section_douga
  }
}
