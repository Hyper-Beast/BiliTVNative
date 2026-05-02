package com.kirin.bilitv.core.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object BiliNumberParser {
  fun toInt(value: JsonElement?): Int {
    val primitive = value?.jsonPrimitive ?: return 0
    primitive.intOrNull?.let { return it }

    val text = primitive.contentOrNull.orEmpty()
    text.toIntOrNull()?.let { return it }

    return when {
      text.endsWith("\u4e07") -> ((text.dropLast(1).toDoubleOrNull() ?: 0.0) * 10_000).toInt()
      text.endsWith("\u4ebf") -> ((text.dropLast(1).toDoubleOrNull() ?: 0.0) * 100_000_000).toInt()
      else -> 0
    }
  }

  fun parseDuration(value: JsonElement?): Int {
    val primitive = value?.jsonPrimitive ?: return 0
    primitive.intOrNull?.let { return it }

    val parts = primitive.contentOrNull.orEmpty().split(":")
    return when (parts.size) {
      2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
      3 -> (parts[0].toIntOrNull() ?: 0) * 3600 +
        (parts[1].toIntOrNull() ?: 0) * 60 +
        (parts[2].toIntOrNull() ?: 0)
      else -> 0
    }
  }
}
