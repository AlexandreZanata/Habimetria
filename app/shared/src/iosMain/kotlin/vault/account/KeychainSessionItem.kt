@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package vault.account

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRetain
import platform.Security.*
import platform.posix.memcpy

/** App access-group default, device-only, non-synchronizing generic password. */
internal class KeychainSessionItem {
    fun read(): ByteArray? = query(listOf(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)) { q ->
        memScoped {
            val result = alloc<CFTypeRefVar>()
            result.value = null
            val status = SecItemCopyMatching(q, result.ptr)
            val data: CFDataRef? = result.value?.reinterpret()
            try {
                if (status == errSecItemNotFound) return@memScoped null
                checked(status)
                if (data == null) throw SessionFailure("storage_unavailable")
                val length = CFDataGetLength(data)
                if (length !in 1..8192) throw SessionFailure("storage_unavailable")
                val source = CFDataGetBytePtr(data) ?: throw SessionFailure("storage_unavailable")
                ByteArray(length.toInt()).apply { usePinned { memcpy(it.addressOf(0), source, size.convert()) } }
            } finally { data?.let { CFRelease(it) } }
        }
    }
    fun write(bytes: ByteArray) {
        if (bytes.size !in 1..8192) throw SessionFailure("storage_unavailable")
        bytes.usePinned { pinned ->
            val data = CFDataCreate(kCFAllocatorDefault, pinned.addressOf(0).reinterpret(), bytes.size.convert())
                ?: throw SessionFailure("storage_unavailable")
            val attrs = dictionary(kSecValueData to data, kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            try {
                val status = query { SecItemUpdate(it, attrs) }
                if (status == errSecItemNotFound) {
                    query(listOf(kSecValueData to data, kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly)) {
                        checked(SecItemAdd(it, null))
                    }
                } else checked(status)
            } finally { CFRelease(attrs); CFRelease(data) }
        }
    }
    fun delete() = query { q ->
        val status = SecItemDelete(q)
        if (status != errSecItemNotFound) checked(status)
    }
    private fun <T> query(extra: List<Pair<Any?, Any?>> = emptyList(), action: (CFDictionaryRef) -> T): T {
        val service = CFBridgingRetain("habimetria.account.v1")
        val account = CFBridgingRetain("native-session")
        try {
            val q = dictionary(*(listOf(kSecClass to kSecClassGenericPassword,
                kSecAttrService to service, kSecAttrAccount to account,
                kSecAttrSynchronizable to kCFBooleanFalse) + extra).toTypedArray())
            try { return action(q) } finally { CFRelease(q) }
        } finally { CFRelease(service); CFRelease(account) }
    }
    private fun checked(status: Int) {
        if (status != errSecSuccess) throw SessionFailure(
            if (status == errSecInteractionNotAllowed) "storage_locked" else "storage_unavailable")
    }
    private fun dictionary(vararg pairs: Pair<Any?, Any?>): CFDictionaryRef = memScoped {
        val keys = allocArrayOf(*Array(pairs.size) { pairs[it].first as CPointer<*>? })
        val values = allocArrayOf(*Array(pairs.size) { pairs[it].second as CPointer<*>? })
        CFDictionaryCreate(null, keys.reinterpret(), values.reinterpret(), pairs.size.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
            ?: throw SessionFailure("storage_unavailable")
    }
}
