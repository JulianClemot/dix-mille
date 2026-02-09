package com.julian.dixmille.domain.util

/**
 * Generates unique identifiers for domain entities.
 */
expect object UuidGenerator {
    /**
     * Generates a new random UUID string.
     */
    fun generate(): String
}
