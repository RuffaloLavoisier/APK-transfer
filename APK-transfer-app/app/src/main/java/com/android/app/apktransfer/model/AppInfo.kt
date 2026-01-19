package com.android.app.apktransfer.model

import android.content.pm.ApplicationInfo


data class AppInfo(
    val name: String,
    val packageName: String,
    val apkPaths: List<String>,
    val applicationInfo: ApplicationInfo
)
