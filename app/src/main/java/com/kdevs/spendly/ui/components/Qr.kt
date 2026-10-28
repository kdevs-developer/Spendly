package com.kdevs.spendly.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Res

/** What a friend's QR code holds. */
fun friendQrPayload(code: String): String = "spendly:friend:" + code.filter { it.isLetterOrDigit() }.uppercase()

/** Pulls the 8-character friend code out of a scanned value. Accepts our QR text or a bare code. */
fun parseFriendCode(raw: String?): String? {
    val v = raw.orEmpty().trim().uppercase().removePrefix("SPENDLY:FRIEND:").filter { it.isLetterOrDigit() }
    return v.takeIf { it.length == 8 }
}

/** Black squares on white, the way scanners like it, in any app theme. */
fun qrImage(
    text: String,
    size: Int = 600,
): ImageBitmap {
    val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 0)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels =
        IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) Res.colorInt(R.color.qr_dark) else Res.colorInt(R.color.qr_light) }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}
