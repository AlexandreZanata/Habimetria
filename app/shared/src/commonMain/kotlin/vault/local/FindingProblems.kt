package vault.local

import vault.findings.AppLocale
import vault.findings.FindingPreset
import vault.findings.label
import vault.local.db.LocalFindingProblem
import vault.local.db.VaultDatabase

const val MAX_PROBLEM_LABEL = 40

// User-customizable finding problems. Built-ins seed lazily with stable
// enum-name codes and pt-BR labels; customs carry "custom:<id>" codes.
// Findings keep storing codes, so sealed bytes stay stable while labels
// and availability evolve. At least one problem stays active.
data class ProblemOption(
    val code: String,
    val label: String,
    val active: Boolean,
    val builtin: Boolean,
)

class FindingProblemRepository(private val db: VaultDatabase) {
    suspend fun ensureSeeded() {
        val dao = db.findingProblemDao()
        for (preset in FindingPreset.entries) {
            dao.insert(
                LocalFindingProblem(
                    code = preset.name,
                    label = preset.label(AppLocale.PT),
                    active = true,
                    builtin = true,
                ),
            )
        }
    }

    suspend fun listAll(): List<ProblemOption> {
        ensureSeeded()
        return db.findingProblemDao().listAll().map { it.toOption() }
    }

    suspend fun listActive(): List<ProblemOption> {
        ensureSeeded()
        return db.findingProblemDao().listActive().map { it.toOption() }
    }

    suspend fun setActive(code: String, active: Boolean) {
        ensureSeeded()
        if (!active && db.findingProblemDao().listActive().none { it.code != code }) {
            throw LocalValidationException("at least one problem stays active")
        }
        if (db.findingProblemDao().setActive(code, active) == 0) {
            throw LocalValidationException("problem not found")
        }
    }

    suspend fun addCustom(label: String): ProblemOption {
        ensureSeeded()
        val clean = label.trim().take(MAX_PROBLEM_LABEL)
        if (clean.isEmpty()) throw LocalValidationException("problem label required")
        val row = LocalFindingProblem(
            code = "custom:${newLocalId("prob")}",
            label = clean,
            active = true,
            builtin = false,
        )
        db.findingProblemDao().insert(row)
        return row.toOption()
    }

    suspend fun deleteCustom(code: String) {
        ensureSeeded()
        if (db.findingProblemDao().deleteCustom(code) == 0) {
            throw LocalValidationException("custom problem not found")
        }
    }

    suspend fun resolveCode(code: String): ProblemOption? {
        ensureSeeded()
        return db.findingProblemDao().get(code)?.toOption()
    }

    private fun LocalFindingProblem.toOption() =
        ProblemOption(code = code, label = label, active = active, builtin = builtin)
}
