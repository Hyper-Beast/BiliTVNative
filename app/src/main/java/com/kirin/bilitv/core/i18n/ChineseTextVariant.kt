package com.kirin.bilitv.core.i18n

enum class ChineseTextVariant(val key: String) {
  Simplified("simplified"),
  HongKong("hong_kong"),
  Taiwan("taiwan");

  companion object {
    fun fromKey(key: String?): ChineseTextVariant {
      return entries.firstOrNull { variant -> variant.key == key } ?: Simplified
    }
  }
}
