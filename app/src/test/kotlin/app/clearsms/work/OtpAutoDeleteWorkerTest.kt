package app.clearsms.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import app.clearsms.domain.model.OtpAutoDeletePolicy
import app.clearsms.testing.FakeMessageRepository
import app.clearsms.testing.FakeSettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

/**
 * Regression test for the OTP auto-delete path.
 *
 * The worker previously deleted through [app.clearsms.data.db.MessageDao] directly,
 * which only removed the app's own Room rows. Android's Telephony provider was
 * left untouched, so "auto-deleted" OTPs stayed on the device and reappeared on
 * reinstall or as soon as another app held the default-SMS role. The worker must
 * delegate to [app.clearsms.data.repository.MessageRepository.deleteOtpOlderThan],
 * the one batched path that also syncs the provider delete.
 */
@RunWith(RobolectricTestRunner::class)
class OtpAutoDeleteWorkerTest {
    private lateinit var context: Context
    private val fakeSettings = FakeSettingsRepository()

    /** Cutoffs the worker asked the repository to delete at, in call order. */
    private val requestedCutoffs = mutableListOf<Long>()

    private val fakeRepository =
        object : FakeMessageRepository() {
            override suspend fun deleteOtpOlderThan(cutoffMs: Long): Int {
                requestedCutoffs += cutoffMs
                return 0
            }
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        requestedCutoffs.clear()
    }

    private fun buildWorker(): OtpAutoDeleteWorker =
        TestListenableWorkerBuilder<OtpAutoDeleteWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker =
                        OtpAutoDeleteWorker(appContext, workerParameters, fakeRepository, fakeSettings)
                },
            )
            .build()

    @Test
    fun `auto-delete goes through the repository so the provider is purged`() =
        runBlocking {
            fakeSettings.otpAutoDeletePolicy.value = OtpAutoDeletePolicy.HOURS_24

            val before = System.currentTimeMillis()
            val result = buildWorker().doWork()
            val after = System.currentTimeMillis()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            // Exactly one repository call: the DAO must not be touched directly.
            assertThat(requestedCutoffs).hasSize(1)

            val cutoff = requestedCutoffs.single()
            val expectedEarliest = before - TimeUnit.HOURS.toMillis(24)
            val expectedLatest = after - TimeUnit.HOURS.toMillis(24)
            assertThat(cutoff).isAtLeast(expectedEarliest)
            assertThat(cutoff).isAtMost(expectedLatest)
        }

    @Test
    fun `never policy deletes nothing`() =
        runBlocking {
            fakeSettings.otpAutoDeletePolicy.value = OtpAutoDeletePolicy.NEVER

            val result = buildWorker().doWork()

            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            assertThat(requestedCutoffs).isEmpty()
        }

    @Test
    fun `three month policy cuts off ninety days back`() =
        runBlocking {
            fakeSettings.otpAutoDeletePolicy.value = OtpAutoDeletePolicy.MONTHS_3

            val before = System.currentTimeMillis()
            buildWorker().doWork()
            val after = System.currentTimeMillis()

            val cutoff = requestedCutoffs.single()
            assertThat(cutoff).isAtLeast(before - TimeUnit.DAYS.toMillis(90))
            assertThat(cutoff).isAtMost(after - TimeUnit.DAYS.toMillis(90))
        }
}
