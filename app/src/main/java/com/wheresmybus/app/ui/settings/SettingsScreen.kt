package com.wheresmybus.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.AccentYellow
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.HeaderGreen
import java.time.Year

@Composable
fun SettingsScreen(
    language: AppLanguage,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    onContactUsClick: () -> Unit,
    onFeedbackClick: () -> Unit,
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    val versionLabel = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrDefault("—")
    }

    Column(modifier = Modifier.fillMaxSize().background(BgCream)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGreen)
                .padding(16.dp),
        ) {
            Text(strings.settingsTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            item {
                SectionLabel(strings.settingsPreferences)
                SettingsCard {
                    SettingsRow(
                        icon = null,
                        title = strings.darkMode,
                        trailing = { Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange) },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel(strings.settingsInformation)
                SettingsCard {
                    SettingsRow(Icons.Filled.Shield, strings.privacyPolicyTitle, onClick = onPrivacyPolicyClick)
                    Divider()
                    SettingsRow(Icons.Filled.Info, strings.aboutUsTitle, onClick = onAboutUsClick)
                    Divider()
                    SettingsRow(Icons.Filled.ContactMail, strings.support, onClick = onContactUsClick)
                    Divider()
                    SettingsRow(Icons.Filled.Feedback, strings.sendFeedback, onClick = onFeedbackClick)
                }

                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel(strings.settingsApp)
                SettingsCard {
                    SettingsRow(Icons.Filled.PhoneAndroid, strings.settingsAppVersion, trailing = {
                        Text(versionLabel, color = Color.Gray, fontSize = 13.sp)
                    })
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "© ${Year.now().value}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = HeaderGreen,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(content = content)
    }
}

@Composable
private fun Divider() {
    androidx.compose.material3.HorizontalDivider(color = Color(0xFFEFEFEF))
}

@Composable
private fun SettingsRow(
    icon: ImageVector?,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentYellow.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
