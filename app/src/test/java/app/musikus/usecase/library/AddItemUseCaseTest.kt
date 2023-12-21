/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.Nullable
import app.musikus.database.entities.InvalidLibraryItemException
import app.musikus.database.entities.LibraryFolderCreationAttributes
import app.musikus.database.entities.LibraryItemCreationAttributes
import app.musikus.repository.FakeLibraryRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AddItemUseCaseTest {
    private lateinit var addItem: AddItemUseCase
    private lateinit var fakeRepository: FakeLibraryRepository

    private val validItemCreationAttributes = LibraryItemCreationAttributes(
        name = "test",
        libraryFolderId = Nullable(null),
        colorIndex = 0
    )

    @BeforeEach
    fun setUp() {
        fakeRepository = FakeLibraryRepository()
        addItem = AddItemUseCase(fakeRepository)

        val folderCreationAttributes = LibraryFolderCreationAttributes(
            name = "test",
        )

        runBlocking {
            fakeRepository.addFolder(folderCreationAttributes)
        }
    }

    @Test
    fun `Add item with empty name, InvalidLibraryItemException('Item name cannot be empty')`() {
        val exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                addItem(validItemCreationAttributes.copy(name = ""))
            }
        }
        assertThat(exception.message).isEqualTo("Item name cannot be empty")
    }


    @Test
    fun `Add item with invalid colorIndex, InvalidLibraryItemException('Color index must be between 0 and 9')`() {
        var exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                addItem(validItemCreationAttributes.copy(colorIndex = -1))
            }
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")

        exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                addItem(validItemCreationAttributes.copy(colorIndex = 10))
            }
        }
        assertThat(exception.message).isEqualTo("Color index must be between 0 and 9")
    }


    @Test
    fun `Add item with non existent folderId, InvalidLibraryItemException('Folder (FOLDER_ID) does not exist')`() {
        val randomId = UUID.randomUUID()
        val exception = assertThrows<InvalidLibraryItemException> {
            runBlocking {
                addItem(validItemCreationAttributes.copy(libraryFolderId = Nullable(randomId)))
            }
        }
        assertThat(exception.message).isEqualTo("Folder (${randomId}) does not exist")
    }


    @Test
    fun `Add valid item to root, true`() {
        runBlocking {
            addItem(validItemCreationAttributes.copy(libraryFolderId = Nullable(null)))

            val addedItem = fakeRepository.items.first().first()

            assertThat(addedItem.name).isEqualTo(validItemCreationAttributes.name)
            assertThat(addedItem.colorIndex).isEqualTo(validItemCreationAttributes.colorIndex)
            assertThat(addedItem.libraryFolderId).isEqualTo(null)
        }
    }

    @Test
    fun `Add valid item to folder, true`() {
        runBlocking {
            val folder = fakeRepository.folders.first().first().folder

            addItem(validItemCreationAttributes.copy(libraryFolderId = Nullable(folder.id)))

            val addedItem = fakeRepository.items.first().first()

            assertThat(addedItem.name).isEqualTo(validItemCreationAttributes.name)
            assertThat(addedItem.colorIndex).isEqualTo(validItemCreationAttributes.colorIndex)

            val folderItems = fakeRepository.folders.first().first().items

            assertThat(folderItems).contains(addedItem)
        }
    }
}