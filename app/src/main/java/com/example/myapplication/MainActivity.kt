package com.example.myapplication

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val PERMISSION_REQUEST_CODE = 123
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContactList()
                }

            }
        }
    }
    private fun requestContactsPermissionAndImport(onContactsImported: (List<Contact>) -> Unit) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, request it
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission is already granted, proceed to import contacts
            getContacts(onContactsImported)
        }
    }

    @SuppressLint("Range")
    private fun getContacts(onContactsImported: (List<Contact>) -> Unit) {
        val context = this
        val contacts = mutableListOf<Contact>()

        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val displayName =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val phoneNumber = getPhoneNumber(contentResolver, contactId)

                contacts.add(Contact(-1,displayName, phoneNumber))
            }
        }

        onContactsImported(contacts)
    }

    @SuppressLint("Range")
    private fun getPhoneNumber(contentResolver: android.content.ContentResolver, contactId: String): String {
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        return if (phoneCursor != null && phoneCursor.moveToFirst()) {
            val phoneNumber =
                phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            phoneCursor.close()
            phoneNumber
        } else {
            phoneCursor?.close()
            ""
        }
    }


    @Composable
    fun ContactList() {
        val context = LocalContext.current
        var databaseHelper by remember { mutableStateOf(DatabaseHelper(context = context)) }

        var allContacts by remember { mutableStateOf(emptyList<Contact>()) }
        var isImported by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if(!isImported) {

                Text(
                    text = "Contact List",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontSize = 25.sp
                )

                Button(
                    onClick = {
                        // Request contacts permission and import contacts
                        requestContactsPermissionAndImport { contacts ->
                            allContacts = contacts
                            isImported = true

                            // Insert contacts into the SQLite database
                            for (contact in contacts) {
                                databaseHelper.insertContact(contact)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF114B5F))


                ) {
                    Text(text = "Import Contacts", fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(6.dp))
                }
            }

            if (isImported) {
                ContactListView(
                    contacts = allContacts,
                    onMessageClicked = { contact ->
                        startMessage(context, contact.phoneNumber)
                    },
                    onCallClicked = { contact ->
                        startCall(context, contact.phoneNumber)
                    },
                    onDeleteClicked = { contact ->
                        // Delete contact from the SQLite database and update the UI
                        databaseHelper.deleteContact(contact)
                        allContacts = databaseHelper.getAllContacts()
                    }
                )
            }
        }
    }

    // Function to launch the messaging app with the selected contact as the recipient
    private fun startMessage(context: Context, phoneNumber: String) {
        val uri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        intent.putExtra("sms_body", "Hello ${phoneNumber}, this is a test message from Hussain Arslan")
        context.startActivity(intent)
    }


    // Function to initiate a call with the provided phone number
    private fun startCall(context: Context, phoneNumber: String) {
        val uri = Uri.parse("tel:$phoneNumber")
        val intent = Intent(Intent.ACTION_DIAL, uri)
        context.startActivity(intent)
    }

    @Composable
    fun ContactListView(
        contacts: List<Contact>,
        onMessageClicked: (Contact) -> Unit,
        onCallClicked: (Contact) -> Unit,
        onDeleteClicked: (Contact) -> Unit
    ) {
        LazyColumn {
            items(contacts) { contact ->
                ContactCard(
                    contact = contact,
                    onMessageClicked = onMessageClicked,
                    onCallClicked = onCallClicked,
                    onDeleteClicked = onDeleteClicked
                )
            }
        }
    }

    @Composable
    fun ContactCard(
        contact: Contact,
        onMessageClicked: (Contact) -> Unit,
        onCallClicked: (Contact) -> Unit,
        onDeleteClicked: (Contact) -> Unit
    ) {
        var isMessageClicked by remember { mutableStateOf(false) }
        var isCallClicked by remember { mutableStateOf(false) }
        var isDeleteClicked by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Handle click on contact item if needed */ }
                .padding(8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F4FA)),
        ) {
            Text(
                text = "${contact.displayName} - ${contact.phoneNumber}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                ,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        // Handle message button click
                        isMessageClicked = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp, start = 8.dp, bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF114B5F))

                ) {
                    Text(text = "Message", fontSize = 10.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        // Handle call button click
                        isCallClicked = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF114B5F))

                ) {
                    Text(text = "Call",  fontSize = 10.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        // Handle delete button click
                        isDeleteClicked = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 8.dp, bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF114B5F))

                ) {
                    Text(text = "Delete",  fontSize = 10.sp, color = Color.White)
                }
            }

            // Handle actions based on button clicks
            if (isMessageClicked) {
                // Add logic for handling message action
                // For example, you can launch the messaging app with the contact's phone number
                onMessageClicked(contact)
                isMessageClicked = false
            } else if (isCallClicked) {
                // Add logic for handling call action
                // For example, you can initiate a call using the contact's phone number
                onCallClicked(contact)
                isCallClicked = false
            } else if (isDeleteClicked) {
                // Add logic for handling delete action
                // For example, you can remove the contact from the list
                onDeleteClicked(contact)
                isDeleteClicked = false
            }
        }
    }


}


