package com.peakconnect.app.feature.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.ThemeMode
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.settings.AppearanceDialog
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.profile_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Avatar circle with initials (colour derived from name from theme palette)
            ProfileAvatar(
                name = state.name,
                email = state.email,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // User Name
            Text(
                text = state.name.ifBlank { "User" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            // Email
            Text(
                text = state.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            // Role Chip
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(top = MaterialTheme.spacing.xs)
            ) {
                Text(
                    text = if (state.role == Role.GUIDE) {
                        stringResource(id = R.string.register_role_guide_title)
                    } else {
                        stringResource(id = R.string.register_role_trekker_title)
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))

            // Settings Menu List
            // 1. Appearance
            val appearanceSubtitle = when (state.themeMode) {
                ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
                ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
                ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system)
            }

            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.profile_menu_appearance),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = appearanceSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onAppearanceClicked() }
            )

            // 2. Notifications (placeholder switch row)
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.profile_menu_notifications),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(id = R.string.profile_notifications_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.onNotificationsToggled(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Language (placeholder row)
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.profile_menu_language),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(id = R.string.profile_language_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. About (placeholder row)
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.profile_menu_about),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(id = R.string.profile_about_version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))

            // 5. Log out row
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.profile_menu_logout),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onLogoutClicked() }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
        }
    }

    // Appearance Dialog
    if (state.showAppearanceDialog) {
        AppearanceDialog(
            currentThemeMode = state.themeMode,
            onThemeSelected = { viewModel.onThemeSelected(it) },
            onDismiss = { viewModel.onDismissAppearanceDialog() }
        )
    }

    // Logout Confirmation Dialog
    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissLogoutDialog() },
            title = {
                Text(
                    text = stringResource(id = R.string.dialog_logout_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.dialog_logout_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onConfirmLogout(onNavigateToLogin)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_logout_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDismissLogoutDialog() }
                ) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileAvatar(
    name: String,
    email: String,
    modifier: Modifier = Modifier
) {
    val initials = extractInitials(name, email)
    val colorScheme = MaterialTheme.colorScheme
    val palette = listOf(
        Pair(colorScheme.primary, colorScheme.onPrimary),
        Pair(colorScheme.secondary, colorScheme.onSecondary),
        Pair(colorScheme.tertiary, colorScheme.onTertiary),
        Pair(colorScheme.primaryContainer, colorScheme.onPrimaryContainer),
        Pair(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer),
        Pair(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer)
    )

    val key = name.ifBlank { email }
    val colorPair = palette[(key.hashCode().absoluteValue) % palette.size]

    Box(
        modifier = modifier
            .background(color = colorPair.first, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colorPair.second
        )
    }
}

private fun extractInitials(name: String, email: String): String {
    val trimmedName = name.trim()
    if (trimmedName.isNotBlank()) {
        val parts = trimmedName.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            return "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        } else if (parts.isNotEmpty()) {
            return parts[0].take(2).uppercase()
        }
    }
    val fallback = email.substringBefore("@").trim()
    return if (fallback.isNotBlank()) fallback.take(2).uppercase() else "U"
}
