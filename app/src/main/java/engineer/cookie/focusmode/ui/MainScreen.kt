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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import engineer.cookie.focusmode.R
import engineer.cookie.focusmode.data.AllowedContact
import engineer.cookie.focusmode.data.PhoneEntry
import engineer.cookie.focusmode.freeze.UserApp
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
    val keepAlive by viewModel.keepAlive.collectAsStateWithLifecycle()
    val userApps by viewModel.userApps.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingNumbers by remember { mutableStateOf<List<PhoneEntry>?>(null) }
    var showDeviceOwnerDialog by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    val appLabels = remember(userApps) { userApps.associate { it.packageName to it.label } }

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
                    onOpenDeviceOwner = { showDeviceOwnerDialog = true },
                )
            }
            if (access.deviceOwner) {
                item {
                    DeviceOwnerCard(onDeactivate = viewModel::deactivateDeviceOwner)
                }
            }
            item { SectionHeader("Allowed Contacts") }
            item {
                ContactsCard(
                    contacts = allowlist,
                    onRemove = { viewModel.remove(it) },
                    onAdd = { contactLauncher.launch(null) },
                )
            }
            item { SectionHeader("Allowed Apps") }
            item {
                AllowedAppsCard(
                    kept = keepAlive,
                    labels = appLabels,
                    onAdd = { showAppPicker = true },
                    onRemove = { viewModel.setKept(it, false) },
                )
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

    if (showDeviceOwnerDialog) {
        DeviceOwnerDialog(
            command = SpecialAccess.deviceOwnerCommand(context),
            onDismiss = { showDeviceOwnerDialog = false },
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = userApps,
            kept = keepAlive,
            onToggle = viewModel::setKept,
            onDismiss = { showAppPicker = false },
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
                } else if (!access.deviceOwner) {
                    Text(
                        text = "Other apps are not frozen yet. Set Focus Mode as device owner below.",
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
    onOpenDeviceOwner: () -> Unit,
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
            AccessRow(
                label = "Freeze other apps (device owner)",
                granted = access.deviceOwner,
                onGrant = onOpenDeviceOwner,
            )
        }
    }
}

@Composable
private fun DeviceOwnerCard(onDeactivate: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Device owner active", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Focus Mode can freeze other user apps.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onDeactivate) { Text("Deactivate") }
        }
    }
}

@Composable
private fun DeviceOwnerDialog(command: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(command))
                    onDismiss()
                },
            ) { Text("Copy command") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Freeze other apps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "To stop other apps while Focus Mode is on, Focus Mode must become " +
                        "the device owner. This is a one-time, root-free step done from a " +
                        "computer with ADB:",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(command, style = MaterialTheme.typography.bodySmall)
                Text(
                    "The device must have no accounts (no Google, Nextcloud, work profile, " +
                        "etc.) when you run this. Afterwards, toggle Focus Mode on.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

@Composable
private fun AllowedAppsCard(
    kept: Set<String>,
    labels: Map<String, String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "These apps can run and display notifications while Focus Mode is on.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (kept.isEmpty()) {
                Text("No apps added yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                kept.sortedBy { (labels[it] ?: it).lowercase() }.forEach { packageName ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = labels[packageName] ?: packageName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = { onRemove(packageName) }) { Text("Remove") }
                    }
                }
            }
            TextButton(onClick = onAdd) { Text("Add app") }
        }
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<UserApp>,
    kept: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Keep running") },
        text = {
            if (apps.isEmpty()) {
                Text("No apps found.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(
                        apps.sortedByDescending { it.packageName in kept },
                        key = { it.packageName },
                    ) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (app.isSystem) {
                                        "${app.packageName} \u00b7 system"
                                    } else {
                                        app.packageName
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Switch(
                                checked = app.packageName in kept,
                                onCheckedChange = { onToggle(app.packageName, it) },
                            )
                        }
                    }
                }
            }
        },
    )
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
private fun ContactsCard(
    contacts: List<AllowedContact>,
    onRemove: (AllowedContact) -> Unit,
    onAdd: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "These contacts can call or message you while Focus Mode is on.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (contacts.isEmpty()) {
                Text(
                    "No contacts added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                contacts.forEach { contact ->
                    ContactRow(contact = contact, onRemove = { onRemove(contact) })
                }
            }
            TextButton(onClick = onAdd) { Text("Add contact") }
        }
    }
}

@Composable
private fun ContactRow(contact: AllowedContact, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        TextButton(onClick = onRemove) { Text("Remove") }
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
