package com.eliteguardian.app.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eliteguardian.app.ui.theme.*
import com.eliteguardian.app.vpn.GuardianVpnService

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var isVpnAuthorized by remember { mutableStateOf(VpnService.prepare(context) == null) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isVpnAuthorized = true
        }
    }

    Scaffold(
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { step ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (step <= currentStep) PrimaryIndigo else DarkSurfaceVariant)
                    )
                }
            }

            // Step Content
            when (currentStep) {
                0 -> WelcomeStep()
                1 -> HowItWorksStep()
                2 -> PermissionsStep(
                    isVpnAuthorized = isVpnAuthorized,
                    onRequestVpn = {
                        val prepareIntent = VpnService.prepare(context)
                        if (prepareIntent != null) {
                            vpnLauncher.launch(prepareIntent)
                        } else {
                            isVpnAuthorized = true
                        }
                    }
                )
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 2) {
                            currentStep++
                        } else {
                            if (isVpnAuthorized) {
                                GuardianVpnService.startService(context)
                            }
                            onFinished()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (currentStep == 2) "Get Started" else "Continue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = com.eliteguardian.app.R.drawable.app_logo),
            contentDescription = "EliteGuardian Logo",
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome to EliteGuardian",
            style = Typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "A private, 100% free adult content blocker for Android. Zero ads, zero subscriptions, zero tracking.",
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HowItWorksStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "How Filtering Works",
            style = Typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureBullet(
            icon = Icons.Default.Security,
            title = "Local DNS Sinkholing",
            desc = "All filtering happens 100% offline on your device using a local VPN loopback. Your browsing history is never uploaded or monitored."
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureBullet(
            icon = Icons.Default.Lock,
            title = "Enforced SafeSearch",
            desc = "Automatically redirects Google, Bing, YouTube, and DuckDuckGo to strict family-friendly modes."
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureBullet(
            icon = Icons.Default.CheckCircle,
            title = "What It Cannot Block",
            desc = "DNS filtering blocks website domains and servers. It cannot inspect encrypted payloads in private messaging apps."
        )
    }
}

@Composable
private fun PermissionsStep(
    isVpnAuthorized: Boolean,
    onRequestVpn: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Setup & Authorization",
            style = Typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "EliteGuardian requires Android VPN authorization to inspect DNS queries locally on your device.",
            style = Typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isVpnAuthorized) Icons.Default.CheckCircle else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isVpnAuthorized) ShieldGreen else PrimaryIndigo,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isVpnAuthorized) "VPN Permission Granted" else "VPN Permission Required",
                    style = Typography.titleMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isVpnAuthorized)
                        "Local DNS filter is authorized and ready to activate."
                    else
                        "Tap below to grant standard Android VPN permission.",
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRequestVpn,
                    enabled = !isVpnAuthorized,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVpnAuthorized) ShieldGreen else PrimaryIndigo,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isVpnAuthorized) "Authorized ✓" else "Authorize VPN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryIndigoLight,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = Typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, style = Typography.bodyMedium, color = TextSecondary)
        }
    }
}

