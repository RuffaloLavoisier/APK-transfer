package com.android.app.apktransfer.data

import android.content.Context
import android.content.pm.PackageManager
import com.android.app.apktransfer.model.AppInfo

class AppRepository(private val context: Context) {
    fun loadInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                AppInfo(
                    name = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    apkPaths = listOf(app.sourceDir) + (app.splitSourceDirs ?: emptyArray()),
                    applicationInfo = app
                )
            }
    }
}
