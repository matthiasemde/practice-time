package app.musikus.repository

import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.SessionWithSectionsWithLibraryItems
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.daos.LibraryItem
import app.musikus.database.daos.Session
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.database.entities.SessionModel
import app.musikus.utils.Timeframe
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.util.UUID

class SessionRepository(
    database: MusikusDatabase
) {
    private val sessionDao = database.sessionDao
    private val sectionDao = database.sectionDao


    /** Accessors */
    val sessions = sessionDao.getAllAsFlow()
    val sections = sectionDao.getAllAsFlow()

    val sessionsWithSectionsWithLibraryItems = sessionDao.getAllWithSectionsWithLibraryItemsAsFlow()
    fun sessionWithSectionsWithLibraryItems(id: UUID) = sessionDao.getWithSectionsWithLibraryItemsAsFlow(id)

    fun sessionsInTimeframe (timeframe: Timeframe) : Flow<List<SessionWithSectionsWithLibraryItems>> {
        assert (timeframe.first < timeframe.second)
        return sessionDao.get(
            startTimestamp = timeframe.first,
            endTimestamp = timeframe.second
        )
    }

    fun sectionsForGoal (
        startTimestamp: ZonedDateTime,
        endTimestamp: ZonedDateTime,
        itemIds: List<UUID>? = null
    ) = if (itemIds == null) sectionDao.get(
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
    ) else sectionDao.get(
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        itemIds = itemIds
    )

    fun sectionsForGoal (goal: GoalInstanceWithDescriptionWithLibraryItems) = sectionsForGoal(
        startTimestamp = goal.instance.startTimestamp,
        endTimestamp = goal.endTimestampInLocalTimezone,
        itemIds = goal.description.libraryItems.map { it.id }.takeIf { it.isNotEmpty() }
    )

    fun sectionsForGoal(
        instance: GoalInstance,
        description: GoalDescription,
        libraryItems: List<LibraryItem>
    ) = sectionsForGoal(
        startTimestamp = instance.startTimestamp,
        endTimestamp = description.endOfInstanceInLocalTimezone(instance),
        itemIds = libraryItems.map { it.id }.takeIf { it.isNotEmpty() }
    )

    /** Mutators */
    /** Add */
    suspend fun add(
        session: SessionCreationAttributes,
        sections: List<SectionCreationAttributes>
    ) : UUID {
        val newSession = SessionModel(
            breakDuration = session.breakDuration,
            rating = session.rating,
            comment = session.comment,
        )
        sessionDao.insert(newSession, sections)
        return newSession.id
    }

    /** Delete / Restore */
    suspend fun delete(sessions: List<Session>) {
        sessionDao.delete(sessions.map { it.id })
    }

    suspend fun restore(sessions: List<Session>) {
        sessionDao.restore(sessions.map { it.id })
    }

    /** Clean */
    suspend fun clean() {
        sessionDao.clean()
    }
}