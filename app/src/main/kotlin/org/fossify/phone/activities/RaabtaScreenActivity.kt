package org.fossify.phone.classic.activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.getTextSize

/** Shared theme-aware shell for the integrated contact and credits screens. */
open class RaabtaScreenActivity : SimpleActivity() {
    protected lateinit var body: LinearLayout
    protected lateinit var toolbar: MaterialToolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getProperBackgroundColor())
        }
        toolbar = MaterialToolbar(this).apply {
            title = "Raabta Phone"
            setTitleTextColor(getProperTextColor())
            setNavigationIcon(org.fossify.phone.classic.R.drawable.ic_arrow_left_vector)
            setNavigationIconTint(getProperTextColor())
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(24))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(body) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
    }
    protected fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    protected fun label(value: String, heading: Boolean = false): TextView = TextView(this).apply {
        text = value
        setTextColor(getProperTextColor())
        val configuredSize = this@RaabtaScreenActivity.getTextSize()
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, if (heading) configuredSize * 1.3f else configuredSize)
        setPadding(0, dp(8), 0, dp(12))
        body.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    protected fun action(value: String, callback: () -> Unit): MaterialButton = MaterialButton(this).apply {
        text = value
        isAllCaps = false
        minHeight = dp(48)
        setOnClickListener { callback() }
        body.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
}
