package app.clearsms.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.clearsms.data.db.ReminderDao
import app.clearsms.data.db.ReminderEntity
import app.clearsms.data.prefs.SettingsRepository
import app.clearsms.receiver.ReminderAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules bill-due alarms via [AlarmManager]: one alarm per reminder, one
 * day before the due date.
 *
 * Exact alarms are used when permitted; on Android S+ where the user has not
 * granted `SCHEDULE_EXACT_ALARM`, the scheduler falls back to an inexact
 * while-idle alarm (a reminder a few minutes late is still useful).
 */
@Singleton
class ReminderAlarmScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val reminderDao: ReminderDao,
        private val settingsRepository: SettingsRepository,
    ) {
        /**
         * Alerts-section gate, consulted before ANY alarm registration: the
         * requirement is that a disabled Alerts section stops the WAKE-UPS,
         * not merely the notification at fire time. Both public entry points
         * ([scheduleForMessage], [rescheduleAll]) check it, so neither a new
         * bill message nor a reboot re-registers alarms while Alerts is off.
         * [app.clearsms.ui.settings.SettingsViewModel] pairs the toggle with
         * [cancelUpcoming] / [rescheduleAll]; the notifier's own gate covers
         * any alarm that still slips through the race.
         *
         * Bounded: this runs on the SMS receive path, where a cold DataStore
         * collector can wait on a concurrent write (the hazard documented in
         * DiModule's `hotGate`). An unbounded `.first()` here would stall
         * ingestion and the broadcast's finish() behind a settings write; a
         * read that cannot answer within [GATE_READ_TIMEOUT_MS] falls back to
         * the settings default (on) instead.
         */
        private suspend fun alertsEnabled(): Boolean =
            withTimeoutOrNull(GATE_READ_TIMEOUT_MS) { settingsRepository.enabledSections.first().alerts } ?: true

        /** Schedules the alarm for the reminder extracted from [messageId], if any. */
        suspend fun scheduleForMessage(messageId: Long) {
            if (!alertsEnabled()) return
            reminderDao.findByRawSmsId(messageId)?.let { schedule(it) }
        }

        /** Re-registers alarms for every upcoming reminder (after boot, an import or a re-sort). */
        suspend fun rescheduleAll() {
            if (!alertsEnabled()) return
            val now = System.currentTimeMillis()
            reminderDao.observeUpcoming(now).first().forEach { schedule(it) }
        }

        /**
         * Cancels every upcoming reminder's alarm - the disable-time half of
         * the Alerts gate (alarms registered while the section was on would
         * otherwise still wake the device).
         */
        suspend fun cancelUpcoming() {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val now = System.currentTimeMillis()
            reminderDao.observeUpcoming(now).first().forEach { reminder ->
                alarmManager.cancel(alarmPendingIntent(reminder))
            }
        }

        /** Schedules a single alarm one day before [reminder]'s due date. */
        private fun schedule(reminder: ReminderEntity) {
            val dueDate = reminder.dueDate ?: return
            val triggerAt = triggerTimeFor(dueDate, System.currentTimeMillis()) ?: return
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val operation = alarmPendingIntent(reminder)
            if (canScheduleExact(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        }

        private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        private fun alarmPendingIntent(reminder: ReminderEntity): PendingIntent {
            val intent =
                Intent(context, ReminderAlarmReceiver::class.java)
                    .setAction(ReminderAlarmReceiver.ACTION_BILL_DUE)
                    .putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
                    .putExtra(ReminderAlarmReceiver.EXTRA_BANK_NAME, reminder.bankName)
                    .putExtra(ReminderAlarmReceiver.EXTRA_ACCOUNT_LAST4, reminder.accountLast4)
                    .putExtra(ReminderAlarmReceiver.EXTRA_TOTAL_DUE, reminder.totalDue ?: Double.NaN)
            return PendingIntent.getBroadcast(
                context,
                // Keyed on the SOURCE MESSAGE, not the reminder row id: a
                // re-sort deletes and re-inserts reminders with new ids, and
                // an id-keyed alarm would then survive unreachable (cancel
                // and re-arm both compute the new id) while carrying the old
                // amount. The raw message id is stable for the same bill, so
                // re-arming REPLACES that bill's alarm instead of stacking a
                // second one, and cancelUpcoming can still address it.
                reminder.rawSmsId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        companion object {
            /** Upper bound on the Alerts-gate settings read on the receive path. */
            private const val GATE_READ_TIMEOUT_MS = 500L

            /**
             * The alarm fires one day before the due date (at the same
             * start-of-day instant). Returns null when that moment has
             * already passed - there is nothing useful to schedule.
             */
            fun triggerTimeFor(
                dueDateMs: Long,
                nowMs: Long,
            ): Long? {
                val triggerAt = dueDateMs - TimeUnit.DAYS.toMillis(1)
                return triggerAt.takeIf { it > nowMs }
            }
        }
    }
