package com.julian.dixmille.domain.util

import platform.Foundation.NSUUID

actual object UuidGenerator {
    actual fun generate(): String = NSUUID().UUIDString()
}
