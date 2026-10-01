package vault.local

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemCopyMatching
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyRef
import platform.Security.SecKeyVerifySignature
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAccessControlPrivateKeyUsage
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrTokenID
import platform.Security.kSecAttrTokenIDSecureEnclave
import platform.Security.kSecClass
import platform.Security.kSecClassKey
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecReturnRef
import platform.posix.int32_tVar
import platform.posix.memcpy

// Keychain installation key: Secure Enclave when the device offers it,
// software keychain otherwise (validation-only, labeled as such). The
// private key never leaves the store; only raw R||S signatures cross.
actual fun platformSigner(filesDir: String): InstallationSigner = IosInstallationSigner()

private const val KEY_TAG = "vault-seal-1"

@OptIn(ExperimentalForeignApi::class)
class IosInstallationSigner : InstallationSigner {
    private var enclave = false
    private val privateKey: SecKeyRef = findKey() ?: createKey()

    override val hardwareBacked: Boolean get() = enclave
    override val validationOnly: Boolean get() = !hardwareBacked

    override val keyId: String get() = keyFingerprint(publicKeyUncompressed())

    override fun publicKeyUncompressed(): ByteArray {
        val public = SecKeyCopyPublicKey(privateKey) ?: throw LocalStoreException("no public key")
        try {
            val exported = SecKeyCopyExternalRepresentation(public, null)
                ?: throw LocalStoreException("public export failed")
            try {
                val bytes = (CFBridgingRelease(exported) as NSData).toByteArray()
                require(bytes.size == 65 && bytes[0] == 0x04.toByte()) { "unexpected key encoding" }
                return bytes
            } finally {
                CFRelease(public)
            }
        } finally {
            CFRelease(public)
        }
    }

    override fun sign(bytes: ByteArray): ByteArray = memScoped {
        val data = bytes.toCFData() ?: throw LocalStoreException("encode failed")
        try {
            val error = alloc<CFErrorRefVar>()
            val signed = SecKeyCreateSignature(
                privateKey,
                kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
                data,
                error.ptr,
            ) ?: throw LocalStoreException("sign failed")
            try {
                val raw = signed.toByteArray()
                require(raw.size == 64) { "unexpected signature size" }
                raw
            } finally {
                CFRelease(signed)
            }
        } finally {
            CFRelease(data)
        }
    }

    override fun verify(publicKeyUncompressed: ByteArray, bytes: ByteArray, signature: ByteArray): Boolean {
        if (signature.size != 64) return false
        return try {
            keyFingerprint(publicKeyUncompressed)
            memScoped {
                val error = alloc<CFErrorRefVar>()
                val public = importPublic(publicKeyUncompressed)
                val data = bytes.toCFData() ?: return false
                try {
                    val sig = signature.toCFData() ?: return false
                    try {
                        SecKeyVerifySignature(public, kSecKeyAlgorithmECDSASignatureMessageX962SHA256, data, sig, error.ptr)
                    } finally {
                        CFRelease(sig)
                    }
                } finally {
                    CFRelease(data)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun findKey(): SecKeyRef? = memScoped {
        val tag = KEY_TAG.encodeToByteArray().toCFData() ?: return null
        try {
            val query = cfDictionary(
                kSecClass to kSecClassKey,
                kSecAttrApplicationTag to tag,
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecReturnRef to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne,
            )
            try {
                val result = alloc<CFTypeRefVar>()
                when (SecItemCopyMatching(query, result.ptr)) {
                    errSecSuccess -> result.value?.reinterpret()
                    errSecItemNotFound -> null
                    else -> null
                }
            } finally {
                CFRelease(query)
            }
        } finally {
            CFRelease(tag)
        }
    }

    private fun createKey(): SecKeyRef =
        createKeyWithEnclave(true) ?: createKeyWithEnclave(false)
        ?: throw LocalStoreException("key creation failed")

    private fun createKeyWithEnclave(useEnclave: Boolean): SecKeyRef? = memScoped {
        val error = alloc<CFErrorRefVar>()
        val tag = KEY_TAG.encodeToByteArray().toCFData() ?: return null
        try {
            val privateAttrsPairs = mutableListOf(
                kSecAttrIsPermanent to kCFBooleanTrue,
                kSecAttrApplicationTag to tag,
            )
            if (useEnclave) {
                val access = SecAccessControlCreateWithFlags(
                    kCFAllocatorDefault,
                    kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    kSecAccessControlPrivateKeyUsage,
                    error.ptr,
                ) ?: return null
                try {
                    privateAttrsPairs.add(kSecAttrAccessControl to access)
                    buildKey(privateAttrsPairs, useEnclave, tag)
                } finally {
                    CFRelease(access)
                }
            } else {
                privateAttrsPairs.add(
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                )
                buildKey(privateAttrsPairs, useEnclave, tag)
            }
        } finally {
            CFRelease(tag)
        }
    }

    private fun buildKey(
        privateAttrsPairs: List<Pair<Any?, Any?>>,
        useEnclave: Boolean,
        tag: CFDataRef,
    ): SecKeyRef? = memScoped {
        val keySize = CFBridgingRetain(NSNumber(int = 256))
        val privateAttrs = cfDictionary(*privateAttrsPairs.toTypedArray())
        try {
            val pairs = mutableListOf(
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeySizeInBits to keySize,
                kSecPrivateKeyAttrs to privateAttrs,
            )
            if (useEnclave) pairs.add(kSecAttrTokenID to kSecAttrTokenIDSecureEnclave)
            val attrs = cfDictionary(*pairs.toTypedArray())
            try {
                val error = alloc<CFErrorRefVar>()
                val created = SecKeyCreateRandomKey(attrs, error.ptr)
                if (created == null) {
                    error.value?.let { CFRelease(it) }
                    return null
                }
                if (useEnclave) enclave = true
                created
            } finally {
                CFRelease(attrs)
            }
        } finally {
            CFRelease(privateAttrs)
            CFRelease(keySize)
        }
    }

    private fun importPublic(uncompressed: ByteArray): SecKeyRef = memScoped {
        val attrs = cfDictionary(
            kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits to CFBridgingRetain(NSNumber(int = 256)),
        )
        try {
            val error = alloc<CFErrorRefVar>()
            val data = uncompressed.toCFData()
                ?: throw LocalStoreException("public import failed")
            try {
                SecKeyCreateWithData(data, attrs, error.ptr)
                    ?: throw LocalStoreException("public import failed")
            } finally {
                CFRelease(data)
            }
        } finally {
            CFRelease(attrs)
        }
    }

    private fun cfDictionary(vararg pairs: Pair<Any?, Any?>): CFDictionaryRef = memScoped {
        val keys = allocArrayOf(*Array(pairs.size) { i -> pairs[i].first as CPointer<*>? })
        val values = allocArrayOf(*Array(pairs.size) { i -> pairs[i].second as CPointer<*>? })
        CFDictionaryCreate(
            null,
            keys.reinterpret(),
            values.reinterpret(),
            pairs.size.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: error("CFDictionaryCreate returned null")
    }

    private fun ByteArray.toCFData(): CFDataRef? {
        if (isEmpty()) return null
        return usePinned {
            CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), size.convert())
        }
    }

    private fun CFDataRef.toByteArray(): ByteArray {
        val length = CFDataGetLength(this).toInt()
        val bytes = CFDataGetBytePtr(this) ?: return ByteArray(0)
        if (length == 0) return ByteArray(0)
        return ByteArray(length).apply {
            usePinned { memcpy(it.addressOf(0), bytes, length.convert()) }
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        if (length == 0) return ByteArray(0)
        val ptr = this.bytes?.reinterpret<ByteVar>() ?: return ByteArray(0)
        return ByteArray(length).apply {
            usePinned { memcpy(it.addressOf(0), ptr, length.convert()) }
        }
    }
}
