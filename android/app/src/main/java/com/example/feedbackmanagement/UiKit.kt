package com.example.feedbackmanagement

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip


object UiKit {

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun card(context: Context, marginBottomDp: Int = 16): Pair<MaterialCardView, LinearLayout> {
        val card = MaterialCardView(context)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = dp(context, marginBottomDp)
        card.layoutParams = params
        card.radius = dp(context, 16).toFloat()
        card.cardElevation = dp(context, 1).toFloat()
        card.strokeWidth = dp(context, 1)
        card.setStrokeColor(ContextCompat.getColor(context, R.color.neutral_outline))
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.neutral_surface))
        card.useCompatPadding = false

        val inner = LinearLayout(context)
        inner.orientation = LinearLayout.VERTICAL
        val pad = dp(context, 18)
        inner.setPadding(pad, pad, pad, pad)
        card.addView(inner)

        return Pair(card, inner)
    }

    fun screenTitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextAppearance(R.style.TextAppearance_Feedback_ScreenTitle)
    }

    fun sectionTitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextAppearance(R.style.TextAppearance_Feedback_SectionTitle)
        val topPad = dp(context, 20)
        setPadding(0, topPad, 0, dp(context, 8))
    }

    fun cardTitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextAppearance(R.style.TextAppearance_Feedback_CardTitle)
    }

    fun bodyText(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextAppearance(R.style.TextAppearance_Feedback_Body)
        val topPad = dp(context, 4)
        setPadding(0, topPad, 0, 0)
    }

    fun captionText(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextAppearance(R.style.TextAppearance_Feedback_Caption)
    }

    fun questionLabel(context: Context, text: String, required: Boolean): TextView = TextView(context).apply {
        this.text = if (required) "$text *" else text
        setTextAppearance(R.style.TextAppearance_Feedback_CardTitle)
        val topPad = dp(context, 4)
        setPadding(0, topPad, 0, dp(context, 10))
    }

    /** Small rounded status pill, e.g. APPROVED / PENDING / ACTIVE / INACTIVE. */
    fun statusChip(context: Context, label: String, bgColorRes: Int, textColorRes: Int): TextView {
        val tv = TextView(context)
        tv.text = label.uppercase()
        tv.setTextColor(ContextCompat.getColor(context, textColorRes))
        tv.textSize = 11.5f
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.letterSpacing = 0.03f
        val h = dp(context, 12)
        val v = dp(context, 5)
        tv.setPadding(h, v, h, v)

        val bg = ContextCompat.getDrawable(context, R.drawable.bg_pill)?.mutate()
        bg?.setTint(ContextCompat.getColor(context, bgColorRes))
        tv.background = bg

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = dp(context, 8)
        params.topMargin = dp(context, 2)
        params.bottomMargin = dp(context, 2)
        tv.layoutParams = params

        return tv
    }

    fun approvalChip(context: Context, status: String): TextView = when (status) {
        "approved" -> statusChip(context, "Approved", R.color.semantic_success_bg, R.color.semantic_success)
        "rejected" -> statusChip(context, "Rejected", R.color.semantic_error_bg, R.color.semantic_error)
        else -> statusChip(context, "Pending", R.color.semantic_warning_bg, R.color.semantic_warning)
    }

    fun activeChip(context: Context, isActive: Boolean): TextView =
        if (isActive) statusChip(context, "Active", R.color.semantic_success_bg, R.color.semantic_success)
        else statusChip(context, "Inactive", R.color.semantic_neutral_bg, R.color.semantic_neutral)

    /** Horizontal row that wraps overflowing children onto new lines (reused for chip badges + action rows). */
    fun wrapRow(context: Context): com.google.android.material.chip.ChipGroup {
        val group = com.google.android.material.chip.ChipGroup(context)
        group.chipSpacingHorizontal = dp(context, 8)
        group.chipSpacingVertical = dp(context, 8)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = dp(context, 4)
        group.layoutParams = params
        return group
    }

    fun actionChip(
        context: Context,
        label: String,
        iconRes: Int? = null,
        destructive: Boolean = false,
        primary: Boolean = false,
        onClick: (View) -> Unit
    ): Chip {
        val chip = Chip(context)
        chip.text = label
        chip.isCheckable = false
        chip.isClickable = true
        chip.chipCornerRadius = dp(context, 10).toFloat()

        if (iconRes != null) {
            chip.chipIcon = ContextCompat.getDrawable(context, iconRes)
            chip.isChipIconVisible = true
        }

        when {
            primary -> {
                chip.setChipBackgroundColorResource(android.R.color.transparent)
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.brand_primary)
                )
                chip.setTextColor(ContextCompat.getColor(context, R.color.text_on_primary))
                chip.chipIconTint = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.text_on_primary)
                )
            }
            destructive -> {
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.semantic_error_bg)
                )
                chip.setTextColor(ContextCompat.getColor(context, R.color.semantic_error))
                chip.chipIconTint = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.semantic_error)
                )
            }
            else -> {
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.neutral_surface_alt)
                )
                chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                chip.chipIconTint = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
            }
        }

        chip.setOnClickListener { onClick(it) }
        return chip
    }

    fun divider(context: Context): View = View(context).apply {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(context, 1)
        )
        params.topMargin = dp(context, 14)
        params.bottomMargin = dp(context, 14)
        layoutParams = params
        setBackgroundColor(ContextCompat.getColor(context, R.color.neutral_outline))
    }

    fun spacer(context: Context, heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(context, heightDp)
        )
    }

    /** Empty state block with an icon, title and subtitle - used across list screens. */
    fun emptyState(context: Context, iconRes: Int, title: String, subtitle: String): View {
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER_HORIZONTAL
        val vPad = dp(context, 40)
        container.setPadding(dp(context, 24), vPad, dp(context, 24), vPad)

        val icon = ImageView(context)
        icon.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
        icon.setColorFilter(ContextCompat.getColor(context, R.color.text_tertiary))
        val iconParams = LinearLayout.LayoutParams(dp(context, 48), dp(context, 48))
        iconParams.bottomMargin = dp(context, 16)
        icon.layoutParams = iconParams
        container.addView(icon)

        val titleView = TextView(context)
        titleView.text = title
        titleView.setTextAppearance(R.style.TextAppearance_Feedback_CardTitle)
        titleView.gravity = Gravity.CENTER
        container.addView(titleView)

        val subtitleView = TextView(context)
        subtitleView.text = subtitle
        subtitleView.setTextAppearance(R.style.TextAppearance_Feedback_Body)
        subtitleView.gravity = Gravity.CENTER
        val subParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        subParams.topMargin = dp(context, 4)
        subtitleView.layoutParams = subParams
        container.addView(subtitleView)

        return container
    }

    fun primaryButton(context: Context, label: String): MaterialButton = MaterialButton(context).apply {
        text = label
        isAllCaps = false
        cornerRadius = dp(context, 12)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(context, 50)
        )
        layoutParams = params
    }

    fun outlinedButton(context: Context, label: String): MaterialButton {
        val btn = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        btn.text = label
        btn.isAllCaps = false
        btn.cornerRadius = dp(context, 12)
        return btn
    }
}
