package com.example.canetracker

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

class WaterRepository(context: Context) {
    private val appState = AppState(context)
    private val excelManager = ExcelManager(context)

    init {
        excelManager.setFiles(appState.loadFiles())
    }

    fun getSavedFiles(): List<String> = appState.loadFiles()

    fun saveFiles(uris: List<String>) {
        appState.saveFiles(uris)
        excelManager.setFiles(uris)
    }

    fun getHistory(): List<HistoryEntry> = appState.loadHistory()

    fun saveHistory(history: List<HistoryEntry>) {
        appState.saveHistory(history)
    }

    fun saveThemeMode(mode: Int) {
        appState.saveThemeMode(mode)
    }

    fun loadThemeMode(): Int = appState.loadThemeMode()

    suspend fun getCustomers(): List<String> = withContext(Dispatchers.IO) {
        excelManager.customers()
    }

    suspend fun addEntry(customer: String, day: Int, received: Int, returned: Int, description: String) = withContext(Dispatchers.IO) {
        excelManager.addEntry(customer, day, received, returned, description)
    }

    suspend fun deleteEntry(customer: String, day: Int) = withContext(Dispatchers.IO) {
        excelManager.deleteEntry(customer, day)
    }

    fun reset() {
        appState.reset()
        excelManager.setFiles(emptyList())
    }

    suspend fun getCustomerSummary(customer: String): CustomerSummary? = withContext(Dispatchers.IO) {
        excelManager.getCustomerSummary(customer)
    }

    suspend fun getCustomerMonthlyRecords(customer: String): List<MonthlyRecordEntry> = withContext(Dispatchers.IO) {
        excelManager.getCustomerMonthlyRecords(customer)
    }

    suspend fun downloadFile(uriString: String, outputStream: OutputStream, history: List<HistoryEntry>) = withContext(Dispatchers.IO) {
        excelManager.downloadFile(uriString, outputStream, history)
    }
}
