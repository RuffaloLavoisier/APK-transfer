// 앱 기본 패키지 선언.
package com.android.app.apktransfer

// 액티비티 생명주기 상태 전달용.
import android.os.Bundle
// Compose 호스트 액티비티.
import androidx.activity.ComponentActivity
// Compose UI 트리를 연결.
import androidx.activity.compose.setContent
// 머티리얼 테마 설정.
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
// 색상 정의.
import androidx.compose.ui.graphics.Color
// 메인 화면 컴포저블.
import com.android.app.apktransfer.ui.APKTransferScreen

// 앱 진입점 액티비티.
class MainActivity : ComponentActivity() {
    // 액티비티 생성 시 Compose UI를 구성한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        // 기본 초기화 호출.
        super.onCreate(savedInstanceState)

        // Compose UI 트리를 설정한다.
        setContent {
            // 다크 컬러 스킴을 지정해 앱 톤을 통일한다.
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF4FACFE),
                    secondary = Color(0xFF00F2FE),
                    tertiary = Color(0xFF667EEA),
                    background = Color(0xFF0F0F1E),
                    surface = Color(0xFF1A1A2E),
                    surfaceVariant = Color(0xFF252542),
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                // 메인 화면을 렌더링한다.
                APKTransferScreen()
            }
        }
    }
}
