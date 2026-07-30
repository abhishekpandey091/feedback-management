package com.example.feedbackmanagement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrHelper {

    private fun generateBitmap(content: String, sizePx: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)

        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }
        return bitmap
    }

    fun show(context: Context, link: String, title: String, instructions: String) {
        try {
            val bitmap = generateBitmap(link, 700)

            val container = LinearLayout(context)
            container.orientation = LinearLayout.VERTICAL
            container.gravity = Gravity.CENTER_HORIZONTAL
            val pad = UiKit.dp(context, 24)
            container.setPadding(pad, UiKit.dp(context, 8), pad, pad)

            val qrSize = UiKit.dp(context, 220)
            val imageView = ImageView(context)
            imageView.layoutParams = LinearLayout.LayoutParams(qrSize, qrSize)
            imageView.setImageBitmap(bitmap)
            container.addView(imageView)

            val subtitle = TextView(context)
            subtitle.text = instructions
            subtitle.gravity = Gravity.CENTER
            subtitle.setTextAppearance(R.style.TextAppearance_Feedback_Body)
            val subtitleParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            subtitleParams.topMargin = UiKit.dp(context, 16)
            subtitle.layoutParams = subtitleParams
            container.addView(subtitle)

            AlertDialog.Builder(context)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("Close", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Could not generate QR: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
