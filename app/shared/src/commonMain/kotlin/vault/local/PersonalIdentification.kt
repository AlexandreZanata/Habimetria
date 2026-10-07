package vault.local

import kotlinx.serialization.Serializable

@Serializable
data class PersonalIdentification(
    val cpf: String = "", val email: String = "", val phone: String = "",
    val exposeCpf: Boolean = false,
) {
    fun validate() {
        require(cpf.isBlank() || validCpf(cpf)) { "invalid CPF check digits" }
        require(email.length <= 254 && (email.isBlank() || Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email))) { "invalid email" }
        require(phone.length <= 40) { "phone too long" }
    }
    fun displayedCpf(): String {
        val digits = cpf.filter(Char::isDigit)
        if (digits.isEmpty()) return ""
        return if (exposeCpf) cpf else "***.***.***-${digits.takeLast(2)}"
    }
}

fun validCpf(raw: String): Boolean {
    if (raw.any { !it.isDigit() && it !in ".- " }) return false
    val digits = raw.filter(Char::isDigit)
    if (digits.length != 11 || digits.toSet().size == 1 || digits.any { it !in '0'..'9' }) return false
    fun check(length: Int): Int {
        val result = (0 until length).sumOf { digits[it].digitToInt() * (length + 1 - it) } * 10 % 11
        return if (result == 10) 0 else result
    }
    return check(9) == digits[9].digitToInt() && check(10) == digits[10].digitToInt()
}
