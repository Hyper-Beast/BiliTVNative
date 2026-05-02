package com.kirin.bilitv.ui.login

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun createQrCodeBitmap(content: String, sizePx: Int): Bitmap {
  val hints = mapOf(
    EncodeHintType.CHARACTER_SET to "UTF-8",
    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    EncodeHintType.MARGIN to 1,
  )
  val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
  val pixels = IntArray(sizePx * sizePx)
  val foreground = Color.Black.toArgb()
  val background = Color.White.toArgb()

  for (y in 0 until sizePx) {
    for (x in 0 until sizePx) {
      pixels[y * sizePx + x] = if (matrix[x, y]) foreground else background
    }
  }

  return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { bitmap ->
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
  }
}
