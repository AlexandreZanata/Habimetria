package vault.nav

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.LocalSession
import vault.local.NavigationProgress
import vault.local.STATUS_SEALED
import vault.local.SyncMode
import vault.local.openLocalSession
import kotlin.test.Test
import kotlin.test.assertEquals

private var restoreSeq = 0

private fun freshSession(): Pair<String, LocalSession> {
    restoreSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-restore-$restoreSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    return dir to openLocalSession(dir, SyncMode.LOCAL_ONLY)
}

class RestoreValidationTest {

    @Test
    fun missingInspectionFallsBackToRoot() = runTest {
        val (_, session) = freshSession()
        session.navigation.save(NavigationProgress("ins_gone", "room_gone", "Sala"))
        assertEquals(listOf(Route.Home), validatedTrail(session))
        session.close()
    }

    @Test
    fun sealedInspectionRestoresReadOnlyReview() = runTest {
        val (_, session) = freshSession()
        val repo = session.repository
        val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua A, 1").id).id)
        session.navigation.save(NavigationProgress(inspection.id))
        session.db.inspectionDao().update(inspection.copy(status = STATUS_SEALED))
        assertEquals(listOf(Route.Home, Route.Review(inspection.id)), validatedTrail(session))
        session.close()
    }

    @Test
    fun deletedPropertyDetailFallsBackToList() = runTest {
        val (_, session) = freshSession()
        val prop = session.repository.createProperty("Rua B, 2")
        session.navigation.save(NavigationProgress(propertyId = prop.id))
        session.propertyRemoval.deleteEmpty(prop.id)
        assertEquals(listOf(Route.Properties), validatedTrail(session))
        session.close()
    }

    @Test
    fun archivedPropertyDetailStaysReachable() = runTest {
        val (_, session) = freshSession()
        val repo = session.repository
        val prop = repo.createProperty("Rua C, 3")
        repo.createTenancy(prop.id)
        session.propertyRemoval.archive(prop.id)
        session.navigation.save(NavigationProgress(propertyId = prop.id))
        assertEquals(
            listOf(Route.Properties, Route.PropertyDetail(prop.id)),
            validatedTrail(session),
        )
        session.close()
    }

    @Test
    fun staleRoomDropsToRoomsListAndDraftTrailSurvives() = runTest {
        val (_, session) = freshSession()
        val repo = session.repository
        val inspection = repo.createInspection(repo.createTenancy(repo.createProperty("Rua D, 4").id).id).id
        val room = repo.addRoom(inspection, "Sala")
        session.navigation.save(NavigationProgress(inspection, room.id, room.label))
        assertEquals(
            listOf(Route.Home, Route.Rooms(inspection), Route.Room(inspection, room.id, room.label)),
            validatedTrail(session),
        )
        repo.deleteRoom(room.id)
        assertEquals(listOf(Route.Home, Route.Rooms(inspection)), validatedTrail(session))
        session.close()
    }
}
