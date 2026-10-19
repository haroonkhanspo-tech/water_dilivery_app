package com.example.canetracker

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SyncRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun backupData(userId: String, history: List<HistoryEntry>, files: List<String>): Timestamp? {
        return try {
            val userDoc = db.collection("users").document(userId)
            val now = Timestamp.now()
            
            val data = hashMapOf(
                "history" to history.map { entry ->
                    hashMapOf(
                        "customer" to entry.customer,
                        "day" to entry.day,
                        "r" to entry.received,
                        "t" to entry.returned,
                        "d" to entry.description,
                        "fd" to entry.fullDate,
                        "b" to entry.balance
                    )
                },
                "files" to files,
                "lastUpdated" to now
            )

            userDoc.set(data, SetOptions.merge()).await()
            Log.d("SyncRepository", "Backup successful")
            now
        } catch (e: Exception) {
            Log.e("SyncRepository", "Backup failed", e)
            null
        }
    }

    suspend fun restoreData(userId: String): Triple<List<HistoryEntry>, List<String>, Timestamp?>? {
        return try {
            val document = db.collection("users").document(userId).get().await()
            if (document.exists()) {
                @Suppress("UNCHECKED_CAST")
                val historyData = document.get("history") as? List<Map<String, Any>>
                @Suppress("UNCHECKED_CAST")
                val files = document.get("files") as? List<String> ?: emptyList()
                val lastUpdated = document.getTimestamp("lastUpdated")

                val history = mutableListOf<HistoryEntry>()
                historyData?.forEach { obj ->
                    try {
                        history.add(
                            HistoryEntry(
                                customer = obj["customer"] as? String ?: "Unknown",
                                day = (obj["day"] as? Long ?: 0L).toInt(),
                                received = (obj["r"] as? Long ?: 0L).toInt(),
                                returned = (obj["t"] as? Long ?: 0L).toInt(),
                                description = obj["d"] as? String ?: "",
                                fullDate = obj["fd"] as? String ?: "",
                                balance = (obj["b"] as? Number)?.toDouble() ?: 0.0
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SyncRepository", "Skipping corrupted history entry", e)
                    }
                }

                Triple(history, files, lastUpdated)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Restore failed", e)
            null
        }
    }
}
