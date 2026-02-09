package com.julian.dixmille.data.source

import platform.Foundation.NSUserDefaults

class IOSLocalStorage : LocalStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    override fun saveString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }
    
    override fun getString(key: String): String? {
        return userDefaults.stringForKey(key)
    }
    
    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }
    
    override fun clear() {
        // Clear all keys for this app
        val domain = platform.Foundation.NSBundle.mainBundle.bundleIdentifier
        if (domain != null) {
            userDefaults.removePersistentDomainForName(domain)
        }
    }
}
