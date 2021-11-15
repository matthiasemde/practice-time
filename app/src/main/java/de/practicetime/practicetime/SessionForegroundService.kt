package de.practicetime.practicetime

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import de.practicetime.practicetime.entities.PracticeSection
import java.util.*
import kotlin.collections.ArrayList





class SessionForegroundService : Service() {
    private val CHANNEL_ID = "PracticeTime Notification Channel ID"
    private val NOTIFICATION_ID = 42
    private val binder = LocalBinder()         // interface for clients that bind
    private var allowRebind: Boolean = true    // indicates whether onRebind should be used

    var sessionActive = false     // keep track of whether a session is active
    // the sectionBuffer will keep track of all the section in the current session
    var sectionBuffer = ArrayList<Pair<PracticeSection, Int>>()
    var paused = false            // flag if session is currently paused
    var pauseDuration = 0         // pause duration, ONLY for displaying on the fab, section pause duration is safed in sectionBuffer!

    var totalPracticeDuration = 0

    override fun onCreate() {
        // The service is being created
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is starting, due to a call to startService()
        startTimer()

        createNotificationChannel()
        // set the Service to foregrund to display the notification
        // this is different to displaying the notification via notify() since it automatically
        // produces a non-cancellable notification
        startForeground(NOTIFICATION_ID, getNotification( "Sepp, Sepp", "sei kein Depp"))

        return START_NOT_STICKY
    }

    private fun startTimer() {
        sessionActive = true
        Handler(Looper.getMainLooper()).also {
            it.post(object : Runnable {
                override fun run() {
                    if (sectionBuffer.isNotEmpty()) {
                        val firstSection = sectionBuffer.first()

                        if (paused) {
                            // increment pause time. Since Pairs<> are not mutable (but ArrayList is)
                            // we have to copy the element and replace the whole element in the ArrayList
                            sectionBuffer[sectionBuffer.lastIndex] =
                                sectionBuffer.last().copy(second = sectionBuffer.last().second + 1)

                            pauseDuration++
                        }

                        val now = Date().time / 1000
                        // calculate total time of all sections (including pauses)
                        totalPracticeDuration = (now - firstSection.first.timestamp).toInt()
                        // subtract all pause durations
                        sectionBuffer.forEach { section ->
                            totalPracticeDuration -= section.second
                        }
                        updateNotification()
                    }
                    // post the code again with a delay of 1 second
                    it.postDelayed(this, 1000)
                }
            })
        }
    }

    /**
     * updates the notification text continuously to show elapsed time
     */
    private fun updateNotification() {
        val title = "Practicing for %02d:%02d:%02d".format(
            totalPracticeDuration / 3600,
            totalPracticeDuration % 3600 / 60,
            totalPracticeDuration % 60
        )
        var desc = ""
        desc = if (paused) {
            "Practicing paused"
        } else {
            "Active Category: ${sectionBuffer.last().first.category_id}"
        }

        val notification: Notification = getNotification(title, desc)
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getNotification(title: String, contentText: String) : Notification {
        val resultIntent = Intent(this, ActiveSessionActivity::class.java)
        // Create the TaskStackBuilder for artificially creating
        // a back stack based on android:parentActivityName in AndroidManifest.xml
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val icon = if (paused) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        return  NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(resultPendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    // the "channel" is required for new Notifications from Oreo onwards https://stackoverflow.com/a/47533338
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PracticeTime Channel"
            val descriptionText = "This is the description of the Notification Channel of PracticeTime"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        // A client is binding to the service with bindService()
        return binder
    }

    fun startNewSection(categoryId: Int) {
        val now = Date().time / 1000L
        sectionBuffer.add(
            Pair(
                PracticeSection(
                    0,  // 0 means auto-increment
                    null,
                    categoryId,
                    null,
                    now,
                ),
                0
            )
        )
    }

    fun endSection() {
        // save duration of last section
        sectionBuffer.last().first.apply {
            duration = getDuration(this)
        }
    }

    /**
     * calculates total Duration (including pauses) of a section
     */
    private fun getDuration(section: PracticeSection): Int {
        val now = Date().time / 1000L
        return (now - section.timestamp).toInt()
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        return allowRebind
    }

    override fun onRebind(intent: Intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called

        // TODO notify activity that Session is running
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of SessionForegroundService so clients can call public methods
        fun getService(): SessionForegroundService = this@SessionForegroundService
    }

}