package com.peakconnect.app.feature.trekker.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.ui.AppAsyncImage
import com.peakconnect.app.core.images.ImageUrlResolver
import com.peakconnect.app.core.ui.AppChip
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.CancellationPolicySummaryCard
import com.peakconnect.app.core.ui.ErrorState
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import com.peakconnect.app.core.ui.PriceText
import com.peakconnect.app.core.ui.formatRupees
import com.peakconnect.app.core.ui.labelRes
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.feature.trekker.matches.getInitials
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingReviewScreen(
    onBack: () -> Unit,
    onBookingSuccess: (bookingId: String) -> Unit,
    onWaitlistSuccess: () -> Unit,
    onNavigateBackToActivity: () -> Unit,
    onNavigateBackToMatches: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline = LocalIsOnline.current
    val snackbarHostState = remember { SnackbarHostState() }

    var waitlistSlotNotFullDialogVisible by remember { mutableStateOf(false) }
    var waitlistTimeoutDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ReviewUiEvent.BookingSuccess -> {
                    onBookingSuccess(event.bookingId)
                }
                is ReviewUiEvent.WaitlistSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                    onWaitlistSuccess()
                }
                is ReviewUiEvent.WaitlistSlotNotFull409 -> {
                    waitlistSlotNotFullDialogVisible = true
                }
                is ReviewUiEvent.WaitlistTimeout -> {
                    waitlistTimeoutDialogVisible = true
                }
                is ReviewUiEvent.NavigateBackToActivity -> {
                    onNavigateBackToActivity()
                }
                is ReviewUiEvent.NavigateBackToMatches -> {
                    onNavigateBackToMatches()
                }
                is ReviewUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // 409 Slot Full Dialog
    if (uiState.showSlotFullDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onSlotFullDismiss(SlotFullAction.BACK_TO_ACTIVITY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(text = stringResource(id = R.string.review_slot_filled_title)) },
            text = { Text(text = stringResource(id = R.string.review_slot_filled_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onSlotFullDismiss(SlotFullAction.JOIN_WAITLIST) }) {
                    Text(text = stringResource(id = R.string.review_slot_filled_waitlist))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onSlotFullDismiss(SlotFullAction.BACK_TO_ACTIVITY) }) {
                    Text(text = stringResource(id = R.string.review_slot_filled_back))
                }
            }
        )
    }

    // General Error Dialog (e.g. 400/409 Guide not available) -> return to matches
    if (uiState.showGeneralErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGeneralErrorDialog() },
            title = { Text(text = stringResource(id = R.string.review_booking_error_title)) },
            text = { Text(text = uiState.generalErrorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissGeneralErrorDialog() }) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            }
        )
    }

    // Waitlist Confirmation Dialog (when switched from 409 slot full)
    if (uiState.showWaitlistConfirmDialog) {
        val guideName = uiState.guide?.name ?: "your guide"
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isWaitlistSubmitting) viewModel.dismissWaitlistConfirmation()
            },
            title = { Text(text = stringResource(id = R.string.waitlist_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.waitlist_dialog_message,
                        guideName
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

    // Waitlist 409 slot has spots dialog
    if (waitlistSlotNotFullDialogVisible) {
        AlertDialog(
            onDismissRequest = { waitlistSlotNotFullDialogVisible = false },
            title = { Text(text = stringResource(id = R.string.waitlist_409_title)) },
            text = { Text(text = stringResource(id = R.string.waitlist_409_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        waitlistSlotNotFullDialogVisible = false
                        viewModel.confirmBooking()
                    }
                ) {
                    Text(text = stringResource(id = R.string.waitlist_409_action_book))
                }
            },
            dismissButton = {
                TextButton(onClick = { waitlistSlotNotFullDialogVisible = false }) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }

    // Waitlist timeout dialog
    if (waitlistTimeoutDialogVisible) {
        AlertDialog(
            onDismissRequest = { waitlistTimeoutDialogVisible = false },
            title = { Text(text = stringResource(id = R.string.common_retry)) },
            text = { Text(text = stringResource(id = R.string.waitlist_timeout_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        waitlistTimeoutDialogVisible = false
                        onWaitlistSuccess()
                    }
                ) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { waitlistTimeoutDialogVisible = false }) {
                    Text(text = stringResource(id = R.string.dismiss))
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
                        text = stringResource(id = R.string.review_title),
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
        },
        bottomBar = {
            // Sticky Bottom Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.review_deposit_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val formattedDeposit = formatRupees(uiState.estimatedDeposit)
                            Text(
                                text = "$formattedDeposit (${uiState.depositPercentage}%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        uiState.slot?.let { slot ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(id = R.string.review_total_price),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PriceText(
                                    amount = slot.currentPrice,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OnlineOnlyButton(
                        text = stringResource(id = R.string.review_action_send_request),
                        actionLabel = stringResource(id = R.string.review_action_book_label),
                        onClick = { viewModel.confirmBooking() },
                        isLoading = uiState.isSubmitting,
                        enabled = !uiState.isSubmitting && uiState.slot != null,
                        style = OnlineOnlyButtonStyle.PRIMARY
                    )
                }
            }
        }
    ) { innerPadding ->
        if (!isOnline) {
            ErrorState(
                message = stringResource(id = R.string.booking_offline_blocked),
                onRetry = { viewModel.loadData() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeout recovery banner if needed
            if (uiState.networkTimeoutError) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoBanner(
                        type = BannerType.ERROR,
                        text = uiState.recoveryMessage ?: "Network error. Tap retry to check status and try again.",
                        actionLabel = stringResource(id = R.string.common_retry),
                        onAction = { viewModel.retryConfirmWithRecoveryCheck() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 1. Activity Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.review_card_activity),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(AppShapes.small)
                            ) {
                                val imageUrl = remember(uiState.activity?.title, uiState.activity?.location) {
                                    ImageUrlResolver.resolve(
                                        uiState.activity?.title ?: "",
                                        uiState.activity?.location ?: ""
                                    )
                                }
                                AppAsyncImage(
                                    url = imageUrl,
                                    contentDescription = uiState.activity?.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.activity?.title ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = uiState.activity?.location ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Date & Time Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.review_card_datetime),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateStr = uiState.slot?.date?.format(
                                DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
                            ) ?: ""
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3. Your Guide Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.review_card_guide),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getInitials(uiState.guide?.name ?: "G"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.guide?.name ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                uiState.guide?.experienceLevel?.let { level ->
                                    AppChip(
                                        text = stringResource(id = level.labelRes()),
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Price Section Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.review_card_price),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Total Price Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.review_total_price),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            uiState.slot?.let { slot ->
                                PriceText(
                                    amount = slot.currentPrice,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Estimated deposit due row
                        val formattedDeposit = formatRupees(uiState.estimatedDeposit)
                        Text(
                            text = stringResource(
                                id = R.string.review_estimated_deposit,
                                formattedDeposit,
                                uiState.depositPercentage
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.review_deposit_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.review_deposit_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Cancellation policy summary (reuse component from Task 7)
            item {
                uiState.activity?.cancellationPolicy?.let { policy ->
                    CancellationPolicySummaryCard(cancellationPolicy = policy)
                }
            }

            // 6. "How booking works" 3-step explainer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.review_how_it_works_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Step 1
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.review_step_1_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(id = R.string.review_step_1_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Step 2
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.review_step_2_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(id = R.string.review_step_2_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Step 3
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.review_step_3_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(id = R.string.review_step_3_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
