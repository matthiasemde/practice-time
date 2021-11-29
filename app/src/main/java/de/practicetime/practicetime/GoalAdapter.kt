package de.practicetime.practicetime

import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import de.practicetime.practicetime.entities.*
import java.util.*

const val SECONDS_PER_HOUR = 60 * 60
const val SECONDS_PER_DAY = 60 * 60 * 24

class GoalAdapter(
    private val goalInstancesWithDescriptionsWithCategories: ArrayList<GoalInstanceWithDescriptionWithCategories>,
    private val context: Activity,
    private val shortClickHandler: (goalId: Int, goalView: View) -> Unit = { _, _ -> },
    private val longClickHandler: (goalId: Int, goalView: View) -> Boolean = { _, _ -> false },
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    override fun getItemCount() = goalInstancesWithDescriptionsWithCategories.size

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.view_goal_item_new, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val (instance, descriptionWithCategories) = goalInstancesWithDescriptionsWithCategories[position]
        val (description, categories) = descriptionWithCategories

        // get the category color for later use in different UI elements
        var categoryColor: ColorStateList? = null
        if(description.type != GoalType.NON_SPECIFIC) {
            categoryColor = ColorStateList.valueOf(
                context.resources.getIntArray(R.array.category_colors)[categories.first().colorIndex]
            )
        }

        // set Click listener
        viewHolder.itemView.setOnClickListener { shortClickHandler(description.id, it) }
        viewHolder.itemView.setOnLongClickListener {
            // tell the event handler we consumed the event
            return@setOnLongClickListener longClickHandler(description.id, it)
        }


        // Goal Title
        if(description.type == GoalType.NON_SPECIFIC) {
            viewHolder.goalNameView.text = context.getString(R.string.goal_name_non_specific)
        } else {
            viewHolder.goalNameView.apply {
                text = categories.first().name
//                setTextColor(categoryColor)
            }
        }


        // Goal Description
        val targetHours = instance.target / 3600
        val targetMinutes = instance.target % 3600 / 60
        var targetHoursString = ""
        var targetMinutesString = ""
        if (targetHours > 0) targetHoursString = "${targetHours}h "
        if (targetMinutes > 0) targetMinutesString = "${targetMinutes}min "

        val periodFormatted =
            if (description.periodInPeriodUnits > 1) {  // plural
                when (description.periodUnit) {
                    GoalPeriodUnit.DAY -> context.getString(R.string.goal_description_days, description.periodInPeriodUnits)
                    GoalPeriodUnit.WEEK -> context.getString(R.string.goal_description_weeks, description.periodInPeriodUnits)
                    GoalPeriodUnit.MONTH -> context.getString(R.string.goal_description_months, description.periodInPeriodUnits)
                }
            } else {    // singular
                when (description.periodUnit) {
                    GoalPeriodUnit.DAY -> context.getString(R.string.goal_description_day)
                    GoalPeriodUnit.WEEK -> context.getString(R.string.goal_description_week)
                    GoalPeriodUnit.MONTH -> context.getString(R.string.goal_description_month)
                }
            }

        viewHolder.goalDescriptionView.text = context.getString(
            R.string.goal_description_complete,
            targetHoursString,
            targetMinutesString,
            periodFormatted
        )


        // ProgressBar
        viewHolder.progressBarView.max = instance.target
        viewHolder.progressBarView.progress = instance.progress

        // tint progressbar and progress TextView
        if(description.type != GoalType.NON_SPECIFIC) {
            viewHolder.progressBarView.progressTintList = categoryColor
//            viewHolder.goalProgressIndicatorView.setTextColor(categoryColor)
        }
        // adapt X position for tv indicating progress. Do it asynchronously otherwise UI dimensions will be 0
        viewHolder.progressBarView.post {
            val width = viewHolder.progressBarView.width
            val progress = viewHolder.progressBarView.progress
            val max = viewHolder.progressBarView.max
            // add the progress width as an offset to the current position
            viewHolder.goalProgressIndicatorView.apply {
                val offset = (progress * width).toFloat() / max.toFloat() - this.width
//                x += if (offset > 0) offset else 0f
            }
        }

        // progress Indicator Text
        val progressHours = instance.progress / 3600
        val progressMinutes = instance.progress % 3600 / 60
        when {
            progressHours > 0 ->
                viewHolder.goalProgressIndicatorView.text = String.format("%02d:%02d", progressHours, progressMinutes)
            progressMinutes > 0 ->
                viewHolder.goalProgressIndicatorView.text = String.format("%d min", progressMinutes)
            else -> viewHolder.goalProgressIndicatorView.text = "<1m"
        }

        // Percentage
        // set the percent text to the progress capped at 100 %
        viewHolder.progressPercentView.apply {
            text = "${minOf(instance.progress * 100 / instance.target, 100)}%"
//            if(description.type != GoalType.NON_SPECIFIC) {
//                setTextColor(categoryColor)
//            }
        }


        // remaining time
        val now = Date().time / 1000L
        val remainingTime = (instance.startTimestamp + instance.periodInSeconds) - now
        // if time left is larger than a day, show the number of days
        when {
            remainingTime > SECONDS_PER_DAY -> {
                viewHolder.remainingTimeView.text =
                    context.getString(
                        R.string.days_left,
                        remainingTime / SECONDS_PER_DAY + 1
                    )
                // otherwise, if time left is larger than an hour, show the number of hours
            }
            remainingTime > SECONDS_PER_HOUR -> {
                viewHolder.remainingTimeView.text =
                    context.getString(
                        R.string.hours_left,
                        remainingTime % SECONDS_PER_DAY / SECONDS_PER_HOUR + 1
                    )
            }
            else -> {   // otherwise, show the number of minutes
                viewHolder.remainingTimeView.text =
                    context.getString(
                        R.string.min_left,
                        remainingTime % SECONDS_PER_HOUR / 60 + 1
                    )
            }
        }
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val progressBarView: ProgressBar = view.findViewById(R.id.goalProgressBar)
        val progressPercentView: TextView = view.findViewById(R.id.goalProgressPercent)
        val goalNameView: TextView = view.findViewById(R.id.goalName)
        val goalDescriptionView: TextView = view.findViewById(R.id.goalDescription)
        val remainingTimeView: Chip = view.findViewById(R.id.goalRemainingTime)
        val goalProgressIndicatorView: TextView = view.findViewById(R.id.goalProgressIndicator)

    }
}
