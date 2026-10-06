package org.rabta.phone.classic.activities

import android.Manifest
import android.content.ClipData
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.*
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.getProperTextColor
import org.rabta.phone.classic.extensions.config
import org.rabta.phone.classic.extensions.startCallWithConfirmationCheck
import org.rabta.phone.classic.extensions.launchOtherContactApp
import org.rabta.phone.classic.helpers.ContactStore
import java.io.File

class ContactActivity : RaabtaScreenActivity() {
    private val store by lazy { ContactStore(contentResolver) }
    private var record: ContactStore.Record? = null
    private var raw: Long? = null
    private var editing = false
    private var busy = false
    private var pendingWrite: (() -> Unit)? = null
    private var restoredId = -1L
    private var restoredRaw = -1L
    private var restoredDraft: String? = null
    private val inputs = mutableListOf<Pair<ContactStore.Field, EditText>>()
    private val readPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) loadIntent() else { label("Contacts permission is needed to open your phonebook."); action("Try again") { requestRead() } }
    }
    private val writePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingWrite; pendingWrite = null
        if (granted) pending?.invoke() else toast("Contacts permission is needed to save changes.")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!config.integratedContacts) {
            launchOtherContactApp(Intent(intent).setComponent(null).setPackage(null))
            finish(); return
        }
        restoredId = savedInstanceState?.getLong("contact_id", -1) ?: -1
        restoredRaw = savedInstanceState?.getLong("contact_raw", -1) ?: -1
        restoredDraft = savedInstanceState?.getString("contact_draft")
        toolbar.title = "Contact"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (busy) return
                if (editing) MaterialAlertDialogBuilder(this@ContactActivity)
                    .setTitle("Discard unsaved changes?").setNegativeButton("Keep editing", null)
                    .setPositiveButton("Discard") { _, _ -> if (record == null) finish() else showDetails() }.show()
                else finish()
            }
        })
        requestRead()
    }
    private fun requestRead() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) loadIntent()
        else readPermission.launch(Manifest.permission.READ_CONTACTS)
    }
    private fun write(action: () -> Unit) {
        if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) action()
        else { pendingWrite = action; writePermission.launch(Manifest.permission.WRITE_CONTACTS) }
    }
    private fun loadIntent() {
        val id = if (restoredId >= 0) restoredId else intent.getLongExtra("raabta_contact_id", -1)
        if (id < 0 && restoredDraft == null && intent.action == Intent.ACTION_INSERT_OR_EDIT) {
            body.removeAllViews()
            label("Save this number", true)
            action("Create new contact") { record = null; editSource(null) }
            action("Add to existing contact") {
                org.rabta.phone.classic.helpers.ContactsCache.load(this, numbersOnly = false) { contacts ->
                    if (isFinishing || isDestroyed) return@load
                    if (contacts.isEmpty()) toast("No contacts found. Create a new contact instead.")
                    else org.rabta.phone.classic.dialogs.SelectContactDialog(this, contacts) { selected ->
                        work({ store.read(selected.contactId.toLong()) }) { record = it; chooseSource() }
                    }
                }
            }
            return
        }
        if (id < 0 && (intent.action == Intent.ACTION_INSERT || intent.data == null)) {
            record = null; editSource(null); return
        }
        work({ store.read(if (id >= 0) id else store.resolve(requireNotNull(intent.data))) }) {
            record = it
            if (restoredDraft != null) editSource(restoredRaw)
            else {
                showDetails()
                if (intent.action == Intent.ACTION_EDIT) chooseSource()
            }
        }
    }
    private fun showDetails() {
        val current = record ?: return
        editing = false; body.removeAllViews(); inputs.clear(); toolbar.title = "Contact"
        if (!current.photo.isNullOrBlank()) {
            val image = ImageView(this).apply { contentDescription = "Contact photo" }
            body.addView(image, LinearLayout.LayoutParams(dp(100), dp(100)))
            Glide.with(this).load(Uri.parse(current.photo)).circleCrop().into(image)
        }
        label(current.name.ifBlank { "Unnamed contact" }, true)
        current.sources.forEach { label(it.label + if (it.writable) "" else " · view only") }
        current.fields.filter { it.kind != StructuredName.CONTENT_ITEM_TYPE }.distinctBy { it.kind to it.value }.forEach { field ->
            when (field.kind) {
                Phone.CONTENT_ITEM_TYPE -> {
                    action("Call ${field.value}") { startCallWithConfirmationCheck(field.value, current.name) }
                    if (config.messengerShortcuts) {
                        action("Message ${field.value}") { external(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", field.value, null))) }
                        action("WhatsApp ${field.value}") {
                            val digits = field.value.filter(Char::isDigit)
                            if (!field.value.startsWith("+")) toast("Save the number with + and its country code first.")
                            else external(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
                        }
                    }
                }
                Email.CONTENT_ITEM_TYPE -> action(field.value) { external(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", field.value, null))) }
                else -> label(field.value)
            }
        }
        action("Edit contact") { chooseSource() }
        if (current.sources.none { it.writable }) action("Open in another Contacts app") {
            launchOtherContactApp(Intent(Intent.ACTION_VIEW,
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, current.id)))
        }
        action(if (current.starred) "Remove from favorites" else "Add to favorites") {
            write { work({ store.star(current); store.read(current.id) }) { record = it; showDetails() } }
        }
        if (config.contactSharing) action("Share contact (.vcf)") { share(current) }
        action("Delete contact") {
            MaterialAlertDialogBuilder(this).setTitle("Delete ${current.name}?")
                .setMessage("This deletes the contact from its linked accounts and may sync to your other devices.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                    write { work({ store.delete(current) }) { finish() } }
                }.show()
        }
    }
    private fun chooseSource() {
        val sources = record?.sources?.filter { it.writable }.orEmpty()
        if (sources.isEmpty()) { toast("This account cannot be edited here. Use the account's Contacts app."); return }
        if (sources.size == 1) editSource(sources[0].id)
        else MaterialAlertDialogBuilder(this).setTitle("Choose the account to edit")
            .setItems(sources.map { it.label }.toTypedArray()) { _, index -> editSource(sources[index].id) }.show()
    }
    private fun editSource(source: Long?) {
        raw = source; editing = true; body.removeAllViews(); inputs.clear()
        toolbar.title = if (record == null) "New contact" else "Edit contact"
        label(if (record == null) "Saved on this device. Other accounts and existing contacts are not modified."
              else "Only the selected account is edited. Photos, groups and other fields are preserved.")
        val restoringDraft = restoredDraft != null
        val fields = restoredDraft?.let { saved ->
            val entries = org.json.JSONArray(saved)
            (0 until entries.length()).map {
                val item = entries.getJSONObject(it)
                ContactStore.Field(item.getLong("id"), item.getLong("raw"), item.getString("kind"), item.getString("value"), item.getInt("type"))
            }
        } ?: record?.fields?.filter { it.raw == source }.orEmpty()
        restoredDraft = null
        fields.forEach { field(it) }
        if (fields.none { it.kind == StructuredName.CONTENT_ITEM_TYPE }) field(ContactStore.Field(-1, source ?: -1, StructuredName.CONTENT_ITEM_TYPE,
            if (restoringDraft) "" else intent.getStringExtra(ContactsContract.Intents.Insert.NAME).orEmpty(), 0))
        if (fields.none { it.kind == Phone.CONTENT_ITEM_TYPE }) field(ContactStore.Field(-1, source ?: -1, Phone.CONTENT_ITEM_TYPE,
            if (restoringDraft) "" else intent.getStringExtra(ContactsContract.Intents.Insert.PHONE).orEmpty(), Phone.TYPE_MOBILE))
        val extraPhone = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE).orEmpty()
        if (!restoringDraft && record != null && extraPhone.isNotBlank() && inputs.none { it.first.kind == Phone.CONTENT_ITEM_TYPE && it.first.value == extraPhone }) {
            field(ContactStore.Field(-1, source ?: -1, Phone.CONTENT_ITEM_TYPE, extraPhone, Phone.TYPE_MOBILE))
        }
        action("Add phone number") { field(ContactStore.Field(-1, source ?: -1, Phone.CONTENT_ITEM_TYPE, "", Phone.TYPE_MOBILE)) }
        action("Add email address") { field(ContactStore.Field(-1, source ?: -1, Email.CONTENT_ITEM_TYPE, "", Email.TYPE_OTHER)) }
        if (fields.none { it.kind == Note.CONTENT_ITEM_TYPE }) action("Add note") {
            if (inputs.none { it.first.kind == Note.CONTENT_ITEM_TYPE }) field(ContactStore.Field(-1, source ?: -1, Note.CONTENT_ITEM_TYPE, "", 0))
        }
        action("Save contact") {
            val changed = inputs.map { (original, text) -> original.copy(value = text.text.toString().trim()) }
            val original = record; val sourceId = raw
            write { work({ store.read(store.save(original, sourceId, changed)) }) { record = it; showDetails(); toast("Contact saved") } }
        }
    }
    private fun field(value: ContactStore.Field) {
        val title = when (value.kind) {
            Phone.CONTENT_ITEM_TYPE -> "Phone number"
            Email.CONTENT_ITEM_TYPE -> "Email address"
            Note.CONTENT_ITEM_TYPE -> "Notes"
            else -> "Full name"
        }
        val holder = TextInputLayout(this).apply { hint = title }
        val input = TextInputEditText(holder.context).apply {
            setText(value.value); setTextColor(getProperTextColor()); minHeight = dp(56)
            inputType = when (value.kind) {
                Phone.CONTENT_ITEM_TYPE -> InputType.TYPE_CLASS_PHONE
                Email.CONTENT_ITEM_TYPE -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                Note.CONTENT_ITEM_TYPE -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
        }
        holder.addView(input, LinearLayout.LayoutParams(-1, -2))
        body.addView(holder, inputs.size.coerceAtMost(body.childCount), LinearLayout.LayoutParams(-1, -2))
        inputs.add(value to input)
    }
    private fun share(current: ContactStore.Record) {
        work({
            val directory = File(cacheDir, "shared-contacts").apply { mkdirs() }
            val file = File(directory, "contact-${java.util.UUID.randomUUID()}.vcf")
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, Uri.encode(current.lookup))
            contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                ?: error("This contact cannot be exported")
            FileProvider.getUriForFile(this, "$packageName.contactfiles", file)
        }) { uri ->
            external(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"; putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Contact", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share contact"))
        }
    }
    private fun external(intent: Intent) {
        try { startActivity(intent) } catch (_: android.content.ActivityNotFoundException) { toast("No compatible app is installed.") }
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    override fun onSaveInstanceState(state: Bundle) {
        state.putLong("contact_id", record?.id ?: -1)
        state.putLong("contact_raw", raw ?: -1)
        if (editing) {
            val entries = org.json.JSONArray()
            inputs.forEach { (field, input) -> entries.put(org.json.JSONObject().apply {
                put("id", field.id); put("raw", field.raw); put("kind", field.kind)
                put("type", field.type); put("value", input.text.toString())
            }) }
            state.putString("contact_draft", entries.toString())
        }
        super.onSaveInstanceState(state)
    }
    private fun enableBody(view: android.view.View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) enableBody(view.getChildAt(i), enabled)
    }
    private fun <T> work(operation: () -> T, done: (T) -> Unit) {
        if (busy) return
        busy = true
        body.alpha = .6f
        enableBody(body, false)
        lifecycleScope.launch {
            try { val result = withContext(Dispatchers.IO) { operation() }; done(result) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { toast(failure.message ?: "Could not complete the contact operation") }
            finally { busy = false; body.alpha = 1f; enableBody(body, true) }
        }
    }
}
