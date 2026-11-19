package com.example.locksurface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.UnlockReason
import com.example.ui.theme.CrimsonGuard
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.LocalTelaJogoTokens
import com.example.ui.theme.OledPureBlack
import com.example.ui.theme.PureWhite

@Composable
fun UnlockGateOverlay(
    reason: UnlockReason?,
    pinError: String?,
    onSubmitPin: (String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalTelaJogoTokens.current
    var enteredPin by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledPureBlack.copy(alpha = 0.96f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, CyanPulse, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Barreira Sandbox",
                    tint = CyanPulse,
                )
            }

            Text(
                text = stringResource(R.string.unlock_title),
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite,
                textAlign = TextAlign.Center,
            )

            Text(
                text = reason?.message ?: stringResource(R.string.unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.mutedText,
                textAlign = TextAlign.Center,
            )

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) CyanPulse else OledPureBlack)
                            .border(1.5.dp, if (filled) CyanPulse else tokens.hairlineBorder, CircleShape),
                    )
                }
            }

            if (pinError != null) {
                Text(
                    text = pinError,
                    style = MaterialTheme.typography.labelMedium,
                    color = CrimsonGuard,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = stringResource(R.string.unlock_pin_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.mutedText,
                )
            }

            // Numeric PIN keypad (48dp+ touch targets)
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { key ->
                            PinKeyButton(
                                key = key,
                                onClick = {
                                    when (key) {
                                        "DEL" -> if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }

                                        "BIO" -> onBiometricUnlock()
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                val updated = enteredPin + key
                                                enteredPin = updated
                                                if (updated.length == 4) {
                                                    onSubmitPin(updated)
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("unlock_cancel_button"),
                ) {
                    Text(stringResource(R.string.unlock_cancel_btn))
                }
                Button(
                    onClick = onBiometricUnlock,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("unlock_biometric_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Biometria")
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    key: String,
    onClick: () -> Unit,
) {
    val tokens = LocalTelaJogoTokens.current
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tokens.hairlineBorder, CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_key_$key"),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            "DEL" -> Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Apagar dígito",
                tint = PureWhite,
            )

            "BIO" -> Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = "Biometria",
                tint = EmeraldSignal,
            )

            else -> Text(
                text = key,
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite,
            )
        }
    }
}
