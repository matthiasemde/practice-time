/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import app.musikus.Musikus
import app.musikus.database.MusikusDatabase
import app.musikus.utils.ExportDatabaseContract
import app.musikus.utils.ImportDatabaseContract
import app.musikus.utils.PermissionChecker
import app.musikus.utils.PermissionCheckerActivity
import app.musikus.utils.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider


@AndroidEntryPoint
class MainActivity : PermissionCheckerActivity() {

    @Inject
    override lateinit var permissionChecker: PermissionChecker

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var databaseProvider: Provider<MusikusDatabase>

    private val database: MusikusDatabase by lazy { databaseProvider.get() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Musikus.exportLauncher = registerForActivityResult(
            ExportDatabaseContract()
        ) { exportDatabaseCallback(applicationContext, it) }

        Musikus.importLauncher = registerForActivityResult(
            ImportDatabaseContract()
        ) { importDatabaseCallback(applicationContext, it) }

        setContent {
            MusikusApp(timeProvider)
        }
    }

    private fun importDatabaseCallback(context: Context, uri: Uri?) {
        uri?.let {
            // close the database to collect all logs
            database.close()

            val databaseFile = context.getDatabasePath(MusikusDatabase.DATABASE_NAME)

            // delete old database
            databaseFile.delete()

            // copy new database
            databaseFile.outputStream().let { outputStream ->
                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                }
                outputStream.close()

                Toast.makeText(context, "Backup loaded successfully, restart your app to complete the process.", Toast.LENGTH_LONG).show()
            }
        }

        // open database again
//                openDatabase(context)
    }

    private fun exportDatabaseCallback(context: Context, uri: Uri?) {
        uri?.let {

            // close the database to collect all logs
            database.close()

            val databaseFile = context.getDatabasePath(MusikusDatabase.DATABASE_NAME)

            // copy database
            context.contentResolver.openOutputStream(it)?.let { outputStream ->
                databaseFile.inputStream().let { inputStream ->
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                }
                outputStream.close()

                Toast.makeText(context, "Backup successful", Toast.LENGTH_LONG).show()
            }

            // open database again
//                openDatabase(context)
        }
    }
}
