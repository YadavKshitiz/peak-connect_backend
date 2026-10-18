package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.spacing

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            shape = AppShapes.small,
            textStyle = MaterialTheme.typography.bodyLarge,
            label = label?.let { { Text(text = it) } },
            placeholder = placeholder?.let { { Text(text = it) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = if (isPassword) {
                {
                    val image = if (passwordVisible) {
                        Icons.Filled.Visibility
                    } else {
                        Icons.Filled.VisibilityOff
                    }
                    val description = stringResource(id = R.string.common_password_toggle)
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            } else {
                trailingIcon
            },
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        val helperOrError = when {
            isError && !errorMessage.isNullOrBlank() -> errorMessage
            !supportingText.isNullOrBlank() -> supportingText
            else -> null
        }

        if (helperOrError != null) {
            Text(
                text = helperOrError,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.sm,
                    top = MaterialTheme.spacing.xs
                )
            )
        }
    }
}

@Preview(name = "Light TextField", showBackground = true)
@Composable
private fun AppTextFieldPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        Column(modifier = Modifier.padding(16.dp)) {
            AppTextField(
                value = "john@example.com",
                onValueChange = {},
                label = "Email Address",
                supportingText = "We will never share your email"
            )
            AppTextField(
                value = "password123",
                onValueChange = {},
                label = "Password",
                isPassword = true
            )
            AppTextField(
                value = "Invalid input",
                onValueChange = {},
                label = "Phone",
                isError = true,
                errorMessage = "Invalid phone number format"
            )
        }
    }
}

@Preview(name = "Dark TextField", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AppTextFieldPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        Column(modifier = Modifier.padding(16.dp)) {
            AppTextField(
                value = "john@example.com",
                onValueChange = {},
                label = "Email Address"
            )
            AppTextField(
                value = "secret",
                onValueChange = {},
                label = "Password",
                isPassword = true
            )
        }
    }
}
