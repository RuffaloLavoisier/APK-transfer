// 모델 패키지 선언.
package com.android.app.apktransfer.model

// 앱 메타 정보를 담는 Android 프레임워크 타입.
import android.content.pm.ApplicationInfo


// 앱 목록/전송 화면에서 필요한 정보를 묶어 전달하는 DTO.
data class AppInfo(
    // 사용자에게 보여줄 앱 이름.
    val name: String,
    // 고유 패키지명(식별자).
    val packageName: String,
    // 기본 APK + split APK들의 경로 목록.
    val apkPaths: List<String>,
    // 아이콘/라벨 로딩에 필요한 ApplicationInfo 원본.
    val applicationInfo: ApplicationInfo
)
