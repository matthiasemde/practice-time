/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.ui.library

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.musikus.di.AppModule
import app.musikus.ui.MainActivity
import app.musikus.ui.MainViewModel
import app.musikus.ui.Screen
import com.google.android.material.composethemeadapter3.Mdc3Theme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import app.musikus.R
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.LibraryItemSortMode
import app.musikus.utils.SortDirection
import app.musikus.utils.SortInfo
import app.musikus.utils.SortMode
import app.musikus.utils.TestTags


@HiltAndroidTest
@UninstallModules(AppModule::class)
class LibraryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            val mainViewModel: MainViewModel = hiltViewModel()

            val navController = rememberNavController()
            Mdc3Theme {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Library.route
                ) {
                    composable(Screen.Library.route) {
                        LibraryScreen(mainViewModel = mainViewModel)
                    }
                }
            }
        }
    }

    @Test
    fun clickFab_multiFabMenuIsShown() {
        composeRule.onNodeWithContentDescription("Folder").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Item").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Add").performClick()

        composeRule.onNodeWithContentDescription("Folder").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Item").assertIsDisplayed()
    }

    @Test
    fun addFolderOrItem_hintDisappears() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Check if hint is displayed initially
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertIsDisplayed()

        // Add a folder
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Folder").performClick()
        composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput("Test")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if hint is not displayed anymore
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertDoesNotExist()

        // Remove the folder
        composeRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete").performClick()

        // Check if hint is displayed again
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertIsDisplayed()

        // Add an item
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("Test")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if hint is not displayed anymore
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertDoesNotExist()

        // Remove the item
        composeRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Delete").performClick()

        // Check if hint is displayed again
        composeRule.onNodeWithText(context.getString(R.string.libraryHint)).assertIsDisplayed()
    }

    @Test
    fun addItemToFolderFromInsideAndOutside() {

        // Add a folder
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Folder").performClick()
        composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput("TestFolder")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Add an item from outside the folder
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithContentDescription("Item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("TestItem1")
        composeRule.onNodeWithContentDescription("Select folder").performClick()

        composeRule.onNode(
            matcher = hasAnyAncestor(hasTestTag(TestTags.ITEM_DIALOG_FOLDER_SELECTOR_DROPDOWN))
            and
            hasText("TestFolder")
        ).performClick()
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Open folder
        composeRule.onNodeWithText("TestFolder").performClick()

        // Check if item is displayed
        composeRule.onNodeWithText("TestItem1").assertIsDisplayed()

        // Add an item from inside the folder (folder should be pre-selected)
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("TestItem2")
        composeRule.onNodeWithContentDescription("Create").performClick()

        // Check if item is displayed
        composeRule.onNodeWithText("TestItem2").assertIsDisplayed()
    }

    private fun addFolders(numberOfFolders: Int = 3): List<Int> {
        val order = (1..numberOfFolders).shuffled()
        order.forEach { i ->
            composeRule.onNodeWithContentDescription("Add").performClick()
            composeRule.onNodeWithContentDescription("Folder").performClick()
            composeRule.onNodeWithTag(TestTags.FOLDER_DIALOG_NAME_INPUT).performTextInput("Test$i")
            composeRule.onNodeWithContentDescription("Create").performClick()
        }
        return order
    }

    private fun addItems(numberOfItems: Int = 3): List<Pair<Int,Int>> {
        val order = (1..numberOfItems).shuffled()
        val orderWithColors = (order).map { it to (1 .. 10).random() }
        orderWithColors.forEach { (i, color) ->
            composeRule.onNodeWithContentDescription("Add").performClick()
            composeRule.onNodeWithContentDescription("Item").performClick()
            composeRule.onNodeWithTag(TestTags.ITEM_DIALOG_NAME_INPUT).performTextInput("Test$i")
            composeRule.onNode(
                matcher = hasAnyAncestor(isDialog())
                        and
                        hasContentDescription("Color $color")
            ).performClick()
            composeRule.onNodeWithContentDescription("Create").performClick()
        }
        return orderWithColors
    }

    private fun clickSortMode(sortMode: SortMode<*>) {
        val sortModeType = when(sortMode) {
            is LibraryItemSortMode -> "items"
            is LibraryFolderSortMode -> "folders"
            else -> throw Exception("Unknown sort mode type")
        }
        composeRule.onNodeWithContentDescription("Select sort mode and direction for $sortModeType").performClick()
        // Select name as sorting mode
        composeRule.onNode(
            matcher = hasAnyAncestor(hasContentDescription("List of sort modes for $sortModeType"))
                    and
                    hasText(sortMode.label)
        ).performClick()
    }

    private fun testSortMode(sortInfo: SortInfo<*>) {
        val testItems = 3

        val (order, colors) = when(sortInfo.mode) {
            is LibraryItemSortMode -> addItems(testItems).unzip()
            is LibraryFolderSortMode -> addFolders(testItems) to listOf()
            else -> throw Exception("Unknown sort mode type")
        }

        // Change sorting mode
        if (!sortInfo.mode.isDefault) clickSortMode(sortInfo.mode)
        if (sortInfo.direction != SortDirection.DEFAULT) clickSortMode(sortInfo.mode)

        // Check if items are displayed in correct order
        val itemNodes = composeRule.onAllNodes(hasText("Test", substring = true))

        itemNodes.assertCountEquals(testItems)
        for (i in 0 until testItems - 1) {
            val itemNumber = if (sortInfo.direction == SortDirection.DESCENDING) testItems - i else i + 1
            when {
                (
                    sortInfo.mode == LibraryItemSortMode.DATE_ADDED ||
                    sortInfo.mode == LibraryFolderSortMode.DATE_ADDED
                ) -> itemNodes[i].assertTextContains("Test${order[itemNumber - 1]}")
                (
                    sortInfo.mode == LibraryItemSortMode.NAME ||
                    sortInfo.mode == LibraryFolderSortMode.NAME
                ) -> itemNodes[i].assertTextContains("Test${itemNumber}")
                sortInfo.mode == LibraryItemSortMode.COLOR ->
                    itemNodes[i].assert(
                        hasAnySibling(
                            hasContentDescription("Color ${colors.sorted()[itemNumber - 1]}")
                        )
                    )
                (
                    sortInfo.mode == LibraryItemSortMode.LAST_MODIFIED ||
                    sortInfo.mode == LibraryFolderSortMode.LAST_MODIFIED
                ) ->
                    itemNodes[i].assertTextContains("Test${order[itemNumber - 1]}")
                else -> throw Exception("Unknown sort mode type")
            }
        }
    }

    @Test
    fun saveNewItems_orderByDateAddedDescending() {
        testSortMode(SortInfo(LibraryItemSortMode.DATE_ADDED, SortDirection.DESCENDING))
    }

    @Test
    fun saveNewItems_orderByDateAddedAscending() {
        testSortMode(SortInfo(LibraryItemSortMode.DATE_ADDED, SortDirection.ASCENDING))
    }

    @Test
    fun saveNewItems_orderByNameDescending() {
        testSortMode(SortInfo(LibraryItemSortMode.NAME, SortDirection.DESCENDING))
    }

    @Test
    fun saveNewItems_orderByNameAscending() {
        testSortMode(SortInfo(LibraryItemSortMode.NAME, SortDirection.ASCENDING))
    }

    @Test
    fun saveNewItems_orderByColorDescending() {
        testSortMode(SortInfo(LibraryItemSortMode.COLOR, SortDirection.DESCENDING))
    }

    @Test
    fun saveNewItems_orderByColorAscending() {
        testSortMode(SortInfo(LibraryItemSortMode.COLOR, SortDirection.ASCENDING))
    }

    @Test
    fun saveNewItems_orderByLastModifiedDescending() {
        testSortMode(SortInfo(LibraryItemSortMode.LAST_MODIFIED, SortDirection.DESCENDING))
    }

    @Test
    fun saveNewItems_orderByLastModifiedAscending() {
        testSortMode(SortInfo(LibraryItemSortMode.LAST_MODIFIED, SortDirection.ASCENDING))
    }

    @Test
    fun saveNewFolders_orderByDateAddedDescending() {
        testSortMode(SortInfo(LibraryFolderSortMode.DATE_ADDED, SortDirection.DESCENDING))
    }

    @Test
    fun saveNewFolders_orderByDateAddedAscending() {
        testSortMode(SortInfo(LibraryFolderSortMode.DATE_ADDED, SortDirection.ASCENDING))
    }

    @Test
    fun saveNewFolders_orderByNameDescending() {
        testSortMode(SortInfo(LibraryFolderSortMode.NAME, SortDirection.DESCENDING))
    }

    @Test
    fun saveNewFolders_orderByNameAscending() {
        testSortMode(SortInfo(LibraryFolderSortMode.NAME, SortDirection.ASCENDING))
    }

    @Test
    fun saveNewFolders_orderByLastModifiedDescending() {
        testSortMode(SortInfo(LibraryFolderSortMode.LAST_MODIFIED, SortDirection.DESCENDING))
    }

    @Test
    fun saveNewFolders_orderByLastModifiedAscending() {
        testSortMode(SortInfo(LibraryFolderSortMode.LAST_MODIFIED, SortDirection.ASCENDING))
    }
}