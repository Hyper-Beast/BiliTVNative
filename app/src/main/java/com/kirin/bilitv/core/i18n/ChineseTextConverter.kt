package com.kirin.bilitv.core.i18n

import com.github.houbb.opencc4j.util.ZhHkConverterUtil
import com.github.houbb.opencc4j.util.ZhTwConverterUtil

fun interface ChineseTextConverter {
  fun convert(text: String): String
}

object ChineseTextConverters {
  val Simplified = ChineseTextConverter { text -> text }

  fun forVariant(variant: ChineseTextVariant): ChineseTextConverter {
    return when (variant) {
      ChineseTextVariant.Simplified -> Simplified
      ChineseTextVariant.HongKong -> ChineseTextConverter { text ->
        if (text.isBlank()) text else ZhHkConverterUtil.toTraditional(text)
      }
      ChineseTextVariant.Taiwan -> ChineseTextConverter { text ->
        if (text.isBlank()) text else ZhTwConverterUtil.toTraditional(text)
      }
    }
  }
}
