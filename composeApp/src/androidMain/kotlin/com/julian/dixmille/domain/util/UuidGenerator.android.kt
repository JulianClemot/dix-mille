package com.julian.dixmille.domain.util

import java.util.UUID

actual object UuidGenerator {
    actual fun generate(): String = UUID.randomUUID().toString()
}
