/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 */

package app.musikus.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musikus.dataStore
import app.musikus.database.Nullable
import app.musikus.database.PTDatabase
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.datastore.ThemeSelections
import app.musikus.repository.GoalRepository
import app.musikus.repository.LibraryRepository
import app.musikus.repository.SessionRepository
import app.musikus.repository.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    /** Database */
    private val database = PTDatabase.getInstance(application, ::prepopulateDatabase)

    /** Initialization */
    init {
        // Clean up soft-deleted items
        viewModelScope.launch {
            Log.d("Musikus", "Clean up soft-deleted items")
            SessionRepository(database).clean()
            GoalRepository(database).clean()
            LibraryRepository(database).clean()
        }
    }

    /** Repositories */
    private val userPreferencesRepository = UserPreferencesRepository(application.dataStore)
    private val libraryRepository = LibraryRepository(database)

    private fun prepopulateDatabase() {
        Log.d("MainViewModel", "prepopulateDatabase")
        viewModelScope.launch {
            listOf(
                LibraryFolderCreationAttributes(name = "Schupra"),
                LibraryFolderCreationAttributes(name = "Fagott"),
                LibraryFolderCreationAttributes(name = "Gesang"),
            ).forEach {
                libraryRepository.addFolder(it)
                Log.d("MainActivity", "Folder ${it.name} created")
                delay(1500) //make sure folders have different createdAt values
            }

            libraryRepository.folders.collect { folders ->
                // populate the libraryItem table on first run
                val items = listOf(
                    LibraryItemCreationAttributes(name = "Die Schöpfung", colorIndex = 0, libraryFolderId = Nullable(folders[0].id)),
                    LibraryItemCreationAttributes(name = "Beethoven Septett",colorIndex = 1,libraryFolderId = Nullable(folders[0].id)),
                    LibraryItemCreationAttributes(name = "Schostakowitsch 9.", colorIndex = 2, libraryFolderId = Nullable(folders[1].id)),
                    LibraryItemCreationAttributes(name = "Trauermarsch c-Moll", colorIndex = 3, libraryFolderId = Nullable(folders[1].id)),
                    LibraryItemCreationAttributes(name = "Adagio", colorIndex = 4, libraryFolderId = Nullable(folders[2].id)),
                    LibraryItemCreationAttributes(name = "Eine kleine Gigue", colorIndex = 5, libraryFolderId = Nullable(folders[2].id)),
                    LibraryItemCreationAttributes(name = "Andantino", colorIndex = 6),
                    LibraryItemCreationAttributes(name = "Klaviersonate", colorIndex = 7),
                    LibraryItemCreationAttributes(name = "Trauermarsch", colorIndex = 8),
                )

                items.forEach {
                    libraryRepository.addItem(it)
                    Log.d("MainActivity", "LibraryItem ${it.name} created")
                    delay(1500) //make sure items have different createdAt values
                }
            }
        }
    }

    /** Menu */

    var showMainMenu = mutableStateOf(false)
    var showThemeSubMenu = mutableStateOf(false)

    /** Import/Export */
    var showExportImportDialog = mutableStateOf(false)

    /** Content Scrim over NavBar for Multi FAB etc */

    val showNavBarScrim = mutableStateOf(false)


    /** Snackbar */

    val snackbarHostState = MutableStateFlow(SnackbarHostState())

    fun showSnackbar(message: String, onUndo: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = snackbarHostState.value.showSnackbar(
                message,
                actionLabel = if(onUndo != null) "Undo" else null,
                duration = SnackbarDuration.Long
            )
            when(result) {
                SnackbarResult.ActionPerformed -> {
                    onUndo?.invoke()
                }
                SnackbarResult.Dismissed -> {
                    // do nothing
                }
            }
        }
    }

    /** Theme */

    val activeTheme = userPreferencesRepository.userPreferences.map { it.theme }

    fun setTheme(theme: ThemeSelections) {
        viewModelScope.launch {
            userPreferencesRepository.updateTheme(theme)
        }
    }
}