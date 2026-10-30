package com.driftwalls.app.data.rotation

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What network a scheduled rotation needs before WorkManager runs it. */
enum class NetworkNeed { NONE, ANY, UNMETERED }

/** Museum images need a download; [RotationSettings.wifiOnly] narrows that to unmetered networks. */
fun RotationSettings.networkNeed(): NetworkNeed = when {
    category.isGenerated && source == RotationSource.CATEGORY -> NetworkNeed.NONE
    wifiOnly -> NetworkNeed.UNMETERED
    else -> NetworkNeed.ANY
}

@Singleton
class RotationScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Starts, updates or cancels the periodic rotation to match [settings]. */
    fun sync(settings: RotationSettings) {
        if (!settings.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val network = when (settings.networkNeed()) {
            NetworkNeed.NONE -> NetworkType.NOT_REQUIRED
            NetworkNeed.ANY -> NetworkType.CONNECTED
            NetworkNeed.UNMETERED -> NetworkType.UNMETERED
        }
        val request = PeriodicWorkRequestBuilder<RotationWorker>(settings.interval.minutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(network)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()
        // UPDATE keeps the current period's timing when only constraints change.
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** When the next scheduled change may run, or null when rotation is off. */
    fun nextRunMillis(): Flow<Long?> = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).map { infos ->
        infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            ?.nextScheduleTimeMillis
            ?.takeIf { it != Long.MAX_VALUE }
    }

    private companion object {
        const val WORK_NAME = "rotation"
    }
}

@HiltWorker
class RotationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: RotationEngine,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        // Skipped (e.g. empty favourites) is not an error: try again next period.
        engine.rotateNow()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
