package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.configurator.ConfiguratorScreen
import com.example.core.model.LockSurfacePhase
import com.example.locksurface.LockScreenSimulatorScreen
import com.example.locksurface.LockSurfaceViewModel
import com.example.ui.theme.TelaJogoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            val viewModel: LockSurfaceViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> viewModel.startSensors()
                        Lifecycle.Event.ON_STOP -> viewModel.stopSensorsAndPersist()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    viewModel.stopSensorsAndPersist()
                }
            }

            if (uiState.phase == LockSurfacePhase.CONFIGURATOR) {
                BackHandler {
                    viewModel.lockDeviceBackToSimulator()
                }
            } else if (uiState.phase == LockSurfacePhase.UNLOCK_REQUIRED) {
                BackHandler {
                    viewModel.cancelUnlockBackToLock()
                }
            }

            TelaJogoTheme(oledPowerSaver = uiState.settings.oledPowerSaver) {
                Crossfade(
                    targetState = uiState.phase == LockSurfacePhase.CONFIGURATOR,
                    label = "rootSurfaceCrossfade",
                ) { isUnlockedConfigurator ->
                    if (isUnlockedConfigurator) {
                        ConfiguratorScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                        )
                    } else {
                        LockScreenSimulatorScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}
