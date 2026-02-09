package com.julian.dixmille.data.source

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AndroidLocalStorage(context: Context) : LocalStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "dixmille_prefs",
        Context.MODE_PRIVATE
    )
    
    override fun saveString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }
    
    override fun getString(key: String): String? {
        return prefs.getString(key, null)
    }
    
    override fun remove(key: String) {
        prefs.edit { remove(key) }
    }
    
    override fun clear() {
        prefs.edit { clear() }
    }
}
