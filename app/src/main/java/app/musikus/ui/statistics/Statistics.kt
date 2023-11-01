/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.musikus.R
import app.musikus.datastore.ThemeSelections
import app.musikus.shared.CommonMenuSelections
import app.musikus.shared.MainMenu
import app.musikus.shared.ThemeMenu
import app.musikus.spacing
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY
import app.musikus.utils.TIME_FORMAT_HUMAN_PRETTY_SHORT
import app.musikus.utils.getDurationString
import app.musikus.viewmodel.MainViewModel
import app.musikus.viewmodel.StatisticsCurrentMonthUiState
import app.musikus.viewmodel.StatisticsGoalCardUiState
import app.musikus.viewmodel.StatisticsPracticeDurationCardUiState
import app.musikus.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Statistics(
    mainViewModel: MainViewModel,
    statisticsViewModel: StatisticsViewModel = viewModel(),
) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    val statisticsUiState by statisticsViewModel.uiState.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Statistics") },
                scrollBehavior = scrollBehavior,
                actions = {
                    val mainMenuUiState = mainUiState.menuUiState

                    IconButton(onClick = {
                        mainViewModel.showMainMenu()
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "more")
                        MainMenu (
                            show = mainMenuUiState.show,
                            onDismissHandler = mainViewModel::hideMainMenu,
                            onSelectionHandler = { commonSelection ->
                                mainViewModel.hideMainMenu()

                                when (commonSelection) {
                                    CommonMenuSelections.APP_INFO -> {}
                                    CommonMenuSelections.THEME -> {
                                        mainViewModel.showThemeSubMenu()
                                    }
                                    CommonMenuSelections.BACKUP -> {
                                        mainViewModel.showExportImportDialog()
                                    }
                                }
                            },
                            uniqueMenuItems = {}
                        )
                        ThemeMenu(
                            expanded = mainMenuUiState.showThemeSubMenu,
                            currentTheme = mainViewModel.activeTheme.collectAsState(initial = ThemeSelections.DAY).value,
                            onDismissHandler = mainViewModel::hideThemeSubMenu,
                            onSelectionHandler = { theme ->
                                mainViewModel.hideThemeSubMenu()
                                mainViewModel.setTheme(theme)
                            }
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            val contentUiState = statisticsUiState.contentUiState
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                StatisticsCurrentMonth(contentUiState.currentMonthUiState)
                StatisticsPracticeDurationCard(contentUiState.practiceDurationCardUiState)
                StatisticsGoalCard(contentUiState.goalCardUiState)
    //            StatisticsRatingsCard(contentUiState.ratingsCardUiState)
            }
        }
    )
}

@Composable
fun StatisticsCurrentMonth(
    uiState: StatisticsCurrentMonthUiState,
) {
    Column {
        Text(text = "Current month")
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
           Column(
               modifier = Modifier
                   .weight(1f)
                   .padding(horizontal = 4.dp),
               horizontalAlignment = CenterHorizontally,
           ) {
               Text(
                   text = getDurationString(
                       uiState.totalPracticeDuration,
                       TIME_FORMAT_HUMAN_PRETTY_SHORT
                   ).toString(),
                   fontSize = 20.sp
               )
               Text(text = "Total duration", fontSize = 12.sp, maxLines = 1)
           }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.average_sign) + " " + getDurationString(
                        uiState.averageDurationPerSession,
                        TIME_FORMAT_HUMAN_PRETTY_SHORT
                    ).toString(),
                    fontSize = 20.sp
                )
                Text(text = "Per session", fontSize = 12.sp, maxLines = 1)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = CenterHorizontally
            ) {
                Text(
                    text = getDurationString(
                        uiState.breakDurationPerHour,
                        TIME_FORMAT_HUMAN_PRETTY_SHORT
                    ).toString(),
                    fontSize = 20.sp
                )
                Text(text = "Break per hour", fontSize = 12.sp, maxLines = 1)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = CenterHorizontally
            ) {
                Text(
                    text =
                        stringResource(R.string.average_sign) + " %.1f".format(
                            uiState.averageRatingPerSession
                        ),
                    fontSize = 20.sp
                )
                Text(text = "Rating", fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun StatisticsPracticeDurationCard(
    uiState: StatisticsPracticeDurationCardUiState,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Practice Time")
                Icon(
                    //                modifier = Modifier.fillMaxHeight(),
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "more",
                )
            }
            Text(
                text = "Last 7 days"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                Column {
                    Text(
                        text = getDurationString(
                            uiState.totalPracticeDuration,
                            TIME_FORMAT_HUMAN_PRETTY
                        ).toString()
                    )
                    Text(text = "Total")
                }

                Spacer(modifier = Modifier.weight(2f))

                if (uiState.lastSevenDayPracticeDuration.isEmpty()) {
                    Text(text = "No data")
                } else {
                    Row(
                        modifier = Modifier
                            .height(80.dp)
                            .weight(3f)
                    ) {
                        val maxDuration = uiState.lastSevenDayPracticeDuration.maxOf { it.duration }

                        uiState.lastSevenDayPracticeDuration.forEachIndexed { index, (day, duration) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f),
                                horizontalAlignment = CenterHorizontally,
                            ) {
                                if (duration < maxDuration) {
                                    Box(modifier = Modifier.weight((maxDuration - duration).toFloat()))
                                }
                                if (duration > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(duration.toFloat())
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 4.dp,
                                                    topEnd = 4.dp
                                                )
                                            )
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = day,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                )
                            }

                            if (index < uiState.lastSevenDayPracticeDuration.size - 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsGoalCard(
    uiState: StatisticsGoalCardUiState
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Your Goals")
                Icon(
                    //                modifier = Modifier.fillMaxHeight(),
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "more",
                )
            }
            Text(
                text = "Last 5 expired goals"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                Column {
                    Text(
                        text = "" +
                                "${uiState.lastGoals.filter { it.progress > it.goal.instance.target }.size}" +
                                "/" +
                                "${uiState.lastGoals.size}",
                    )
                    Text(text = "Achieved")
                }

                Spacer(modifier = Modifier.weight(2f))

                if (uiState.lastGoals.isEmpty()) {
                    Text(text = "No data")
                } else {
                    Row(
                        modifier = Modifier
                            .height(80.dp)
                            .weight(3f)
                    ) {
                        uiState.lastGoals.forEach { (goal, progress) ->
                            Column {
                                CircularProgressIndicator(
                                    progress = progress.toFloat() / goal.instance.target.toFloat(),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}