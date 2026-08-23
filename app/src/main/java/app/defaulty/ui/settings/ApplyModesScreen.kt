package app.defaulty.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.defaulty.DefaultyApp
import app.defaulty.R
import app.defaulty.data.preferences.ApplyMode
import app.defaulty.data.system.RootShellManager
import app.defaulty.data.system.ShizukuManager
import app.defaulty.ui.components.DefaultyTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Dedicated full screen for Apply Modes — Root, Shizuku, Wireless Debugging, and PC ADB setup.
 *
 * Serves as a diagnostic hub where the user can:
 * - Select their preferred Apply Mode (Auto / Root / Shizuku / Standard)
 * - View live Root & Shizuku status
 * - Request Root access or Shizuku authorization
 * - Copy ADB commands and access troubleshooting guides
 */
@Composable
fun ApplyModesScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as DefaultyApp
    val currentApplyMode by app.userPreferences.applyMode.collectAsStateWithLifecycle(
        initialValue = ApplyMode.AUTO,
    )

    var isShizukuActive by remember { mutableStateOf(ShizukuManager.hasShizukuPermission()) }
    var isShizukuAvailable by remember { mutableStateOf(ShizukuManager.isShizukuAvailable()) }
    var isRootAvailable by remember { mutableStateOf(false) }

    val shizukuStartCommand = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"

    // Probe root availability on launch
    LaunchedEffect(Unit) {
        isRootAvailable = withContext(Dispatchers.IO) { RootShellManager.isRootAvailable() }
    }

    DisposableEffect(Unit) {
        val refresh = {
            isShizukuActive = ShizukuManager.hasShizukuPermission()
            isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        }
        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            refresh()
        }
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            refresh()
        }
        val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            isShizukuActive = grantResult == PackageManager.PERMISSION_GRANTED || ShizukuManager.hasShizukuPermission()
        }
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (_: Throwable) {}
        onDispose {
            try {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(permissionListener)
            } catch (_: Throwable) {}
        }
    }

    Scaffold(
        topBar = {
            DefaultyTopBar(
                title = stringResource(R.string.adb_commands_title),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Apply Mode Selector
            item(key = "mode_selector") {
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
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.apply_mode_selector_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.apply_mode_selector_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-Detect
                        ApplyModeOption(
                            icon = Icons.Outlined.AutoAwesome,
                            title = stringResource(R.string.apply_mode_auto),
                            subtitle = stringResource(R.string.apply_mode_auto_desc),
                            selected = currentApplyMode == ApplyMode.AUTO,
                            onClick = {
                                coroutineScope.launch { app.userPreferences.setApplyMode(ApplyMode.AUTO) }
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Direct Root
                        ApplyModeOption(
                            icon = Icons.Outlined.AdminPanelSettings,
                            title = stringResource(R.string.apply_mode_root),
                            subtitle = stringResource(R.string.apply_mode_root_desc),
                            selected = currentApplyMode == ApplyMode.ROOT,
                            statusBadge = if (isRootAvailable) stringResource(R.string.status_available) else null,
                            onClick = {
                                coroutineScope.launch { app.userPreferences.setApplyMode(ApplyMode.ROOT) }
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // ADB / Shizuku
                        ApplyModeOption(
                            icon = Icons.Outlined.Terminal,
                            title = stringResource(R.string.apply_mode_shizuku),
                            subtitle = stringResource(R.string.apply_mode_shizuku_desc),
                            selected = currentApplyMode == ApplyMode.SHIZUKU,
                            statusBadge = if (isShizukuActive) stringResource(R.string.status_available) else null,
                            onClick = {
                                coroutineScope.launch { app.userPreferences.setApplyMode(ApplyMode.SHIZUKU) }
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Standard
                        ApplyModeOption(
                            icon = Icons.Outlined.Settings,
                            title = stringResource(R.string.apply_mode_standard),
                            subtitle = stringResource(R.string.apply_mode_standard_desc),
                            selected = currentApplyMode == ApplyMode.STANDARD,
                            onClick = {
                                coroutineScope.launch { app.userPreferences.setApplyMode(ApplyMode.STANDARD) }
                            },
                        )
                    }
                }
            }

            // Mode-specific method and setup section displayed dynamically based on selection
            when (currentApplyMode) {
                ApplyMode.AUTO -> {
                    item(key = "auto_detect_card") {
                        AutoDetectCard(
                            isRootAvailable = isRootAvailable,
                            isShizukuActive = isShizukuActive,
                            onRequestRoot = {
                                coroutineScope.launch {
                                    RootShellManager.clearCache()
                                    isRootAvailable = withContext(Dispatchers.IO) {
                                        RootShellManager.isRootAvailable()
                                    }
                                    if (isRootAvailable) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.root_granted_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.root_not_available_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                            onAuthorizeShizuku = {
                                if (ShizukuManager.hasShizukuPermission()) {
                                    isShizukuActive = true
                                    Toast.makeText(context, context.getString(R.string.shizuku_active_badge), Toast.LENGTH_SHORT).show()
                                } else {
                                    ShizukuManager.requestPermission()
                                    isShizukuActive = ShizukuManager.hasShizukuPermission()
                                }
                            },
                        )
                    }
                }

                ApplyMode.ROOT -> {
                    item(key = "root_setup") {
                        RootSetupCard(
                            isRootAvailable = isRootAvailable,
                            onRequestRoot = {
                                coroutineScope.launch {
                                    RootShellManager.clearCache()
                                    isRootAvailable = withContext(Dispatchers.IO) {
                                        RootShellManager.isRootAvailable()
                                    }
                                    if (isRootAvailable) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.root_granted_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.root_not_available_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                        )
                    }
                }

                ApplyMode.SHIZUKU -> {
                    item(key = "method_wireless") {
                        ShizukuWirelessCard(
                            onOpenShizuku = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                if (launchIntent != null) {
                                    try {
                                        context.startActivity(launchIntent)
                                    } catch (_: Exception) {}
                                } else {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                                        )
                                    } catch (_: Exception) {}
                                }
                            },
                            onOpenDevOptions = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                                } catch (_: Exception) {
                                    Toast.makeText(context, context.getString(R.string.unable_to_open_settings), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAuthorize = {
                                if (ShizukuManager.hasShizukuPermission()) {
                                    isShizukuActive = true
                                    Toast.makeText(context, context.getString(R.string.shizuku_active_badge), Toast.LENGTH_SHORT).show()
                                } else {
                                    ShizukuManager.requestPermission()
                                    isShizukuActive = ShizukuManager.hasShizukuPermission()
                                }
                            },
                        )
                    }

                    item(key = "method_pc_adb") {
                        ShizukuPcAdbCard(
                            shizukuStartCommand = shizukuStartCommand,
                            onCopyCommand = {
                                clipboardManager.setText(AnnotatedString(shizukuStartCommand))
                                Toast.makeText(context, context.getString(R.string.adb_command_copied), Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }

                ApplyMode.STANDARD -> {
                    item(key = "standard_setup") {
                        StandardModeCard(
                            onOpenDefaultAppsSettings = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.unable_to_open_settings), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoDetectCard(
    isRootAvailable: Boolean,
    isShizukuActive: Boolean,
    onRequestRoot: () -> Unit,
    onAuthorizeShizuku: () -> Unit,
) {
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
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.auto_mode_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auto_mode_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Active resolution status banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRootAvailable || isShizukuActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isRootAvailable || isShizukuActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = if (isRootAvailable || isShizukuActive) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Outlined.Settings
                        },
                        contentDescription = null,
                        tint = if (isRootAvailable || isShizukuActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.auto_mode_active_method_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isRootAvailable || isShizukuActive) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ) {
                                Text(
                                    text = when {
                                        isRootAvailable -> stringResource(R.string.apply_mode_root)
                                        isShizukuActive -> stringResource(R.string.apply_mode_shizuku)
                                        else -> stringResource(R.string.apply_mode_standard)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRootAvailable || isShizukuActive) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                isRootAvailable -> stringResource(R.string.auto_mode_active_root_desc)
                                isShizukuActive -> stringResource(R.string.auto_mode_active_shizuku_desc)
                                else -> stringResource(R.string.auto_mode_active_standard_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        )
                    }
                }
            }

            if (!isRootAvailable && !isShizukuActive) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onRequestRoot,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_request_root), maxLines = 1)
                    }
                    Button(
                        onClick = onAuthorizeShizuku,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_authorize), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RootSetupCard(
    isRootAvailable: Boolean,
    onRequestRoot: () -> Unit,
) {
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
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.root_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isRootAvailable) {
                    Spacer(modifier = Modifier.width(8.dp))
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
                                text = stringResource(R.string.status_granted),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.root_setup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRequestRoot,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (isRootAvailable) {
                        stringResource(R.string.btn_recheck_root)
                    } else {
                        stringResource(R.string.btn_request_root)
                    },
                )
            }
        }
    }
}

@Composable
private fun ShizukuWirelessCard(
    onOpenShizuku: () -> Unit,
    onOpenDevOptions: () -> Unit,
    onAuthorize: () -> Unit,
) {
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
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.adb_pc_wireless_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.adb_pc_wireless_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenShizuku,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.btn_open_shizuku), maxLines = 1)
                }

                FilledTonalButton(
                    onClick = onOpenDevOptions,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.btn_open_dev_options), maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAuthorize,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.btn_authorize))
            }
        }
    }
}

@Composable
private fun ShizukuPcAdbCard(
    shizukuStartCommand: String,
    onCopyCommand: () -> Unit,
) {
    val context = LocalContext.current
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
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.adb_pc_usb_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.adb_pc_usb_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Terminal Command Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = shizukuStartCommand,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                    )
                    IconButton(
                        onClick = onCopyCommand,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_command),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PC Troubleshooting Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.adb_pc_troubleshoot_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.adb_pc_troubleshoot_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardModeCard(
    onOpenDefaultAppsSettings: () -> Unit,
) {
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
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.standard_mode_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.standard_mode_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenDefaultAppsSettings,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.btn_open_default_apps_settings))
            }
        }
    }
}

@Composable
private fun ApplyModeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    statusBadge: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (statusBadge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = statusBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
