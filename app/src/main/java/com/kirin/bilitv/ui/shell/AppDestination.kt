package com.kirin.bilitv.ui.shell

import androidx.annotation.DrawableRes
import com.kirin.bilitv.R

enum class AppDestination(
  val titleRes: Int,
  @DrawableRes val iconRes: Int,
) {
  Search(R.string.nav_search, R.drawable.ic_nav_search),
  Recommend(R.string.nav_recommend, R.drawable.ic_nav_home),
  Dynamic(R.string.nav_dynamic, R.drawable.ic_nav_dynamic),
  History(R.string.nav_history, R.drawable.ic_nav_history),
  Settings(R.string.nav_settings, R.drawable.ic_nav_settings),
}
