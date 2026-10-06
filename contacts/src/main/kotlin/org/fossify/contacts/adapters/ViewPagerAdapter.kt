package org.fossify.contacts.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.helpers.TAB_CONTACTS
import org.fossify.commons.helpers.TAB_FAVORITES
import org.fossify.commons.helpers.TAB_GROUPS
import org.fossify.contacts.R
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.fragments.MyViewPagerFragment
import org.fossify.contacts.helpers.TAB_KEYPAD
import org.fossify.contacts.helpers.TAB_RECENTS

class ViewPagerAdapter(val activity: SimpleActivity, val currTabsList: List<Int>, val showTabs: Int = Int.MAX_VALUE) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = getFragment(position)
        val view = activity.layoutInflater.inflate(layout, container, false)
        container.addView(view)

        if (layout == R.layout.rc_fragment_tab_action) {
            view.findViewById<TextView>(R.id.tab_action_hint)?.text = activity.getString(
                if (visibleTabs()[position] == TAB_KEYPAD) R.string.rc_dialpad else R.string.rc_recents
            )
        }

        (view as? MyViewPagerFragment<*>)?.apply {
            setupFragment(activity)
            setupColors(activity.getProperTextColor(), activity.getProperPrimaryColor())
        }

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
    }

    private fun visibleTabs() = currTabsList.filter { it and showTabs != 0 }

    override fun getCount() = visibleTabs().size

    override fun isViewFromObject(view: View, item: Any) = view == item

    private fun getFragment(position: Int): Int {
        return when (visibleTabs()[position]) {
            TAB_KEYPAD, TAB_RECENTS -> R.layout.rc_fragment_tab_action
            TAB_CONTACTS -> R.layout.rc_fragment_contacts
            TAB_FAVORITES -> R.layout.rc_fragment_favorites
            TAB_GROUPS -> R.layout.rc_fragment_groups
            else -> R.layout.rc_fragment_contacts
        }
    }
}
