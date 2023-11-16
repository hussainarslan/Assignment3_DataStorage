package com.example.assignment3_datastorage

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.assignment3_datastorage.ui.theme.Assignment3_DataStorageTheme

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var database: AppDatabase
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "contacts_database"
        ).build()
        setContent {
            Assignment3_DataStorageTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "contactList") {
                        composable("contactList") { ContactListScreen(navController) }

                        composable("contactEdit/{contactId}") { backStackEntry ->
                            val contactId = backStackEntry.arguments?.getString("contactId")
                            ContactEditScreen(navController, contactId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactListScreen(navController: NavController) {
    val contactRepository = ContactRepository(context = LocalContext.current)
    var importedContacts: List<Contact> by remember { mutableStateOf(emptyList()) }

    fun refreshContacts() {
        importedContacts = contactRepository.getContacts()
    }
    // Fetch contacts from the device when the component is first composed
    LaunchedEffect(Unit) {
        // Store imported contacts in the database
        val contactsFromDevice = contactRepository.getContacts()
        for (contact in contactsFromDevice) {
            val contactEntity = Contact(
                id = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber
            )

            contactRepository.insertContact(contactEntity)
        }

        // Update the UI with the newly imported contacts
        importedContacts = contactRepository.getContacts()
    }



    Column {
        // Display the imported contacts
        LazyColumn {
            items(importedContacts) { contact ->
                ContactItem(contact = contact, contactRepository = contactRepository,  navigateToEdit = { navController.navigate("contactEdit/${contact.id}") })
            }
        }

        // Button to trigger contact import
        Button(onClick = {  }) {
            Text("Import Contacts")
        }
        Button(onClick = { refreshContacts() }) {
            Text("Refresh Contacts")
        }
    }
}


@Composable
fun ContactItem(contact: Contact, contactRepository: ContactRepository, navigateToEdit: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    // Function to show a confirmation dialog before deleting a contact
    fun showDeleteConfirmationDialog() {
        showDialog = true
    }

    // Function to dismiss the confirmation dialog
    fun dismissDialog() {
        showDialog = false
    }

    // Actual delete operation
    LaunchedEffect(contact) {
        dismissDialog()
        // Call the suspend function using LaunchedEffect
            contactRepository.deleteContact(contact.id)
    }
    // Inside the ContactItem Composable
    val makeCall = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            // Handle the result if needed
        }
    }

    // UI components
    Column {
        Text(text = "Name: ${contact.name}, Phone: ${contact.phoneNumber}")

        Row {
            // Update Button
            Button(onClick = { navigateToEdit() }) {
                Text("Update")
            }


            // Delete Button
            Button(onClick = { showDeleteConfirmationDialog() }) {
                Text("Delete")
            }

            // Call Button
            Button(onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contact.phoneNumber}")
                }
                try {
                    makeCall.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle if the dialer app is not available
                }
            }) {
                Text("Call")
            }
            // Message Button
            Button(onClick = { /* Implement message logic here */ }) {
                Text("Message")
            }
        }
    }

    // Confirmation dialog for delete operation
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { dismissDialog() },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete this contact?") },
            confirmButton = {
                Button(
                    onClick = {  }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = { dismissDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(navController: NavController, contactId: String?) {
    // Retrieve the contact details based on the contactId and display them
    val contactRepository = ContactRepository(context = LocalContext.current)
    val contact: Contact? = contactId?.let { contactRepository.getContactById(it.toLong()) }

    // Create mutable state for editable fields
    var newName by remember { mutableStateOf(contact?.name.orEmpty()) }
    var newPhoneNumber by remember { mutableStateOf(contact?.phoneNumber.orEmpty()) }

    // UI components
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Editable fields
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Name") }
        )

        OutlinedTextField(
            value = newPhoneNumber,
            onValueChange = { newPhoneNumber = it },
            label = { Text("Phone Number") }
        )

        // Button to save changes
        Button(onClick = {
            // Update contact in the database
            contact?.let {
                val updatedContact = it.copy(name = newName, phoneNumber = newPhoneNumber)
                viewModel.updateContact(updatedContact)
            }

            // Navigate back to contact details
            navController.popBackStack()
        }) {
            Text("Save Changes")
        }
    }
}
