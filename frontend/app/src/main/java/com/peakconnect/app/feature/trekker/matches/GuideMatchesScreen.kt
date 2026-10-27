package com.peakconnect.app.feature.trekker.matches

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.EmptyState
import com.peakconnect.app.core.ui.ErrorState
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import com.peakconnect.app.core.ui.PriceText
import com.peakconnect.app.core.ui.ShimmerBox
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.domain.model.MatchedGuide
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideMatchesScreen(
    onBack: () -> Unit,
    onNavigateToReview: (slotId: String, guideId: String) -> Unit,
    onNavigateToWaitlist: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuideMatchesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline = LocalIsOnline.current
    val snackbarHostState = remember { SnackbarHostState() }

    var slotNotFullDialogVisible by remember { mutableStateOf(false) }
    var timeoutDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MatchesUiEvent.WaitlistSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                    onNavigateToWaitlist()
                }
                is MatchesUiEvent.WaitlistSlotNotFull409 -> {
                    slotNotFullDialogVisible = true
                }
                is MatchesUiEvent.WaitlistTimeout -> {
                    timeoutDialogVisible = true
                }
                is MatchesUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // 409 slot not full dialog
    if (slotNotFullDialogVisible) {
        AlertDialog(
            onDismissRequest = { slotNotFullDialogVisible = false },
            title = { Text(text = stringResource(id = R.string.waitlist_409_title)) },
            text = { Text(text = stringResource(id = R.string.waitlist_409_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        slotNotFullDialogVisible = false
                        viewModel.switchToBookMode()
                    }
                ) {
                    Text(text = stringResource(id = R.string.waitlist_409_action_book))
                }
            },
            dismissButton = {
                TextButton(onClick = { slotNotFullDialogVisible = false }) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }

    // Timeout dialog
    if (timeoutDialogVisible) {
        AlertDialog(
            onDismissRequest = { timeoutDialogVisible = false },
            title = { Text(text = stringResource(id = R.string.common_retry)) },
            text = { Text(text = stringResource(id = R.string.waitlist_timeout_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeoutDialogVisible = false
                        onNavigateToWaitlist()
                    }
                ) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { timeoutDialogVisible = false }) {
                    Text(text = stringResource(id = R.string.dismiss))
                }
            }
        )
    }

    // Waitlist Join Confirmation Dialog
    uiState.selectedGuideForWaitlistDialog?.let { guide ->
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isWaitlistSubmitting) viewModel.dismissWaitlistConfirmation()
            },
            title = { Text(text = stringResource(id = R.string.waitlist_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.waitlist_dialog_message,
                        guide.name
                    )
                )
            },
            confirmButton = {
                OnlineOnlyButton(
                    text = stringResource(id = R.string.waitlist_dialog_confirm),
                    actionLabel = stringResource(id = R.string.waitlist_dialog_action_label),
                    onClick = { viewModel.confirmJoinWaitlist() },
                    isLoading = uiState.isWaitlistSubmitting,
                    style = OnlineOnlyButtonStyle.PRIMARY
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissWaitlistConfirmation() },
                    enabled = !uiState.isWaitlistSubmitting
                ) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }

    // Guide Profile Bottom Sheet
    uiState.selectedGuideForProfile?.let { guide ->
        GuideProfileBottomSheet(
            guide = guide,
            isWaitlist = uiState.isWaitlist,
            onDismiss = { viewModel.selectGuideForProfile(null) },
            onSelectGuide = { selectedGuide ->
                viewModel.selectGuideForProfile(null)
                if (uiState.isWaitlist) {
                    viewModel.openWaitlistConfirmation(selectedGuide)
                } else {
                    onNavigateToReview(uiState.slotId, selectedGuide.guideId)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isWaitlist) {
                            stringResource(id = R.string.matches_waitlist_title)
                        } else {
                            stringResource(id = R.string.matches_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (!isOnline) {
            ErrorState(
                message = stringResource(id = R.string.booking_offline_blocked),
                onRetry = { viewModel.fetchMatches() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Slow server warning
            AnimatedVisibility(visible = uiState.isServerWakingUp) {
                InfoBanner(
                    type = BannerType.INFO,
                    text = stringResource(id = R.string.matches_server_waking),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Waitlist info header if in waitlist mode
            if (uiState.isWaitlist) {
                InfoBanner(
                    type = BannerType.INFO,
                    text = stringResource(id = R.string.matches_waitlist_info),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Header summary card: Activity title, slot date/time, price
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = uiState.activity?.title ?: "Activity Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            uiState.slot?.let { slot ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val formattedDate = slot.date.format(
                                        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
                                    )
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                PriceText(
                                    amount = slot.currentPrice,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }

                // "What matters most?" panel
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleWeightsPanel() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.matches_weights_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = if (uiState.isWeightsPanelExpanded) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(
                                visible = uiState.isWeightsPanelExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.matches_weights_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Skills slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.matches_weights_skills),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "${uiState.skillSlider}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = uiState.skillSlider.toFloat(),
                                        onValueChange = { viewModel.onSkillSliderChanged(it.toInt()) },
                                        valueRange = 0f..100f,
                                        steps = 9, // 10 steps: 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Location slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.matches_weights_location),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "${uiState.locationSlider}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = uiState.locationSlider.toFloat(),
                                        onValueChange = { viewModel.onLocationSliderChanged(it.toInt()) },
                                        valueRange = 0f..100f,
                                        steps = 9,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    // Reset button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { viewModel.resetWeights() }) {
                                            Text(text = stringResource(id = R.string.matches_weights_reset))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading skeletons
                if (uiState.isLoading && uiState.guides.isEmpty()) {
                    items(3) {
                        GuideCardSkeleton()
                    }
                } else if (uiState.errorMessage != null && uiState.guides.isEmpty()) {
                    item {
                        ErrorState(
                            message = uiState.errorMessage ?: "Failed to load guides",
                            onRetry = { viewModel.fetchMatches() }
                        )
                    }
                } else if (!uiState.isLoading && uiState.guides.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.PersonSearch,
                            title = stringResource(id = R.string.matches_empty_title),
                            message = "",
                            actionLabel = stringResource(id = R.string.matches_empty_action_back),
                            onAction = onBack,
                            modifier = Modifier.testTag("matches_empty_state")
                        )
                    }
                } else {
                    itemsIndexed(uiState.guides, key = { _, guide -> guide.guideId }) { index, guide ->
                        GuideCard(
                            guide = guide,
                            rank = index + 1,
                            onClick = { viewModel.selectGuideForProfile(guide) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun GuideCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(modifier = Modifier.size(width = 80.dp, height = 24.dp).clip(AppShapes.extraSmall))
                ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 24.dp).clip(AppShapes.extraSmall))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    ShimmerBox(modifier = Modifier.size(width = 140.dp, height = 18.dp).clip(AppShapes.extraSmall))
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.size(width = 90.dp, height = 14.dp).clip(AppShapes.extraSmall))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp).clip(AppShapes.extraSmall))
        }
    }
}
