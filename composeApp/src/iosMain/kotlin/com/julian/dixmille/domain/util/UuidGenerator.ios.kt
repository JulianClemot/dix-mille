package com.julian.dixmille.core.domain.util

import platform.Foundation.NSUUID

actual object UuidGenerator {
    actual fun generate(): String = NSUUID().UUIDString()
}
