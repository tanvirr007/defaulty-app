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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.defaulty.R
import app.defaulty.data.system.ShizukuManager
import app.defaulty.domain.model.SupportedRole
import app.defaulty.ui.components.AdbCommandsDialog
import app.defaulty.ui.components.AppIcon
import app.defaulty.ui.components.CandidateAppCard
import app.defaulty.ui.components.DefaultyTopBar
import kotlinx.coroutines.launch

/**
 * Default App Details screen.
 *
 * Shows the current default for a role and provides an interactive "Mark & Apply"
 * candidate app selection list with dual apply methods (Android Settings & ADB shell).
 */
@Composable
fun DefaultAppDetailsScreen(
    role: SupportedRole,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val application = context.applicationContext as Application
    val viewModel: DefaultAppDetailsViewModel = viewModel(
        factory = DefaultAppDetailsViewModel.Factory(application, role),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeDefaultPackage = uiState.defaultApp?.holderPackageName
    var selectedPackage by rememberSaveable(role.roleName) {
        mutableStateOf<String?>(null)
    }
    var showAdbDialog by remember { mutableStateOf(false) }

    val effectiveSelectedPackage = selectedPackage ?: activeDefaultPackage
    val candidateApps = uiState.candidateApps
    val selectedCandidate = candidateApps.find { it.packageName == effectiveSelectedPackage }
    val isNonDefaultSelected = effectiveSelectedPackage != null &&
        effectiveSelectedPackage != activeDefaultPackage

    if (showAdbDialog) {
        AdbCommandsDialog(
            role = role,
            onDismiss = { showAdbDialog = false }
        )
    }

    // Re-query after returning from system UI (Product Rule 11)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launcher for the role-change system UI
    val roleChangeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        // Re-query actual state when returning
        viewModel.refresh()
    }

    val launchRoleChange: (String?, String?) -> Unit = { _, _ ->
        val intent = viewModel.getChangeDefaultIntent()
        try {
            Toast.makeText(
                context,
                context.getString(R.string.prompt_select_in_system),
                Toast.LENGTH_SHORT,
            ).show()
            roleChangeLauncher.launch(intent)
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
                title = role.displayLabel,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAdbDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = stringResource(R.string.adb_commands_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isNonDefaultSelected,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                                        R.string.apply_as_default_role,
                                        role.displayLabel,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        val candidateCmd = selectedCandidate?.packageName?.let { role.getAdbCommand(it) }
                            ?: role.getAdbCommand("<package_name>")
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(candidateCmd))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.adb_command_copied),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.copy_adb_command),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                val targetPkg = selectedCandidate?.packageName
                                if (targetPkg != null && ShizukuManager.hasShizukuPermission()) {
                                    coroutineScope.launch {
                                        val success = viewModel.applyDefaultViaShizuku(targetPkg)
                                        if (success) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.apply_done),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            selectedPackage = null
                                        } else {
                                            launchRoleChange(
                                                targetPkg,
                                                selectedCandidate?.appLabel,
                                            )
                                        }
                                    }
                                } else {
                                    launchRoleChange(
                                        selectedCandidate?.packageName,
                                        selectedCandidate?.appLabel,
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.apply_default),
                                style = MaterialTheme.typography.labelLarge,
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
                                    imageVector = role.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.default_role_title, role.displayLabel),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = stringResource(R.string.change_default_description, role.description),
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
                                    text = stringResource(R.string.no_current_default, role.description),
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
