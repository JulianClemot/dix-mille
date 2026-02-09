package com.julian.dixmille.data.source

/**
 * Platform-specific local storage interface.
 * 
 * Provides simple key-value storage using platform-native mechanisms:
 * - Android: SharedPreferences
 * - iOS: UserDefaults
 */
interface LocalStorage {
    /**
     * Saves a string value for the given key.
     */
    fun saveString(key: String, value: String)
    
    /**
     * Retrieves a string value for the given key.
     * 
     * @return The value if found, null otherwise
     */
    fun getString(key: String): String?
    
    /**
     * Removes the value for the given key.
     */
    fun remove(key: String)
    
    /**
     * Clears all stored values.
     */
    fun clear()
}
