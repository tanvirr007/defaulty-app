package app.defaulty.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.defaulty.R
import app.defaulty.domain.model.SupportedRole
import app.defaulty.ui.components.DefaultAppRow

/**
 * Home screen — primary screen showing all available defaults.
 * Split into "Your defaults" (primary roles) and "Other supported defaults".
 * Includes "Opening links" entry in the Other section.
 *
 * Refreshes on ON_RESUME (Spec Section 15).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetails: (SupportedRole) -> Unit,
    onNavigateToLinks: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh on resume — detect changes made externally (Spec Section 18)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (uiState.isLoading) {
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
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                // "Your defaults" section
                if (uiState.primaryDefaults.isNotEmpty()) {
                    item(key = "header_primary") {
                        SectionHeader(text = stringResource(R.string.your_defaults))
                    }
                    items(
                        items = uiState.primaryDefaults,
                        key = { it.role.roleName },
                    ) { info ->
                        DefaultAppRow(
                            roleIcon = info.role.icon,
                            roleLabel = info.role.displayLabel,
                            appName = info.holderAppLabel,
                            appIcon = info.holderAppIcon,
                            onClick = { onNavigateToDetails(info.role) },
                        )
                    }
                }

                // "Other supported defaults" section
                val hasOther = uiState.otherDefaults.isNotEmpty()
                item(key = "header_other") {
                    SectionHeader(text = stringResource(R.string.other_defaults))
                }

                if (hasOther) {
                    items(
                        items = uiState.otherDefaults,
                        key = { it.role.roleName },
                    ) { info ->
                        DefaultAppRow(
                            roleIcon = info.role.icon,
                            roleLabel = info.role.displayLabel,
                            appName = info.holderAppLabel,
                            appIcon = info.holderAppIcon,
                            onClick = { onNavigateToDetails(info.role) },
                        )
                    }
                }

                // "Opening links" entry
                item(key = "opening_links") {
                    DefaultAppRow(
                        roleIcon = Icons.Default.Link,
                        roleLabel = stringResource(R.string.opening_links),
                        appName = stringResource(R.string.opening_links_subtitle),
                        appIcon = null,
                        onClick = onNavigateToLinks,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

// Modifier extension for padding — import workaround
private val Modifier.Companion get() = Modifier
