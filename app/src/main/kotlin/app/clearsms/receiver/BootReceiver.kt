package app.clearsms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.clearsms.di.ApplicationScope
import app.clearsms.diagnostics.Diag
import app.clearsms.ui.common.UiPrefs
import app.clearsms.work.BackupWorker
import app.clearsms.work.MessageScheduler
import app.clearsms.work.ReminderAlarmScheduler
import app.clearsms.work.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores background work after a reboot: periodic WorkManager jobs are
 * re-enqueued and every pending alarm - bill-due reminders and scheduled
 * sends - is re-registered (AlarmManager alarms do not survive a reboot).
 * A scheduled send whose time passed while the device was off fires
 * immediately.
 *
 * TIME_SET / TIMEZONE_CHANGED re-arm scheduled sends too: their alarms were
 * registered against the old wall clock, and "send at 9:00" means 9:00 on
 * the clock the user sees now. All three actions are protected broadcasts
 * only the system can send.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var reminderAlarmScheduler: ReminderAlarmScheduler

    @Inject
    lateinit var messageScheduler: MessageScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var uiPrefs: UiPrefs

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                WorkScheduler.scheduleAll(context)
                val pendingResult = goAsync()
                applicationScope.launch {
                    try {
                        // Re-apply the user's backup frequency: its schedule
                        // must survive a reboot, and an OFF must stay off
                        // (scheduling the worker unconditionally is what used
                        // to wake the app every 24 h for a feature the user
                        // had disabled).
                        runCatching { BackupWorker.applyFrequency(context, uiPrefs.backupFrequency.first()) }
                            .onFailure { Diag.w(TAG, "backup schedule re-apply after boot failed", it) }
                        // Each re-arm is isolated: a failure while rescheduling
                        // one kind (a bad alarm, a closed database mid-restore)
                        // must never leave the OTHER kind unarmed - a pending
                        // scheduled send silently lost until the next reboot is
                        // worse than a missed bill alarm.
                        runCatching { reminderAlarmScheduler.rescheduleAll() }
                            .onFailure { Diag.w(TAG, "reminder re-arm after boot failed", it) }
                        runCatching { messageScheduler.rearmAll() }
                            .onFailure { Diag.w(TAG, "scheduled-send re-arm after boot failed", it) }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                val pendingResult = goAsync()
                applicationScope.launch {
                    try {
                        messageScheduler.rearmAll()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
