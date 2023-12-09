package com.example.myapplication
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ContactDatabase"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CONTACTS = "contacts"
        private const val KEY_ID = "id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PHONE_NUMBER = "phone_number"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_CONTACTS (
                $KEY_ID INTEGER PRIMARY KEY,
                $KEY_DISPLAY_NAME TEXT,
                $KEY_PHONE_NUMBER TEXT
            )
        """.trimIndent()

        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades if needed
    }

    fun insertContact(contact: Contact) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_DISPLAY_NAME, contact.displayName)
            put(KEY_PHONE_NUMBER, contact.phoneNumber)
        }

        db.insert(TABLE_CONTACTS, null, values)
        db.close()
        Log.d("dbInsert", "Contact inserted to DB")
    }

    fun deleteContact(contact: Contact) {
        val db = writableDatabase
        db.delete(TABLE_CONTACTS, "$KEY_PHONE_NUMBER=?", arrayOf(contact.phoneNumber))
        db.close()
        Log.d("dbDelete", "Contact deleted from DB")
    }

    @SuppressLint("Range")
    fun getAllContacts(): List<Contact> {
        val contactList = mutableListOf<Contact>()
        val selectQuery = "SELECT * FROM $TABLE_CONTACTS"
        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndex(KEY_ID))
                val displayName = it.getString(it.getColumnIndex(KEY_DISPLAY_NAME))
                val phoneNumber = it.getString(it.getColumnIndex(KEY_PHONE_NUMBER))
                contactList.add(Contact(id, displayName, phoneNumber))
            }
        }

        return contactList
    }
}
