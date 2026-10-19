package com.example.canetracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppState(private val context: Context) {

    private val prefs =
        context.getSharedPreferences("water_delivery_state", Context.MODE_PRIVATE)

    fun saveFiles(uris: List<String>) {
        val array = JSONArray()

        uris.forEach {
            array.put(it)
        }

        prefs.edit()
            .putString("files", array.toString())
            .apply()
    }

    fun loadFiles(): List<String> {

        val json = prefs.getString("files", null)
            ?: return emptyList()

        val array = JSONArray(json)

        val result = mutableListOf<String>()

        for (i in 0 until array.length()) {
            result.add(array.getString(i))
        }

        return result
    }

    fun saveHistory(history: List<HistoryEntry>) {

        val array = JSONArray()

        history.forEach { entry ->

            val objectJson = JSONObject()

            objectJson.put("customer", entry.customer)
            objectJson.put("day", entry.day)
            objectJson.put("r", entry.received)
            objectJson.put("t", entry.returned)
            objectJson.put("d", entry.description)
            objectJson.put("fd", entry.fullDate)
            objectJson.put("b", entry.balance)

            array.put(objectJson)
        }

        prefs.edit()
            .putString("history", array.toString())
            .apply()
    }

    fun saveThemeMode(mode: Int) {
        prefs.edit()
            .putInt("theme_mode", mode)
            .apply()
    }

    fun loadThemeMode(): Int {
        return prefs.getInt("theme_mode", 0) // 0 = System, 1 = Light, 2 = Dark
    }

    fun loadHistory(): MutableList<HistoryEntry> {

        val json = prefs.getString("history", null)
            ?: return mutableListOf()

        val array = JSONArray(json)

        val result = mutableListOf<HistoryEntry>()

        for (i in 0 until array.length()) {

            val obj = array.getJSONObject(i)

            result.add(
                HistoryEntry(
                    customer = obj.getString("customer"),
                    day = obj.getInt("day"),
                    received = obj.getInt("r"),
                    returned = obj.getInt("t"),
                    description = obj.optString("d", ""),
                    fullDate = obj.optString("fd", ""),
                    balance = obj.optDouble("b", 0.0)
                )
            )
        }

        return result
    }

    fun reset() {

        prefs.edit()
            .clear()
            .apply()
    }
}