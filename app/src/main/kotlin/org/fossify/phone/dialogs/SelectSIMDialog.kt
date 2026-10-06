package org.fossify.phone.classic.dialogs

import android.annotation.SuppressLint
import android.telecom.PhoneAccountHandle
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.phone.classic.R
import org.fossify.phone.classic.extensions.config
import org.fossify.phone.classic.extensions.getAvailableSIMCardLabels
import org.fossify.phone.classic.models.SIMAccount

@SuppressLint("MissingPermission")
class SelectSIMDialog(
    private val activity: BaseSimpleActivity,
    private val phoneNumber: String,
    onDismiss: () -> Unit = {},
    private val callback: (handle: PhoneAccountHandle?) -> Unit
) {
    private val dialog = BottomSheetDialog(activity)
    private val backgroundColor = activity.getProperBackgroundColor()
    private val textColor = activity.getProperTextColor()
    private val accentColor = activity.getProperPrimaryColor()
    private val remember = CheckBox(activity)

    init {
        val accounts = activity.getAvailableSIMCardLabels()
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(30))
            background = roundedBackground(backgroundColor, 24)
        }
        content.addView(View(activity).apply {
            background = roundedBackground(textColor, 4)
        }, LinearLayout.LayoutParams(dp(36), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(18)
        })

        val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(activity).apply {
            text = activity.getString(R.string.select_sim_for_call, phoneNumber)
            setTextColor(textColor)
            textSize = 21f
            gravity = Gravity.CENTER
            maxLines = 2
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(activity).apply {
            text = "×"
            contentDescription = activity.getString(android.R.string.cancel)
            setTextColor(textColor)
            textSize = 28f
            gravity = Gravity.CENTER
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(header)

        val cards = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(22), 0, dp(18))
        }
        accounts.forEachIndexed { index, account ->
            cards.addView(createSimCard(account), LinearLayout.LayoutParams(dp(154), dp(132)).apply {
                if (index > 0) leftMargin = dp(10)
            })
        }
        content.addView(HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            addView(cards)
        })

        remember.apply {
            text = activity.getString(R.string.default_for_this_phone_number)
            setTextColor(textColor)
            textSize = 16f
            buttonTintList = android.content.res.ColorStateList.valueOf(accentColor)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        content.addView(remember, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(TextView(activity).apply {
            text = activity.getString(R.string.sim_preference_hint)
            setTextColor(textColor)
            alpha = 0.7f
            textSize = 14f
            setPadding(0, dp(10), 0, 0)
        })
        if (accounts.isEmpty()) {
            content.addView(TextView(activity).apply {
                text = activity.getString(R.string.no_available_sim)
                setTextColor(textColor)
            })
        }
        dialog.setContentView(content)
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
    }

    private fun createSimCard(account: SIMAccount): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBackground(backgroundColor, 18, textColor)
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.sim_card_description, account.slotNumber, account.label, account.network)
            addView(TextView(activity).apply {
                text = activity.getString(R.string.sim_slot_number, account.slotNumber)
                setTextColor(accentColor)
                textSize = 19f
                gravity = Gravity.CENTER
            })
            addView(TextView(activity).apply {
                text = account.label
                setTextColor(textColor)
                textSize = 17f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
            })
            if (account.network.isNotBlank() && !account.network.equals(account.label, true)) {
                addView(TextView(activity).apply {
                    text = account.network
                    setTextColor(textColor)
                    alpha = 0.7f
                    textSize = 13f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER
                })
            }
            setOnClickListener { selectedSIM(account.handle) }
        }
    }

    private fun selectedSIM(handle: PhoneAccountHandle) {
        if (remember.isChecked) activity.config.saveCustomSIM(phoneNumber, handle)
        callback(handle)
        dialog.dismiss()
    }

    private fun roundedBackground(color: Int, radiusDp: Int, strokeColor: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }

    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density + 0.5f).toInt()
}
