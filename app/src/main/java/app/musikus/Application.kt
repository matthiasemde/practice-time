/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde, Michael Prommersberger
 */

package app.musikus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import app.musikus.database.MusikusDatabase
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Provider

const val METRONOME_NOTIFICATION_CHANNEL_ID = "metronome_notification_channel"
const val METRONOME_NOTIFICATION_CHANNEL_NAME = "Metronome notification"

const val SESSION_NOTIFICATION_CHANNEL_ID = "session_notification_channel"
const val SESSION_NOTIFICATION_CHANNEL_NAME = "Session notification"

const val RECORDER_NOTIFICATION_CHANNEL_ID = "recorder_notification_channel"
const val RECORDER_NOTIFICATION_CHANNEL_NAME = "Recorder notification"


@HiltAndroidApp
class Musikus : Application() {

    @Inject
    lateinit var databaseProvider: Provider<MusikusDatabase>

    private val database: MusikusDatabase by lazy { databaseProvider.get() }


    fun importDatabaseCallback(context: Context, uri: Uri?) {
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

    fun exportDatabaseCallback(context: Context, uri: Uri?) {
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


    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val sessionNotificationChannel = NotificationChannel(
                SESSION_NOTIFICATION_CHANNEL_ID,
                SESSION_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Notification to keep track of the running session"
            }

            // Register the channel with the system
            notificationManager.createNotificationChannel(sessionNotificationChannel)

            val metronomeNotificationChannel = NotificationChannel(
                METRONOME_NOTIFICATION_CHANNEL_ID,
                METRONOME_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notification to keep track of the metronome"
            }

            // Register the channel with the system
            notificationManager.createNotificationChannel(metronomeNotificationChannel)

            val recorderNotificationChannel = NotificationChannel(
                RECORDER_NOTIFICATION_CHANNEL_ID,
                RECORDER_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notification to keep track of the recorder"
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(recorderNotificationChannel)
        }
    }

    companion object {
        val executorService: ExecutorService = Executors.newFixedThreadPool(4)
        private val IO_EXECUTOR = Executors.newSingleThreadExecutor()

        fun ioThread(f : () -> Unit) {
            IO_EXECUTOR.execute(f)
        }

        var exportLauncher: ActivityResultLauncher<String>? = null
        var importLauncher: ActivityResultLauncher<Array<String>>? = null

        var noSessionsYet = true
        var serviceIsRunning = false

        const val USER_PREFERENCES_NAME = "user_preferences"

        fun getRandomQuote(context: Context) : CharSequence {
            return context.resources.getTextArray(R.array.quotes).random()
        }

        fun dp(context: Context, dp: Int): Float {
            return context.resources.displayMetrics.density * dp
        }


        fun importDatabase() {
            importLauncher?.launch(arrayOf("*/*"))
        }


        fun exportDatabase() {
            exportLauncher?.launch("musikus_backup")
        }
    }
}