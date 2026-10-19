package com.almeidajr.molda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import app.rive.runtime.kotlin.core.Rive
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Rive.init(this)
        enableEdgeToEdge()
        // Read once here: the package manager is I/O and stays out of composition.
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            MoldaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MoldaRoute(
                        modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
                        versionName = versionName,
                    )
                }
            }
        }
    }
}
