package com.kirin.bilitv.core.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal fun JsonElement?.asObjectOrNull(): JsonObject? {
  return this as? JsonObject
}

internal fun JsonObject.string(name: String): String {
  return this[name]?.jsonPrimitive?.contentOrNull.orEmpty()
}

internal fun JsonObject.int(name: String): Int {
  return this[name]?.jsonPrimitive?.intOrNull ?: 0
}

internal fun JsonObject.long(name: String): Long {
  return this[name]?.jsonPrimitive?.longOrNull ?: 0L
}

internal fun JsonObject.boolean(name: String): Boolean {
  return this[name]?.jsonPrimitive?.booleanOrNull ?: false
}

internal fun JsonObject.obj(name: String): JsonObject? {
  return this[name]?.asObjectOrNull()
}

internal fun JsonElement.rootObject(): JsonObject {
  return jsonObject
}

internal fun JsonPrimitive.stringOrEmpty(): String {
  return contentOrNull.orEmpty()
}

