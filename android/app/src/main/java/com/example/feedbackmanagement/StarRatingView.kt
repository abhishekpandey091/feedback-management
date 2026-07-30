package com.example.feedbackmanagement

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat


class StarRatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var maxStars: Int = 5
        set(value) {
            field = if (value > 0) value else 5
            rebuild()
        }

    var rating: Int = 0
        set(value) {
            field = value.coerceIn(0, maxStars)
            refreshIcons()
        }

    var onRatingChanged: ((Int) -> Unit)? = null

    private val starViews = mutableListOf<ImageView>()

    init {
        orientation = HORIZONTAL
        rebuild()
    }

    private fun rebuild() {
        removeAllViews()
        starViews.clear()

        val starSizeMax =
            resources.getDimensionPixelSize(R.dimen.star_size_max)

        for (i in 0 until maxStars) {
            val star = ImageView(context)

            star.adjustViewBounds = true
            star.scaleType = ImageView.ScaleType.FIT_CENTER
            star.setPadding(4, 4, 4, 4)
            star.contentDescription = "Star ${i + 1} of $maxStars"

            val params = LayoutParams(0, starSizeMax, 1f)
            star.layoutParams = params

            val index = i
            star.setOnClickListener {
                rating = index + 1
                onRatingChanged?.invoke(rating)
            }

            addView(star)
            starViews.add(star)
        }

        refreshIcons()
    }

    private fun refreshIcons() {
        val filled = ContextCompat.getDrawable(context, R.drawable.ic_star_filled)
        val outline = ContextCompat.getDrawable(context, R.drawable.ic_star_outline)

        val filledColor = ContextCompat.getColor(context, R.color.star_filled)
        val emptyColor = ContextCompat.getColor(context, R.color.star_empty)

        starViews.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageDrawable(filled)
                star.setColorFilter(filledColor)
            } else {
                star.setImageDrawable(outline)
                star.setColorFilter(emptyColor)
            }
        }
    }
}
