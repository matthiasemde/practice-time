/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package de.practicetime.practicetime.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.database.entities.LibraryFolder
import de.practicetime.practicetime.database.entities.LibraryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ThemeSelections {
    SYSTEM,
    DAY,
    NIGHT,
}

enum class LibrarySortMode {
    DATE_ADDED,
    LAST_MODIFIED,
    NAME,
    COLOR,
    CUSTOM
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

@Composable
fun rememberMainState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(coroutineScope) { MainState(coroutineScope) }

class MainState(
    private val coroutineScope: CoroutineScope
) {

    // Initialization

    init {
        coroutineScope.launch {
            loadLibraryItems()
            loadLibraryFolders()
            sortLibrary()
        }
    }

    // Content Scrim over NavBar for Multi FAB etc
    val showNavBarScrim = mutableStateOf(false)


    // Theme

    val activeTheme = mutableStateOf(ThemeSelections.SYSTEM)

    fun setTheme(theme: ThemeSelections) {
        PracticeTime.prefs.edit().putInt(PracticeTime.PREFERENCES_KEY_THEME, theme.ordinal).apply()
        activeTheme.value = theme
        AppCompatDelegate.setDefaultNightMode(theme.ordinal)
    }


    // Library Items and Folders

    private val _libraryFolders = MutableStateFlow(emptyList<LibraryFolder>())
    private val _libraryItems = MutableStateFlow(emptyList<LibraryItem>())

    val libraryFolders = _libraryFolders.asStateFlow()
    var libraryItems = _libraryItems.asStateFlow()

    var librarySortMode = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
            LibrarySortMode.DATE_ADDED.name
        )?.let { LibrarySortMode.valueOf(it) } ?: LibrarySortMode.DATE_ADDED
    } catch (ex: Exception) {
        LibrarySortMode.DATE_ADDED
    })

    var librarySortDirection = mutableStateOf(try {
        PracticeTime.prefs.getString(
            PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_DIRECTION,
            SortDirection.ASCENDING.name
        )?.let { SortDirection.valueOf(it) } ?: SortDirection.ASCENDING
    } catch (ex: Exception) {
        SortDirection.ASCENDING
    })

    // Accessors
    // Load
    private suspend fun loadLibraryFolders() {
        _libraryFolders.update { PracticeTime.libraryFolderDao.getAll() }
    }

    private suspend fun loadLibraryItems() {
        _libraryItems.update {
            PracticeTime.libraryItemDao.get(activeOnly = true).onEach { item ->
                // check if the items folderId actually exists
                item.libraryFolderId?.let {
                    if(PracticeTime.libraryFolderDao.get(it) == null) {
                        item.libraryFolderId = null
                        PracticeTime.libraryItemDao.update(item)
                    }

                }
            }
        }
    }

    // Mutators
    // Add
    fun addLibraryFolder(newFolder: LibraryFolder) {
        coroutineScope.launch {
            PracticeTime.libraryFolderDao.insertAndGet(newFolder)?.let { insertedFolder ->
                _libraryFolders.update { listOf(insertedFolder) + it }
            }
            sortLibrary()
        }
    }

    fun addLibraryItem(item: LibraryItem) {
        coroutineScope.launch {
            PracticeTime.libraryItemDao.insertAndGet(item)?.let { insertedItem ->
                _libraryItems.update { listOf(insertedItem) + it }
            }
            sortLibrary()
        }
    }

    // Edit
    fun editFolder(editedFolder: LibraryFolder) {
        coroutineScope.launch {
            PracticeTime.libraryFolderDao.update(editedFolder)
            _libraryFolders.update { folders ->
                folders.map { if (it.id == editedFolder.id) editedFolder else it }
            }
            sortLibrary()
        }
    }

    fun editItem(item: LibraryItem) {
        coroutineScope.launch {
            PracticeTime.libraryItemDao.update(item)
            sortLibrary()
        }
    }

    // Delete / Archive
    fun deleteFolders(folderIds: List<Long>) {
        coroutineScope.launch {
            folderIds.forEach { folderId ->
                PracticeTime.libraryFolderDao.deleteAndResetItems(folderId)
            }
            _libraryFolders.update { folders ->
                folders.filter { !folderIds.contains(it.id) }
            }
            loadLibraryItems() // reload items to show those which were in a folder
            sortLibrary()
        }
    }

    fun archiveItems(itemIds: List<Long>) {
        coroutineScope.launch {
            itemIds.forEach { itemId ->
                // returns false in case item couldnt be archived
                if (PracticeTime.libraryItemDao.archive(itemId)) {
                    _libraryItems.update { items ->
                        items.filter { it.id == itemId }
                    }
                }
            }
        }
    }

    // Sort
    fun sortLibrary(mode: LibrarySortMode? = null) {
        if(mode != null) {
            if (mode == librarySortMode.value) {
                when (librarySortDirection.value) {
                    SortDirection.ASCENDING -> librarySortDirection.value = SortDirection.DESCENDING
                    SortDirection.DESCENDING -> librarySortDirection.value = SortDirection.ASCENDING
                }
            } else {
                librarySortDirection.value = SortDirection.ASCENDING
                librarySortMode.value = mode
                PracticeTime.prefs.edit().putString(
                    PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_MODE,
                    librarySortMode.value.name
                ).apply()
            }
            PracticeTime.prefs.edit().putString(
                PracticeTime.PREFERENCES_KEY_LIBRARY_SORT_DIRECTION,
                librarySortDirection.value.name
            ).apply()
        }
        when (librarySortDirection.value) {
            SortDirection.ASCENDING -> {
                when (librarySortMode.value) {
                    LibrarySortMode.DATE_ADDED -> {
                        _libraryItems.update { items -> items.sortedBy { it.createdAt } }
                        _libraryFolders.update { folders -> folders.sortedBy { it.createdAt } }
                    }
                    LibrarySortMode.LAST_MODIFIED -> {
                        _libraryItems.update { items -> items.sortedBy { it.modifiedAt } }
                        _libraryFolders.update { folders -> folders.sortedBy { it.modifiedAt } }
                    }
                    LibrarySortMode.NAME -> {
                        _libraryItems.update { items -> items.sortedBy { it.name } }
                        _libraryFolders.update { folders -> folders.sortedBy { it.name } }
                    }
                    LibrarySortMode.COLOR -> {
                        _libraryItems.update { items -> items.sortedBy { it.colorIndex } }
                        _libraryFolders.update { folders -> folders.sortedBy { it.createdAt } }// problem ?
                    }
                    LibrarySortMode.CUSTOM -> TODO()
                }
            }
            SortDirection.DESCENDING -> {
                when (librarySortMode.value) {
                    LibrarySortMode.DATE_ADDED -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.createdAt } }
                        _libraryFolders.update { folders -> folders.sortedByDescending { it.createdAt } }
                    }
                    LibrarySortMode.LAST_MODIFIED -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.modifiedAt } }
                        _libraryFolders.update { folders -> folders.sortedByDescending { it.modifiedAt } }
                    }
                    LibrarySortMode.NAME -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.name } }
                        _libraryFolders.update { folders -> folders.sortedByDescending { it.name } }
                    }
                    LibrarySortMode.COLOR -> {
                        _libraryItems.update { items -> items.sortedByDescending { it.colorIndex } }
                        _libraryFolders.update { folders -> folders.sortedByDescending { it.createdAt } }// problem ?
                    }
                    LibrarySortMode.CUSTOM -> TODO()
                }
            }
        }
    }
}