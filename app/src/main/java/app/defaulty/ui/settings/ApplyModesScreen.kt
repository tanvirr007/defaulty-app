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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Dedicated full screen for Apply Modes — Root, Shizuku, Wireless Debugging, and PC ADB setup.
 *
 * Serves as a diagnostic hub where the user can:
 * - Preview and select an Apply Mode (Root / Shizuku / Standard)
 * - View live Root & Shizuku status and setup guides
 * - Verify permissions and apply the chosen mode with fallback to Standard
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentApplyMode by app.userPreferences.applyMode.collectAsStateWithLifecycle(
        initialValue = ApplyMode.AUTO,
    )

    var isShizukuActive by remember { mutableStateOf(ShizukuManager.hasShizukuPermission()) }
    var isShizukuAvailable by remember { mutableStateOf(ShizukuManager.isShizukuAvailable()) }
    var isRootAvailable by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }

    val effectivePersistedMode = when (currentApplyMode) {
        ApplyMode.AUTO -> when {
            isRootAvailable -> ApplyMode.ROOT
            isShizukuActive -> ApplyMode.SHIZUKU
            else -> ApplyMode.STANDARD
        }
        else -> currentApplyMode
    }

    var selectedMode by rememberSaveable { mutableStateOf<ApplyMode?>(null) }
    val currentSelectedMode = selectedMode ?: effectivePersistedMode

    val shizukuStartCommand = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"

    // Probe privileges on launch and sanitize persisted mode if privileges were revoked
    LaunchedEffect(Unit) {
        val root = withContext(Dispatchers.IO) { RootShellManager.isRootAvailable() }
        isRootAvailable = root
        val shizuku = ShizukuManager.hasShizukuPermission()
        isShizukuActive = shizuku
        isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        val savedMode = app.userPreferences.applyMode.first()
        if (savedMode == ApplyMode.AUTO) {
            val resolved = when {
                root -> ApplyMode.ROOT
                shizuku -> ApplyMode.SHIZUKU
                else -> ApplyMode.STANDARD
            }
            app.userPreferences.setApplyMode(resolved)
        } else if (savedMode == ApplyMode.ROOT && !root) {
            app.userPreferences.setApplyMode(ApplyMode.STANDARD)
        } else if (savedMode == ApplyMode.SHIZUKU && !shizuku) {
            app.userPreferences.setApplyMode(ApplyMode.STANDARD)
        }
    }

    // Refresh privilege status on lifecycle resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isShizukuActive = ShizukuManager.hasShizukuPermission()
                isShizukuAvailable = ShizukuManager.isShizukuAvailable()
                coroutineScope.launch {
                    val root = withContext(Dispatchers.IO) { RootShellManager.isRootAvailable() }
                    isRootAvailable = root
                    val savedMode = app.userPreferences.applyMode.first()
                    if (savedMode == ApplyMode.ROOT && !root) {
                        app.userPreferences.setApplyMode(ApplyMode.STANDARD)
                    } else if (savedMode == ApplyMode.SHIZUKU && !isShizukuActive) {
                        app.userPreferences.setApplyMode(ApplyMode.STANDARD)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            val granted = grantResult == PackageManager.PERMISSION_GRANTED || ShizukuManager.hasShizukuPermission()
            isShizukuActive = granted
            isShizukuAvailable = ShizukuManager.isShizukuAvailable()
            if (granted) {
                selectedMode = ApplyMode.SHIZUKU
                coroutineScope.launch {
                    app.userPreferences.setApplyMode(ApplyMode.SHIZUKU)
                    Toast.makeText(
                        context,
                        context.getString(R.string.mode_applied_toast, context.getString(R.string.apply_mode_shizuku)),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
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

    val handleApplyMode: () -> Unit = {
        coroutineScope.launch {
            isApplying = true
            when (currentSelectedMode) {
                ApplyMode.ROOT -> {
                    RootShellManager.clearCache()
                    val root = withContext(Dispatchers.IO) { RootShellManager.isRootAvailable() }
                    isRootAvailable = root
                    if (root) {
                        app.userPreferences.setApplyMode(ApplyMode.ROOT)
                        selectedMode = ApplyMode.ROOT
                        Toast.makeText(
                            context,
                            context.getString(R.string.mode_applied_toast, context.getString(R.string.apply_mode_root)),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.root_fallback_toast),
                            Toast.LENGTH_SHORT,
                        ).show()
                        selectedMode = ApplyMode.STANDARD
                        app.userPreferences.setApplyMode(ApplyMode.STANDARD)
                    }
                }
                ApplyMode.SHIZUKU -> {
                    val shizuku = ShizukuManager.hasShizukuPermission()
                    isShizukuActive = shizuku
                    isShizukuAvailable = ShizukuManager.isShizukuAvailable()
                    if (shizuku) {
                        app.userPreferences.setApplyMode(ApplyMode.SHIZUKU)
                        selectedMode = ApplyMode.SHIZUKU
                        Toast.makeText(
                            context,
                            context.getString(R.string.mode_applied_toast, context.getString(R.string.apply_mode_shizuku)),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        if (isShizukuAvailable) {
                            ShizukuManager.requestPermission()
                            Toast.makeText(
                                context,
                                context.getString(R.string.onboarding_shizuku_need_auth),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.shizuku_fallback_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                            selectedMode = ApplyMode.STANDARD
                            app.userPreferences.setApplyMode(ApplyMode.STANDARD)
                        }
                    }
                }
                ApplyMode.STANDARD, ApplyMode.AUTO -> {
                    app.userPreferences.setApplyMode(ApplyMode.STANDARD)
                    selectedMode = ApplyMode.STANDARD
                    Toast.makeText(
                        context,
                        context.getString(R.string.mode_applied_toast, context.getString(R.string.apply_mode_standard)),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            isApplying = false
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

                        // Direct Root
                        ApplyModeOption(
                            icon = Icons.Outlined.AdminPanelSettings,
                            title = stringResource(R.string.apply_mode_root),
                            subtitle = stringResource(R.string.apply_mode_root_desc),
                            selected = currentSelectedMode == ApplyMode.ROOT,
                            statusBadge = if (effectivePersistedMode == ApplyMode.ROOT) {
                                stringResource(R.string.badge_current_active)
                            } else if (isRootAvailable) {
                                stringResource(R.string.status_available)
                            } else null,
                            onClick = {
                                selectedMode = ApplyMode.ROOT
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // ADB / Shizuku
                        ApplyModeOption(
                            icon = Icons.Outlined.Terminal,
                            title = stringResource(R.string.apply_mode_shizuku),
                            subtitle = stringResource(R.string.apply_mode_shizuku_desc),
                            selected = currentSelectedMode == ApplyMode.SHIZUKU,
                            statusBadge = if (effectivePersistedMode == ApplyMode.SHIZUKU) {
                                stringResource(R.string.badge_current_active)
                            } else if (isShizukuActive) {
                                stringResource(R.string.status_available)
                            } else null,
                            onClick = {
                                selectedMode = ApplyMode.SHIZUKU
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Standard
                        ApplyModeOption(
                            icon = Icons.Outlined.Settings,
                            title = stringResource(R.string.apply_mode_standard),
                            subtitle = stringResource(R.string.apply_mode_standard_desc),
                            selected = currentSelectedMode == ApplyMode.STANDARD,
                            statusBadge = if (effectivePersistedMode == ApplyMode.STANDARD) {
                                stringResource(R.string.badge_current_active)
                            } else null,
                            onClick = {
                                selectedMode = ApplyMode.STANDARD
                            },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Apply Button
                        val isCurrentModeActive = currentSelectedMode == effectivePersistedMode
                        if (isCurrentModeActive) {
                            FilledTonalButton(
                                onClick = handleApplyMode,
                                enabled = !isApplying,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_mode_already_active),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else {
                            Button(
                                onClick = handleApplyMode,
                                enabled = !isApplying,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_apply_mode),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // Mode-specific method and setup section displayed dynamically based on selection
            when (currentSelectedMode) {
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
                                        selectedMode = ApplyMode.ROOT
                                        app.userPreferences.setApplyMode(ApplyMode.ROOT)
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
                            isShizukuActive = isShizukuActive,
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
                                    selectedMode = ApplyMode.SHIZUKU
                                    coroutineScope.launch {
                                        app.userPreferences.setApplyMode(ApplyMode.SHIZUKU)
                                        Toast.makeText(context, context.getString(R.string.shizuku_authorized_toast), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    ShizukuManager.requestPermission()
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

                ApplyMode.STANDARD, ApplyMode.AUTO -> {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    modifier = Modifier.weight(1f),
                )
                if (isRootAvailable) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.status_granted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.root_setup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))
            if (isRootAvailable) {
                FilledTonalButton(
                    onClick = onRequestRoot,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_root_granted),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Button(
                    onClick = onRequestRoot,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.btn_request_root),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShizukuWirelessCard(
    isShizukuActive: Boolean,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    modifier = Modifier.weight(1f),
                )
                if (isShizukuActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.status_granted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onOpenShizuku,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.btn_open_shizuku),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }

                FilledTonalButton(
                    onClick = onOpenDevOptions,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.btn_open_dev_options),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (isShizukuActive) {
                FilledTonalButton(
                    onClick = onAuthorize,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_authorized),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Button(
                    onClick = onAuthorize,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.btn_authorize),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (statusBadge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = statusBadge,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
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
