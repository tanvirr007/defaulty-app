package app.defaulty.ui.details

import android.app.Application
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.defaulty.R
import app.defaulty.domain.model.SupportedRole
import app.defaulty.ui.components.AppIcon

/**
 * Default App Details screen.
 *
 * Shows the current default for a role and provides a button to change it.
 * Uses ActivityResultContracts to launch the system role-change UI
 * and re-checks the actual state on return (Product Rule 11).
 *
 * Handles: ActivityNotFoundException, user cancellation, unsupported role,
 * no compatible apps (Spec Section 19).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAppDetailsScreen(
    role: SupportedRole,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: DefaultAppDetailsViewModel = viewModel(
        factory = DefaultAppDetailsViewModel.Factory(application, role),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

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
        // Result doesn't matter — we always re-query actual state
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(role.displayLabel) },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Role section heading
            Text(
                text = stringResource(R.string.default_role_title, role.displayLabel),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current default app info
            val info = uiState.defaultApp
            if (info?.holderPackageName != null) {
                AppIcon(
                    drawable = info.holderAppIcon,
                    contentDescription = info.holderAppLabel?.let {
                        stringResource(R.string.cd_app_icon, it)
                    },
                    size = 64.dp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(
                        R.string.current_default,
                        info.holderAppLabel ?: info.holderPackageName,
                        role.description,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Icon(
                    imageVector = role.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.no_current_default, role.description),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Change default section
            Text(
                text = stringResource(R.string.change_default),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.change_default_description, role.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = viewModel.getChangeDefaultIntent()
                    if (intent != null) {
                        try {
                            roleChangeLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            // System Settings activity unavailable (Spec Section 19)
                            Toast.makeText(
                                context,
                                context.getString(R.string.unable_to_open_settings),
                                Toast.LENGTH_LONG,
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.unable_to_open_settings),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    } else {
                        // Fallback: open general app settings
                        val fallback = viewModel.getFallbackSettingsIntent()
                        if (fallback != null) {
                            try {
                                context.startActivity(fallback)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.unable_to_open_settings),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.no_apps_available),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.change_default))
            }
        }
    }
}
