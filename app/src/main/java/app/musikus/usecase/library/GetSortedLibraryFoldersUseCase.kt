/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2023 Matthias Emde
 */

package app.musikus.usecase.library

import app.musikus.database.LibraryFolderWithItems
import app.musikus.repository.LibraryRepository
import app.musikus.usecase.userpreferences.GetFolderSortInfoUseCase
import app.musikus.utils.LibraryFolderSortMode
import app.musikus.utils.sorted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetSortedLibraryFoldersUseCase(
    private val libraryRepository: LibraryRepository,
    private val getFolderSortInfo: GetFolderSortInfoUseCase,
) {

    operator fun invoke() : Flow<List<LibraryFolderWithItems>> {
        return combine(
            libraryRepository.folders,
            getFolderSortInfo()
        ) { folders, folderSortInfo ->
            folders.sorted(
                folderSortInfo.mode as LibraryFolderSortMode,
                folderSortInfo.direction
            )
        }
    }
}