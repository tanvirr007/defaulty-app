package app.defaulty.ui.others

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.defaulty.R
import app.defaulty.domain.model.MediaHandlerType
import app.defaulty.domain.model.SupportedRole
import app.defaulty.ui.components.DefaultAppRow
import app.defaulty.ui.components.DefaultyTopBar
import app.defaulty.ui.home.HomeViewModel

/**
 * Dedicated full-view screen for "Others" defaults.
 * Categorizes Media & File handlers, secondary Android system roles,
 * and deep link / app link management in organized cards.
 */
@Composable
fun OthersScreen(
    onNavigateToDetails: (SupportedRole) -> Unit,
    onNavigateToMediaDetails: (MediaHandlerType) -> Unit,
    onNavigateToLinks: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh on resume to reflect external changes immediately
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            DefaultyTopBar(
                title = stringResource(R.string.other_defaults),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (uiState.isLoading && !uiState.hasOtherContent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Section 1: Media & File Defaults (Video, Gallery, Music, PDF, Email)
                if (uiState.mediaDefaults.isNotEmpty()) {
                    item(key = "header_media") {
                        SectionHeader(text = stringResource(R.string.section_media_defaults))
                    }
                    item(key = "media_defaults_card") {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                uiState.mediaDefaults.forEachIndexed { index, info ->
                                    DefaultAppRow(
                                        roleIcon = info.type.icon,
                                        roleLabel = stringResource(info.type.displayLabelRes),
                                        appName = info.holderAppLabel ?: stringResource(R.string.no_default_set),
                                        appIcon = info.holderAppIcon,
                                        onClick = { onNavigateToMediaDetails(info.type) },
                                        containerColor = Color.Transparent,
                                    )
                                    if (index < uiState.mediaDefaults.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Additional System Roles (Notes, Wallet, Emergency, Call Screening, etc.)
                if (uiState.otherRoleDefaults.isNotEmpty()) {
                    item(key = "header_roles") {
                        SectionHeader(text = stringResource(R.string.section_system_roles))
                    }
                    item(key = "other_roles_card") {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                uiState.otherRoleDefaults.forEachIndexed { index, info ->
                                    DefaultAppRow(
                                        roleIcon = info.role.icon,
                                        roleLabel = info.role.displayLabel,
                                        appName = info.holderAppLabel ?: stringResource(R.string.no_default_set),
                                        appIcon = info.holderAppIcon,
                                        onClick = { onNavigateToDetails(info.role) },
                                        containerColor = Color.Transparent,
                                    )
                                    if (index < uiState.otherRoleDefaults.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: "Opening links" entry
                item(key = "header_links") {
                    SectionHeader(text = stringResource(R.string.opening_links))
                }
                item(key = "opening_links_card") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DefaultAppRow(
                            roleIcon = Icons.Default.Link,
                            roleLabel = stringResource(R.string.opening_links),
                            appName = stringResource(R.string.opening_links_subtitle),
                            appIcon = null,
                            onClick = onNavigateToLinks,
                            containerColor = Color.Transparent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .semantics { heading() },
    )
}

