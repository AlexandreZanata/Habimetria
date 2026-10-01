package vault.local

// Guided shot categories. Coverage counts distinct applicable categories,
// never photo counts. Non-applicable categories are recorded explicitly
// per room (LocalRoom.naCategories as comma-separated names).
enum class ShotCategory {
    OVERVIEW, WALLS, OPENINGS, FLOOR, CEILING;

    companion object {
        fun all(): List<ShotCategory> = entries.toList()

        fun parse(raw: String?): ShotCategory? =
            raw?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}

fun parseNaCategories(raw: String): Set<ShotCategory> =
    raw.split(",").mapNotNull { ShotCategory.parse(it.trim().takeIf { s -> s.isNotEmpty() }) }.toSet()

fun applicableCategories(naCategories: String): List<ShotCategory> {
    val na = parseNaCategories(naCategories)
    return ShotCategory.all().filter { it !in na }
}
