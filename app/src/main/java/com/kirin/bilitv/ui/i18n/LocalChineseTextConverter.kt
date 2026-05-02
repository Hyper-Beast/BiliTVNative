package com.kirin.bilitv.ui.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kirin.bilitv.core.i18n.ChineseTextConverter
import com.kirin.bilitv.core.i18n.ChineseTextConverters
import com.kirin.bilitv.core.i18n.ChineseTextVariant
import java.util.Locale

val LocalChineseTextConverter = staticCompositionLocalOf<ChineseTextConverter> {
  ChineseTextConverters.Simplified
}

@Composable
fun convertChineseText(text: String): String {
  return LocalChineseTextConverter.current.convert(text)
}

fun Context.localizedContext(variant: ChineseTextVariant): Context {
  val locale = when (variant) {
    ChineseTextVariant.Simplified -> Locale.SIMPLIFIED_CHINESE
    ChineseTextVariant.HongKong -> Locale("zh", "HK")
    ChineseTextVariant.Taiwan -> Locale.TRADITIONAL_CHINESE
  }
  val configuration = Configuration(resources.configuration)
  configuration.setLocale(locale)
  val localized = createConfigurationContext(configuration)
  return object : ContextWrapper(this) {
    override fun getResources(): Resources = localized.resources
  }
}
