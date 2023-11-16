package com.example.assignment3_datastorage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<Contact>

    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: Long): Contact?

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: Long)
}
