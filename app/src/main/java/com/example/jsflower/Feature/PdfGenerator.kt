package com.example.jsflower.Feature

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.util.Log
import com.example.jsflower.Model.OrderDetails
import com.example.jsflower.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun createInvoicePDF(context: Context, order: OrderDetails): File? {
        return try {
            val pdfDocument = PrintedPdfDocument(
                context,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("res", "res", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
            )

            val page = pdfDocument.startPage(0)
            val canvas: Canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }

            var y = 40

            // Logo (centered)
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_)
            val scaledLogo = Bitmap.createScaledBitmap(logo, 100, 100, false)
            val logoX = (canvas.width - scaledLogo.width) / 2
            canvas.drawBitmap(scaledLogo, logoX.toFloat(), y.toFloat(), null)
            y += 120

            // Title
            paint.textSize = 18f
            paint.isFakeBoldText = true
            val title = "HÓA ĐƠN MUA HÀNG - JSFlower"
            canvas.drawText(title, centerX(canvas, paint, title), y.toFloat(), paint)
            paint.textSize = 14f
            paint.isFakeBoldText = false
            y += 30

            // Customer info
            canvas.drawText("Tên khách hàng: ${order.userName}", 40f, y.toFloat(), paint); y += 20
            canvas.drawText("SĐT: ${order.phoneNumber}", 40f, y.toFloat(), paint); y += 20
            canvas.drawText("Địa chỉ: ${order.address}", 40f, y.toFloat(), paint); y += 30

            // Timestamp
            val timeStamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Thời gian: $timeStamp", 40f, y.toFloat(), paint); y += 30

            // Items
            canvas.drawText("Sản phẩm đã mua:", 40f, y.toFloat(), paint); y += 20
            order.flowerNames?.forEachIndexed { index, name ->
                val qty = order.flowerQuantities?.getOrNull(index) ?: 1
                val price = order.flowerPrices?.getOrNull(index) ?: "0"
                canvas.drawText("- $name (x$qty): $price", 40f, y.toFloat(), paint)
                y += 20
            }

            y += 20
            paint.isFakeBoldText = true
            canvas.drawText("Tổng tiền: ${order.totalPrice} VND", 40f, y.toFloat(), paint)
            paint.isFakeBoldText = false
            y += 30

            val method = if (order.paymentReceived) "ZaloPay (đã thanh toán)" else "Thanh toán khi nhận hàng"
            canvas.drawText("Phương thức: $method", 40f, y.toFloat(), paint)
            y += 40

            // QR content (internal)
            val qrContent = "Thanh toan hoa don: ${order.flowerNames}\nSo tien: ${order.totalPrice} VND\n STK: 0325090532 (MB Bank)\n" +
                    "Cảm ơn quý khách đã ủng hộ <3"
            val qrBitmap = generateQRCodeBitmap(qrContent)
            val qrSize = 200
            val qrLeft = (canvas.width - qrSize) / 2
            val qrTop = canvas.height - qrSize - 60

            // QR label
            val qrLabel = "Quét mã để thanh toán"
            canvas.drawText(qrLabel, centerX(canvas, paint, qrLabel), (qrTop - 10).toFloat(), paint)

            // Draw QR
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, false),
                qrLeft.toFloat(),
                qrTop.toFloat(),
                null
            )

            pdfDocument.finishPage(page)

            // Save file
            val safeUserName = order.userName?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "KhachHang"
            val fileName = "HoaDon_${safeUserName}_${order.itemPushKey}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Log.d("PdfGenerator", "PDF saved: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("PdfGenerator", "PDF creation failed", e)
            null
        }
    }

    private fun generateQRCodeBitmap(content: String): Bitmap {
        val size = 512
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private fun centerX(canvas: Canvas, paint: Paint, text: String): Float {
        return (canvas.width - paint.measureText(text)) / 2
    }
}
