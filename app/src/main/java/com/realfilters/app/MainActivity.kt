package com.realfilters.app

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfilters.app.ui.screens.MainScreen
import com.realfilters.app.ui.screens.ThemeMode
import com.realfilters.app.ui.screens.ThemeViewModel
import com.realfilters.app.ui.theme.RealFiltersTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingImageUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Try to take a persistable read permission so the URI survives process death.
        extractImageUri(intent)?.let { uri ->
            tryTakePersistableRead(uri)
            pendingImageUri = uri
        }
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RealFiltersTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        themeViewModel = themeViewModel,
                        initialImageUri = pendingImageUri,
                        onInitialImageConsumed = { pendingImageUri = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Do NOT call setContent here - the existing composition is reused.
        // Surface the new URI through the state and clear it once consumed.
        extractImageUri(intent)?.let { uri ->
            tryTakePersistableRead(uri)
            pendingImageUri = uri
        }
    }

    private fun tryTakePersistableRead(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // URI was not granted with FLAG_GRANT_PERSISTABLE_URI_PERMISSION; that's fine
        } catch (_: Throwable) {
            // Some content providers don't support persistable permissions; ignore
        }
    }

    private fun extractImageUri(intent: Intent?): Uri? {
        if (intent == null) return null
        val type = intent.type ?: return null
        if (!type.startsWith("image/")) return null

        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }
    }
}
