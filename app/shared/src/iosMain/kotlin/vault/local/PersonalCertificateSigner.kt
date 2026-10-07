package vault.local

// PKCS#12 personal signing is an Android/JVM capability in this delivery.
// Installation verification remains available; certificate packages use
// Android or the independent Go verifier for the additional signature check.
actual fun personalCertificateSigner(): PersonalCertificateSigner? = null
