package app.clearsms.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues the always-on periodic background jobs as unique work, keeping any
 * schedule that already exists.
 *
 * The automatic backup is deliberately NOT here: its schedule is derived from
 * the user's backup frequency, whose only writer is
 * [BackupWorker.applyFrequency]. Enqueuing it unconditionally re-created a
 * 24-hour wake-up at every boot that the user had explicitly switched off
 * (the worker then cancelled itself on each run, every day, forever).
 */
object WorkScheduler {
    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            OtpAutoDeleteWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<OtpAutoDeleteWorker>(6, TimeUnit.HOURS).build(),
        )
    }
}
