package br.com.cielotickets.feature.receipt.presentation

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Requisito opcional: QR Code do ingresso, vinculado à compra CONCLUÍDA
 * (só é chamado a partir de um PurchaseReceiptModel com status APPROVED —
 * ver ReceiptScreen).
 */
object QrCodeGenerator {
    fun generate(content: String, sizePx: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bmp
    }
}
