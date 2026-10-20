package com.example.engine.actions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val photoUri: String? = null
) {
    fun getMaskedNumber(): String {
        val digits = normalizedNumber.filter { it.isDigit() }
        return if (digits.length > 4) {
            val last4 = digits.takeLast(4)
            val prefix = if (normalizedNumber.startsWith("+")) "+•• " else "•• "
            "$prefix•••• $last4"
        } else {
            normalizedNumber
        }
    }
}

class ContactsManager(private val context: Context) {

    companion object {
        private const val TAG = "ContactsManager"
    }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Searches contacts by name, alias, or phone digits.
     * Supports case-insensitive partial name matching and number cleaning.
     */
    fun searchContacts(query: String): List<ContactInfo> {
        if (!hasContactsPermission()) {
            Log.w(TAG, "Cannot search contacts: READ_CONTACTS permission not granted.")
            return emptyList()
        }

        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return emptyList()

        val results = mutableListOf<ContactInfo>()
        val seenNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        // Query by display name LIKE %query% or phone number LIKE %query%
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$cleanQuery%", "%$cleanQuery%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val rawNumber = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null

                    val normalized = normalizePhoneNumber(rawNumber)
                    if (normalized.isNotBlank() && !seenNumbers.contains(normalized)) {
                        seenNumbers.add(normalized)
                        results.add(
                            ContactInfo(
                                id = id,
                                name = name,
                                phoneNumber = rawNumber,
                                normalizedNumber = normalized,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        // If no direct SQL LIKE match, search all contacts in memory for token matching (e.g., "Raman" inside "Raman Kumar")
        if (results.isEmpty()) {
            return searchContactsFuzzy(cleanQuery)
        }

        return results
    }

    /**
     * Fallback in-memory search for common Hindi/English transliterations and token subsets.
     */
    private fun searchContactsFuzzy(query: String): List<ContactInfo> {
        val allContacts = getAllContacts()
        val queryTokens = query.split("\\s+".toRegex()).filter { it.length >= 2 }

        return allContacts.filter { contact ->
            val nameLower = contact.name.lowercase()
            // 1. Direct contains
            if (nameLower.contains(query)) return@filter true

            // 2. Token match (e.g. query "Raman", contact "Er. Raman Kumar")
            queryTokens.any { token -> nameLower.contains(token) }
        }
    }

    fun getAllContacts(limit: Int = 100): List<ContactInfo> {
        if (!hasContactsPermission()) return emptyList()

        val results = mutableListOf<ContactInfo>()
        val seenNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (it.moveToNext() && results.size < limit) {
                    val id = if (idIdx >= 0) it.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val rawNumber = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null

                    val normalized = normalizePhoneNumber(rawNumber)
                    if (normalized.isNotBlank() && !seenNumbers.contains(normalized)) {
                        seenNumbers.add(normalized)
                        results.add(
                            ContactInfo(
                                id = id,
                                name = name,
                                phoneNumber = rawNumber,
                                normalizedNumber = normalized,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all contacts: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        return results
    }

    /**
     * Sanitizes phone number: strips spaces, dashes, braces, and ensures international code.
     * For 10-digit Indian numbers, defaults to prepending '91'.
     */
    fun normalizePhoneNumber(rawNumber: String): String {
        var digits = rawNumber.replace(Regex("[^0-9+]"), "")
        if (digits.startsWith("+")) {
            digits = digits.substring(1)
        }
        // If 10 digits (standard Indian mobile number without country code)
        if (digits.length == 10) {
            digits = "91$digits"
        }
        // If starts with 0 (e.g. 09876543210)
        if (digits.startsWith("0") && digits.length == 11) {
            digits = "91" + digits.substring(1)
        }
        return digits
    }
}
