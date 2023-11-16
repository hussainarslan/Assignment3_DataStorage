package com.example.assignment3_datastorage

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class ContactRepository(private val context: Context) {
    private val contactDao: ContactDao = MainActivity.database.contactDao()

    suspend fun insertContact(contact: Contact) {
        withContext(Dispatchers.IO) {
            contactDao.insertContact(contact)
        }
    }

    suspend fun getContactsFromDatabase(): List<Contact> {
        return withContext(Dispatchers.IO) {
            contactDao.getAllContacts()
        }
    }

    suspend fun getContactById(contactId: Long): Contact? {
        return withContext(Dispatchers.IO) {
            contactDao.getContactById(contactId)
        }
    }

    suspend fun deleteContact(contactId: Long) {
        withContext(Dispatchers.IO) {
            contactDao.deleteContact(contactId)
        }
    }

    suspend fun importContacts(contacts: List<Contact>) {
        withContext(Dispatchers.IO) {
            for (contact in contacts) {
                val contactEntity = Contact(
                    id = contact.id,
                    name = contact.name,
                    phoneNumber = contact.phoneNumber
                )
                contactDao.insertContact(contactEntity)
            }
        }
    }
    fun getContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val contactId =
                    it.getLong(it.getColumnIndex(ContactsContract.Contacts._ID))
                val contactName =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))

                // Retrieve the phone number
                val phoneNumber = getPhoneNumber(contactId)

                // Create a Contact object and add it to the list
                val contact = Contact(contactId, contactName, phoneNumber)
                contacts.add(contact)
            }
        }

        return contacts
    }

    private fun getPhoneNumber(contactId: Long): String {
        val phoneNumber: StringBuilder = StringBuilder()
        val contentResolver: ContentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = $contactId",
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                phoneNumber.append(it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)))
            }
        }

        return phoneNumber.toString()
    }
}
