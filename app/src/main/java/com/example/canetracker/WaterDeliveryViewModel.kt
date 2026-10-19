package com.example.canetracker

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AppScreen {
    HOME,
    CUSTOMER_PROFILES,
    HISTORY
}

class  WaterDeliveryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WaterRepository(application)
    private val syncRepository = SyncRepository()
    private val auth = FirebaseAuth.getInstance()

    var currentUser by mutableStateOf<FirebaseUser?>(auth.currentUser)
        private set

    var isGuest by mutableStateOf(false)
        private set

    var showSplash by mutableStateOf(true)
        private set

    var lastSyncTime by mutableStateOf<String?>(null)
        private set

    var files by mutableStateOf(repository.getSavedFiles())
        private set

    var themeMode by mutableStateOf(repository.loadThemeMode())
        private set

    var currentScreen by mutableStateOf(AppScreen.HOME)
        private set

    val history = mutableStateListOf<HistoryEntry>()

    var allCustomers by mutableStateOf<List<String>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var filteredCustomers by mutableStateOf<List<String>>(emptyList())
        private set

    var selectedCustomer by mutableStateOf<String?>(null)
        private set

    var menuSearchQuery by mutableStateOf("")
        private set

    var menuFilteredCustomers by mutableStateOf<List<String>>(emptyList())
        private set

    var selectedMonthlyCustomer by mutableStateOf<String?>(null)
        private set

    var monthlyRecords by mutableStateOf<List<MonthlyRecordEntry>>(emptyList())
        private set

    val monthlyTotalReceived by derivedStateOf {
        monthlyRecords.sumOf { it.received }
    }

    val monthlyTotalReturned by derivedStateOf {
        monthlyRecords.sumOf { it.returned }
    }

    val monthlyNetBalance by derivedStateOf {
        monthlyRecords.lastOrNull()?.balance ?: 0.0
    }

    var isLoadingMonthly by mutableStateOf(false)
        private set

    var isRestoringData by mutableStateOf(false)
        private set

    var hasInaccessibleFiles by mutableStateOf(false)
        private set

    var isLoadingCustomers by mutableStateOf(false)
        private set

    var customerSummary by mutableStateOf<CustomerSummary?>(null)
        private set

    val groupedHistory by derivedStateOf {
        val filteredHistory = if (searchQuery.isEmpty()) {
            history
        } else {
            history.filter { it.customer.contains(searchQuery, ignoreCase = true) }
        }
        filteredHistory.groupBy { it.fullDate }
    }

    val recentCustomers by derivedStateOf {
        history.map { it.customer }.distinct().take(5)
    }

    fun getDailyTotal(date: String): Int {
        return history.filter { it.fullDate == date }.sumOf { it.received }
    }

    init {
        viewModelScope.launch {
            history.addAll(repository.getHistory())
            loadCustomers()
            delay(2000)
            showSplash = false
        }
    }

    fun uploadFiles(uris: List<String>) {
        repository.saveFiles(uris)
        files = uris
        loadCustomers()
        backupToCloud()
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            isLoadingCustomers = true
            allCustomers = repository.getCustomers()
            updateFilteredCustomers()
            isLoadingCustomers = false
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        updateFilteredCustomers()
    }

    fun updateMenuSearchQuery(query: String) {
        menuSearchQuery = query
        updateMenuFilteredCustomers()
    }

    private fun updateFilteredCustomers() {
        filteredCustomers = if (searchQuery.isEmpty()) {
            emptyList()
        } else {
            allCustomers.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    private fun updateMenuFilteredCustomers() {
        menuFilteredCustomers = if (menuSearchQuery.isEmpty()) {
            emptyList()
        } else {
            allCustomers.filter { it.contains(menuSearchQuery, ignoreCase = true) }
        }
    }

    fun selectCustomer(customer: String) {
        selectedCustomer = customer
        searchQuery = ""
        filteredCustomers = emptyList()
        fetchCustomerSummary(customer)
    }

    fun selectMonthlyCustomer(customer: String) {
        selectedMonthlyCustomer = customer
        menuSearchQuery = ""
        menuFilteredCustomers = emptyList()
        isLoadingMonthly = true
        viewModelScope.launch {
            monthlyRecords = repository.getCustomerMonthlyRecords(customer)
            isLoadingMonthly = false
        }
    }

    fun clearMonthlySelection() {
        selectedMonthlyCustomer = null
        monthlyRecords = emptyList()
        isLoadingMonthly = false
    }

    fun clearSelection() {
        selectedCustomer = null
        customerSummary = null
        searchQuery = ""
    }

    fun updateThemeMode(mode: Int) {
        themeMode = mode
        repository.saveThemeMode(mode)
    }

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
        if (screen == AppScreen.CUSTOMER_PROFILES) {
            clearSelection()
            clearMonthlySelection()
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            try {
                auth.signInWithCredential(credential).await()
                currentUser = auth.currentUser
                syncWithCloud()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun signInAsGuest() {
        isGuest = true
    }

    fun signOut() {
        auth.signOut()
        currentUser = null
        isGuest = false
        reset()
    }

    private fun syncWithCloud() {
        val userId = currentUser?.uid ?: return
        viewModelScope.launch {
            isRestoringData = true
            val cloudData = syncRepository.restoreData(userId)
            if (cloudData != null) {
                val (cloudHistory, cloudFiles, timestamp) = cloudData
                lastSyncTime = formatTimestamp(timestamp)
                
                if (cloudHistory.isNotEmpty() || cloudFiles.isNotEmpty()) {
                    history.clear()
                    history.addAll(cloudHistory)
                    repository.saveHistory(history)
                    
                    repository.saveFiles(cloudFiles)
                    files = cloudFiles
                    checkFileAccessibility()
                    loadCustomers()
                }
            }
            isRestoringData = false
            backupToCloud()
        }
    }

    private fun checkFileAccessibility() {
        var inaccessible = false
        files.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                getApplication<Application>().contentResolver.openInputStream(uri)?.close()
            } catch (e: Exception) {
                inaccessible = true
            }
        }
        hasInaccessibleFiles = inaccessible
    }

    fun dismissFileAlert() {
        hasInaccessibleFiles = false
    }

    private fun backupToCloud() {
        val userId = currentUser?.uid ?: return
        viewModelScope.launch {
            val timestamp = syncRepository.backupData(userId, history.toList(), files)
            if (timestamp != null) {
                lastSyncTime = formatTimestamp(timestamp)
            }
        }
    }

    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String? {
        if (timestamp == null) return null
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    fun isDarkTheme(systemDark: Boolean): Boolean {
        return when (themeMode) {
            1 -> false
            2 -> true
            else -> systemDark
        }
    }

    fun removeFile(uriString: String, fileName: String) {
        viewModelScope.launch {
            // 1. Remove from files list
            val updatedFiles = files.filter { it != uriString }
            repository.saveFiles(updatedFiles)
            files = updatedFiles

            // 2. Reload customers and clear selection if needed
            loadCustomers()
            if (selectedCustomer?.endsWith("($fileName)") == true) {
                clearSelection()
            }
            backupToCloud()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            history.clear()
            repository.saveHistory(history)
            backupToCloud()
        }
    }

    fun addEntry(customer: String, day: Int, received: Int, returned: Int, description: String) {
        viewModelScope.launch {
            val fullDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                .format(Calendar.getInstance().time).uppercase()
                
            repository.addEntry(customer, day, received, returned, description)
            
            // Calculate the new balance for the entry bubble
            val currentBal = customerSummary?.currentBalance ?: 0.0
            val newBal = currentBal + received - returned
            
            val entry = HistoryEntry(customer, day, received, returned, description, fullDate, newBal)
            history.add(0, entry) // Add to top for fast feedback
            repository.saveHistory(history)
            
            // Refresh summary to show updated balance
            fetchCustomerSummary(customer)
            backupToCloud()
        }
    }

    fun deleteEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry.customer, entry.day)
            history.remove(entry)
            repository.saveHistory(history)
            if (selectedCustomer == entry.customer) {
                fetchCustomerSummary(entry.customer)
            }
            backupToCloud()
        }
    }

    fun reset() {
        repository.reset()
        files = emptyList()
        history.clear()
        allCustomers = emptyList()
        clearSelection()
    }

    private fun fetchCustomerSummary(customer: String) {
        viewModelScope.launch {
            customerSummary = repository.getCustomerSummary(customer)
        }
    }

    fun performDownload(uriString: String, destinationUri: Uri) {
        viewModelScope.launch {
            getApplication<Application>().contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                repository.downloadFile(uriString, outputStream, history.toList())
            }
        }
    }
}
