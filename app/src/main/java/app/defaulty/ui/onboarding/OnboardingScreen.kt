package app.defaulty.ui.onboarding

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.defaulty.R
import app.defaulty.data.preferences.ThemeMode
import app.defaulty.data.system.ShizukuManager
import rikka.shizuku.Shizuku

/**
 * First-time setup wizard (Spec Section 11).
 *
 * 4 pages:
 *   1. Welcome — what the app does
 *   2. Apply Mode — Standard Mode vs 1-Tap ADB/Shizuku Mode (requires Shizuku before advancing)
 *   3. Appearance — System/Light/Dark selection
 *   4. Finished — ready to go
 *
 * Shown only on first launch. Completion stored in DataStore.
 * Uses animated page transitions for natural Material motion.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val totalPages = 4

    var isShizukuActive by remember { mutableStateOf(ShizukuManager.hasShizukuPermission()) }
    var isShizukuAvailable by remember { mutableStateOf(ShizukuManager.isShizukuAvailable()) }
    var selectedMode by rememberSaveable { mutableIntStateOf(if (isShizukuActive) 1 else 0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isShizukuActive = ShizukuManager.hasShizukuPermission()
                isShizukuAvailable = ShizukuManager.isShizukuAvailable()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            isShizukuActive = grantResult == PackageManager.PERMISSION_GRANTED
            isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        }
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (_: Throwable) {}
        onDispose {
            try {
                Shizuku.removeRequestPermissionResultListener(listener)
            } catch (_: Throwable) {}
        }
    }

    val canProceed = when (currentPage) {
        1 -> selectedMode == 0 || isShizukuActive
        else -> true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            // Page indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(totalPages) { index ->
                    val color = if (index == currentPage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .then(
                                Modifier
                                    .height(8.dp)
                                    .width(8.dp)
                            ),
                    ) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            drawCircle(color = color)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Page content with animated transitions
            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * direction } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it * direction } + fadeOut())
                },
                label = "onboarding_page",
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> ChooseModePage(
                        selectedMode = selectedMode,
                        onModeSelect = { selectedMode = it },
                        isShizukuActive = isShizukuActive,
                        isShizukuAvailable = isShizukuAvailable,
                    )
                    2 -> AppearancePage(
                        themeMode = themeMode,
                        onThemeModeChange = viewModel::setThemeMode,
                    )
                    3 -> ReadyPage()
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (currentPage > 0) {
                    OutlinedButton(onClick = { currentPage-- }) {
                        Text(stringResource(R.string.back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (currentPage < totalPages - 1) {
                    Button(
                        onClick = { currentPage++ },
                        enabled = canProceed,
                    ) {
                        Text(stringResource(R.string.next))
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.completeOnboarding {
                                onFinished()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.get_started))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    OnboardingPageContent(
        icon = Icons.Default.Verified,
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_description),
    )
}

@Composable
private fun ChooseModePage(
    selectedMode: Int,
    onModeSelect: (Int) -> Unit,
    isShizukuActive: Boolean,
    isShizukuAvailable: Boolean,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Terminal,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.onboarding_mode_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_mode_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Option 1: Standard Mode
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (selectedMode == 0) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = if (selectedMode == 0) 1.5.dp else 1.dp,
                color = if (selectedMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onModeSelect(0) },
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedMode == 0,
                    onClick = { onModeSelect(0) },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_mode_standard),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboarding_mode_standard_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Option 2: ADB / Shizuku Mode
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (selectedMode == 1) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = if (selectedMode == 1) 1.5.dp else 1.dp,
                color = if (selectedMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onModeSelect(1) },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedMode == 1,
                        onClick = { onModeSelect(1) },
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.onboarding_mode_shizuku),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (isShizukuActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = stringResource(R.string.shizuku_active_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.onboarding_mode_shizuku_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (selectedMode == 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isShizukuActive) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_shizuku_ready),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isShizukuAvailable) {
                                        stringResource(R.string.onboarding_shizuku_need_auth)
                                    } else {
                                        stringResource(R.string.onboarding_shizuku_need_start)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isShizukuAvailable) {
                                    Button(
                                        onClick = { ShizukuManager.requestPermission() },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.btn_authorize))
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                                if (launchIntent != null) {
                                                    try {
                                                        context.startActivity(launchIntent)
                                                    } catch (_: Exception) {}
                                                } else {
                                                    try {
                                                        val storeIntent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"),
                                                        )
                                                        context.startActivity(storeIntent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, context.getString(R.string.no_browser_found), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(stringResource(R.string.btn_open_shizuku), maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                                    context.startActivity(devIntent)
                                                } catch (_: Exception) {
                                                    try {
                                                        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                                                        context.startActivity(settingsIntent)
                                                    } catch (_: Exception) {}
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(stringResource(R.string.btn_open_dev_options), maxLines = 1)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = stringResource(R.string.onboarding_shizuku_setup_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearancePage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_appearance_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.selectableGroup()) {
            ThemeRadio(
                label = stringResource(R.string.theme_system),
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
            )
            ThemeRadio(
                label = stringResource(R.string.theme_light),
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
            )
            ThemeRadio(
                label = stringResource(R.string.theme_dark),
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) },
            )
        }
    }
}

@Composable
private fun ReadyPage() {
    OnboardingPageContent(
        icon = Icons.Default.CheckCircle,
        title = stringResource(R.string.onboarding_ready_title),
    )
}

@Composable
private fun OnboardingPageContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun ThemeRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
