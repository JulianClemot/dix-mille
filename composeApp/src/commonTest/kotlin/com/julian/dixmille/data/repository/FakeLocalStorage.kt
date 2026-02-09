package com.julian.dixmille.data.repository

import com.julian.dixmille.data.source.LocalStorage

/**
 * Fake implementation of LocalStorage for testing.
 */
class FakeLocalStorage : LocalStorage {
    private val storage = mutableMapOf<String, String>()
    
    override fun saveString(key: String, value: String) {
        storage[key] = value
    }
    
    override fun getString(key: String): String? {
        return storage[key]
    }
    
    override fun remove(key: String) {
        storage.remove(key)
    }
    
    override fun clear() {
        storage.clear()
    }
    
    fun getAll(): Map<String, String> = storage.toMap()
}
