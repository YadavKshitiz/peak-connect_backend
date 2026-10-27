package com.peakconnect.app.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppTextField
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.Logo
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.TertiaryTextButton
import com.peakconnect.app.domain.model.Role

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSuccess: (Role) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.successRole) {
        state.successRole?.let { role ->
            onSuccess(role)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg)
                .padding(top = MaterialTheme.spacing.xl, bottom = MaterialTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Logo(size = 72.dp)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Text(
                text = stringResource(id = R.string.login_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = stringResource(id = R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // Session Expired Banner
            AnimatedVisibility(
                visible = state.showSessionExpiredBanner,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InfoBanner(
                    type = BannerType.WARNING,
                    text = stringResource(id = R.string.session_expired_message),
                    actionLabel = stringResource(id = R.string.dismiss),
                    onAction = { viewModel.onDismissSessionExpiredBanner() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = MaterialTheme.spacing.md)
                )
            }

            // Email Field
            AppTextField(
                value = state.email,
                onValueChange = { viewModel.onEmailChanged(it) },
                label = stringResource(id = R.string.login_field_email),
                placeholder = "alex@example.com",
                isError = state.emailTouched && state.emailErrorRes != null,
                errorMessage = state.emailErrorRes?.let { stringResource(id = it) },
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Password Field
            AppTextField(
                value = state.password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = stringResource(id = R.string.login_field_password),
                isPassword = true,
                isError = state.passwordTouched && state.passwordErrorRes != null,
                errorMessage = state.passwordErrorRes?.let { stringResource(id = it) },
                leadingIcon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.onSubmit(onSuccess)
                    }
                ),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // Dismissible Backend Error Message above submit button
            AnimatedVisibility(
                visible = state.backendError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.backendError?.let { errorText ->
                    Surface(
                        shape = AppShapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = MaterialTheme.spacing.md)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onDismissBackendError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(id = R.string.dismiss),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Submit Button
            PrimaryButton(
                text = stringResource(id = R.string.login_button),
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onSubmit(onSuccess)
                },
                isLoading = state.isLoading,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Create Account Link
            TertiaryTextButton(
                text = stringResource(id = R.string.login_create_account),
                onClick = onNavigateToRegister,
                enabled = !state.isLoading
            )
        }
    }
}
