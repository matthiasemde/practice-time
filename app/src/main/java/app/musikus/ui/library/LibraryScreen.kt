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

package app.musikus.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musikus.Musikus
import app.musikus.R
import app.musikus.database.daos.LibraryFolder
import app.musikus.database.daos.LibraryItem
import app.musikus.datastore.ThemeSelections
import app.musikus.shared.ActionBar
import app.musikus.shared.CommonMenuSelections
import app.musikus.shared.MainMenu
import app.musikus.shared.MiniFABData
import app.musikus.shared.MultiFAB
import app.musikus.shared.MultiFabState
import app.musikus.shared.Selectable
import app.musikus.shared.SortMenu
import app.musikus.shared.ThemeMenu
import app.musikus.spacing
import app.musikus.ui.MainViewModel
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    mainViewModel: MainViewModel,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(
        enabled = libraryUiState.topBarUiState.showBackButton,
        onBack = libraryViewModel::onTopBarBackPressed
    )

    BackHandler(
        enabled = libraryUiState.actionModeUiState.isActionMode,
        onBack = libraryViewModel::clearActionMode
    )

    BackHandler(
        enabled = mainUiState.multiFabState == MultiFabState.EXPANDED,
        onBack = mainViewModel::collapseMultiFab
    )

    Scaffold(
        contentWindowInsets = WindowInsets(bottom = 0.dp), // makes sure FAB is not shifted up
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            val fabUiState = libraryUiState.fabUiState
            if(fabUiState.activeFolder != null) {
                FloatingActionButton(
                    onClick = {
                        libraryViewModel.showItemDialog(fabUiState.activeFolder.id)
                        mainViewModel.collapseMultiFab()
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            } else {
                MultiFAB(
                    state = mainUiState.multiFabState,
                    onStateChange = { state ->
                        mainViewModel.onMultiFabStateChanged(state)
                        if(state == MultiFabState.EXPANDED) {
                            libraryViewModel.clearActionMode()
                        }
                    },
                    contentDescription = "Add",
                    miniFABs = listOf(
                        MiniFABData(
                            onClick = {
                                libraryViewModel.showItemDialog()
                                mainViewModel.collapseMultiFab()
                            },
                            label = "Item",
                            icon = Icons.Rounded.MusicNote
                        ),
                        MiniFABData(
                            onClick = {
                                libraryViewModel.showFolderDialog()
                                mainViewModel.collapseMultiFab()
                            },
                            label = "Folder",
                            icon = Icons.Rounded.Folder
                        )
                    )
                )
            }
        },
        topBar = {
            val topBarUiState = libraryUiState.topBarUiState
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(text = topBarUiState.title) },
                navigationIcon = {
                    if(topBarUiState.showBackButton) {
                        IconButton(onClick = libraryViewModel::onTopBarBackPressed) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
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
                            }
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

            // Action bar
            val actionModeUiState = libraryUiState.actionModeUiState
            if(actionModeUiState.isActionMode) {
                ActionBar(
                    numSelectedItems = actionModeUiState.numberOfSelections,
                    onDismissHandler = libraryViewModel::clearActionMode,
                    onEditHandler = libraryViewModel::onEditAction,
                    onDeleteHandler = {
                        libraryViewModel.onDeleteAction()
                        mainViewModel.showSnackbar(
                            message = "Deleted ${actionModeUiState.numberOfSelections} items",
                            onUndo = libraryViewModel::onRestoreAction
                        )
                    }
                )
            }
        },
        content = { paddingValues ->
            val contentUiState = libraryUiState.contentUiState

            LibraryContent(
                contentPadding = paddingValues,
                contentUiState = contentUiState,
                onShowFolderSortMenuChange = libraryViewModel::onFolderSortMenuChanged,
                onFolderSortModeSelected = libraryViewModel::onFolderSortModeSelected,
                onShowItemSortMenuChange = libraryViewModel::onItemSortMenuChanged,
                onItemSortModeSelected = libraryViewModel::onItemSortModeSelected,
                onFolderClicked = libraryViewModel::onFolderClicked,
                onItemClicked = libraryViewModel::onItemClicked,
            )

            // Show hint if no items or folders are in the library
            if (contentUiState.showHint) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.libraryHint),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val dialogUiState = libraryUiState.dialogUiState

            val folderDialogUiState = dialogUiState.folderDialogUiState
            val itemDialogUiState = dialogUiState.itemDialogUiState

            if(folderDialogUiState != null) {
                LibraryFolderDialog(
                    mode = folderDialogUiState.mode,
                    folderData = folderDialogUiState.folderData,
                    onFolderNameChange = libraryViewModel::onFolderDialogNameChanged,
                    onConfirmHandler = libraryViewModel::onFolderDialogConfirmed,
                    onDismissHandler = libraryViewModel::clearFolderDialog,
                )
            }

            if(itemDialogUiState != null) {
                LibraryItemDialog(
                    mode = itemDialogUiState.mode,
                    folders = itemDialogUiState.folders,
                    itemData = itemDialogUiState.itemData,
                    folderSelectorExpanded = itemDialogUiState.isFolderSelectorExpanded,
                    onNameChange = libraryViewModel::onItemDialogNameChanged,
                    onColorIndexChange = libraryViewModel::onItemDialogColorIndexChanged,
                    onSelectedFolderIdChange = libraryViewModel::onItemDialogFolderIdChanged,
                    onFolderSelectorExpandedChange = libraryViewModel::onFolderSelectorExpandedChanged,
                    onConfirmHandler = libraryViewModel::onItemDialogConfirmed,
                    onDismissHandler = libraryViewModel::clearItemDialog,
                )
            }

            // Content Scrim for multiFAB
            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f),
                visible = mainUiState.multiFabState == MultiFabState.EXPANDED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.surface.copy(alpha = 0.9f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = mainViewModel::collapseMultiFab
                        )
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryContent(
    contentPadding: PaddingValues,
    contentUiState: LibraryContentUiState,
    onShowFolderSortMenuChange: (Boolean) -> Unit,
    onFolderSortModeSelected: (SortMode<LibraryFolder>) -> Unit,
    onShowItemSortMenuChange: (Boolean) -> Unit,
    onItemSortModeSelected: (SortMode<LibraryItem>) -> Unit,
    onFolderClicked: (LibraryFolder, Boolean) -> Unit,
    onItemClicked: (LibraryItem, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 56.dp,
        ),
    ) {
        val foldersUiState = contentUiState.foldersUiState
        val itemsUiState = contentUiState.itemsUiState

        /** Folders */
        if(foldersUiState != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = MaterialTheme.spacing.small,
                            horizontal = MaterialTheme.spacing.large
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Folders",
                        style = MaterialTheme.typography.titleLarge
                    )
                    val sortMenuUiState = foldersUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryFolderSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = "folders",
                        onShowMenuChanged = onShowFolderSortMenuChange,
                        onSelectionHandler = onFolderSortModeSelected
                    )
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // header and footer items replace contentPadding
                    // but also serve to fixate the list when inserting items
                    item {
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    }
                    items(
                        items = foldersUiState.foldersWithItems,
                        key = { it.folder.id }
                    ) { folderWithItems ->
                        val folder = folderWithItems.folder
                        Row(
                            modifier = Modifier
                                .animateItemPlacement()
                        ) {
                            LibraryFolder(
                                folder = folder,
                                numItems = folderWithItems.items.size,
                                selected = folder in foldersUiState.selectedFolders,
                                onShortClick = { onFolderClicked(folder, false) },
                                onLongClick = { onFolderClicked(folder, true) }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    }
                }
            }
        }

        /** Items */
        if(itemsUiState != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = MaterialTheme.spacing.small,
                            horizontal = MaterialTheme.spacing.large
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleLarge
                    )
                    val sortMenuUiState = itemsUiState.sortMenuUiState
                    SortMenu(
                        show = sortMenuUiState.show,
                        sortModes = LibraryItemSortMode.entries,
                        currentSortMode = sortMenuUiState.mode,
                        currentSortDirection = sortMenuUiState.direction,
                        sortItemDescription = "items",
                        onShowMenuChanged = onShowItemSortMenuChange,
                        onSelectionHandler = onItemSortModeSelected
                    )
                }
            }
            items(
                items=itemsUiState.items,
                key = { item -> item.id }
            ) { item ->
                Box(
                    modifier = Modifier.animateItemPlacement()
                ) {
                    LibraryItem(
                        modifier = Modifier.padding(
                            vertical = MaterialTheme.spacing.small,
                            horizontal = MaterialTheme.spacing.large
                        ),
                        item = item,
                        selected = item in itemsUiState.selectedItems,
                        onShortClick = { onItemClicked(item, false) },
                        onLongClick = { onItemClicked(item, true) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryFolder(
    folder: LibraryFolder,
    numItems: Int,
    selected: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Selectable(
        selected = selected,
        onShortClick = onShortClick,
        onLongClick = onLongClick,
        shape = MaterialTheme.shapes.large,
    ) {
        Surface(
            modifier = Modifier
                .size(150.dp),
            color = colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "$numItems items",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun LibraryItem(
    modifier: Modifier = Modifier,
    item: LibraryItem,
    selected: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Selectable(
        selected = selected,
        onShortClick = onShortClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .align(Alignment.CenterVertically)
                    .background(
                        Color(Musikus.getLibraryItemColors(LocalContext.current)[item.colorIndex])
                    )
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "last practiced: yesterday",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
