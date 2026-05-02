package com.kirin.bilitv.core.auth

import java.security.MessageDigest

internal fun md5Hex(input: String): String {
  val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
  return digest.joinToString(separator = "") { byte ->
    "%02x".format(byte)
  }
}

