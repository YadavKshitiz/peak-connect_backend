package com.peakconnect.app.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.TertiaryTextButton
import kotlinx.coroutines.launch

private data class OnboardingPageData(
    val titleRes: Int,
    val bodyRes: Int,
    val primaryIcon: ImageVector,
    val secondaryIcon: ImageVector,
    val containerColor: @Composable () -> Color,
    val contentColor: @Composable () -> Color
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPageData(
            titleRes = R.string.onboarding_page1_title,
            bodyRes = R.string.onboarding_page1_body,
            primaryIcon = Icons.Outlined.Terrain,
            secondaryIcon = Icons.Outlined.Explore,
            containerColor = { MaterialTheme.colorScheme.primaryContainer },
            contentColor = { MaterialTheme.colorScheme.onPrimaryContainer }
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_page2_title,
            bodyRes = R.string.onboarding_page2_body,
            primaryIcon = Icons.Outlined.LocalOffer,
            secondaryIcon = Icons.Outlined.Shield,
            containerColor = { MaterialTheme.colorScheme.secondaryContainer },
            contentColor = { MaterialTheme.colorScheme.onSecondaryContainer }
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_page3_title,
            bodyRes = R.string.onboarding_page3_body,
            primaryIcon = Icons.Outlined.Hiking,
            secondaryIcon = Icons.Outlined.People,
            containerColor = { MaterialTheme.colorScheme.tertiaryContainer },
            contentColor = { MaterialTheme.colorScheme.onTertiaryContainer }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TertiaryTextButton(
                        text = stringResource(id = R.string.onboarding_skip),
                        onClick = {
                            viewModel.completeOnboarding(onFinish)
                        },
                        modifier = Modifier.width(88.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spacing.lg)
                .padding(bottom = MaterialTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                OnboardingPageView(page = page)
            }

            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.lg)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(durationMillis = 250),
                        label = "dot_width"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        animationSpec = tween(durationMillis = 250),
                        label = "dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // Action Button
            PrimaryButton(
                text = stringResource(
                    id = if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next
                ),
                onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding(onFinish)
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingPageView(
    page: OnboardingPageData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Illustration with soft circular background
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(page.containerColor()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.primaryIcon,
                contentDescription = null,
                tint = page.contentColor(),
                modifier = Modifier.size(96.dp)
            )
            // Smaller overlapping secondary icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.secondaryIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))

        Text(
            text = stringResource(id = page.titleRes),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

        Text(
            text = stringResource(id = page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
