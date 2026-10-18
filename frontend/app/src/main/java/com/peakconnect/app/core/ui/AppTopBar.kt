package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.PeakConnectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    centerAligned: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val navigationIcon: @Composable () -> Unit = {
        if (onBackClick != null) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.common_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    val titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface
    )

    if (centerAligned) {
        CenterAlignedTopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    } else {
        TopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Light TopBar", showBackground = true)
@Composable
private fun AppTopBarPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        AppTopBar(
            title = "Explore Treks",
            onBackClick = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Dark TopBar", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AppTopBarPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        AppTopBar(
            title = "Activity Details",
            onBackClick = {}
        )
    }
}
