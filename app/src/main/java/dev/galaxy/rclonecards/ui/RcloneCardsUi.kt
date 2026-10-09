@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.galaxy.rclonecards.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.galaxy.rclonecards.BuildConfig
import dev.galaxy.rclonecards.data.AppSettings
import dev.galaxy.rclonecards.data.CardStore
import dev.galaxy.rclonecards.data.ConfigManager
import dev.galaxy.rclonecards.data.JobHistoryStore
import dev.galaxy.rclonecards.engine.RcloneConfigQuestion
import dev.galaxy.rclonecards.engine.RcloneEngine
import dev.galaxy.rclonecards.model.CardColor
import dev.galaxy.rclonecards.model.CardIcon
import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import dev.galaxy.rclonecards.model.TaskCard
import dev.galaxy.rclonecards.service.JobRepository
import dev.galaxy.rclonecards.service.RcloneService
import dev.galaxy.rclonecards.service.TaskScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Amoled = Color.Black
private val Surface = Color.Black
private val Surface2 = Color.Black
private val Outline = Color(0xFF232328)
private val TextPrimary = Color(0xFFF3F4F6)
private val TextSecondary = Color(0xFFA1A1AA)
private val TextDim = Color(0xFF71717A)
private val IconMuted = Color(0xFFC3C6CC)
private val Green = Color(0xFF22C55E)
private val Purple = Color(0xFFA855F7)
private val Amber = Color(0xFFF59E0B)
private val Cyan = Color(0xFF06B6D4)
private val Red = Color(0xFFEF4444)
private val Slate = Color(0xFF737373)

private sealed interface Screen {
    data object Home : Screen
    data class Edit(val cardId: String) : Screen
    data class Detail(val cardId: String) : Screen
    data object Settings : Screen
    data object History : Screen
}

@Composable
fun RcloneCardsTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * 1.13f
        )
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Amoled,
                surface = Surface,
                surfaceVariant = Surface2,
                primary = Green,
                secondary = Purple,
                error = Red,
                onBackground = TextPrimary,
                onSurface = TextPrimary,
                outline = Outline
            ),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcloneCardsRoot(
    externalNavigation: StateFlow<String?>,
    onImportConfig: () -> Unit,
    onExportConfig: () -> Unit,
    onImportCards: () -> Unit,
    onExportCards: () -> Unit,
    onImportFullBackup: () -> Unit,
    onExportFullBackup: () -> Unit,
    onRequestAllFiles: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    hasAllFilesAccess: () -> Boolean,
    hasNotificationPermission: () -> Boolean,
    canExactAlarm: () -> Boolean,
    onPinShortcut: (TaskCard) -> Unit
) {
    val context = LocalContext.current
    val cards by CardStore.cards.collectAsState()
    val jobs by JobRepository.jobs.collectAsState()
    val external by externalNavigation.collectAsState()

    val driveSetup: DriveSetupViewModel = viewModel()
    var screen: Screen by rememberSaveable(stateSaver = Saver<Screen, String>(
        save = { when (it) {
            Screen.Home -> "home"
            Screen.Settings -> "settings"
            Screen.History -> "history"
            is Screen.Edit -> "edit:${it.cardId}"
            is Screen.Detail -> "detail:${it.cardId}"
        } },
        restore = { when {
            it == "settings" -> Screen.Settings
            it == "history" -> Screen.History
            it.startsWith("edit:") -> Screen.Edit(it.substringAfter(":"))
            it.startsWith("detail:") -> Screen.Detail(it.substringAfter(":"))
            else -> Screen.Home
        } }
    )) { mutableStateOf(if (driveSetup.open.value) Screen.Settings else Screen.Home) }
    var menuCardId by remember { mutableStateOf<String?>(null) }
    var deleteCardId by remember { mutableStateOf<String?>(null) }
    var exitConfirm by remember { mutableStateOf(false) }
    BackHandler {
        when {
            deleteCardId != null -> deleteCardId = null
            menuCardId != null -> menuCardId = null
            screen is Screen.History -> screen = Screen.Settings
            screen != Screen.Home -> screen = Screen.Home
            else -> exitConfirm = true
        }
    }

    LaunchedEffect(external) {
        val value = external ?: return@LaunchedEffect
        when {
            value.startsWith("detail:") -> {
                screen = Screen.Detail(value.substringAfter("detail:"))
            }
            value.startsWith("oauth:") -> {
                screen = Screen.Settings
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Amoled)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Crossfade(targetState = screen, animationSpec = tween(180), label = "screen") { current ->
            when (current) {
            Screen.Home -> HomeScreen(
                cards = cards,
                jobs = jobs,
                onRun = { card ->
                    if (needsStorage(card) && !hasAllFilesAccess()) {
                        toast(context, "Grant “All files access” first.")
                        onRequestAllFiles()
                    } else {
                        if (!hasNotificationPermission()) onRequestNotification()
                        sendService(context, RcloneService.ACTION_START, card.id)
                    }
                },
                onOpenDetail = { screen = Screen.Detail(it.id) },
                onEdit = { screen = Screen.Edit(it.id) },
                onLongPress = { menuCardId = it.id },
                onSettings = { screen = Screen.Settings },
                onAdd = {
                    val card = CardStore.addBlank()
                    screen = Screen.Edit(card.id)
                }
            )

            is Screen.Edit -> {
                val card = cards.firstOrNull { it.id == current.cardId }
                if (card == null) {
                    screen = Screen.Home
                } else {
                    EditCardScreen(
                        card = card,
                        canExactAlarm = canExactAlarm(),
                        onRequestExactAlarm = onRequestExactAlarm,
                        onBack = { screen = Screen.Home },
                        onSave = {
                            CardStore.save(it)
                            TaskScheduler.apply(context, it.id)
                            screen = Screen.Home
                        }
                    )
                }
            }

            is Screen.Detail -> DetailScreen(
                card = cards.firstOrNull { it.id == current.cardId },
                job = jobs[current.cardId],
                onBack = { screen = Screen.Home },
                onPause = { sendService(context, RcloneService.ACTION_PAUSE, current.cardId) },
                onResume = { sendService(context, RcloneService.ACTION_RESUME, current.cardId) },
                onStop = { sendService(context, RcloneServ…10764 tokens truncated…          valueColor = if (notificationOk) Green else Amber,
                    onClick = onRequestNotification
                )
                SettingsRow(
                    Icons.Default.Schedule,
                    "Exact scheduling",
                    if (alarmOk) "Ready" else "Required for exact daily schedules",
                    valueColor = if (alarmOk) Green else Amber,
                    onClick = onRequestExactAlarm
                )
            }

            SettingsGroup("Data") {
                SettingsRow(
                    Icons.Default.Save,
                    "Back Up Entire App",
                    "Cards, rclone.conf, settings and custom icons. Contains credentials.",
                    onClick = onExportFullBackup
                )
                SettingsRow(
                    Icons.Default.FileOpen,
                    "Restore Entire App",
                    "Restore a Basic Rclone Flow full backup",
                    onClick = onImportFullBackup
                )
                SettingsRow(
                    Icons.Default.FileDownload,
                    "Back Up Cards",
                    "Export task cards as JSON",
                    onClick = onExportCards
                )
                SettingsRow(
                    Icons.Default.FileOpen,
                    "Restore Cards",
                    "Import a previous card backup",
                    onClick = onImportCards
                )
                SettingsRow(
                    Icons.Default.Delete,
                    "Clear All Cards",
                    "Remove every task card without touching Drive or local files",
                    valueColor = Amber,
                    onClick = { clearCardsDialog = true }
                )
            }

            SettingsGroup("Other") {
                SettingsRow(
                    Icons.Default.Delete,
                    "Clear App Data",
                    "Reset cards, rclone.conf and app settings",
                    valueColor = Red,
                    onClick = { clearDataDialog = true }
                )
                SettingsRow(
                    Icons.Default.Code,
                    "About",
                    "Basic Rclone Flow · BlackWare + OpenAI ChatGPT",
                    onClick = { aboutDialog = true }
                )
            }

            Text(
                "Cloud support is intentionally limited to Google Drive for now. Local Android storage remains available as a task source or destination.",
                color = TextDim,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }

    if (termuxConfigDialog) {
        val clipboard = LocalClipboardManager.current
        val termuxCommand = "CFG=\"$(rclone config file | tail -n 1)\"\ncp \"\$CFG\" ~/storage/downloads/rclone.conf"

        AlertDialog(

            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),

            tonalElevation = 0.dp,

            containerColor = Color.Black,
            onDismissRequest = { termuxConfigDialog = false },
            title = { Text("Import from Termux") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Run this in Termux. It copies the active rclone.conf to Downloads, where Basic Rclone Flow can import it.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    SelectionContainer {
                        Text(
                            termuxCommand,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Amoled)
                                .border(1.dp, Outline, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            color = Color(0xFF86EFAC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Text(
                        "Then use Import rclone.conf in Settings.",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(termuxCommand))
                    toast(context, "Command copied.")
                }) { Text("Copy command", color = Green) }
            },
            dismissButton = {
                TextButton(onClick = { termuxConfigDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (driveSetupDialog) {
        val question: RcloneConfigQuestion? = driveStep?.question
        val setupCompleted = driveStep?.done == true
        val setupVerified = driveStep?.verified == true

        AlertDialog(

            modifier = Modifier.fillMaxWidth(.94f).border(1.dp, Outline, RoundedCornerShape(24.dp)),

            tonalElevation = 0.dp,

            containerColor = Color.Black,
            onDismissRequest = { if (!driveBusy) driveSetup.close() },
            title = {
                Text(
                    when {
                        setupCompleted -> "Google Drive Verification"
                        question == null -> "Google Drive Setup"
                        else -> question.name
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (driveBusy) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Green,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (setupCompleted) "Testing Google Drive…" else "Waiting for rclone…",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "OAuth runs through rclone itself. After Google approval, the browser can return directly to Basic Rclone Flow.",
                                    color = TextDim,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    } else if (setupCompleted) {
                        Text(
                            if (setupVerified) "Google Drive connected and verified by a live root listing. You can close this window."
                            else "Configuration was saved, but the live Drive test failed. Test again or review the connection in Manage Google Drive.",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    } else if (question == null) {
                        Text(
                            "The setup screen uses rclone's own persistent RC configuration protocol. rclone supplies every question, default and choice; the app does not fabricate Drive settings.",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = driveRemoteName,
                            onValueChange = {
                                driveRemoteName = it
                                    .replace(":", "")
                                    .replace("[", "")
                                    .replace("]", "")
                                    .take(40)
                            },
                            label = { Text("Remote name") },
                            placeholder = { Text("gdrive") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = darkTextFieldColors()
                        )

                        Text(
                            "Current cloud backend: Google Drive only. The normal rclone flow covers Client ID, Client Secret, scope / Full Access, Service Account, OAuth and Shared Drive.",
                            color = TextDim,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    } else {
                        if (question.help.isNotBlank()) {
                            Text(
                                question.help,
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }

                        question.examples.forEach { example ->
                            val selected = driveAnswer == example.value
                            OutlinedButton(
                                onClick = { driveAnswer = example.value },
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) Green else Outline
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selected) Green else TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        example.help.ifBlank { example.value },
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (example.help.isNotBlank() && example.value.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            example.value,
                                            color = TextDim,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (question.name.equals("service_account_file", ignoreCase = true)) {
                            OutlinedButton(
                                onClick = { serviceAccountPicker.launch("application/json") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Choose Service Account JSON")
                            }
                        }

                        if (!question.exclusive) {
                            OutlinedTextField(
                                value = driveAnswer,
                                onValueChange = { driveAnswer = it },
                                label = {
                                    Text(
                                        when {
                                            question.defaultString.isNotBlank() ->
                                                "Value · default ${question.defaultString}"
                                            question.defaultValue.isNotBlank() ->
                                                "Value · default ${question.defaultValue}"
                                            else -> "Value"
                                        }
                                    )
                                },
                                singleLine = question.type != "stringArray",
                                visualTransformation = if (question.isPassword) {
                                    PasswordVisualTransformation()
                                } else {
                                    androidx.compose.ui.text.input.VisualTransformation.None
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = darkTextFieldColors()
                            )
                        } else if (driveAnswer.isNotBlank()) {
                            Text(
                                "Selected: $driveAnswer",
                                color = Green,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    driveWizardError?.let { message ->
                        Text(
                            message,
                            color = Red,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !driveBusy,
                    onClick = {
                        when {
                            setupVerified -> driveSetup.close()
                            setupCompleted -> driveSetup.verify()
                            question == null -> driveSetup.start()
                            else -> driveSetup.next()
                        }
                    }
                ) {
                    Text(
                        when {
                            setupVerified -> "Done"
                            setupCompleted -> "Test Again"
                            question == null -> "Start Setup"
                            else -> "Continue"
                        },
                        color = if (driveBusy) TextDim else Green
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !driveBusy,
                    onClick = { driveSetup.close() }
                ) { Text("Close") }
            }
        )
    }

    if (remotesDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp,
            containerColor = Color.Black,
            onDismissRequest = { if (remoteBusyName == null) remotesDialog = false },
            title = { Text("Manage Google Drive") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (remotes.isEmpty()) {
                        Text(
                            "No Google Drive remotes are configured.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    remotes.forEach { remote ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .border(1.dp, Outline, RoundedCornerShape(15.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Cloud,
                                    null,
                                    tint = Cyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "$remote:",
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Google Drive",
                                        color = TextDim,
                                        fontSize = 10.5.sp
                                    )
                                }
                                if (remoteBusyName == remote) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Green
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    enabled = remoteBusyName == null,
                                    onClick = {
                                        remoteBusyName = remote
                                        remoteActionMessage = "Testing $remote…"
                                        settingsScope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                RcloneEngine.testRemote(context, remote)
                                            }
                                            remoteActionMessage = result.fold(
                                                onSuccess = { "$remote: connection verified." },
                                                onFailure = { "$remote: ${it.message ?: "connection test failed"}" }
                                            )
                                            remoteBusyName = null
                                        }
                                    }
                                ) { Text("Test", color = Green) }

                                TextButton(
                                    enabled = remoteBusyName == null,
                                    onClick = {
                                        remoteEditName = remote
                                        remoteEditText = ConfigManager.readRemoteBlock(remote).orEmpty()
                                    }
                                ) { Text("Edit", color = Cyan) }

                                TextButton(
                                    enabled = remoteBusyName == null,
                                    onClick = { deleteRemoteName = remote }
                                ) { Text("Delete", color = Red) }
                            }
                        }
                    }

                    if (remoteActionMessage.isNotBlank()) {
                        Text(
                            remoteActionMessage,
                            color = if (remoteActionMessage.contains("verified")) Green else TextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    remoteActionMessage = ""
                    refreshRemotes()
                }) { Text("Refresh", color = Green) }
            },
            dismissButton = {
                TextButton(
                    enabled = remoteBusyName == null,
                    onClick = { remotesDialog = false }
                ) { Text("Close") }
            }
        )
    }

    remoteEditName?.let { remote ->
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp,
            containerColor = Color.Black,
            onDismissRequest = { remoteEditName = null },
            title = { Text("Edit $remote") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Advanced editor for this Google Drive remote only. OAuth tokens are sensitive.",
                        color = TextDim,
                        fontSize = 10.5.sp
                    )
                    OutlinedTextField(
                        value = remoteEditText,
                        onValueChange = { remoteEditText = it },
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = darkTextFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ConfigManager.replaceRemoteBlock(remote, remoteEditText)
                        .onSuccess {
                            remoteEditName = null
                            remoteActionMessage = "$remote: configuration saved. Run Test to verify it."
                            refreshRemotes()
                        }
                        .onFailure {
                            toast(context, it.message ?: "Remote configuration could not be saved.")
                        }
                }) { Text("Save", color = Green) }
            },
            dismissButton = {
                TextButton(onClick = { remoteEditName = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteRemoteName?.let { remote ->
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp,
            containerColor = Color.Black,
            onDismissRequest = { deleteRemoteName = null },
            title = { Text("Delete $remote?") },
            text = {
                Text(
                    "The remote will be removed from rclone.conf. Task cards are not deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ConfigManager.deleteRemote(remote)
                        .onSuccess {
                            deleteRemoteName = null
                            remoteActionMessage = "$remote: remote deleted."
                            refreshRemotes()
                        }
                        .onFailure {
                            toast(context, it.message ?: "Remote could not be deleted.")
                        }
                }) { Text("Delete", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteRemoteName = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (aboutDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp,
            containerColor = Color.Black,
            onDismissRequest = { aboutDialog = false },
            title = { Text("Basic Rclone Flow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Created by BlackWare with OpenAI ChatGPT.",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "A lightweight AMOLED-first Android task runner for rclone. Build reusable cards for copy, sync, move and other rclone workflows, then run them with live progress and background execution.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Text(
                        "Cloud provider support is intentionally limited to Google Drive in this version. Local Android storage remains supported.",
                        color = TextDim,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Text("Version ${BuildConfig.VERSION_NAME}", color = TextDim, fontSize = 10.5.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { aboutDialog = false }) {
                    Text("Close", color = Green)
                }
            }
        )
    }

    if (defaultsDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp,
            containerColor = Color.Black,
            onDismissRequest = { defaultsDialog = false },
            title = { Text("Default rclone options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "These values apply when a card leaves its advanced options blank. Values written directly in the command take priority.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = defaultsTransfersText,
                        onValueChange = { defaultsTransfersText = it.filter(Char::isDigit).take(2) },
                        label = { Text("--transfers") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = darkTextFieldColors()
                    )
                    OutlinedTextField(
                        value = defaultsCheckersText,
                        onValueChange = { defaultsCheckersText = it.filter(Char::isDigit).take(3) },
                        label = { Text("--checkers") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = darkTextFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setDefaults(
                        defaultsTransfersText.toIntOrNull() ?: 4,
                        defaultsCheckersText.toIntOrNull() ?: 8
                    )
                    defaultsDialog = false
                    toast(context, "Default rclone options saved")
                }) { Text("Save", color = Green) }
            },
            dismissButton = {
                TextButton(onClick = { defaultsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (clearCardsDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp,
            containerColor = Color.Black,
            onDismissRequest = { clearCardsDialog = false },
            title = { Text("Clear all cards?") },
            text = {
                Text(
                    "Every task card and its schedule will be removed. Drive and local files are not touched.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    CardStore.cards.value.forEach { TaskScheduler.cancel(context, it.id) }
                    CardStore.clearAll()
                    clearCardsDialog = false
                    toast(context, "All cards cleared")
                }) { Text("Clear", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { clearCardsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (clearDataDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(.94f).offset(y = 70.dp).border(1.dp, Outline, RoundedCornerShape(24.dp)),
            tonalElevation = 0.dp,
            containerColor = Color.Black,
            onDismissRequest = { clearDataDialog = false },
            title = { Text("Clear app data?") },
            text = {
                Text(
                    "Cards, rclone.conf and Basic Rclone Flow settings will be reset. Files on Drive or local storage are not deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    CardStore.cards.value.forEach { TaskScheduler.cancel(context, it.id) }
                    ConfigManager.writeText("")
                    AppSettings.reset()
                    CardStore.clearAll()
                    JobHistoryStore.clear()
                    JobRepository.clear()
                    clearDataDialog = false
                    toast(context, "App data reset")
                }) { Text("Reset", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { clearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun HistoryScreen(onBack:()->Unit){
    val history by JobHistoryStore.entries.collectAsState()
    LaunchedEffect(Unit){ JobHistoryStore.pruneNow() }
    Column(Modifier.fillMaxSize().background(Amoled)){
        TopBar("Activity History",onBack)
        if(history.isEmpty()){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Text("No operations in the last 72 hours.",color=TextDim)
            }
        } else LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding=androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            items(history,key={it.id}){h->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp,Outline,RoundedCornerShape(18.dp)).padding(13.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(h.title,color=TextPrimary,fontWeight=FontWeight.Bold,fontSize=14.sp)
                            Text(SimpleDateFormat("dd MMM · HH:mm",Locale.US).format(Date(h.finishedAtMillis)),color=TextDim,fontSize=10.5.sp)
                        }
                        Text(statusText(h.status),color=statusColor(h.status),fontWeight=FontWeight.Bold,fontSize=11.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(mainCommand(h.command).uppercase(Locale.US),color=TextSecondary,fontWeight=FontWeight.Bold,fontSize=10.5.sp)
                    Text(commandSummary(h.command),color=TextDim,fontFamily=FontFamily.Monospace,fontSize=10.5.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    val sec=h.startedAtMillis?.let{((h.finishedAtMillis-it).coerceAtLeast(0)/1000)}
                    Text(buildString{
                        append(formatBytes(h.bytes));append(" · ");append(h.transfers);append(" files")
                        sec?.let{append(" · ");append(formatDuration(it))}
                        h.exitCode?.let{append(" · exit ");append(it)}
                    },color=TextSecondary,fontSize=10.5.sp)
                    h.lastError?.takeIf{it.isNotBlank()}?.let{Text(it,color=Red,fontSize=10.5.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(Locale.getDefault()), color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Amoled).border(1.dp, Outline, RoundedCornerShape(20.dp)),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueColor: Color = TextSecondary,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.combinedClickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = valueColor, fontSize = 10.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary) }
        Text(title, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (action != null) action() else Spacer(Modifier.size(48.dp))
    }
    HorizontalDivider(color = Color(0xFF111114))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun darkTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Black,
    unfocusedContainerColor = Color.Black,
    focusedBorderColor = Green,
    unfocusedBorderColor = Outline,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextDim,
    cursorColor = Green
)

private fun customAccent(hex: String): Color? = runCatching {
    val raw = hex.trim()
    if (raw.isBlank()) return@runCatching null
    val normalized = if (raw.startsWith("#")) raw else "#$raw"
    require(Regex("^#[0-9A-Fa-f]{6}$").matches(normalized))
    Color(android.graphics.Color.parseColor(normalized))
}.getOrNull()

private fun accent(@Suppress("UNUSED_PARAMETER") card: TaskCard): Color = IconMuted

private fun mainCommand(command: String): String {
    val tokens = command.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return "command"
    val rcloneIndex = tokens.indexOfFirst {
        it.trim('"', '\'').substringAfterLast('/').equals("rclone", ignoreCase = true)
    }
    val index = if (rcloneIndex >= 0) rcloneIndex + 1 else 0
    return tokens.getOrNull(index)
        ?.trim('"', '\'')
        ?.lowercase(Locale.US)
        ?.ifBlank { "command" }
        ?: "command"
}

private fun accent(color: CardColor): Color = when (color) {
    CardColor.GREEN -> Green
    CardColor.PURPLE -> Purple
    CardColor.AMBER -> Amber
    CardColor.BLUE -> Cyan
    CardColor.RED -> Red
    CardColor.SLATE -> Slate
}

private fun cardIcon(icon: CardIcon): ImageVector = when (icon) {
    CardIcon.PHONE -> Icons.Default.PhoneAndroid
    CardIcon.CAMERA -> Icons.Default.PhotoCamera
    CardIcon.FOLDER -> Icons.Default.Folder
    CardIcon.CLOUD -> Icons.Default.Cloud
    CardIcon.DESKTOP -> Icons.Default.Computer
    CardIcon.SETTINGS -> Icons.Default.Settings
}

private fun statusColor(status: JobStatus): Color = when (status) {
    JobStatus.RUNNING -> Green
    JobStatus.PAUSED -> Amber
    JobStatus.COMPLETED -> Green
    JobStatus.ERROR -> Red
    JobStatus.QUEUED -> Cyan
    JobStatus.STOPPED -> Slate
    JobStatus.IDLE -> Slate
}

private fun statusText(status: JobStatus): String = when (status) {
    JobStatus.RUNNING -> "Running"
    JobStatus.PAUSED -> "Paused"
    JobStatus.COMPLETED -> "Completed"
    JobStatus.ERROR -> "Error"
    JobStatus.QUEUED -> "Queued"
    JobStatus.STOPPED -> "Stopped"
    JobStatus.IDLE -> "Idle"
}

private fun logColor(line: String): Color = when {
    line.startsWith("ERROR") -> Color(0xFFFCA5A5)
    line.startsWith("WARNING") || line.startsWith("WARN") -> Color(0xFFFCD34D)
    line.startsWith("STOP") -> Color(0xFFFCA5A5)
    line.startsWith("PAUSED") -> Color(0xFFFCD34D)
    else -> Color(0xFFBFC4CC)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024.0; i++ }
    return if (v >= 100 || i == 0) "%.0f %s".format(Locale.US, v, units[i]) else "%.1f %s".format(Locale.US, v, units[i])
}

private fun formatSpeed(speed: Double): String = if (speed <= 0.0) "0 B/s" else "${formatBytes(speed.toLong())}/s"

private fun formatEta(seconds: Long?): String {
    if (seconds == null || seconds < 0 || seconds > 365L * 86400L) return "ETA —"
    return "${formatDuration(seconds)} remaining"
}

private fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "%dh %02dm".format(h, m)
        m > 0 -> "%dm %02ds".format(m, sec)
        else -> "${sec}s"
    }
}

private fun commandSummary(command: String): String = command.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()

private fun needsStorage(card: TaskCard): Boolean = card.command.contains("/storage/") || card.command.contains("/sdcard/")

private fun sendService(context: Context, action: String, cardId: String) {
    val intent = Intent(context, RcloneService::class.java).apply {
        this.action = action
        putExtra(RcloneService.EXTRA_CARD_ID, cardId)
    }
    ContextCompat.startForegroundService(context, intent)
}

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
