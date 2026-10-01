package vault.ui.property

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
