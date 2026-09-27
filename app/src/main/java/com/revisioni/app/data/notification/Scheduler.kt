package com.revisioni.app.data.notification

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val WORK_NAME = "deadline_check_daily"

    fun scheduleDailyCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<DeadlineCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runCheckNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DeadlineCheckWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
