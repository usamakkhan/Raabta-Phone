package org.rabta.phone.classic.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.checkAppSideloading
import org.fossify.commons.extensions.getColorStateList
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.isDefaultDialer
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.extensions.onTextChangeListener
import org.fossify.commons.extensions.performHapticFeedback
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.value
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.KeypadHelper
import org.fossify.commons.helpers.LOWER_ALPHA_INT
import org.fossify.commons.helpers.MyContactsContentProvider
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.REQUEST_CODE_SET_DEFAULT_DIALER
import org.fossify.commons.models.contacts.Contact
import org.rabta.phone.classic.R
import org.rabta.phone.classic.adapters.ContactsAdapter
import org.rabta.phone.classic.databinding.ActivityDialpadBinding
import org.rabta.phone.classic.extensions.addCharacter
import org.rabta.phone.classic.extensions.areMultipleSIMsAvailable
import org.rabta.phone.classic.extensions.boundingBox
import org.rabta.phone.classic.extensions.config
import org.rabta.phone.classic.extensions.matchesClassicSearch
import org.rabta.phone.classic.extensions.matchesClassicQuery
import org.rabta.phone.classic.extensions.disableKeyboard
import org.rabta.phone.classic.extensions.getKeyEvent
import org.rabta.phone.classic.extensions.setupWithContacts
import org.rabta.phone.classic.extensions.startAddContactIntent
import org.rabta.phone.classic.extensions.startCallWithConfirmationCheck
import org.rabta.phone.classic.extensions.startContactDetailsIntent
import org.rabta.phone.classic.helpers.DIALPAD_TONE_LENGTH_MS
import org.rabta.phone.classic.helpers.DialpadInputPolicy
import org.rabta.phone.classic.helpers.RecentsHelper
import org.rabta.phone.classic.helpers.ToneGeneratorHelper
import org.rabta.phone.classic.models.SpeedDial
import java.util.Locale
import kotlin.math.roundToInt

class DialpadActivity : SimpleActivity() {
    private val searchRunner = org.rabta.phone.classic.helpers.SearchRunner(this)
    private val binding by viewBinding(ActivityDialpadBinding::inflate)

    private var allContacts = ArrayList<Contact>()
    private var inputRevision = 0L
    private var speedDialValues = ArrayList<SpeedDial>()
    private var toneGeneratorHelper: ToneGeneratorHelper? = null
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val pressedKeys = mutableSetOf<Char>()

    private var hasRussianLocale = false
    private val russianCharsMap by lazy {
        hashMapOf(
            'а' to 2, 'б' to 2, 'в' to 2, 'г' to 2,
            'д' to 3, 'е' to 3, 'ё' to 3, 'ж' to 3, 'з' to 3,
            'и' to 4, 'й' to 4, 'к' to 4, 'л' to 4,
            'м' to 5, 'н' to 5, 'о' to 5, 'п' to 5,
            'р' to 6, 'с' to 6, 'т' to 6, 'у' to 6,
            'ф' to 7, 'х' to 7, 'ц' to 7, 'ч' to 7,
            'ш' to 8, 'щ' to 8, 'ъ' to 8, 'ы' to 8,
            'ь' to 9, 'э' to 9, 'ю' to 9, 'я' to 9
        )
    }

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        hasRussianLocale = Locale.getDefault().language == "ru"

        binding.apply {
            setupEdgeToEdge(
                padBottomImeAndSystem = listOf(dialpadList, dialpadHolder)
            )
            setupMaterialScrollListener(binding.dialpadList, binding.dialpadAppbar)
        }

        if (checkAppSideloading()) {
            return
        }

        binding.dialpadWrapper.apply {
            if (config.hideDialpadNumbers) {
                dialpad1Holder.isVisible = false
                dialpad2Holder.isVisible = false
                dialpad3Holder.isVisible = false
                dialpad4Holder.isVisible = false
                dialpad5Holder.isVisible = false
                dialpad6Holder.isVisible = false
                dialpad7Holder.isVisible = false
                dialpad8Holder.isVisible = false
                dialpad9Holder.isVisible = false
                dialpadPlusHolder.isVisible = true
                dialpad0Holder.visibility = View.INVISIBLE
            }

            arrayOf(
                dialpad0Holder,
                dialpad1Holder,
                dialpad2Holder,
                dialpad3Holder,
                dialpad4Holder,
                dialpad5Holder,
                dialpad6Holder,
                dialpad7Holder,
                dialpad8Holder,
                dialpad9Holder,
                dialpadPlusHolder,
                dialpadAsteriskHolder,
                dialpadHashtagHolder
            ).forEach {
                it.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.pill_background, theme)
                it.background?.alpha = LOWER_ALPHA_INT
            }
        }

        setupOptionsMenu()
        speedDialValues = config.getSpeedDialValues()

        toneGeneratorHelper = ToneGeneratorHelper(this, DIALPAD_TONE_LENGTH_MS)

        binding.dialpadWrapper.apply {
            if (hasRussianLocale) {
                dialpad2Letters.append("\nАБВГ")
                dialpad3Letters.append("\nДЕЁЖЗ")
                dialpad4Letters.append("\nИЙКЛ")
                dialpad5Letters.append("\nМНОП")
                dialpad6Letters.append("\nРСТУ")
                dialpad7Letters.append("\nФХЦЧ")
                dialpad8Letters.append("\nШЩЪЫ")
                dialpad9Letters.append("\nЬЭЮЯ")

                val fontSize = resources.getDimension(R.dimen.small_text_size)
                arrayOf(
                    dialpad2Letters,
                    dialpad3Letters,
                    dialpad4Letters,
                    dialpad5Letters,
                    dialpad6Letters,
                    dialpad7Letters,
                    dialpad8Letters,
                    dialpad9Letters
                ).forEach {
                    it.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
                }
            }

            setupCharClick(dialpad1Holder, '1')
            setupCharClick(dialpad2Holder, '2')
            setupCharClick(dialpad3Holder, '3')
            setupCharClick(dialpad4Holder, '4')
            setupCharClick(dialpad5Holder, '5')
            setupCharClick(dialpad6Holder, '6')
            setupCharClick(dialpad7Holder, '7')
            setupCharClick(dialpad8Holder, '8')
            setupCharClick(dialpad9Holder, '9')
            setupCharClick(dialpad0Holder, '0')
            setupCharClick(dialpadPlusHolder, '+', longClickable = false)
            setupCharClick(dialpadAsteriskHolder, '*', longClickable = false)
            setupCharClick(dialpadHashtagHolder, '#', longClickable = false)
        }

        binding.apply {
            dialpadClearChar.setOnClickListener { clearChar(it) }
            dialpadClearChar.setOnLongClickListener { clearInput(); true }
            dialpadCallButton.setOnClickListener { initCall(dialpadInput.value) }
            dialpadCallButton.setOnLongClickListener { initCallWithSimSelector() }
            dialpadInput.onTextChangeListener { inputRevision++; dialpadValueChanged(it) }
            dialpadInput.requestFocus()
            dialpadInput.disableKeyboard()
        }

        // Consume incoming data before the asynchronous contact load, not after the user starts typing.
        if (savedInstanceState == null) checkDialIntent()
        org.rabta.phone.classic.helpers.ContactsCache.loadWithPrivate(this) { allContacts ->
            if (isFinishing || isDestroyed) return@loadWithPrivate
            gotContacts(allContacts)
        }

        val properPrimaryColor = getProperPrimaryColor()
        binding.apply {
            val callIcon = resources.getColoredDrawableWithColor(
                drawableId = R.drawable.ic_phone_vector,
                color = properPrimaryColor.getContrastColor()
            )
            dialpadCallButton.setImageDrawable(callIcon)
            dialpadCallButton.background.applyColorFilter(properPrimaryColor)

            letterFastscroller.textColor = getProperTextColor().getColorStateList()
            letterFastscroller.pressedTextColor = properPrimaryColor
            letterFastscrollerThumb.setupWithFastScroller(letterFastscroller)
            letterFastscrollerThumb.textColor = properPrimaryColor.getContrastColor()
            letterFastscrollerThumb.thumbColor = properPrimaryColor.getColorStateList()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(binding.dialpadHolder)
        binding.dialpadClearChar.applyColorFilter(getProperTextColor())
        setupTopAppBar(binding.dialpadAppbar, NavigationIcon.Arrow)
    }

    override fun onPause() {
        longPressHandler.removeCallbacksAndMessages(null)
        pressedKeys.clear()
        toneGeneratorHelper?.stopImmediately()
        super.onPause()
    }

    override fun onDestroy() {
        longPressHandler.removeCallbacksAndMessages(null)
        toneGeneratorHelper?.release()
        toneGeneratorHelper = null
        super.onDestroy()
    }

    private fun setupOptionsMenu() {
        binding.dialpadToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_number_to_contact -> addNumberToContact()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun checkDialIntent(): Boolean {
        return if (
            (intent.action == Intent.ACTION_DIAL || intent.action == Intent.ACTION_VIEW)
            && intent.data?.scheme == "tel"
        ) {
            val number = intent.data?.schemeSpecificPart.orEmpty()
            binding.dialpadInput.setText(number)
            binding.dialpadInput.setSelection(number.length)
            true
        } else {
            false
        }
    }

    private fun addNumberToContact() {
        startAddContactIntent(binding.dialpadInput.value)
    }

    private fun dialpadPressed(char: Char, view: View?) {
        binding.dialpadInput.addCharacter(char)
        maybePerformDialpadHapticFeedback(view)
    }

    private fun clearChar(view: View) {
        binding.dialpadInput.dispatchKeyEvent(binding.dialpadInput.getKeyEvent(KeyEvent.KEYCODE_DEL))
        maybePerformDialpadHapticFeedback(view)
    }

    private fun clearInput() {
        binding.dialpadInput.setText("")
    }

    private fun clearInputWithDelay() {
        val revision = inputRevision
        lifecycleScope.launch {
            delay(1000)
            if (inputRevision == revision) clearInput()
        }
    }

    private fun gotContacts(newContacts: ArrayList<Contact>) {
        if (isDestroyed || isFinishing) return
        allContacts = newContacts

        runOnUiThread {
            dialpadValueChanged(binding.dialpadInput.value)
        }
    }

    private fun dialpadValueChanged(text: String) {
        (binding.dialpadList.adapter as? ContactsAdapter)?.finishActMode()

        val classic = config.classicSearch
        val favoritesFirst = config.favoritesFirst
        val prepared = org.rabta.phone.classic.helpers.ClassicSearch.prepare(text)
        val snapshot = allContacts.toList()
        searchRunner.submit({
        val filtered = snapshot.filter { contact ->
            if (classic) {
                return@filter contact.matchesClassicQuery(prepared, keypad = true)
            }
            var convertedName = KeypadHelper.convertKeypadLettersToDigits(
                contact.name.normalizeString()
            ).filterNot { it.isWhitespace() }

            if (hasRussianLocale) {
                var currConvertedName = ""
                convertedName.lowercase(Locale.getDefault()).forEach { char ->
                    val convertedChar = russianCharsMap.getOrElse(char) { char }
                    currConvertedName += convertedChar
                }
                convertedName = currConvertedName
            }

            contact.doesContainPhoneNumber(text) || (convertedName.contains(text, true))
        }.sortedWith(compareBy {
            !it.doesContainPhoneNumber(text)
        }).toMutableList() as ArrayList<Contact>
        if (favoritesFirst) filtered.sortByDescending { it.starred }
        filtered
        }) { filtered ->

        binding.letterFastscroller.setupWithContacts(binding.dialpadList, filtered)

        val existingAdapter = binding.dialpadList.adapter as? ContactsAdapter
        if (existingAdapter != null) {
            existingAdapter.updateItems(filtered, text)
        } else ContactsAdapter(
            activity = this,
            contacts = filtered,
            recyclerView = binding.dialpadList,
            highlightText = text,
            itemClick = {
                startCallWithConfirmationCheck(it as Contact)
                clearInputWithDelay()
            },
            profileIconClick = {
                startContactDetailsIntent(it as Contact)
            }).apply {
            binding.dialpadList.adapter = this
        }

        binding.dialpadPlaceholder.beVisibleIf(filtered.isEmpty())
        binding.dialpadList.beVisibleIf(filtered.isNotEmpty())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER && isDefaultDialer()) {
            dialpadValueChanged(binding.dialpadInput.value)
        }
    }

    private fun initCall(number: String = binding.dialpadInput.value, name: String? = null) {
        if (runSecretCodeFromUserAction(number)) return
        if (number.isNotEmpty()) {
            startCallWithConfirmationCheck(number, name ?: number)
            clearInputWithDelay()
        } else {
            val revision = inputRevision
            RecentsHelper(this).getRecentCalls(queryLimit = 1) {
                val mostRecentNumber = it.firstOrNull()?.phoneNumber
                if (!mostRecentNumber.isNullOrEmpty()) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && inputRevision == revision && binding.dialpadInput.value.isEmpty()) {
                            binding.dialpadInput.setText(mostRecentNumber)
                        }
                    }
                }
            }
        }
    }

    private fun initCallWithSimSelector(): Boolean {
        val number = binding.dialpadInput.value
        if (runSecretCodeFromUserAction(number)) return true
        return if (areMultipleSIMsAvailable() && number.isNotEmpty()) {
            startCallWithConfirmationCheck(
                recipient = number,
                name = number,
                forceSimSelector = true
            )
            true
        } else {
            false
        }
    }

    private fun runSecretCodeFromUserAction(number: String): Boolean {
        val code = DialpadInputPolicy.secretCode(number) ?: return false
        try {
            if (isDefaultDialer()) getSystemService(TelephonyManager::class.java)?.sendDialerSpecialCode(code)
            else launchSetDefaultDialerIntent()
        } catch (failure: Exception) { showErrorToast(failure) }
        return true
    }

    private fun speedDial(id: Int): Boolean {
        if (binding.dialpadInput.value.length == 1) {
            val speedDial = speedDialValues.firstOrNull { it.id == id }
            if (speedDial?.isValid() == true) {
                initCall(speedDial.number, speedDial.getName(this))
                return true
            }
        }
        return false
    }

    private fun startDialpadTone(char: Char) {
        if (config.dialpadBeeps) {
            pressedKeys.add(char)
            toneGeneratorHelper?.startTone(char)
        }
    }

    private fun stopDialpadTone(char: Char) {
        if (config.dialpadBeeps) {
            if (!pressedKeys.remove(char)) return
            if (pressedKeys.isEmpty()) {
                toneGeneratorHelper?.stopTone()
            } else {
                startDialpadTone(pressedKeys.last())
            }
        }
    }

    private fun maybePerformDialpadHapticFeedback(view: View?) {
        if (config.dialpadVibration) {
            view?.performHapticFeedback()
        }
    }

    private fun performLongClick(view: View, char: Char) {
        if (char == '0') {
            clearChar(view)
            dialpadPressed('+', view)
        } else {
            val result = speedDial(char.digitToInt())
            if (result) {
                stopDialpadTone(char)
                clearChar(view)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCharClick(view: View, char: Char, longClickable: Boolean = true) {
        view.isClickable = true
        view.isLongClickable = true
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dialpadPressed(char, view)
                    startDialpadTone(char)
                    if (longClickable) {
                        longPressHandler.removeCallbacksAndMessages(null)
                        longPressHandler.postDelayed({
                            performLongClick(view, char)
                        }, longPressTimeout)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopDialpadTone(char)
                    if (longClickable) {
                        longPressHandler.removeCallbacksAndMessages(null)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val viewContainsTouchEvent = if (event.rawX.isNaN() || event.rawY.isNaN()) {
                        false
                    } else {
                        view.boundingBox.contains(event.rawX.roundToInt(), event.rawY.roundToInt())
                    }

                    if (!viewContainsTouchEvent) {
                        stopDialpadTone(char)
                        if (longClickable) {
                            longPressHandler.removeCallbacksAndMessages(null)
                        }
                    }
                }
            }
            false
        }
    }
}
