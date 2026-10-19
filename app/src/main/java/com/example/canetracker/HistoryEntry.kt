package com.example.canetracker

data class HistoryEntry(
    val customer: String,
    val day: Int,
    val received: Int,
    val returned: Int,
    val description: String = "",
    val fullDate: String = "",
    val balance: Double = 0.0
)
