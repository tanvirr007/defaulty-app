package app.defaulty.ui.details

import android.app.Application
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.defaulty.R
import app.defaulty.domain.model.MediaHandlerType
import app.defaulty.ui.components.AppIcon
import app.defaulty.ui.components.CandidateAppCard
import app.defaulty.ui.components.DefaultyTopBar

/**
 * Media & File Handler Details screen.
 *
 * Shows the current default for a media/file category (e.g. Video Player, Gallery)
 * and provides an interactive "Mark & Apply" candidate app selection list with
 * animated bottom action bar.
 */
@Composable
fun MediaHandlerDetailsScreen(
    type: MediaHandlerType,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: MediaHandlerDetailsViewModel = viewModel(
        factory = MediaHandlerDetailsViewModel.Factory(application, type),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeDefaultPackage = uiState.defaultApp?.holderPackageName
    var selectedPackage by rememberSaveable(type.id) {
        mutableStateOf<String?>(null)
    }

    val effectiveSelectedPackage = selectedPackage ?: activeDefaultPackage
    val candidateApps = uiState.candidateApps
    val selectedCandidate = candidateApps.find { it.packageName == effectiveSelectedPackage }
    val isNonDefaultSelected = effectiveSelectedPackage != null &&
        effectiveSelectedPackage != activeDefaultPackage

    // Re-query after returning from system UI
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launcher for the chooser
    val chooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refresh()
    }

    val launchChooser: (String?, String?) -> Unit = { _, _ ->
        val promptTitle = context.getString(R.string.media_open_with_prompt)
        val intent = viewModel.getMediaChooserIntent(promptTitle)
        try {
            Toast.makeText(
                context,
                context.getString(R.string.media_toast_hint),
                Toast.LENGTH_LONG,
            ).show()
            chooserLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            val fallback = viewModel.getFallbackSettingsIntent()
            if (fallback != null) {
                try {
                    context.startActivity(fallback)
                } catch (ex: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.unable_to_open_settings),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.unable_to_open_settings),
                    Toast.LENGTH_LONG,
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_to_open_settings),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    Scaffold(
        topBar = {
            DefaultyTopBar(
                title = stringResource(type.displayLabelRes),
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
        bottomBar = {
            AnimatedVisibility(
                visible = isNonDefaultSelected && selectedCandidate != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            selectedCandidate?.appIcon?.let { icon ->
                                AppIcon(
                                    drawable = icon,
                                    contentDescription = selectedCandidate.appLabel,
                                    size = 36.dp,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = selectedCandidate?.appLabel.orEmpty(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.apply_as_default_media,
                                        stringResource(type.displayLabelRes),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                selectedCandidate?.let {
                                    try {
                                        context.startActivity(viewModel.getManageLinksIntent(it.packageName))
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(viewModel.getAppSettingsIntent(it.packageName))
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.media_open_defaults_action),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                launchChooser(
                                    selectedCandidate?.packageName,
                                    selectedCandidate?.appLabel,
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.media_test_chooser_action),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // Streamlined Hero Card showing current default status
            val info = uiState.defaultApp
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
                Column(
                    modifier = Modifier.padding(18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(
                                    R.string.default_role_title,
                                    stringResource(type.displayLabelRes),
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = stringResource(
                                    R.string.change_default_description,
                                    stringResource(type.descriptionRes),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (info?.holderPackageName != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(
                                    drawable = info.holderAppIcon,
                                    contentDescription = info.holderAppLabel?.let {
                                        stringResource(R.string.cd_app_icon, it)
                                    },
                                    size = 48.dp,
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = info.holderAppLabel ?: info.holderPackageName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                                Text(
                                                    text = stringResource(R.string.badge_active_default),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = info.holderPackageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(
                                        R.string.no_current_default,
                                        stringResource(type.descriptionRes),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Compatible installed apps section header
            Column {
                Text(
                    text = if (candidateApps.isNotEmpty()) {
                        stringResource(R.string.installed_candidate_apps_count, candidateApps.size)
                    } else {
                        stringResource(R.string.installed_candidate_apps)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.select_app_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (candidateApps.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_compatible_apps_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                candidateApps.forEach { app ->
                    val isCardSelected = effectiveSelectedPackage == app.packageName
                    CandidateAppCard(
                        app = app,
                        selected = isCardSelected,
                        onSelect = {
                            selectedPackage = app.packageName
                        },
                        onOpenSettings = {
                            try {
                                context.startActivity(viewModel.getAppSettingsIntent(app.packageName))
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.unable_to_open_settings),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onOpenLinks = {
                            try {
                                context.startActivity(viewModel.getManageLinksIntent(app.packageName))
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.unable_to_open_settings),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onLaunch = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                try {
                                    context.startActivity(launchIntent)
                                } catch (e: Exception) {
                                    // Ignore launch error
                                }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
