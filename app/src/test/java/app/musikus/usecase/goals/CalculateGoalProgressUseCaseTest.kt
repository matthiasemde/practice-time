package app.musikus.usecase.goals

import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalInstanceCreationAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalProgressType
import app.musikus.database.entities.GoalType
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.database.entities.SectionCreationAttributes
import app.musikus.database.entities.SessionCreationAttributes
import app.musikus.repository.FakeGoalRepository
import app.musikus.repository.FakeLibraryRepository
import app.musikus.repository.FakeSessionRepository
import app.musikus.usecase.sessions.GetSessionsInTimeframeUseCase
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class CalculateGoalProgressUseCaseTest {


    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository

    /** SUT */
    private lateinit var calculateGoalProgress: CalculateGoalProgressUseCase

    @BeforeEach
    fun setUp() {

        val fakeTimeProvider = FakeTimeProvider()
        val fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeSessionRepository = FakeSessionRepository(
            fakeLibraryRepository,
            fakeTimeProvider,
            fakeIdProvider
        )

        fakeGoalRepository = FakeGoalRepository(
            fakeLibraryRepository,
            fakeTimeProvider,
            fakeIdProvider
        )

        val getSessionsInTimeframe = GetSessionsInTimeframeUseCase(fakeSessionRepository)

        /** SUT */
        calculateGoalProgress = CalculateGoalProgressUseCase(
            getSessionsInTimeframe,
            timeProvider = fakeTimeProvider
        )


        // Set up test data
        runBlocking {
            fakeLibraryRepository.addItem(LibraryItemCreationAttributes(
                name = "Test Item 1",
                colorIndex = 5,
                libraryFolderId = Nullable(null)
            ))
        }
    }

    @Test
    fun `calculate progress for non-specific goal`() = runTest {
        fakeGoalRepository.addNewGoal(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.NON_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                descriptionId = UUIDConverter.fromInt(2),
                previousInstanceId = null,
                startTimestamp = FakeTimeProvider.START_TIME,
                target = 1.hours
            ),
            libraryItemIds = null
        )

        // add a session during the goals active period
        fakeSessionRepository.add(
            sessionCreationAttributes = SessionCreationAttributes(
                rating = 3,
                breakDuration = 10.minutes,
                comment = "Test comment"
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        2.days.toJavaDuration() // section should still be counted towards the goal because the session started during the goals runtime
                    ),
                    duration = 1.minutes
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME,
                    duration = 2.minutes
                ),
            )
        )

        // add a session after the goal instance has ended
        fakeSessionRepository.add(
            sessionCreationAttributes = SessionCreationAttributes(
                rating = 3,
                breakDuration = 10.minutes,
                comment = "Test comment"
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME.plus(
                        1.days.toJavaDuration()
                    ),
                    duration = 10.minutes
                )
            )
        )

        val goals = fakeGoalRepository.allGoals.first()

        val goalProgress = calculateGoalProgress(goals = goals).first()

        assertThat(goalProgress).isEqualTo(listOf(listOf(3.minutes)))
    }

    @Test
    fun `calculate progress for item-specific goal`() = runTest {
        fakeLibraryRepository.addItem(LibraryItemCreationAttributes(
            name = "Test Item 2",
            colorIndex = 5,
            libraryFolderId = Nullable(null)
        ))

        fakeGoalRepository.addNewGoal(
            descriptionCreationAttributes = GoalDescriptionCreationAttributes(
                type = GoalType.ITEM_SPECIFIC,
                repeat = true,
                periodInPeriodUnits = 1,
                periodUnit = GoalPeriodUnit.DAY,
                progressType = GoalProgressType.TIME,
            ),
            instanceCreationAttributes = GoalInstanceCreationAttributes(
                descriptionId = UUIDConverter.fromInt(2),
                previousInstanceId = null,
                startTimestamp = FakeTimeProvider.START_TIME,
                target = 1.hours
            ),
            libraryItemIds = listOf(
                UUIDConverter.fromInt(1),
            )
        )

        // add a session during the goals active period
        fakeSessionRepository.add(
            sessionCreationAttributes = SessionCreationAttributes(
                rating = 3,
                breakDuration = 10.minutes,
                comment = "Test comment"
            ),
            sectionCreationAttributes = listOf(
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(1),
                    startTimestamp = FakeTimeProvider.START_TIME,
                    duration = 1.minutes
                ),
                SectionCreationAttributes(
                    libraryItemId = UUIDConverter.fromInt(2),
                    startTimestamp = FakeTimeProvider.START_TIME,
                    duration = 2.minutes
                ),
            )
        )

        val goals = fakeGoalRepository.allGoals.first()

        val goalProgress = calculateGoalProgress(goals = goals).first()

        assertThat(goalProgress).isEqualTo(listOf(listOf(1.minutes)))
    }
}