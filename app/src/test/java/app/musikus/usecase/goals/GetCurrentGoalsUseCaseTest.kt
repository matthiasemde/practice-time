/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.usecase.goals

import app.musikus.database.GoalDescriptionWithLibraryItems
import app.musikus.database.Nullable
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.GoalDescription
import app.musikus.database.daos.GoalInstance
import app.musikus.database.entities.GoalDescriptionCreationAttributes
import app.musikus.database.entities.GoalDescriptionUpdateAttributes
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
import app.musikus.repository.FakeUserPreferencesRepository
import app.musikus.usecase.sessions.GetSessionsInTimeframeUseCase
import app.musikus.usecase.userpreferences.GetGoalSortInfoUseCase
import app.musikus.utils.FakeIdProvider
import app.musikus.utils.FakeTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class GetCurrentGoalsUseCaseTest {
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeIdProvider: FakeIdProvider

    private lateinit var fakeLibraryRepository: FakeLibraryRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    private lateinit var getSessionInTimeframe: GetSessionsInTimeframeUseCase
    private lateinit var getGoalSortInfo: GetGoalSortInfoUseCase
    private lateinit var sortGoals: SortGoalsUseCase

    private lateinit var calculateGoalProgress: CalculateGoalProgressUseCase

    /** SUT */
    private lateinit var getCurrentGoals: GetCurrentGoalsUseCase

    private val baseDescription = GoalDescriptionCreationAttributes(
        type = GoalType.NON_SPECIFIC,
        repeat = true,
        periodInPeriodUnits = 1,
        periodUnit = GoalPeriodUnit.DAY,
        progressType = GoalProgressType.TIME,
    )

    private val baseInstance = GoalInstanceCreationAttributes(
        descriptionId = UUIDConverter.fromInt(1),
        previousInstanceId = null,
        startTimestamp = FakeTimeProvider.START_TIME,
        target = 1.hours
    )

    @BeforeEach
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeIdProvider = FakeIdProvider()

        fakeLibraryRepository = FakeLibraryRepository(fakeTimeProvider, fakeIdProvider)
        fakeGoalRepository = FakeGoalRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)
        fakeSessionRepository = FakeSessionRepository(fakeLibraryRepository, fakeTimeProvider, fakeIdProvider)
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()

        getSessionInTimeframe = GetSessionsInTimeframeUseCase(fakeSessionRepository)
        getGoalSortInfo = GetGoalSortInfoUseCase(fakeUserPreferencesRepository)
        sortGoals = SortGoalsUseCase(getGoalSortInfo)

        calculateGoalProgress = CalculateGoalProgressUseCase(
            getSessionsInTimeframe = getSessionInTimeframe,
            timeProvider = fakeTimeProvider,
        )

        /** SUT */
        getCurrentGoals = GetCurrentGoalsUseCase(
            goalRepository = fakeGoalRepository,
            sortGoals = sortGoals,
            calculateProgress = calculateGoalProgress,
        )

        runBlocking {
            fakeLibraryRepository.addItem(
                LibraryItemCreationAttributes(
                    name = "Test item",
                    colorIndex = 5,
                    libraryFolderId = Nullable(null)
                )
            )

            fakeGoalRepository.addNewGoal(
                descriptionCreationAttributes = baseDescription,
                instanceCreationAttributes = baseInstance,
                libraryItemIds = null
            )

            fakeGoalRepository.updateGoalDescriptions(listOf(
                UUIDConverter.fromInt(2) to
                GoalDescriptionUpdateAttributes(paused = true)
            ))

            fakeSessionRepository.add(
                sessionCreationAttributes = SessionCreationAttributes(
                    rating = 3,
                    comment = "",
                    breakDuration = 0.seconds,
                ),
                sectionCreationAttributes = listOf(
                    SectionCreationAttributes(
                        libraryItemId = UUIDConverter.fromInt(1),
                        startTimestamp = FakeTimeProvider.START_TIME,
                        duration = 1.hours
                    )
                )
            )
        }
    }

    @Test
    fun `Get current goals including paused, returns goal`() = runTest {
        val goals = getCurrentGoals(excludePaused = false).first()

        assertThat(goals).containsExactly(
            GoalInstanceWithProgressAndDescriptionWithLibraryItems(
                description = GoalDescriptionWithLibraryItems(
                    description = GoalDescription(
                        id = UUIDConverter.fromInt(2),
                        createdAt = FakeTimeProvider.START_TIME,
                        modifiedAt = FakeTimeProvider.START_TIME,
                        type = GoalType.NON_SPECIFIC,
                        repeat = true,
                        periodInPeriodUnits = 1,
                        periodUnit = GoalPeriodUnit.DAY,
                        progressType = GoalProgressType.TIME,
                        paused = true,
                        archived = false,
                        customOrder = null
                    ),
                    libraryItems = emptyList()
                ),
                instance = GoalInstance(
                    id = UUIDConverter.fromInt(3),
                    createdAt = FakeTimeProvider.START_TIME,
                    modifiedAt = FakeTimeProvider.START_TIME,
                    descriptionId = UUIDConverter.fromInt(2),
                    previousInstanceId = null,
                    startTimestamp = FakeTimeProvider.START_TIME,
                    targetSeconds = 3600,
                    endTimestamp = null
                ),
                progress = 1.hours
            )
        )
    }

    @Test
    fun `Get current goals excluding paused, returns nothing`() = runTest {
        val goals = getCurrentGoals(excludePaused = true).first()

        assertThat(goals).isEmpty()
    }
}