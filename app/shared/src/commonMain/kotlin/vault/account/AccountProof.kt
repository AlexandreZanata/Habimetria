package vault.account


/** Extract only the purpose-specific opaque proof; never send an entire link. */
fun accountProof(input: String, purpose: String): String {
    val trimmed = input.trim()
    val proof = if ('#' in trimmed) {
        val fragment = trimmed.substringAfter('#')
        if (!fragment.startsWith("$purpose=")) throw SessionFailure("invalid_request")
        fragment.removePrefix("$purpose=")
    } else trimmed
    if (!proof.matches(Regex("[A-Za-z0-9_-]{43}"))) throw SessionFailure("invalid_request")
    return proof
}
