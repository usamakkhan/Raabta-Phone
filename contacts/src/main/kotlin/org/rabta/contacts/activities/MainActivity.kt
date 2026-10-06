package org.rabta.contacts.activities

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import me.grantland.widget.AutofitHelper
import org.fossify.commons.databases.ContactsDatabase
import org.fossify.commons.databinding.BottomTablayoutItemBinding
import org.fossify.commons.dialogs.ChangeViewTypeDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.FAQItem
import org.fossify.commons.models.RadioItem
import org.fossify.commons.models.Release
import org.fossify.commons.models.contacts.Contact
import org.rabta.contacts.BuildConfig
import org.rabta.contacts.R
import org.rabta.contacts.adapters.ViewPagerAdapter
import org.rabta.contacts.databinding.RcActivityMainBinding
import org.rabta.contacts.dialogs.ChangeSortingDialog
import org.rabta.contacts.dialogs.FilterContactSourcesDialog
import org.rabta.contacts.extensions.config
import org.rabta.contacts.extensions.handleGenericContactClick
import org.rabta.contacts.extensions.tryImportContactsFromFile
import org.rabta.contacts.fragments.FavoritesFragment
import org.rabta.contacts.fragments.MyViewPagerFragment
import org.rabta.contacts.helpers.ALL_TABS_MASK
import org.rabta.contacts.helpers.TAB_KEYPAD
import org.rabta.contacts.helpers.TAB_RECENTS
import org.rabta.contacts.interfaces.RefreshContactsListener
import java.util.Arrays

class MainActivity : SimpleActivity(), RefreshContactsListener {
    private var werePermissionsHandled = false
    private var isFirstResume = true
    private var isGettingContacts = false
    private var initialActionTabHandled = false

    private var storedShowContactThumbnails = false
    private var storedShowPhoneNumbers = false
    private var storedStartNameWithSurname = false
    private var storedFontSize = 0
    private var storedShowTabs = 0
    private var storedTabOrder = ""

    override var isSearchBarEnabled = true

    private val binding by viewBinding(RcActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        setupEdgeToEdge(
            padBottomImeAndSystem = listOf(binding.mainTabsHolder),
        )
        storeStateVariables()
        setupTabs()
        checkContactPermissions()
        checkWhatsNewDialog()
    }

    private fun checkContactPermissions() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            werePermissionsHandled = true
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    handlePermission(PERMISSION_GET_ACCOUNTS) {
                        initFragments()
                    }
                }
            } else {
                initFragments()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (storedShowPhoneNumbers != config.showPhoneNumbers) {
            System.exit(0)
            return
        }

        if (storedShowTabs != config.showTabs || storedTabOrder != config.orderedTabs.joinToString(",")) {
            config.contactsLastTab = 0
            finish()
            startActivity(intent)
            return
        }

        val configShowContactThumbnails = config.showContactThumbnails
        if (storedShowContactThumbnails != configShowContactThumbnails) {
            getAllFragments().forEach {
                it?.showContactThumbnailsChanged(configShowContactThumbnails)
            }
        }

        val properPrimaryColor = getProperPrimaryColor()
        binding.mainTabsHolder.background = ColorDrawable(getProperBackgroundColor())
        binding.mainTabsHolder.setSelectedTabIndicatorColor(properPrimaryColor)
        getAllFragments().forEach {
            it?.setupColors(getProperTextColor(), properPrimaryColor)
        }

        updateMenuColors()
        setupTabColors()

        val configStartNameWithSurname = config.startNameWithSurname
        if (storedStartNameWithSurname != configStartNameWithSurname) {
            findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.startNameWithSurnameChanged(configStartNameWithSurname)
            findViewById<MyViewPagerFragment<*>>(R.id.favorites_fragment)?.startNameWithSurnameChanged(configStartNameWithSurname)
        }

        val configFontSize = config.fontSize
        if (storedFontSize != configFontSize) {
            getAllFragments().forEach {
                it?.fontSizeChanged()
            }
        }

        if (werePermissionsHandled && !isFirstResume) {
            if (binding.viewPager.adapter == null) {
                initFragments()
            } else {
                refreshContacts(ALL_TABS_MASK)
            }
        }

        val dialpadIcon =
            resources.getColoredDrawableWithColor(org.fossify.commons.R.drawable.ic_dialpad_vector, properPrimaryColor.getContrastColor())
        binding.mainDialpadButton.apply {
            setImageDrawable(dialpadIcon)
            background.applyColorFilter(properPrimaryColor)
            beVisibleIf(config.showDialpadButton)
        }

        isFirstResume = false
        checkShortcuts()
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        config.contactsLastTab = binding.viewPager.currentItem
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            ContactsDatabase.destroyInstance()
        }
    }

    override fun onBackPressedCompat(): Boolean {
        return if (binding.mainMenu.isSearchOpen) {
            binding.mainMenu.closeSearch()
            true
        } else {
            false
        }
    }

    private fun refreshMenuItems() {
        val currentFragment = getCurrentFragment()
        binding.mainMenu.requireToolbar().menu.apply {
            findItem(R.id.sort).isVisible = currentFragment != null && currentFragment != findViewById(R.id.groups_fragment)
            findItem(R.id.filter).isVisible = currentFragment != null && currentFragment != findViewById(R.id.groups_fragment)
            findItem(R.id.dialpad).isVisible = !config.showDialpadButton
            findItem(R.id.change_view_type).isVisible = currentFragment == findViewById(R.id.favorites_fragment)
            findItem(R.id.column_count).isVisible = currentFragment == findViewById(R.id.favorites_fragment) && config.viewType == VIEW_TYPE_GRID
        }
    }

    private fun setupOptionsMenu() {
        binding.mainMenu.requireToolbar().inflateMenu(R.menu.rc_menu)
        binding.mainMenu.toggleHideOnScroll(false)
        binding.mainMenu.setupMenu()

        binding.mainMenu.onSearchClosedListener = {
            getAllFragments().forEach {
                it?.onSearchClosed()
            }
        }

        binding.mainMenu.onSearchTextChangedListener = { text ->
            getCurrentFragment()?.onSearchQueryChanged(text)
        }

        binding.mainMenu.requireToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> showSortingDialog(showCustomSorting = getCurrentFragment() is FavoritesFragment)
                R.id.filter -> showFilterDialog()
                R.id.dialpad -> launchDialpad()
                R.id.change_view_type -> changeViewType()
                R.id.column_count -> changeColumnCount()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(this) {
            refreshMenuItems()
            findViewById<FavoritesFragment>(R.id.favorites_fragment)?.updateFavouritesAdapter()
        }
    }

    private fun changeColumnCount() {
        val items = ArrayList<RadioItem>()
        for (i in 1..CONTACTS_GRID_MAX_COLUMNS_COUNT) {
            items.add(RadioItem(i, resources.getQuantityString(org.fossify.commons.R.plurals.column_counts, i, i)))
        }

        val currentColumnCount = config.contactsGridColumnCount
        RadioGroupDialog(this, items, currentColumnCount) {
            val newColumnCount = it as Int
            if (currentColumnCount != newColumnCount) {
                config.contactsGridColumnCount = newColumnCount
                findViewById<FavoritesFragment>(R.id.favorites_fragment)?.columnCountChanged()
            }
        }
    }

    private fun updateMenuColors() {
        binding.mainMenu.updateColors()
    }

    private fun storeStateVariables() {
        config.apply {
            storedShowContactThumbnails = showContactThumbnails
            storedShowPhoneNumbers = showPhoneNumbers
            storedStartNameWithSurname = startNameWithSurname
            storedShowTabs = showTabs
            storedTabOrder = orderedTabs.joinToString(",")
            storedFontSize = fontSize
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val createNewContact = getCreateNewContactShortcut(appIconColor)

            try {
                shortcutManager.dynamicShortcuts = Arrays.asList(createNewContact)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getCreateNewContactShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(org.fossify.commons.R.string.create_new_contact)
        val drawable = resources.getDrawable(org.fossify.commons.R.drawable.shortcut_plus)
        (drawable as LayerDrawable).findDrawableByLayerId(org.fossify.commons.R.id.shortcut_plus_background).applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, EditContactActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        return ShortcutInfo.Builder(this, "create_new_contact")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    private fun getCurrentFragment(): MyViewPagerFragment<*>? {
        return when (visibleTabIds().getOrNull(binding.viewPager.currentItem)) {
            TAB_CONTACTS -> findViewById(R.id.contacts_fragment)
            TAB_FAVORITES -> findViewById(R.id.favorites_fragment)
            TAB_GROUPS -> findViewById(R.id.groups_fragment)
            else -> null
        }
    }

    private fun setupTabColors() {
        val activeView = binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.customView
        updateBottomTabItemColors(activeView, true, getSelectedTabDrawableIds()[binding.viewPager.currentItem])

        getInactiveTabIndexes(binding.viewPager.currentItem).forEach { index ->
            val inactiveView = binding.mainTabsHolder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false, getDeselectedTabDrawableIds()[index])
        }

        val bottomBarColor = getBottomNavigationBackgroundColor()
        binding.mainTabsHolder.setBackgroundColor(bottomBarColor)
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until binding.mainTabsHolder.tabCount).filter { it != activeIndex }

    private fun visibleTabIds() = config.orderedTabs.filter { config.showTabs and it != 0 }

    private fun getTabDrawableId(tabId: Int, selected: Boolean): Int = when (tabId) {
        TAB_KEYPAD -> org.fossify.commons.R.drawable.ic_dialpad_vector
        TAB_RECENTS -> if (selected) org.fossify.commons.R.drawable.ic_clock_filled_vector else org.fossify.commons.R.drawable.ic_clock_vector
        TAB_CONTACTS -> if (selected) org.fossify.commons.R.drawable.ic_person_vector else org.fossify.commons.R.drawable.ic_person_outline_vector
        TAB_FAVORITES -> if (selected) org.fossify.commons.R.drawable.ic_star_vector else org.fossify.commons.R.drawable.ic_star_outline_vector
        else -> if (selected) org.fossify.commons.R.drawable.ic_people_vector else org.fossify.commons.R.drawable.ic_people_outline_vector
    }

    private fun getConfiguredTabLabel(tabId: Int): String = getString(
        when (tabId) {
            TAB_KEYPAD -> R.string.rc_dialpad
            TAB_RECENTS -> R.string.rc_recents
            TAB_CONTACTS -> org.fossify.commons.R.string.contacts_tab
            TAB_FAVORITES -> org.fossify.commons.R.string.favorites_tab
            else -> org.fossify.commons.R.string.groups_tab
        }
    )

    private fun getSelectedTabDrawableIds(): ArrayList<Int> {
        return ArrayList(visibleTabIds().map { getTabDrawableId(it, true) })
    }

    private fun getDeselectedTabDrawableIds(): ArrayList<Int> {
        return ArrayList(visibleTabIds().map { getTabDrawableId(it, false) })
    }

    private fun initFragments() {
        binding.viewPager.offscreenPageLimit = maxOf(1, visibleTabIds().size - 1)
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                binding.mainTabsHolder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it?.finishActMode()
                }
                when (visibleTabIds().getOrNull(position)) {
                    TAB_KEYPAD -> launchDialpad()
                    TAB_RECENTS -> launchRecents()
                }
                refreshMenuItems()
            }
        })

        binding.viewPager.onGlobalLayout {
            refreshContacts(ALL_TABS_MASK)
            if (!initialActionTabHandled) {
                initialActionTabHandled = true
                when (visibleTabIds().getOrNull(binding.viewPager.currentItem)) {
                    TAB_KEYPAD -> launchDialpad()
                    TAB_RECENTS -> launchRecents()
                }
            }
            refreshMenuItems()
        }

        handleExternalIntent()
        binding.mainDialpadButton.setOnClickListener {
            launchDialpad()
        }
    }

    private fun handleExternalIntent() {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }

        if (uri != null) {
            tryImportContactsFromFile(uri) { success ->
                if (success) {
                    runOnUiThread {
                        refreshContacts(ALL_TABS_MASK)
                    }
                }
            }
            intent.action = null
        }
    }

    private fun setupTabs() {
        binding.mainTabsHolder.removeAllTabs()
        visibleTabIds().forEach { tabId ->
            if (config.showTabs and tabId != 0) {
                binding.mainTabsHolder.newTab().setCustomView(org.fossify.commons.R.layout.bottom_tablayout_item).apply tab@{
                    customView?.let {
                        BottomTablayoutItemBinding.bind(it)
                    }?.apply {
                        tabItemIcon.setImageDrawable(resources.getColoredDrawableWithColor(getTabDrawableId(tabId, false), getProperTextColor()))
                        tabItemLabel.text = getConfiguredTabLabel(tabId)
                        AutofitHelper.create(tabItemLabel)
                        binding.mainTabsHolder.addTab(this@tab)
                    }
                }
            }
        }

        binding.mainTabsHolder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(it.customView, false, getDeselectedTabDrawableIds()[it.position])
            },
            tabSelectedAction = {
                getCurrentFragment()?.onSearchQueryChanged(binding.mainMenu.getCurrentQuery())
                binding.viewPager.currentItem = it.position
                updateBottomTabItemColors(it.customView, true, getSelectedTabDrawableIds()[it.position])
            }
        )

        binding.mainTabsHolder.beGoneIf(binding.mainTabsHolder.tabCount == 1)
    }

    private fun showSortingDialog(showCustomSorting: Boolean) {
        ChangeSortingDialog(this, showCustomSorting) {
            refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.forceListRedraw = true
            refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
        }
    }

    private fun launchDialpad() {
        hideKeyboard()
        selectContactsTabAfterExternalScreen()
        Intent(Intent.ACTION_DIAL).apply {
            setClassName(packageName, "org.rabta.phone.classic.activities.DialpadActivity")
            try {
                startActivity(this)
            } catch (e: ActivityNotFoundException) {
                toast(org.fossify.commons.R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun launchRecents() {
        hideKeyboard()
        selectContactsTabAfterExternalScreen()
        Intent(Intent.ACTION_VIEW).apply {
            setClassName(packageName, "org.rabta.phone.classic.activities.MainActivity")
            try {
                startActivity(this)
            } catch (e: ActivityNotFoundException) {
                toast(org.fossify.commons.R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    /**
     * Keypad and Recents are full-screen activities in the Phone module. Keep
     * the Contacts screen on a real Contacts page while those activities are
     * open so returning with Back never exposes the old action placeholder.
     */
    private fun selectContactsTabAfterExternalScreen() {
        val contactsIndex = visibleTabIds().indexOf(TAB_CONTACTS)
        if (contactsIndex >= 0 && binding.viewPager.currentItem != contactsIndex) {
            binding.viewPager.currentItem = contactsIndex
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        startActivity(Intent().setClassName(packageName, "org.rabta.phone.classic.activities.CreditsActivity"))
    }

    override fun refreshContacts(refreshTabsMask: Int) {
        if (isDestroyed || isFinishing || isGettingContacts) {
            return
        }

        isGettingContacts = true

        if (binding.viewPager.adapter == null) {
            binding.viewPager.adapter = ViewPagerAdapter(this, visibleTabIds())
            binding.viewPager.currentItem = getDefaultTab()
        }

        ContactsHelper(this).getContacts { contacts ->
            isGettingContacts = false
            if (isDestroyed || isFinishing) {
                return@getContacts
            }

            if (refreshTabsMask and TAB_CONTACTS != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.apply {
                    skipHashComparing = true
                    refreshContacts(contacts)
                }
            }

            if (refreshTabsMask and TAB_FAVORITES != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.favorites_fragment)?.apply {
                    skipHashComparing = true
                    refreshContacts(contacts)
                }
            }

            if (refreshTabsMask and TAB_GROUPS != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.groups_fragment)?.apply {
                    if (refreshTabsMask == TAB_GROUPS) {
                        skipHashComparing = true
                    }
                    refreshContacts(contacts)
                }
            }

            if (binding.mainMenu.isSearchOpen) {
                getCurrentFragment()?.onSearchQueryChanged(binding.mainMenu.getCurrentQuery())
            }
        }
    }

    override fun contactClicked(contact: Contact) {
        handleGenericContactClick(contact)
    }

    private fun getAllFragments() = arrayListOf<MyViewPagerFragment<*>?>(
        findViewById(R.id.contacts_fragment),
        findViewById(R.id.favorites_fragment),
        findViewById(R.id.groups_fragment)
    )

    private fun getDefaultTab(): Int {
        val visible = visibleTabIds()
        if (visible.isEmpty()) return 0
        if (config.contactsDefaultTab == TAB_LAST_USED) {
            return config.contactsLastTab.coerceIn(0, visible.lastIndex)
        }
        return visible.indexOf(config.contactsDefaultTab).takeIf { it >= 0 } ?: 0
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
