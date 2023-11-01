/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.database.daos

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.musikus.database.GoalInstanceWithDescription
import app.musikus.database.GoalInstanceWithDescriptionWithLibraryItems
import app.musikus.database.MusikusDatabase
import app.musikus.database.Nullable
import app.musikus.database.entities.GoalDescriptionModel
import app.musikus.database.entities.GoalInstanceModel
import app.musikus.database.entities.GoalInstanceUpdateAttributes
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.TimestampModelDisplayAttributes
import app.musikus.utils.getCurrTimestamp
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

data class GoalInstance(
    @ColumnInfo(name="goal_description_id") val goalDescriptionId: UUID,
    @ColumnInfo(name="start_timestamp") val startTimestamp: Long,
    @ColumnInfo(name="period_in_seconds") val periodInSeconds: Int,
    @ColumnInfo(name="target") val target: Int,
    @ColumnInfo(name="renewed") val renewed: Boolean,
) : TimestampModelDisplayAttributes() {
    val isOutdated : Boolean
        get() = getCurrTimestamp() > startTimestamp + periodInSeconds
}

@Dao
abstract class GoalInstanceDao(
    database : MusikusDatabase
) : TimestampDao<GoalInstanceModel, GoalInstanceUpdateAttributes, GoalInstance>(
    tableName = "goal_instance",
    database = database,
    displayAttributes = GoalInstanceModel::class.java.declaredFields.map { it.name }
) {

    /**
     * @Insert
     */

    suspend fun insert(
        goalDescription: GoalDescription,
        timeFrame: Calendar,
        target: Int,
    ) {
        insert(
            goalDescriptionId = goalDescription.id,
            periodUnit = goalDescription.periodUnit,
            periodInPeriodUnits = goalDescription.periodInPeriodUnits,
            timeFrame = timeFrame,
            target = target
        )
    }

    suspend fun insert(
        goalDescription: GoalDescriptionModel,
        timeFrame: Calendar,
        target: Int,
    ) {
        insert(
            goalDescriptionId = goalDescription.id,
            periodUnit = goalDescription.periodUnit,
            periodInPeriodUnits = goalDescription.periodInPeriodUnits,
            timeFrame = timeFrame,
            target = target
        )
    }

    // create a new instance of this goal, storing the target and progress during a single period
    private suspend fun insert(
        goalDescriptionId: UUID,
        periodUnit: GoalPeriodUnit,
        periodInPeriodUnits: Int,
        timeFrame: Calendar,
        target: Int,
    ) {
        var startTimestamp = 0L

        // to find the correct starting point and period for the goal, we execute these steps:
        // 1. clear the minutes, seconds and millis from the time frame and set hour to 0
        // 2. set the time frame to the beginning of the day, week or month
        // 3. save the time in seconds as startTimeStamp
        // 4. then set the day to the end of the period according to the periodInPeriodUnits
        // 5. calculate the period in seconds from the difference of the two timestamps
        timeFrame.clear(Calendar.MINUTE)
        timeFrame.clear(Calendar.SECOND)
        timeFrame.clear(Calendar.MILLISECOND)
        timeFrame.set(Calendar.HOUR_OF_DAY, 0)

        when(periodUnit) {
            GoalPeriodUnit.DAY -> {
                startTimestamp = timeFrame.timeInMillis / 1000L
                timeFrame.add(Calendar.DAY_OF_YEAR, periodInPeriodUnits)
            }
            GoalPeriodUnit.WEEK -> {
                if(timeFrame.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    timeFrame.add(Calendar.DAY_OF_WEEK, - 1)
                }
                timeFrame.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                startTimestamp = timeFrame.timeInMillis / 1000L

                timeFrame.add(Calendar.WEEK_OF_YEAR, periodInPeriodUnits)
            }
            GoalPeriodUnit.MONTH -> {
                timeFrame.set(Calendar.DAY_OF_MONTH, 1)
                startTimestamp = timeFrame.timeInMillis / 1000L

                timeFrame.add(Calendar.MONTH, periodInPeriodUnits)
            }
        }

        // calculate the period in second from these two timestamps
        val periodInSeconds = ((timeFrame.timeInMillis / 1000) - startTimestamp).toInt()

        assert(startTimestamp > 0) {
            Log.e("Assertion Failed", "startTimeStamp can not be 0")
        }

        super.insert(
            GoalInstanceModel(
                goalDescriptionId = Nullable(goalDescriptionId),
                startTimestamp = startTimestamp,
                periodInSeconds = periodInSeconds,
                target = target
            )
        )
    }

    /**
     * @Update
     */

    override fun applyUpdateAttributes(
        old: GoalInstanceModel,
        updateAttributes: GoalInstanceUpdateAttributes
    ): GoalInstanceModel = super.applyUpdateAttributes(old, updateAttributes).apply {
        target = updateAttributes.target ?: old.target
        renewed = updateAttributes.renewed ?: old.renewed
    }

    @Transaction
    open suspend fun renewGoalInstance(id: UUID) {
        update(id, GoalInstanceUpdateAttributes(renewed = true))
    }

    /**
     * @Queries
     */

    /**
     * Get all [GoalInstance] entities matching a specific pattern
     * @param goalDescriptionId
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp] / 1000L
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    suspend fun get(
        goalDescriptionId: UUID,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstance> {
        return get(
            goalDescriptionIds = listOf(goalDescriptionId),
            from = from,
            to = to,
            inclusiveFrom = inclusiveFrom,
            inclusiveTo = inclusiveTo
        )
    }

    /**
     * Get all [GoalInstance] entities matching a specific pattern
     * @param goalDescriptionIds
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp] / 1000L
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
        "WHERE (" +
            "start_timestamp>:from AND NOT :inclusiveFrom OR " +
            "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
        ")" +
        "AND (" +
            "start_timestamp<:to AND :inclusiveTo OR " +
            "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
        ") " +
        "AND goal_description_id IN (" +
        "SELECT id FROM goal_description " +
        "WHERE id in (:goalDescriptionIds) " +
        "AND archived=0 " +
        "AND deleted=0" +
        ")"
    )
    abstract suspend fun get(
        goalDescriptionIds: List<UUID>,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstance>


    /**
     * Get all [GoalInstanceWithDescription] entities matching a specific pattern
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp]
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
        "WHERE (" +
            "start_timestamp>:from AND NOT :inclusiveFrom OR " +
            "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
        ")" +
        "AND (" +
            "start_timestamp<:to AND :inclusiveTo OR " +
            "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
        ") " +
        "AND goal_description_id IN (" +
        "SELECT id FROM goal_description " +
        "WHERE archived=0 " +
        "AND deleted=0" +
        ")"
    )
    abstract suspend fun getWithDescription(
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ) : List<GoalInstanceWithDescription>

    /**
     * Get all [GoalInstanceWithDescriptionWithLibraryItems] entities matching a specific pattern
     * @param goalDescriptionId
     * @param from optional timestamp in seconds marking beginning of selection. **default**: [getCurrTimestamp]
     * @param to optional timestamp in seconds marking end of selection. **default** [Long.MAX_VALUE]
     * @param inclusiveFrom decides whether the beginning of the selection is inclusive. **default**: true
     * @param inclusiveTo decides whether the end of the selection is inclusive. **default**: false
     */
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
        "WHERE (" +
        "start_timestamp>:from AND NOT :inclusiveFrom OR " +
        "start_timestamp+period_in_seconds>:from AND :inclusiveFrom" +
        ")" +
        "AND (" +
        "start_timestamp<:to AND :inclusiveTo OR " +
        "start_timestamp+period_in_seconds<:to AND NOT :inclusiveTo" +
        ") " +
        "AND EXISTS (" +
        "SELECT id FROM goal_description " +
        "WHERE id = :goalDescriptionId " +
        "AND archived=0 " +
        "AND deleted=0" +
        ")"
    )
    abstract suspend fun getWithDescriptionWithLibraryItems(
        goalDescriptionId: UUID,
        from: Long = getCurrTimestamp(),
        to: Long = Long.MAX_VALUE,
        inclusiveFrom: Boolean = true,
        inclusiveTo: Boolean = false,
    ): List<GoalInstanceWithDescriptionWithLibraryItems>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
            "WHERE renewed=0 " +
            "AND start_timestamp + period_in_seconds < :now " +
            "AND goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE archived=0 " +
                "AND deleted=0" +
            ")"
    )
    abstract suspend fun getOutdatedWithDescriptions(
        now : Long = getCurrTimestamp()
    ) : List<GoalInstanceWithDescription>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE start_timestamp < :now " +
                "AND start_timestamp+period_in_seconds > :now " +
                "AND goal_description_id IN (" +
                    "SELECT id FROM goal_description " +
                    "WHERE (archived=0 OR :checkArchived) " +
                    "AND deleted=0" +
                ")"
    )
    abstract fun getWithDescriptionsWithLibraryItems(
        checkArchived : Boolean = false,
        now : Long = getCurrTimestamp(),
    ) : Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "Select * FROM goal_instance " +
                "WHERE goal_instance.start_timestamp=(" +
                "SELECT MAX(start_timestamp) FROM goal_instance WHERE " +
                "goal_description_id = :goalDescriptionId" +
                ") " +
                "AND EXISTS (" +
                "SELECT id FROM goal_description " +
                "WHERE id = :goalDescriptionId " +
                "AND archived=0 " +
                "AND deleted=0" +
                ")"
    )
    abstract suspend fun getLatest(
        goalDescriptionId: UUID
    ): GoalInstance?

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE paused=1 " +
                "AND archived=0 " +
                "AND deleted=0" +
                ")" +
                "AND renewed=0"
    )
    abstract fun getLatestPausedWithDescriptions(): Flow<List<GoalInstanceWithDescription>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM goal_instance " +
                "WHERE goal_description_id IN (" +
                "SELECT id FROM goal_description " +
                "WHERE deleted=0" +
                ")" +
                "AND (start_timestamp + period_in_seconds) < :now " +
                "ORDER BY (start_timestamp + period_in_seconds) DESC " +
                "LIMIT 5"
    )
    abstract fun getLastFiveCompletedWithDescriptionsWithLibraryItems(
       now: Long = getCurrTimestamp()
    ): Flow<List<GoalInstanceWithDescriptionWithLibraryItems>>
}