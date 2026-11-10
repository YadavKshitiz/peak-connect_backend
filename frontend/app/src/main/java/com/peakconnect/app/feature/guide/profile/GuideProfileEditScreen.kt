package com.peakconnect.app.feature.guide.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.map.OsmMapView
import com.peakconnect.app.domain.model.ExperienceLevel
import org.osmdroid.util.GeoPoint

private val suggestedSkills = listOf(
    "High Altitude",
    "Wilderness First Aid",
    "Navigation",
    "Flora & Fauna",
    "Camp Setup",
    "Rock Climbing",
    "Winter Trekking"
)

private val suggestedLanguages = listOf(
    "English",
    "Hindi",
    "Punjabi",
    "Nepali",
    "Garhwali"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GuideProfileEditScreen(
    onBack: () -> Unit,
    onNavigateToMapPicker: (Double?, Double?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuideProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var skillInput by remember { mutableStateOf("") }
    var languageInput by remember { mutableStateOf("") }

    val successMsg = stringResource(id = R.string.guide_profile_save_success)

    LaunchedEffect(uiState.isSavedSuccess) {
        if (uiState.isSavedSuccess) {
            snackbarHostState.showSnackbar(successMsg)
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.guide_profile_edit_title),
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
        ) {
            // Validation error message banner if present
            if (uiState.validationError != null) {
                Text(
                    text = uiState.validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("profile_validation_error")
                )
            }

            // 1. Skills section
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.guide_profile_skills_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                // Input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = skillInput,
                        onValueChange = { skillInput = it },
                        placeholder = { Text(stringResource(id = R.string.guide_profile_skills_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (skillInput.isNotBlank()) {
                                viewModel.addSkill(skillInput)
                                skillInput = ""
                            }
                        }),
                        shape = AppShapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("skill_input_field")
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Button(
                        onClick = {
                            if (skillInput.isNotBlank()) {
                                viewModel.addSkill(skillInput)
                                skillInput = ""
                            }
                        },
                        shape = AppShapes.small,
                        modifier = Modifier.testTag("add_skill_btn")
                    ) {
                        Text(stringResource(id = R.string.guide_profile_skills_add))
                    }
                }

                // Selected Skills chips
                if (uiState.skills.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                        modifier = Modifier.padding(top = MaterialTheme.spacing.xs)
                    ) {
                        uiState.skills.forEach { skill ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(skill) },
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { viewModel.removeSkill(skill) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "${stringResource(id = R.string.common_remove)} $skill",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                shape = AppShapes.small,
                                modifier = Modifier.testTag("selected_skill_$skill")
                            )
                        }
                    }
                }

                // Suggested Skills
                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    suggestedSkills.filter { !uiState.skills.contains(it) }.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { viewModel.addSkill(suggestion) },
                            label = { Text(suggestion) },
                            shape = AppShapes.small
                        )
                    }
                }
            }

            // 2. Languages section
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.guide_profile_languages_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                // Input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = languageInput,
                        onValueChange = { languageInput = it },
                        placeholder = { Text(stringResource(id = R.string.guide_profile_languages_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (languageInput.isNotBlank()) {
                                viewModel.addLanguage(languageInput)
                                languageInput = ""
                            }
                        }),
                        shape = AppShapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("language_input_field")
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Button(
                        onClick = {
                            if (languageInput.isNotBlank()) {
                                viewModel.addLanguage(languageInput)
                                languageInput = ""
                            }
                        },
                        shape = AppShapes.small,
                        modifier = Modifier.testTag("add_language_btn")
                    ) {
                        Text(stringResource(id = R.string.guide_profile_languages_add))
                    }
                }

                // Selected Languages chips
                if (uiState.languages.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                        modifier = Modifier.padding(top = MaterialTheme.spacing.xs)
                    ) {
                        uiState.languages.forEach { lang ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(lang) },
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { viewModel.removeLanguage(lang) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "${stringResource(id = R.string.common_remove)} $lang",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                shape = AppShapes.small,
                                modifier = Modifier.testTag("selected_lang_$lang")
                            )
                        }
                    }
                }

                // Suggested Languages
                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    suggestedLanguages.filter { !uiState.languages.contains(it) }.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { viewModel.addLanguage(suggestion) },
                            label = { Text(suggestion) },
                            shape = AppShapes.small
                        )
                    }
                }
            }

            // 3. Experience Level segmented options
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.guide_profile_experience_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                ) {
                    ExperienceLevel.entries.forEach { level ->
                        val isSelected = uiState.experienceLevel == level
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setExperienceLevel(level) },
                            label = {
                                Text(
                                    text = level.name.lowercase().replaceFirstChar { it.uppercase() }
                                )
                            },
                            shape = AppShapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("exp_chip_${level.name.lowercase()}")
                        )
                    }
                }
            }

            // 4. Base Location Text
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.guide_profile_location_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = { viewModel.setLocation(it) },
                    placeholder = { Text(stringResource(id = R.string.guide_profile_location_hint)) },
                    singleLine = true,
                    shape = AppShapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_input_field")
                )
            }

            // 5. Location on map Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_location_card"),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                ) {
                    Text(
                        text = stringResource(id = R.string.guide_profile_map_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val hasCoords = uiState.latitude != null && uiState.longitude != null
                    if (hasCoords) {
                        val geo = GeoPoint(uiState.latitude!!, uiState.longitude!!)
                        OsmMapView(
                            center = geo,
                            zoom = 14.0,
                            markerPositions = listOf(geo),
                            interactive = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(AppShapes.small)
                        )
                        Text(
                            text = stringResource(
                                id = R.string.map_picker_coords_format,
                                uiState.latitude!!,
                                uiState.longitude!!
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onNavigateToMapPicker(uiState.latitude, uiState.longitude)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("choose_on_map_btn"),
                        shape = AppShapes.small
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                        Text(text = stringResource(id = R.string.guide_profile_map_action_choose))
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            // Save Button
            OnlineOnlyButton(
                text = stringResource(id = R.string.guide_profile_action_save),
                actionLabel = stringResource(id = R.string.guide_profile_action_save_label),
                onClick = { viewModel.saveProfile() },
                isLoading = uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_profile_button")
            )
        }
    }
}
