package cz.twocom.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import cz.twocom.core.database.entity.ContactEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onAddContact: () -> Unit,
    onOpenChat: (String) -> Unit,
    vm: ChatListViewModel = hiltViewModel(),
) {
    val contacts by vm.contacts.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("2Com") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddContact) {
                Icon(Icons.Default.Add, "Add contact")
            }
        },
    ) { padding ->
        if (contacts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Text(
                    "No contacts yet.\nTap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(contacts, key = { it.id }) { contact ->
                    ContactRow(contact = contact, onClick = { onOpenChat(contact.peerHash) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: ContactEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(contact.displayName ?: contact.peerHash.take(16) + "…")
        },
        supportingContent = {
            Text(
                contact.peerHash,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(Icons.Default.Person, null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
