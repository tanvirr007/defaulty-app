package app.defaulty.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import app.defaulty.domain.model.SupportedRole
import app.defaulty.ui.components.DefaultAppRow
import app.defaulty.ui.components.DefaultyTopBar

/**
 * Main dashboard screen.
 * Displays high-frequency "Your defaults" (Browser, Phone, SMS, Launcher, Assistant)
 * inside grouped card containers and a dedicated entry card for "Others".
 */
@Composable
fun HomeScreen(
    onNavigateToDetails: (SupportedRole) -> Unit,
    onNavigateToOthers: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh on resume — detect changes made externally
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
                title = stringResource(R.string.app_name),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (uiState.isLoading && uiState.primaryDefaults.isEmpty()) {
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
                // Section 1: "Your defaults" (Primary grouped card)
                if (uiState.primaryDefaults.isNotEmpty()) {
                    item(key = "header_primary") {
                        Text(
                            text = stringResource(R.string.your_defaults),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .semantics { heading() },
                        )
                    }

                    item(key = "primary_defaults_card") {
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
                                uiState.primaryDefaults.forEachIndexed { index, info ->
                                    DefaultAppRow(
                                        roleIcon = info.role.icon,
                                        roleLabel = info.role.displayLabel,
                                        appName = info.holderAppLabel,
                                        appIcon = info.holderAppIcon,
                                        onClick = { onNavigateToDetails(info.role) },
                                        containerColor = Color.Transparent,
                                    )
                                    if (index < uiState.primaryDefaults.lastIndex) {
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

                // Section 2: "Others" Hub
                item(key = "header_others") {
                    Text(
                        text = stringResource(R.string.other_defaults),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .semantics { heading() },
                    )
                }

                item(key = "others_entry_card") {
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
                            roleIcon = Icons.Default.Category,
                            roleLabel = stringResource(R.string.other_defaults),
                            appName = stringResource(R.string.other_defaults_subtitle),
                            appIcon = null,
                            onClick = onNavigateToOthers,
                            containerColor = Color.Transparent,
                        )
                    }
                }
            }
        }
    }
}

