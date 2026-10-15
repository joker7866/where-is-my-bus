package com.wheresmybus.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wheresmybus.app.notification.NotificationHelper
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.nav.WhereIsMyBusApp
import com.wheresmybus.app.ui.theme.WhereIsMyBusTheme

class MainActivity : ComponentActivity() {

    private val busViewModel: BusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)

        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemDarkTheme) }
            WhereIsMyBusTheme(darkTheme = darkTheme) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { }
                LaunchedEffect(Unit) {
                    val permissions = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }
                WhereIsMyBusApp(
                    viewModel = busViewModel,
                    darkTheme = darkTheme,
                    onDarkThemeChange = { darkTheme = it },
                )
            }
        }
    }
}
