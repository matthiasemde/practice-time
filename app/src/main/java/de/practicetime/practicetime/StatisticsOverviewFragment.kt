package de.practicetime.practicetime

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class StatisticsOverviewFragment : Fragment(R.layout.fragment_statistics_overview) {

    private lateinit var dao: PTDao
    private lateinit var lastDaysChart: BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        openDatabase()

        view.findViewById<Button>(R.id.btn_open_goals_history).setOnClickListener {
            val i = Intent(requireContext(), GoalStatsActivity::class.java)
            requireActivity().startActivity(i)
        }

        val sessionDetailClickListener = View.OnClickListener {
            val i = Intent(requireContext(), SessionStatsActivity::class.java)
            requireActivity().startActivity(i)
        }
        view.findViewById<CardView>(R.id.stats_ov_cardview).setOnClickListener(sessionDetailClickListener)
        view.findViewById<ImageButton>(R.id.stats_ov_card_ib_more_details).setOnClickListener(sessionDetailClickListener)

        initLast7DaysChart(view)
        setLast7DaysChartData()

    }

    private fun initLast7DaysChart(view: View) {
        lastDaysChart = view.findViewById(R.id.stats_ov_card_bar_chart)
        lastDaysChart.apply {
            setTouchEnabled(false)
            description.isEnabled = false
            legend.isEnabled = false
        }

        // x axis
        lastDaysChart.xAxis.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            position = XAxis.XAxisPosition.BOTTOM
            labelCount = 7
            valueFormatter = XAxisValueFormatter()
            textColor = getThemeColor(R.attr.colorOnSurface)
        }

        lastDaysChart.axisLeft.isEnabled = false
        lastDaysChart.axisRight.isEnabled = false

        lastDaysChart.notifyDataSetChanged()
        lastDaysChart.invalidate()
    }

    private fun setLast7DaysChartData() {
        lifecycleScope.launch {

            // get all total durations from the last 7 days
            val barChartArray = arrayListOf<BarEntry>()
            for (day in 0 downTo -6) {
                Log.d("TZASG", "Day is: $day")
                val dur = dao.getSectionsWithCategories(
                        getStartOfDay(day.toLong()).toEpochSecond(),
                        getEndOfDay(day.toLong()).toEpochSecond()
                    ).sumOf {
                        it.section.duration ?: 0
                    }
                barChartArray.add(0, BarEntry(day.toFloat(), dur.toFloat()))
            }

            val dataSetBarChart = BarDataSet(barChartArray, "Label")
            dataSetBarChart.apply {
                setDrawValues(false)
                color = getThemeColor(R.attr.colorPrimary)
            }

            val barData = BarData(dataSetBarChart)
            barData.apply {
                barWidth = 0.4f
                isHighlightEnabled = false

            }
            lastDaysChart.apply {
                data = barData
                animateXY(500, 1000, Easing.EaseOutBack)
                notifyDataSetChanged()
                invalidate()
            }

            requireView()
                .findViewById<TextView>(R.id.stats_ov_card_tv_avg_time)
                .text = getAvgText(barChartArray)
        }
    }

    private fun getAvgText(barChartArray: ArrayList<BarEntry>): String {
        val totalSec = barChartArray.sumOf { it.y.toInt() }

        val hours = totalSec.div(3600)
        val minutes = (totalSec.rem(3600)).div(60)

        return if (hours > 0) {
            "%dh %dmin".format(hours, minutes)
        } else if (minutes == 0 && hours > 0){
            "<1min"
        } else {
            "%dmin".format(minutes)
        }
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

    /**
     * formats x axis value according to Last 7 days
     * // TODO re-use the Formatter from the Statistics Activities
     */
    private inner class XAxisValueFormatter: ValueFormatter() {

        override fun getFormattedValue(xValue: Float): String {
            return ZonedDateTime.now()
                .plusDays(xValue.toLong())
                .format(DateTimeFormatter.ofPattern("EEEEE"))
        }
    }

    // get the Beginning of daysInPastOffset Days in the past
    private fun getStartOfDay(daysInPastOffset: Long): ZonedDateTime {
        val day = ZonedDateTime.now()
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusDays(daysInPastOffset)

        Log.d("TAG", "getting start of $day")
        return day
    }

    // get the End of daysInPastOffset Days in the past. Half-open: Actually get the Start of the Next day
    private fun getEndOfDay(daysInPastOffset: Long): ZonedDateTime {
        return ZonedDateTime.now()
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())  // make sure time is 00:00
            .plusDays(daysInPastOffset + 1)
    }

    private fun getThemeColor(color: Int): Int {
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(color, typedValue, true)
        return typedValue.data
    }
}