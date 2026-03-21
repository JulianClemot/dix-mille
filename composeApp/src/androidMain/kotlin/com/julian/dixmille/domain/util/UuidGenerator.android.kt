package com.julian.dixmille.core.domain.util

import java.util.UUID

actual object UuidGenerator {
    actual fun generate(): String = UUID.randomUUID().toString()
}
