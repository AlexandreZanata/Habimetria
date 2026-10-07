package vault.ui.property

import vault.local.LocalSession
import vault.local.NavigationProgress
import vault.local.formatAddress
import vault.ui.AppContainer

// Shared creation helpers: Home and Imóveis start inspections the same
// way. Navigation progress persists before onReady so restart resumes.
suspend fun startOnProperty(
    container: AppContainer,
    propertyId: String,
    onReady: (inspectionId: String) -> Unit,
) {
    val session = container.local ?: return
    val ten = session.repository.createTenancy(propertyId)
    val ins = session.repository.createInspection(ten.id)
    session.navigation.save(NavigationProgress(ins.id))
    onReady(ins.id)
}

suspend fun createPropertyAndStart(
    container: AppContainer,
    nickname: String,
    street: String,
    number: String,
    complement: String,
    neighborhood: String,
    onReady: (inspectionId: String) -> Unit,
) {
    val session = container.local
    if (session != null) {
        val prop = session.repository.createPropertyStructured(
            street = street,
            number = number,
            complement = complement.ifBlank { null },
            neighborhood = neighborhood,
            nickname = nickname,
        )
        val ten = session.repository.createTenancy(prop.id)
        val ins = session.repository.createInspection(ten.id)
        session.navigation.save(NavigationProgress(ins.id))
        onReady(ins.id)
    } else {
        val line = formatAddress(street.trim(), number.trim(), complement.trim(), neighborhood.trim())
        val prop = container.api.createProperty(line)
        val ten = container.api.createTenancy(prop.id)
        val ins = container.api.createInspection(ten.id)
        container.api.ensureTemplate(ins.id)
        onReady(ins.id)
    }
}

// After a committed deletion, drop only stale navigation references to the
// removed Property or to inspections that no longer exist. Unrelated
// Inspection progress is preserved; DB and progress.json stay separate.
suspend fun revalidateNavigationAfterDelete(session: LocalSession, deletedPropertyId: String) {
    val progress = session.navigation.load()
    val inspectionId = progress.inspectionId
    val inspectionGone = inspectionId != null && session.repository.getInspection(inspectionId) == null
    val propertyId = progress.propertyId
    val propertyGone = propertyId != null &&
        (propertyId == deletedPropertyId || session.repository.getProperty(propertyId) == null)
    if (!inspectionGone && !propertyGone) return
    session.navigation.save(
        progress.copy(
            inspectionId = inspectionId.takeUnless { inspectionGone },
            roomId = progress.roomId.takeUnless { inspectionGone },
            roomLabel = progress.roomLabel.takeUnless { inspectionGone },
            propertyId = propertyId.takeUnless { propertyGone },
        ),
    )
}
