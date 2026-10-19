package com.example.canetracker

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayOutputStream
import java.io.OutputStream

data class CustomerSummary(
    val name: String,
    val totalReceived: Double,
    val totalReturned: Double,
    val currentBalance: Double
)

data class MonthlyRecordEntry(
    val day: Int,
    val received: Double,
    val returned: Double,
    val balance: Double,
    val description: String
)

class ExcelManager(
    private val context: Context
) {

    private val contentResolver: ContentResolver =
        context.contentResolver

        private var excelFiles: MutableList<String> = mutableListOf()

    fun setFiles(paths: List<String>) {
        excelFiles = paths.toMutableList()
    }

    // =========================================================
    // DOWNLOAD FILE (EXPORT)
    // Ensures all app changes are included in the output
    // =========================================================

    fun downloadFile(uriString: String, outputStream: OutputStream, history: List<HistoryEntry>) {
        val uri = Uri.parse(uriString)
        val fileName = getFileName(uri)

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)

                // Re-apply all history entries belonging to this file
                // to ensure the export is 100% consistent with the app state.
                history.forEach { entry ->
                    val parsed = parseCustomerText(entry.customer)
                    if (parsed.second == fileName) {
                        applyUpdateToWorkbook(workbook, parsed.first, entry.day, entry.received, entry.returned, entry.description)
                    }
                }

                // Use a buffer to ensure the workbook is fully finalized before writing to disk
                val byteBuffer = ByteArrayOutputStream()
                workbook.write(byteBuffer)
                workbook.close()

                outputStream.write(byteBuffer.toByteArray())
                outputStream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // =========================================================
    // GET ALL CUSTOMERS
    // =========================================================

    fun customers(): List<String> {
        val result = mutableListOf<String>()
        for (uriString in excelFiles) {
            val uri = Uri.parse(uriString)
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    val fileName = getFileName(uri)
                    for (sheetIndex in 0 until workbook.numberOfSheets) {
                        val sheet = workbook.getSheetAt(sheetIndex)
                        result.add("${sheet.sheetName} ($fileName)")
                    }
                    workbook.close()
                }
            } catch (e: Exception) {
                Log.e("ExcelManager", "Error reading customers from $uriString", e)
            }
        }
        return result
    }

    // =========================================================
    // GET CUSTOMER SUMMARY (DESCRIBE)
    // =========================================================

    fun getCustomerSummary(customerText: String): CustomerSummary? {
        val parsed = parseCustomerText(customerText)
        val sheetName = parsed.first
        val fileName = parsed.second

        for (uriString in excelFiles) {
            val uri = Uri.parse(uriString)
            if (getFileName(uri) == fileName) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val workbook = WorkbookFactory.create(inputStream)
                        val sheet = workbook.getSheet(sheetName) ?: return null

                        var totalRec = 0.0
                        var totalRet = 0.0

                        // Summary based on columns C (index 2) and D (index 3)
                        // Rows 5 downwards (index 4+)
                        for (i in 4 until sheet.lastRowNum + 1) {
                            val row = sheet.getRow(i) ?: continue
                            totalRec += getNumericValue(row.getCell(2))
                            totalRet += getNumericValue(row.getCell(3))
                        }

                        // Get the latest balance for the summary
                        val balance = findPreviousBalance(sheet, sheet.lastRowNum + 1)

                        workbook.close()
                        return CustomerSummary(sheetName, totalRec, totalRet, balance)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun getCustomerMonthlyRecords(customerText: String): List<MonthlyRecordEntry> {
        val parsed = parseCustomerText(customerText)
        val sheetName = parsed.first
        val fileName = parsed.second
        val result = mutableListOf<MonthlyRecordEntry>()

        for (uriString in excelFiles) {
            val uri = Uri.parse(uriString)
            if (getFileName(uri) == fileName) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val workbook = WorkbookFactory.create(inputStream)
                        val sheet = workbook.getSheet(sheetName) ?: return emptyList()

                        // Records based on Column B (Day), C (Received), D (Returned), E (Balance), F (Note)
                        // Rows 5 downwards (index 4+)
                        for (i in 4 until sheet.lastRowNum + 1) {
                            val row = sheet.getRow(i) ?: continue
                            val day = getNumericValue(row.getCell(1)).toInt()
                            if (day > 0) {
                                result.add(
                                    MonthlyRecordEntry(
                                        day = day,
                                        received = getNumericValue(row.getCell(2)),
                                        returned = getNumericValue(row.getCell(3)),
                                        balance = getNumericValue(row.getCell(4)),
                                        description = getStringValue(row.getCell(5))
                                    )
                                )
                            }
                        }
                        workbook.close()
                        return result
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return emptyList()
    }

    // =========================================================
    // ADD ENTRY
    // =========================================================

    fun addEntry(
        customerText: String,
        day: Int,
        received: Int,
        returned: Int,
        description: String
    ) {
        val parsed = parseCustomerText(customerText)
        val sheetName = parsed.first
        val fileName = parsed.second

        for (uriString in excelFiles) {
            val uri = Uri.parse(uriString)
            if (getFileName(uri) == fileName) {
                updateAndSave(uri, sheetName, day, received, returned, description)
                break
            }
        }
    }

    // =========================================================
    // DELETE ENTRY
    // =========================================================

    fun deleteEntry(
        customerText: String,
        day: Int
    ) {
        val parsed = parseCustomerText(customerText)
        val sheetName = parsed.first
        val fileName = parsed.second

        for (uriString in excelFiles) {
            val uri = Uri.parse(uriString)
            if (getFileName(uri) == fileName) {
                deleteAndSave(uri, sheetName, day)
                break
            }
        }
    }

    private fun updateAndSave(uri: Uri, sheetName: String, day: Int, received: Int, returned: Int, description: String) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                applyUpdateToWorkbook(workbook, sheetName, day, received, returned, description)
                saveWorkbook(uri, workbook)
                workbook.close()
            }
        } catch (e: Exception) {
            Log.e("ExcelManager", "Error updating $uri", e)
        }
    }

    private fun deleteAndSave(uri: Uri, sheetName: String, day: Int) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                applyDeletionToWorkbook(workbook, sheetName, day)
                saveWorkbook(uri, workbook)
                workbook.close()
            }
        } catch (e: Exception) {
            Log.e("ExcelManager", "Error deleting from $uri", e)
        }
    }

    // =========================================================
    // REFINED CORE LOGIC (Running Balance B-E, Row Search)
    // =========================================================

    private fun applyUpdateToWorkbook(workbook: Workbook, sheetName: String, day: Int, received: Int, returned: Int, description: String) {
        val sheet = workbook.getSheet(sheetName) ?: return
        
        // Find row where Column B (index 1) matches day
        val targetRow = findRowForDay(sheet, day) ?: return
        val targetRowIndex = targetRow.rowNum

        // Rule: Fetch previous balance or E5 as starting point
        val previousBalance = findPreviousBalance(sheet, targetRowIndex)

        // New Running Balance = Previous Running Balance + (Delivered - Returned)
        val newBalance = previousBalance + received.toDouble() - returned.toDouble()

        // Update Column C (index 2) and D (index 3)
        targetRow.getCellOrCreate(2).setCellValue(received.toDouble())
        targetRow.getCellOrCreate(3).setCellValue(returned.toDouble())
        
        // Update Column E (index 4) with New Balance
        targetRow.getCellOrCreate(4).setCellValue(newBalance)

        // Update Description in Column F (index 5)
        if (description.isNotEmpty()) {
            targetRow.getCellOrCreate(5).setCellValue(description)
        }
    }

    private fun applyDeletionToWorkbook(workbook: Workbook, sheetName: String, day: Int) {
        val sheet = workbook.getSheet(sheetName) ?: return
        val targetRow = findRowForDay(sheet, day) ?: return

        // Clear row data in Columns C, D, E and F
        // Column B (Date) remains untouched
        targetRow.getCell(2)?.setCellValue("")
        targetRow.getCell(3)?.setCellValue("")
        targetRow.getCell(4)?.setCellValue("")
        targetRow.getCell(5)?.setCellValue("")
    }

    private fun findPreviousBalance(sheet: org.apache.poi.ss.usermodel.Sheet, targetRowIndex: Int): Double {
        // 1. Search Column E (index 4) upwards from the row above targetRowIndex
        // starting from the targetRowIndex-1 down to index 4 (Row 5)
        for (i in (targetRowIndex - 1) downTo 4) {
            val row = sheet.getRow(i) ?: continue
            val cell = row.getCell(4) // Column E
            if (cell != null && cell.cellType != org.apache.poi.ss.usermodel.CellType.BLANK) {
                val value = getNumericValue(cell)
                if (value != 0.0 || cell.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                    return value
                }
            }
        }

        // 2. If no previous balance found in rows, fetch original starting value from E5 (Row 5, Col E)
        // Row 5 is index 4, Column E is index 4.
        val e5Row = sheet.getRow(4)
        val e5Cell = e5Row?.getCell(4)
        return getNumericValue(e5Cell)
    }

    private fun findRowForDay(sheet: org.apache.poi.ss.usermodel.Sheet, day: Int): org.apache.poi.ss.usermodel.Row? {
        // Search Column B (index 1) starting from B5 (index 4)
        for (i in 4 until sheet.lastRowNum + 1) {
            val row = sheet.getRow(i) ?: continue
            val dateCell = row.getCell(1) // Column B
            if (getNumericValue(dateCell).toInt() == day) {
                return row
            }
        }
        return null
    }

    private fun saveWorkbook(uri: Uri, workbook: Workbook) {
        val output = ByteArrayOutputStream()
        workbook.write(output)
        // Ensure workbook is closed before writing out the bytes
        // Although this doesn't strictly affect the 'output' stream which is already populated
        val bytes = output.toByteArray()
        contentResolver.openOutputStream(uri, "wt")?.use { 
            it.write(bytes)
            it.flush()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "Unknown.xlsx"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) result = cursor.getString(index)
            }
        }
        return result
    }

    private fun parseCustomerText(text: String): Pair<String, String> {
        val separator = text.lastIndexOf(" (")
        if (separator == -1) return Pair(text, "")
        val sheetName = text.substring(0, separator)
        val fileName = text.substring(separator + 2, text.length - 1)
        return Pair(sheetName, fileName)
    }

    private fun getNumericValue(cell: org.apache.poi.ss.usermodel.Cell?): Double {
        if (cell == null) return 0.0
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.toDoubleOrNull() ?: 0.0
            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                try {
                    cell.numericCellValue
                } catch (e: Exception) {
                    0.0
                }
            }
            else -> 0.0
        }
    }

    private fun getStringValue(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toInt().toString()
            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                try {
                    cell.stringCellValue
                } catch (e: Exception) {
                    cell.numericCellValue.toInt().toString()
                }
            }
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }
}

private fun org.apache.poi.ss.usermodel.Row.getCellOrCreate(index: Int): org.apache.poi.ss.usermodel.Cell {
    return getCell(index) ?: createCell(index)
}
