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

package app.musikus.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.musikus.R
import app.musikus.components.NumberInput
import app.musikus.database.daos.LibraryItem
import app.musikus.database.entities.GoalPeriodUnit
import app.musikus.database.entities.GoalType
import app.musikus.shared.DialogActions
import app.musikus.shared.DialogHeader
import app.musikus.shared.IntSelectionSpinnerOption
import app.musikus.shared.MyToggleButton
import app.musikus.shared.SelectionSpinner
import app.musikus.shared.ToggleButtonOption
import app.musikus.shared.UUIDSelectionSpinnerOption
import app.musikus.viewmodel.GoalDialogData

@Composable
fun TimeInput(
    value: Int,
    onValueChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        var hours = (value / 3600).toString().padStart(2, '0')
        var minutes = ((value % 3600) / 60).toString().padStart(2, '0')

        NumberInput(
            value = hours,
            onValueChange = {
                hours = it
                onValueChanged((hours.toIntOrNull() ?: 0) * 3600 +
                        (minutes.toIntOrNull() ?: 0) * 60
                )
            },
            showLeadingZero = true,
            textSize = 40.sp,
            maxValue = 99,
            imeAction = ImeAction.Next,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            value = minutes,
            onValueChange = {
                minutes = it
                onValueChanged((hours.toIntOrNull() ?: 0) * 3600 +
                        (minutes.toIntOrNull() ?: 0) * 60
                )
            },
            showLeadingZero = true,
            textSize = 40.sp,
            maxValue = 59,
            imeAction = ImeAction.Done,
            label = { modifier ->
                Text(modifier = modifier, text = "m", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}

@Composable
fun PeriodInput(
    periodInPeriodUnits: Int,
    periodUnit: GoalPeriodUnit,
    periodUnitSelectorExpanded: Boolean,
    onPeriodChanged: (Int) -> Unit,
    onPeriodUnitChanged: (GoalPeriodUnit) -> Unit,
    onPeriodUnitSelectorExpandedChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "in")
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            value = periodInPeriodUnits.toString(),
            onValueChange = { onPeriodChanged(it.toIntOrNull() ?: 0) },
            textSize = 20.sp,
            maxValue = 99,
            imeAction = ImeAction.Next,
        )
        Spacer(modifier = Modifier.width(8.dp))
        SelectionSpinner(
            modifier = Modifier.width(130.dp),
            expanded = periodUnitSelectorExpanded,
            options = GoalPeriodUnit.values().map { IntSelectionSpinnerOption(it.ordinal, GoalPeriodUnit.toString(it)) },
            selected = IntSelectionSpinnerOption(periodUnit.ordinal, GoalPeriodUnit.toString(periodUnit)),
            onExpandedChange = onPeriodUnitSelectorExpandedChanged,
            onSelectedChange = {selection ->
                onPeriodUnitChanged(GoalPeriodUnit.values()[(selection as IntSelectionSpinnerOption?)?.id ?: 0])
                onPeriodUnitSelectorExpandedChanged(false)
            }
        )
    }
}

@Composable
fun GoalDialog(
    dialogData: GoalDialogData,
    periodUnitSelectorExpanded: Boolean,
    libraryItems: List<LibraryItem>,
    libraryItemsSelectorExpanded: Boolean,
    onTargetChanged: (Int) -> Unit,
    onPeriodChanged: (Int) -> Unit,
    onPeriodUnitChanged: (GoalPeriodUnit) -> Unit,
    onPeriodUnitSelectorExpandedChanged: (Boolean) -> Unit,
    onGoalTypeChanged: (GoalType) -> Unit,
    onLibraryItemsSelectorExpandedChanged: (Boolean) -> Unit,
    onSelectedLibraryItemsChanged: (List<LibraryItem>) -> Unit,
    onConfirmHandler: () -> Unit,
    onDismissHandler: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissHandler,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp) // TODO: figure out a better way to do this
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogHeader(title = stringResource(id = R.string.addGoalDialogTitle))

            var confirmButtonEnabled = true
            TimeInput(dialogData.target, onTargetChanged)
            confirmButtonEnabled = confirmButtonEnabled && dialogData.target > 0

            Spacer(modifier = Modifier.height(12.dp))
            PeriodInput(
                periodInPeriodUnits = dialogData.periodInPeriodUnits,
                periodUnit = dialogData.periodUnit,
                periodUnitSelectorExpanded = periodUnitSelectorExpanded,
                onPeriodChanged = onPeriodChanged,
                onPeriodUnitChanged = onPeriodUnitChanged,
                onPeriodUnitSelectorExpandedChanged = onPeriodUnitSelectorExpandedChanged
            )
            confirmButtonEnabled = confirmButtonEnabled && dialogData.periodInPeriodUnits > 0

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.padding(horizontal = 32.dp)){ Divider() }

            Spacer(modifier = Modifier.height(12.dp))

            MyToggleButton(
                modifier = Modifier.padding(horizontal = 32.dp),
                options = GoalType.values().map {
                    ToggleButtonOption(it.ordinal, GoalType.toString(it))
                },
                selected = ToggleButtonOption(
                    dialogData.goalType.ordinal,
                    GoalType.toString(dialogData.goalType)
                ),
                onSelectedChanged = { option ->
                    onGoalTypeChanged(GoalType.values()[option.id])
                }
            )

            if(dialogData.goalType == GoalType.ITEM_SPECIFIC) {
                Spacer(modifier = Modifier.height(12.dp))

                if(libraryItems.isNotEmpty()) {
                    SelectionSpinner(
                        expanded = libraryItemsSelectorExpanded,
                        options = libraryItems.map {
                            UUIDSelectionSpinnerOption(
                                it.id,
                                it.name
                            )
                        },
                        selected = dialogData.selectedLibraryItems.firstOrNull()?.let {
                            UUIDSelectionSpinnerOption(it.id, it.name)
                        } ?: libraryItems.first().let {
                            UUIDSelectionSpinnerOption(it.id, it.name)
                        },
                        onExpandedChange = onLibraryItemsSelectorExpandedChanged,
                        onSelectedChange = { selection ->
                            onSelectedLibraryItemsChanged(libraryItems.filter {
                                it.id == (selection as UUIDSelectionSpinnerOption).id
                            })
                            onLibraryItemsSelectorExpandedChanged(false)
                        }
                    )
                } else {
                    Text(text = "No items in your library.")

                }
            }

            confirmButtonEnabled = confirmButtonEnabled && (
                dialogData.goalType == GoalType.NON_SPECIFIC ||
                dialogData.selectedLibraryItems.isNotEmpty()
            )

            DialogActions(
                onConfirmHandler = onConfirmHandler,
                onDismissHandler = onDismissHandler,
                confirmButtonEnabled = confirmButtonEnabled,
                confirmButtonText = "Create"
            )
        }
    }
}

@Composable
fun EditGoalDialog(
    value: Int,
    onValueChanged: (Int) -> Unit,
    onConfirmHandler: () -> Unit,
    onDismissHandler: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissHandler
    ) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DialogHeader(title = stringResource(id = R.string.goalDialogTitleEdit))
            TimeInput(value, onValueChanged)
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismissHandler,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Cancel")
                }
                TextButton(
                    onClick = onConfirmHandler,
//                    enabled = name.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(id = R.string.goalDialogOkEdit))
                }
            }
        }
    }
}
