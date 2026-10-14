package engineer.cookie.focusmode.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import engineer.cookie.focusmode.AppGraph
import engineer.cookie.focusmode.data.AllowedContact
import engineer.cookie.focusmode.data.PhoneEntry
import engineer.cookie.focusmode.freeze.UserApp
import engineer.cookie.focusmode.util.AccessState
import engineer.cookie.focusmode.util.PhoneNormalizer
import engineer.cookie.focusmode.util.SpecialAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _access = MutableStateFlow(SpecialAccess.current(app))
    val access: StateFlow<AccessState> = _access.asStateFlow()

    val allowlist: StateFlow<List<AllowedContact>> = AppGraph.allowlistRepository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isActive: StateFlow<Boolean> = AppGraph.focusModeController.isActive

    val keepAlive: StateFlow<Set<String>> = AppGraph.keepAliveRepository.packages

    private val _userApps = MutableStateFlow<List<UserApp>>(emptyList())
    val userApps: StateFlow<List<UserApp>> = _userApps.asStateFlow()

    val runtimePermissions: Array<String> = SpecialAccess.runtimePermissions

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _userApps.value = runCatching { AppGraph.appCatalog.userApps() }.getOrDefault(emptyList())
        }
    }

    fun refreshAccess() {
        _access.value = SpecialAccess.current(app)
    }

    fun setKept(packageName: String, kept: Boolean) {
        AppGraph.keepAliveRepository.setKept(packageName, kept)
    }

    fun setEnabled(enabled: Boolean) {
        AppGraph.focusModeController.setEnabled(enabled)
        refreshAccess()
    }

    fun deactivateDeviceOwner() {
        AppGraph.appSuspendController.deactivateDeviceOwner()
        refreshAccess()
    }

    fun loadNumbers(contactUri: Uri, onLoaded: (List<PhoneEntry>) -> Unit) {
        viewModelScope.launch {
            val entries = runCatching {
                AppGraph.contactRepository.phonesForContactUri(contactUri)
            }.getOrDefault(emptyList())
            onLoaded(entries)
        }
    }

    fun addContact(entry: PhoneEntry, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val e164 = PhoneNormalizer.toE164(app, entry.number)
                ?: PhoneNormalizer.digits(entry.number)
            val success = runCatching {
                AppGraph.allowlistRepository.add(
                    AllowedContact(
                        contactId = entry.contactId,
                        lookupKey = entry.lookupKey,
                        displayName = entry.displayName.ifBlank { entry.number },
                        rawNumber = entry.number,
                        e164 = e164,
                    ),
                )
            }.isSuccess
            onResult(success)
        }
    }

    fun remove(contact: AllowedContact) {
        viewModelScope.launch {
            AppGraph.allowlistRepository.removeById(contact.id)
        }
    }
}
