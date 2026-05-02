package com.kirin.bilitv.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliMotion
import com.kirin.bilitv.ui.theme.BiliTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ClockOverlay(
  modifier: Modifier = Modifier,
) {
  var clockText by remember { mutableStateOf(currentClockText()) }

  LaunchedEffect(Unit) {
    while (isActive) {
      clockText = currentClockText()
      delay(BiliMotion.PlayerClockUpdateMs)
    }
  }

  Text(
    text = clockText,
    color = BiliColors.TextPrimary,
    fontSize = BiliTypography.PlayerMeta,
    fontWeight = FontWeight.Bold,
    modifier = modifier,
  )
}

private fun currentClockText(): String {
  return SimpleDateFormat("HH:mm", Locale.US).format(Date())
}
