package com.kirin.bilitv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TvGridNavigationEntryRowTest {
  @Test
  fun skipsTopRowWhenOnlySmallPartRemainsVisible() {
    val targetRow = resolveTvGridNavigationEntryRow(
      viewportStartOffset = 0,
      viewportEndOffset = 220,
      visibleRows = listOf(
        TvGridVisibleRow(index = 3, offset = -90, size = 100),
        TvGridVisibleRow(index = 4, offset = 20, size = 100),
      ),
    )

    assertEquals(4, targetRow)
  }

  @Test
  fun keepsFirstRowWhenItIsFullyVisible() {
    val targetRow = resolveTvGridNavigationEntryRow(
      viewportStartOffset = 0,
      viewportEndOffset = 220,
      visibleRows = listOf(
        TvGridVisibleRow(index = 3, offset = 0, size = 100),
        TvGridVisibleRow(index = 4, offset = 110, size = 100),
      ),
    )

    assertEquals(3, targetRow)
  }

  @Test
  fun usesMostVisibleRowWhenNoRowFitsCompletely() {
    val targetRow = resolveTvGridNavigationEntryRow(
      viewportStartOffset = 0,
      viewportEndOffset = 100,
      visibleRows = listOf(
        TvGridVisibleRow(index = 3, offset = -80, size = 120),
        TvGridVisibleRow(index = 4, offset = 40, size = 120),
      ),
    )

    assertEquals(4, targetRow)
  }
}
