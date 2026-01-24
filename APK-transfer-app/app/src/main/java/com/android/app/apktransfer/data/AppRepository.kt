// 데이터 계층 패키지 선언.
package com.android.app.apktransfer.data

// Android 컨텍스트 접근을 위해 필요.
import android.content.Context
// 앱 메타데이터 조회에 사용.
import android.content.pm.PackageManager
// UI/뷰모델에 전달할 앱 정보 모델.
import com.android.app.apktransfer.model.AppInfo

// 설치된 앱 목록을 가져오는 리포지토리.
class AppRepository(private val context: Context) {
    // 런처가 있는 앱만 추려 UI에 필요한 모델로 변환한다.
    fun loadInstalledApps(): List<AppInfo> {
        // 패키지 매니저는 앱 메타 정보를 조회하는 핵심 API.
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            // 실행 가능한(런처 인텐트가 있는) 앱만 노출.
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            // UI에서 쓰기 쉬운 AppInfo로 변환.
            .map { app ->
                AppInfo(
                    // 라벨을 문자열로 변환해 표시용 이름으로 사용.
                    name = pm.getApplicationLabel(app).toString(),
                    // 고유 패키지명.
                    packageName = app.packageName,
                    // 기본 APK + split APK 경로까지 모두 수집.
                    apkPaths = listOf(app.sourceDir) + (app.splitSourceDirs ?: emptyArray()),
                    // 아이콘/기타 메타 조회를 위해 원본 저장.
                    applicationInfo = app
                )
            }
    }
}
