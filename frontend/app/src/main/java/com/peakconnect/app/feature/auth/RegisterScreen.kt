package com.peakconnect.app.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onSuccess: (Role) -> Unit,
    onNavigateToLogin: () -> Unit,
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
                .padding(top = MaterialTheme.spacing.lg, bottom = MaterialTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(size = 56.dp)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Text(
                text = stringResource(id = R.string.register_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = stringResource(id = R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            // Role Selector: Two large selectable cards (NEVER ADMIN)
            Text(
                text = stringResource(id = R.string.register_role_selector_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.xs)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                RoleSelectionCard(
                    title = stringResource(id = R.string.register_role_trekker_title),
                    subtitle = stringResource(id = R.string.register_role_trekker_subtitle),
                    isSelected = state.selectedRole == Role.TREKKER,
                    onSelect = { viewModel.onRoleSelected(Role.TREKKER) },
                    modifier = Modifier.weight(1f)
                )

                RoleSelectionCard(
                    title = stringResource(id = R.string.register_role_guide_title),
                    subtitle = stringResource(id = R.string.register_role_guide_subtitle),
                    isSelected = state.selectedRole == Role.GUIDE,
                    onSelect = { viewModel.onRoleSelected(Role.GUIDE) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Guide Approval Info Card
            AnimatedVisibility(
                visible = state.selectedRole == Role.GUIDE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InfoBanner(
                    type = BannerType.INFO,
                    text = stringResource(id = R.string.register_guide_approval_notice),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Full Name Field
            AppTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChanged(it) },
                label = stringResource(id = R.string.register_field_name),
                placeholder = "Alex Trekker",
                isError = state.nameTouched && state.nameErrorRes != null,
                errorMessage = state.nameErrorRes?.let { stringResource(id = it) },
                leadingIcon = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

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
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Confirm Password Field
            AppTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                label = stringResource(id = R.string.register_field_confirm_password),
                isPassword = true,
                isError = state.confirmPasswordTouched && state.confirmPasswordErrorRes != null,
                errorMessage = state.confirmPasswordErrorRes?.let { stringResource(id = it) },
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
                text = stringResource(id = R.string.register_button),
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onSubmit(onSuccess)
                },
                isLoading = state.isLoading,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            // Have account link
            TertiaryTextButton(
                text = stringResource(id = R.string.register_have_account),
                onClick = onNavigateToLogin,
                enabled = !state.isLoading
            )
        }
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        shape = AppShapes.medium,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
