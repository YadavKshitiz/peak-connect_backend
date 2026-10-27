package com.peakconnect.app.feature.trekker.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.peakconnect.app.R
import com.peakconnect.app.core.images.ImageUrlResolver
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.ui.AppAsyncImage
import com.peakconnect.app.core.ui.AppChip
import com.peakconnect.app.core.ui.AppPullToRefreshBox
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.DifficultyChip
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import com.peakconnect.app.core.ui.PriceText
import com.peakconnect.app.core.ui.RiskBadge
import com.peakconnect.app.core.ui.SeasonChip
import com.peakconnect.app.core.ui.map.OsmMapView
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.domain.model.CancellationPolicyInfo
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.RiskLevel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onBack: () -> Unit,
    onNavigateToMatches: (slotId: String, isWaitlist: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityDetailViewModel = hiltViewModel()
) {
    val activityWithSlots by viewModel.activityWithSlots.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isFavourite by viewModel.isFavourite.collectAsState()
    val checklistItems by viewModel.checklistItems.collectAsState()
    val isOnline = LocalIsOnline.current

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var selectedRiskForExplanation by remember { mutableStateOf<RiskLevel?>(null) }
    var showChecklistSheet by remember { mutableStateOf(false) }

    val activity = activityWithSlots?.activity

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    LaunchedEffect(Unit) {
        viewModel.snackBarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AppPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh(isManual = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activity != null) {
                val imageUrl = remember(activity.title, activity.location) {
                    ImageUrlResolver.resolve(activity.title, activity.location)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Offline InfoBanner
                    if (!isOnline) {
                        InfoBanner(
                            type = BannerType.INFO,
                            text = stringResource(id = R.string.explore_offline_banner),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Collapsing Hero Area (16:10 aspect ratio with gradient scrim)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                    ) {
                        AppAsyncImage(
                            url = imageUrl,
                            contentDescription = activity.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Top & Bottom gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )

                        // Action Bar: Back, Favourite, Share
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Button
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.common_back),
                                        tint = Color.White
                                    )
                                }
                            }

                            // Right actions (Favourite Heart, Share)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Favourite Heart
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.45f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleFavourite()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = stringResource(
                                                id = if (isFavourite) R.string.explore_favourite_remove else R.string.explore_favourite_add
                                            ),
                                            tint = if (isFavourite) Color(0xFFE53935) else Color.White
                                        )
                                    }
                                }

                                // Share Button
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.45f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val shareText = ActivityDetailUtils.buildShareText(
                                                title = activity.title,
                                                location = activity.location,
                                                lowestPrice = viewModel.getLowestPrice()
                                            )
                                            ActivityDetailUtils.openShareSheet(context, shareText)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = stringResource(id = R.string.activity_detail_share),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Title
                        Text(
                            text = activity.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Location row with pin icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activity.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chips Row: DifficultyChip & PolicyChip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DifficultyChip(level = activity.difficulty)

                            val policyChipLabel = when (activity.cancellationPolicy) {
                                CancellationPolicyType.FLEXIBLE -> R.string.policy_flexible_chip
                                CancellationPolicyType.MODERATE -> R.string.policy_moderate_chip
                                CancellationPolicyType.STRICT -> R.string.policy_strict_chip
                            }
                            AppChip(
                                text = stringResource(id = policyChipLabel),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Description (collapsed to 4 lines with Read more / Show less)
                        if (!activity.description.isNullOrBlank()) {
                            Column(modifier = Modifier.animateContentSize()) {
                                Text(
                                    text = activity.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { isDescriptionExpanded = !isDescriptionExpanded },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = stringResource(
                                            id = if (isDescriptionExpanded) R.string.activity_detail_show_less else R.string.activity_detail_read_more
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Location Card (180dp, 16dp corners, OsmMapView + Get directions button)
                        if (activity.latitude != null && activity.longitude != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(id = R.string.activity_detail_location_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val geoPoint = GeoPoint(activity.latitude, activity.longitude)
                                    OsmMapView(
                                        center = geoPoint,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        zoom = 13.0,
                                        markerPositions = listOf(geoPoint),
                                        markerTitles = listOf(activity.title),
                                        interactive = false
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val noMapAppMsg = stringResource(id = R.string.activity_detail_no_map_app)
                                    OutlinedButton(
                                        onClick = {
                                            val launched = ActivityDetailUtils.openMapDirections(
                                                context = context,
                                                latitude = activity.latitude,
                                                longitude = activity.longitude,
                                                title = activity.title
                                            )
                                            if (!launched) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(noMapAppMsg)
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = AppShapes.small
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Directions,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(id = R.string.activity_detail_get_directions))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Cancellation Policy Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(id = R.string.activity_detail_cancellation_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.activity_detail_cancellation_disclaimer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Tiers table
                                val tiers = CancellationPolicyInfo.getTiers(activity.cancellationPolicy)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppShapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.activity_detail_timeline_header),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(id = R.string.activity_detail_refund_header),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                    tiers.forEach { tier ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = tier.timeline,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = if (tier.refundPercentage > 0) {
                                                    stringResource(id = R.string.activity_detail_refund_percent, tier.refundPercentage)
                                                } else {
                                                    stringResource(id = R.string.activity_detail_no_refund)
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (tier.refundPercentage > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Deposit Note
                                Text(
                                    text = stringResource(
                                        id = R.string.activity_detail_deposit_note,
                                        CancellationPolicyInfo.DEPOSIT_PERCENTAGE_INT
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Packing Checklist Entry Card
                        Card(
                            onClick = { showChecklistSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Checklist,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.checklist_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(id = R.string.checklist_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Available Slots Section
                        Text(
                            text = stringResource(id = R.string.activity_detail_slots_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val upcomingSlots = viewModel.getUpcomingSlots()
                        if (upcomingSlots.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.activity_detail_no_upcoming_slots),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            upcomingSlots.forEach { slot ->
                                SlotCard(
                                    slot = slot,
                                    isOnWaitlist = viewModel.isSlotOnWaitlist(slot.id),
                                    onRiskClick = { selectedRiskForExplanation = slot.riskLevel },
                                    onRequestBooking = { onNavigateToMatches(slot.id, false) },
                                    onJoinWaitlist = { onNavigateToMatches(slot.id, true) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Dynamic pricing info row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(id = R.string.activity_detail_dynamic_pricing_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Risk Explanation Bottom Sheet
    selectedRiskForExplanation?.let { riskLevel ->
        RiskExplanationBottomSheet(
            riskLevel = riskLevel,
            onDismiss = { selectedRiskForExplanation = null }
        )
    }

    // Packing Checklist Bottom Sheet
    if (showChecklistSheet) {
        PackingChecklistBottomSheet(
            items = checklistItems,
            onToggleItem = { viewModel.toggleChecklistItem(it) },
            onAddItem = { category, text -> viewModel.addCustomChecklistItem(category, text) },
            onDeleteItem = { viewModel.deleteChecklistItem(it) },
            onResetChecklist = { viewModel.resetChecklist() },
            onDismiss = { showChecklistSheet = false }
        )
    }
}

@Composable
fun SlotCard(
    slot: SlotEntity,
    isOnWaitlist: Boolean,
    onRiskClick: () -> Unit,
    onRequestBooking: () -> Unit,
    onJoinWaitlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spotsLeft = (slot.capacity - slot.currentOccupancy).coerceAtLeast(0)
    val isFull = spotsLeft <= 0

    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Date & SeasonChip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slot.date.format(dateTimeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SeasonChip(season = slot.season)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Price & RiskBadge (tappable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    PriceText(
                        amount = slot.currentPrice,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.activity_detail_per_person),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Box(modifier = Modifier.clickable { onRiskClick() }) {
                    RiskBadge(riskLevel = slot.riskLevel)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Capacity indicator progress bar & spots left text
            val occupancyProgress = if (slot.capacity > 0) {
                (slot.currentOccupancy.toFloat() / slot.capacity.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (!isFull) {
                            stringResource(id = R.string.activity_detail_spots_left, spotsLeft, slot.capacity)
                        } else {
                            stringResource(id = R.string.activity_detail_spots_full, slot.capacity)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { occupancyProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AppShapes.extraSmall),
                    color = if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Request booking vs Join waitlist vs On the waitlist
            if (!isFull) {
                OnlineOnlyButton(
                    text = stringResource(id = R.string.activity_detail_request_booking),
                    actionLabel = stringResource(id = R.string.activity_detail_action_book),
                    onClick = onRequestBooking,
                    style = OnlineOnlyButtonStyle.PRIMARY
                )
            } else {
                if (isOnWaitlist) {
                    // Disabled "On the waitlist" state
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(text = stringResource(id = R.string.activity_detail_on_waitlist))
                    }
                } else {
                    OnlineOnlyButton(
                        text = stringResource(id = R.string.activity_detail_join_waitlist),
                        actionLabel = stringResource(id = R.string.activity_detail_action_join_waitlist),
                        onClick = onJoinWaitlist,
                        style = OnlineOnlyButtonStyle.SECONDARY
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.activity_detail_waitlist_note),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
