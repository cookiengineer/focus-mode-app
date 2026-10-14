package engineer.cookie.focusmode.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import engineer.cookie.focusmode.R
import engineer.cookie.focusmode.data.AllowedContact
import engineer.cookie.focusmode.data.PhoneEntry
import engineer.cookie.focusmode.util.AccessState
import engineer.cookie.focusmode.util.SpecialAccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val access by viewModel.access.collectAsStateWithLifecycle()
    val allowlist by viewModel.allowlist.collectAsStateWithLifecycle()
    val isActive by viewModel.isActive.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingNumbers by remember { mutableStateOf<List<PhoneEntry>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshAccess() }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAccess() }

    val contactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) viewModel.loadNumbers(uri) { pendingNumbers = it }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { contactLauncher.launch(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add contact") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FocusToggleCard(
                    isActive = isActive,
                    access = access,
                    onToggle = viewModel::setEnabled,
                )
            }
            item {
                SetupCard(
                    access = access,
                    onRequestPermissions = { permissionLauncher.launch(viewModel.runtimePermissions) },
                    onOpenDnd = { settingsLauncher.launch(SpecialAccess.dndSettingsIntent()) },
                    onRequestRole = {
                        SpecialAccess.callScreeningRoleIntent(context)?.let { settingsLauncher.launch(it) }
                    },
                )
            }
            item { SectionHeader("Allowed contacts") }
            if (allowlist.isEmpty()) {
                item { EmptyState() }
            } else {
                items(allowlist, key = { it.id }) { contact ->
                    AllowlistRow(contact = contact, onRemove = { viewModel.remove(contact) })
                }
            }
        }
    }

    pendingNumbers?.let { numbers ->
        NumberPickerDialog(
            numbers = numbers,
            onDismiss = { pendingNumbers = null },
            onPick = { entry ->
                pendingNumbers = null
                viewModel.addContact(entry) { success ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (success) "Added ${entry.displayName}" else "That number is already on the list",
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun FocusToggleCard(isActive: Boolean, access: AccessState, onToggle: (Boolean) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isActive) "Focus Mode is on" else "Focus Mode is off",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (isActive) {
                        "Only your allowlisted contacts can reach you."
                    } else {
                        "Silence everything except your allowlist."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!access.coreGranted) {
                    Text(
                        text = "Grant the setup permissions below to enable this.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                enabled = access.coreGranted,
            )
        }
    }
}

@Composable
private fun SetupCard(
    access: AccessState,
    onRequestPermissions: () -> Unit,
    onOpenDnd: () -> Unit,
    onRequestRole: () -> Unit,
) {
    if (access.allGranted) return

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Setup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Focus Mode needs these one-time grants. LineageOS shows the same " +
                    "system dialogs as any other app.",
                style = MaterialTheme.typography.bodySmall,
            )
            AccessRow(
                label = "Contacts and notifications",
                granted = access.readContacts && access.writeContacts && access.postNotifications,
                onGrant = onRequestPermissions,
            )
            AccessRow(
                label = "Do Not Disturb access",
                granted = access.dndPolicy,
                onGrant = onOpenDnd,
            )
            AccessRow(
                label = "Call screening role",
                granted = access.callScreeningRole,
                onGrant = onRequestRole,
            )
        }
    }
}

@Composable
private fun AccessRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!granted) {
            TextButton(onClick = onGrant) { Text("Grant") }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyState() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No contacts yet. Tap \"Add contact\" to allow someone through in Focus Mode.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AllowlistRow(contact: AllowedContact, onRemove: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contact.rawNumber,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove")
            }
        }
    }
}

@Composable
private fun NumberPickerDialog(
    numbers: List<PhoneEntry>,
    onDismiss: () -> Unit,
    onPick: (PhoneEntry) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Choose a number") },
        text = {
            if (numbers.isEmpty()) {
                Text("This contact has no phone numbers.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    numbers.forEach { entry ->
                        TextButton(
                            onClick = { onPick(entry) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(entry.number, style = MaterialTheme.typography.bodyLarge)
                                entry.label?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
