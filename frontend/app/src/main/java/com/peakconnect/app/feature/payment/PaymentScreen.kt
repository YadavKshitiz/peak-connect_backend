package com.peakconnect.app.feature.payment

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppCard
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.LoadingIndicator
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.PriceText
import com.peakconnect.app.core.ui.SecondaryButton
import com.peakconnect.app.core.ui.formatRupees
import com.peakconnect.app.domain.model.Booking
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onViewBooking: (String) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentViewModel = hiltViewModel(),
    paymentManager: PaymentManager? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // Obtain PaymentManager via local or Hilt entry point if not passed
    val manager = remember(paymentManager, context) {
        paymentManager ?: dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            PaymentEntryPoint::class.java
        ).paymentManager()
    }

    LaunchedEffect(viewModel, activity) {
        viewModel.checkoutRequests.collect { request ->
            if (activity != null) {
                manager.startPayment(
                    activity = activity,
                    activityTitle = request.activityTitle,
                    paymentOrderId = request.paymentOrderId,
                    prefillEmail = request.email
                )
            }
        }
    }

    when (val state = uiState) {
        is PaymentUiState.Loading -> {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = stringResource(id = R.string.payment_title),
                        onBackClick = onBack
                    )
                },
                modifier = modifier.fillMaxSize()
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }

        is PaymentUiState.InvalidStatus -> {
            InvalidStatusContent(
                booking = state.booking,
                onBack = onBack,
                modifier = modifier
            )
        }

        is PaymentUiState.Summary -> {
            PaymentSummaryContent(
                state = state,
                onBack = onBack,
                onPayClick = { viewModel.onPayClicked() },
                modifier = modifier
            )
        }

        is PaymentUiState.Processing,
        is PaymentUiState.Success,
        is PaymentUiState.Failed,
        is PaymentUiState.Delayed -> {
            PaymentResultScreen(
                state = state,
                onTryAgain = { viewModel.onTryAgain() },
                onCheckAgain = { viewModel.onCheckAgain() },
                onViewBooking = onViewBooking,
                onBackToBooking = onBack,
                onExplore = onExplore,
                modifier = modifier
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSummaryContent(
    state: PaymentUiState.Summary,
    onBack: () -> Unit,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val booking = state.booking
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.payment_title),
                onBackClick = onBack
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = MaterialTheme.spacing.xs,
                shadowElevation = MaterialTheme.spacing.sm
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.md)
                ) {
                    val depositAmount = booking.depositAmount ?: 0.0
                    val formattedAmount = formatRupees(BigDecimal.valueOf(depositAmount))

                    OnlineOnlyButton(
                        text = stringResource(id = R.string.payment_pay_button, formattedAmount),
                        actionLabel = stringResource(id = R.string.payment_action_label),
                        onClick = onPayClick,
                        enabled = state.isPayEnabled,
                        isLoading = state.isCheckoutInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_pay_button")
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("payment_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Test Mode Banner
            if (state.isTestMode) {
                InfoBanner(
                    type = BannerType.INFO,
                    text = stringResource(id = R.string.payment_test_mode_banner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.spacing.md,
                            vertical = MaterialTheme.spacing.sm
                        )
                        .testTag("payment_test_banner")
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // Summary Card
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.md)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.md)
                ) {
                    Text(
                        text = stringResource(id = R.string.payment_deposit_summary_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    // Activity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = stringResource(id = R.string.payment_activity_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = booking.activityTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(0.65f)
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    // Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.payment_date_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = booking.date.format(dateFormatter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    // Guide
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.payment_guide_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = booking.guideName ?: stringResource(id = R.string.payment_guide_unassigned),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                    // Deposit amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.payment_deposit_amount_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val depositAmount = booking.depositAmount ?: 0.0
                        PriceText(
                            amount = BigDecimal.valueOf(depositAmount),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    // Non-refundable note
                    Text(
                        text = stringResource(id = R.string.payment_deposit_non_refundable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun InvalidStatusContent(
    booking: Booking,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.payment_title),
                onBackClick = onBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(MaterialTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            InfoBanner(
                type = BannerType.WARNING,
                text = stringResource(id = R.string.payment_status_not_payable, booking.status.name),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            SecondaryButton(
                text = stringResource(id = R.string.payment_back_to_booking),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
