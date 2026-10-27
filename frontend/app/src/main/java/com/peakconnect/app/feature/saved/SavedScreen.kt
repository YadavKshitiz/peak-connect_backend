package com.peakconnect.app.feature.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peakconnect.app.R
import com.peakconnect.app.core.ui.ActivityCard
import com.peakconnect.app.core.ui.EmptyState

@Composable
fun SavedScreen(
    onNavigateToActivityDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel()
) {
    val savedActivities by viewModel.savedActivities.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Text(
                text = stringResource(id = R.string.saved_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            if (savedActivities.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = stringResource(id = R.string.saved_title),
                    message = stringResource(id = R.string.saved_empty_message),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = savedActivities,
                        key = { it.id }
                    ) { activity ->
                        ActivityCard(
                            activity = activity,
                            isFavourite = true,
                            onCardClick = { activityId ->
                                viewModel.onActivityClicked(activityId)
                                onNavigateToActivityDetail(activityId)
                            },
                            onFavouriteToggle = { activityId ->
                                viewModel.toggleFavourite(activityId)
                            }
                        )
                    }
                }
            }
        }
    }
}
