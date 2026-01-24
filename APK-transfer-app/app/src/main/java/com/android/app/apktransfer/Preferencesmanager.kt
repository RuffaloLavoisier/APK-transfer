// 앱 기본 패키지 선언.
package com.android.app.apktransfer

// SharedPreferences 접근을 위한 컨텍스트.
import android.content.Context
// 키-값 저장소 인터페이스.
import android.content.SharedPreferences

/**
 * 앱 설정을 저장하고 불러오는 클래스
 * SharedPreferences를 사용하여 앱 재시작 후에도 설정 유지
 */
object PreferencesManager {
    // SharedPreferences 파일명.
    private const val PREF_NAME = "apk_transfer_settings"
    // 서버 URL 저장 키.
    private const val KEY_SERVER_URL = "server_url"
    // 기본 서버 URL.
    private const val DEFAULT_SERVER_URL = "http://192.168.45.163:80"

    /**
     * SharedPreferences 인스턴스 가져오기
     */
    private fun getPreferences(context: Context): SharedPreferences {
        // 앱 전용 SharedPreferences를 반환한다.
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 서버 URL 저장
     * @param context Context 객체
     * @param url 저장할 서버 URL
     */
    fun saveServerUrl(context: Context, url: String) {
        // 변경 사항을 비동기로 저장한다.
        getPreferences(context).edit().apply {
            putString(KEY_SERVER_URL, url)
            apply()
        }
    }

    /**
     * 서버 URL 불러오기
     * @param context Context 객체
     * @return 저장된 서버 URL (없으면 기본값)
     */
    fun getServerUrl(context: Context): String {
        // 저장된 값이 없으면 기본값을 반환한다.
        return getPreferences(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL)
            ?: DEFAULT_SERVER_URL
    }

    /**
     * 모든 설정 초기화
     * @param context Context 객체
     */
    fun clearAll(context: Context) {
        // 모든 설정 값을 제거한다.
        getPreferences(context).edit().clear().apply()
    }
}
