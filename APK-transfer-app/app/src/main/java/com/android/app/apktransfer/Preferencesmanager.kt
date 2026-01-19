package com.android.app.apktransfer

import android.content.Context
import android.content.SharedPreferences

/**
 * 앱 설정을 저장하고 불러오는 클래스
 * SharedPreferences를 사용하여 앱 재시작 후에도 설정 유지
 */
object PreferencesManager {
    private const val PREF_NAME = "apk_transfer_settings"
    private const val KEY_SERVER_URL = "server_url"
    private const val DEFAULT_SERVER_URL = "http://192.168.45.163:80"

    /**
     * SharedPreferences 인스턴스 가져오기
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 서버 URL 저장
     * @param context Context 객체
     * @param url 저장할 서버 URL
     */
    fun saveServerUrl(context: Context, url: String) {
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
        return getPreferences(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL)
            ?: DEFAULT_SERVER_URL
    }

    /**
     * 모든 설정 초기화
     * @param context Context 객체
     */
    fun clearAll(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}