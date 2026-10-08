@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.galaxy.rclonecards.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import dev.galaxy.rclonecards.engine.RcloneConfigQuestion
import dev.galaxy.rclonecards.engine.RcloneConfigStep
import dev.galaxy.rclonecards.engine.RcloneConfigWizard
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
import java.util.Locale

private val Amoled = Color.Black
private val Surface = Color.Black
private val Surface2 = Color.Black
private val Outline = Color(0xFF232328)
private val TextPrimary = Color(0xFFF3F4F6)
private val TextSecondary = Color(0xFFA1A1AA)
private val TextDim = Color(0xFF71717A)
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

    var screen: Screen by remember { mutableStateOf(Screen.Home) }
    var menuCardId by remember { mutableStateOf<String?>(null) }
    var deleteCardId by remember { mutableStateOf<String?>(null) }

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
        when (val current = screen) {
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
                onStop = { sendService(context, RcloneService.ACTION_STOP, current.cardId) },
                onRun = { sendService(context, RcloneService.ACTION_START, current.cardId) },
                onMenu = { menuCardId = current.cardId }
            )

            Screen.Settings -> SettingsScreen(
                onBack = { screen = Screen.Home },
                onImportConfig = onImportConfig,
                onExportConfig = onExportConfig,
                onImportCards = onImportCards,
                onExportCards = onExportCards,
                onImportFullBackup = onImportFullBackup,
                onExportFullBackup = onExportFullBackup,
                onRequestAllFiles = onRequestAllFiles,
                onRequestNotification = onRequestNotification,
                onRequestExactAlarm = onRequestExactAlarm,
                hasAllFilesAccess = hasAllFilesAccess,
                hasNotificationPermission = hasNotificationPermission,
                canExactAlarm = canExactAlarm
            )
        }

        val menuCard = cards.firstOrNull { it.id == menuCardId }
        if (menuCard != null) {
            CardMenuSheet(
                card = menuCard,
                job = jobs[menuCard.id],
                onDismiss = { menuCardId = null },
                onRun = {
                    sendService(context, RcloneService.ACTION_START, menuCard.id)
                    menuCardId = null
                },
                onEdit = {
                    menuCardId = null
                    screen = Screen.Edit(menuCard.id)
                },
                onCopy = {
                    val newCard = CardStore.duplicate(menuCard.id)
                    menuCardId = null
                    if (newCard != null) screen = Screen.Edit(newCard.id)
                },
                onQueue = {
                    sendService(context, RcloneService.ACTION_ENQUEUE, menuCard.id)
                    menuCardId = null
                },
                onShortcut = {
                    onPinShortcut(menuCard)
                    menuCardId = null
                    toast(context, "Home-screen shortcut request sent")
                },
                onDelete = {
                    deleteCardId = menuCard.id
                    menuCardId = null
                }
            )
        }

        val deleteCard = cards.firstOrNull { it.id == deleteCardId }
        if (deleteCard != null) {
            AlertDialog(
                onDismissRequest = { deleteCardId = null },
                containerColor = Amoled,
                title = { Text("Delete card?") },
                text = { Text("${deleteCard.title} card and its settings will be deleted.", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        TaskScheduler.cancel(context, deleteCard.id)
                        CardStore.delete(deleteCard.id)
                        deleteCardId = null
                    }) { Text("Delete", color = Red) }
                },
                dismissButton = { TextButton(onClick = { deleteCardId = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    cards: List<TaskCard>,
    jobs: Map<String, JobState>,
    onRun: (TaskCard) -> Unit,
    onOpenDetail: (TaskCard) -> Unit,
    onLongPress: (TaskCard) -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = 94.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tasks", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val active = jobs.values.count {
                        it.status == JobStatus.RUNNING ||
                            it.status == JobStatus.PAUSED ||
                            it.status == JobStatus.QUEUED
                    }
                    if (active > 0) Text("$active active", color = Green, fontSize = 12.sp)
                }
            }

            items(cards, key = { it.id }) { card ->
                TaskCardItem(
                    card = card,
                    job = jobs[card.id],
                    onDoubleClick = { onOpenDetail(card) },
                    onLongPress = { onLongPress(card) },
                    onMenu = { onLongPress(card) },
                    onRun = { onRun(card) }
                )
            }

            if (cards.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No tasks yet", color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text("Use + to add your first task.", color = TextDim, fontSize = 13.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DimActionButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onSettings
            )
            Spacer(Modifier.width(4.dp))
            DimActionButton(
                icon = Icons.Default.Add,
                contentDescription = "Add task",
                onClick = onAdd
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCardItem(
    card: TaskCard,
    job: JobState?,
    onDoubleClick: () -> Unit,
    onLongPress: () -> Unit,
    onMenu: () -> Unit,
    onRun: () -> Unit
) {
    val accent = accent(card)
    val iconTint = accent.copy(alpha = card.iconAlpha.coerceIn(0.15f, 1f))
    val active = job?.status == JobStatus.RUNNING || job?.status == JobStatus.PAUSED
    val queued = job?.status == JobStatus.QUEUED
    val completed = job?.status == JobStatus.COMPLETED
    val error = job?.status == JobStatus.ERROR

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Amoled)
            .border(
                if (active) 1.5.dp else 1.dp,
                if (active) accent.copy(alpha = .72f) else Outline,
                RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                onClick = { },
                onDoubleClick = onDoubleClick,
                onLongClick = onLongPress
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(accent.copy(alpha = 0.10f * card.iconAlpha.coerceIn(0.15f, 1f)))
                    .border(
                        1.dp,
                        accent.copy(alpha = 0.28f * card.iconAlpha.coerceIn(0.15f, 1f)),
                        RoundedCornerShape(17.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CardIconGraphic(
                    customIconPath = card.customIconPath,
                    icon = card.icon,
                    tint = iconTint,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        mainCommand(card.command),
                        color = TextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    card.subtitle,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    active -> Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(iconTint.copy(alpha = .12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (job?.status == JobStatus.PAUSED) Amber else iconTint)
                        )
                    }

                    else -> IconButton(
                        onClick = onRun,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(iconTint.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            if (error || completed) "Run again" else "Run",
                            tint = iconTint,
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onMenu,
                    modifier = Modifier.size(28.dp).alpha(.55f)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        "Task menu",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when {
                active && job != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = job.progressPercent / 100f,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape),
                            color = accent,
                            trackColor = Color(0xFF202024)
                        )
                        Spacer(Modifier.width(11.dp))
                        Text(
                            "${job.progressPercent}%",
                            color = accent,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                error -> {
                    Text(
                        job?.lastError?.ifBlank { null } ?: "Error",
                        color = Red,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                queued -> {
                    Text(
                        "Queued",
                        color = accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                completed -> {
                    Text(
                        "Completed",
                        color = Green,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                job?.status == JobStatus.STOPPED -> {
                    Text(
                        "Stopped",
                        color = TextDim,
                        fontSize = 11.5.sp
                    )
                }

                else -> {
                    Text(
                        commandSummary(card.command),
                        color = TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DimActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).alpha(0.58f)
    ) {
        Icon(icon, contentDescription, tint = TextSecondary, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun CardMenuSheet(
    card: TaskCard,
    job: JobState?,
    onDismiss: () -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onQueue: () -> Unit,
    onShortcut: () -> Unit,
    onDelete: () -> Unit
) {
    val cardAccent = accent(card)
    val running = job?.status == JobStatus.RUNNING || job?.status == JobStatus.PAUSED

    Popup(
        alignment = Alignment.BottomCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(24.dp))
                .background(Amoled)
                .border(1.dp, Outline, RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(cardAccent.copy(alpha = .09f)),
                    contentAlignment = Alignment.Center
                ) {
                    CardIconGraphic(
                        customIconPath = card.customIconPath,
                        icon = card.icon,
                        tint = cardAccent.copy(alpha = card.iconAlpha),
                        modifier = Modifier.size(29.dp)
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 3.dp)
                ) {
                    Text(
                        card.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        card.subtitle,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    NeonMenuIcon(
                        icon = Icons.Default.Edit,
                        tint = Color(0xFFB8BBC4),
                        contentDescription = "Edit"
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                MenuGridButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PlayArrow,
                    label = "Run",
                    tint = cardAccent,
                    onClick = { onDismiss(); onRun() }
                )
                MenuGridButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ContentCopy,
                    label = "Duplicate",
                    tint = Color(0xFFB8BBC4),
                    onClick = { onDismiss(); onCopy() }
                )
                MenuGridButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PlaylistAdd,
                    label = "Add to Queue",
                    tint = Cyan,
                    onClick = { onDismiss(); onQueue() }
                )
            }

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                MenuGridButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    label = "Schedule",
                    tint = Amber,
                    onClick = { onDismiss(); onEdit() }
                )
                MenuGridButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AddToHomeScreen,
                    label = "Add to Home",
                    tint = Color(0xFFB8BBC4),
                    onClick = { onDismiss(); onShortcut() }
                )
                MenuGridButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    tint = Red,
                    enabled = !running,
                    onClick = { onDismiss(); onDelete() }
                )
            }
        }
    }
}

@Composable
private fun NeonMenuIcon(
    icon: ImageVector,
    tint: Color,
    contentDescription: String? = null
) {
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = .045f))
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = .055f))
        )
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(29.dp)
        )
    }
}

@Composable
private fun MenuGridButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val effectiveTint = if (enabled) tint else TextDim.copy(alpha = .35f)

    Column(
        modifier = modifier
            .height(80.dp)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NeonMenuIcon(
            icon = icon,
            tint = effectiveTint
        )

        Spacer(Modifier.height(3.dp))

        Text(
            label,
            color = if (enabled) TextPrimary else TextDim.copy(alpha = .45f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2
        )
    }
}

@Composable
private fun EditCardScreen(
    card: TaskCard,
    canExactAlarm: Boolean,
    onRequestExactAlarm: () -> Unit,
    onBack: () -> Unit,
    onSave: (TaskCard) -> Unit
) {
    val context = LocalContext.current
    var title by remember(card.id) { mutableStateOf(card.title) }
    var subtitle by remember(card.id) { mutableStateOf(card.subtitle) }
    var actionLabel by remember(card.id) { mutableStateOf(card.actionLabel) }
    var command by remember(card.id) { mutableStateOf(card.command) }
    var workDir by remember(card.id) { mutableStateOf(card.workDir) }
    var color by remember(card.id) { mutableStateOf(card.color) }
    var customColorHex by remember(card.id) { mutableStateOf(card.customColorHex) }
    var iconAlpha by remember(card.id) { mutableStateOf(card.iconAlpha.coerceIn(0.15f, 1f)) }
    var icon by remember(card.id) { mutableStateOf(card.icon) }
    var customIconPath by remember(card.id) { mutableStateOf(card.customIconPath) }
    val currentAccent = customAccent(customColorHex) ?: accent(color)
    var transfers by remember(card.id) { mutableStateOf(card.transfers?.toString().orEmpty()) }
    var checkers by remember(card.id) { mutableStateOf(card.checkers?.toString().orEmpty()) }
    var bwlimit by remember(card.id) { mutableStateOf(card.bwlimit) }
    var advancedExpanded by remember(card.id) { mutableStateOf(false) }
    var scheduleEnabled by remember(card.id) { mutableStateOf(card.scheduleEnabled) }
    var hour by remember(card.id) { mutableStateOf((card.scheduleHour ?: 3).toString()) }
    var minute by remember(card.id) { mutableStateOf((card.scheduleMinute ?: 0).toString().padStart(2, '0')) }

    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val dir = File(context.filesDir, "card-icons").apply { mkdirs() }
                val dest = File(dir, "${card.id}.img")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Icon file could not be read")
                customIconPath = dest.absolutePath
            }.onFailure {
                toast(context, "Icon could not be imported: ${it.message}")
            }
        }
    }

    fun buildCard(): TaskCard {
        val h = hour.toIntOrNull()?.coerceIn(0, 23)
        val m = minute.toIntOrNull()?.coerceIn(0, 59)
        return card.copy(
            title = title.ifBlank { "Task" },
            subtitle = subtitle,
            actionLabel = actionLabel.ifBlank { "Run" },
            command = command.trim(),
            workDir = workDir.trim().ifBlank { "/storage/emulated/0/" },
            color = color,
            customColorHex = customColorHex.trim(),
            iconAlpha = iconAlpha,
            customIconPath = customIconPath,
            icon = icon,
            transfers = transfers.toIntOrNull()?.coerceIn(1, 64),
            checkers = checkers.toIntOrNull()?.coerceIn(1, 128),
            bwlimit = bwlimit.trim(),
            scheduleEnabled = scheduleEnabled && h != null && m != null,
            scheduleHour = if (scheduleEnabled) h else null,
            scheduleMinute = if (scheduleEnabled) m else null
        )
    }

    Column(Modifier.fillMaxSize().background(Amoled)) {
        TopBar(
            title = "Edit Card",
            onBack = onBack,
            action = {
                IconButton(onClick = { onSave(buildCard()) }) {
                    Icon(Icons.Default.Check, "Save", tint = Green)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .width(58.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(currentAccent.copy(alpha = .10f))
                        .border(1.dp, currentAccent.copy(alpha = .38f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CardIconGraphic(
                        customIconPath = customIconPath,
                        icon = icon,
                        tint = currentAccent.copy(alpha = iconAlpha),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = darkTextFieldColors()
                )
            }

            OutlinedTextField(
                value = subtitle,
                onValueChange = { subtitle = it },
                label = { Text("Subtitle") },
                modifier = Modifier.fillMaxWidth(),
                colors = darkTextFieldColors()
            )

            OutlinedTextField(
                value = actionLabel,
                onValueChange = { actionLabel = it },
                label = { Text("Action label") },
                modifier = Modifier.fillMaxWidth(),
                colors = darkTextFieldColors()
            )

            SectionLabel("Icon")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardIcon.entries.forEach { candidate ->
                    IconChoice(
                        candidate,
                        selected = icon == candidate && customIconPath.isBlank(),
                        accent = currentAccent
                    ) {
                        icon = candidate
                        customIconPath = ""
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { iconPicker.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add Custom Icon")
                }

                if (customIconPath.isNotBlank()) {
                    OutlinedButton(
                        onClick = { customIconPath = "" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Remove")
                    }
                }
            }

            SectionLabel("Color")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardColor.entries.forEach { candidate ->
                    val c = accent(candidate)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                if (color == candidate && customColorHex.isBlank()) 3.dp else 0.dp,
                                if (color == candidate && customColorHex.isBlank()) Color.White else Color.Transparent,
                                CircleShape
                            )
                            .combinedClickable(onClick = {
                                color = candidate
                                customColorHex = ""
                            })
                    )
                }
            }

            OutlinedTextField(
                value = customColorHex,
                onValueChange = { value -> customColorHex = value.take(7) },
                label = { Text("Custom color (#RRGGBB)") },
                placeholder = { Text("#06B6D4") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = darkTextFieldColors()
            )

            Text("Icon opacity ${(iconAlpha * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = iconAlpha,
                onValueChange = { iconAlpha = it },
                valueRange = 0.15f..1f,
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("Command")
            Text(
                "Enter a normal rclone command. Quoted paths and Termux-style shared-storage paths are supported.",
                color = TextDim,
                fontSize = 11.sp
            )

            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth().height(170.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF86EFAC)
                ),
                colors = darkTextFieldColors(),
                label = { Text("rclone ...") }
            )

            OutlinedTextField(
                value = workDir,
                onValueChange = { workDir = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                colors = darkTextFieldColors(),
                label = { Text("Working directory") }
            )

            OutlinedButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (advancedExpanded) "Hide Advanced Options" else "Advanced Options")
            }

            if (advancedExpanded) {
                Text(
                    "If left blank, the app default is used. A value written directly in the command always takes priority.",
                    color = TextDim,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = transfers,
                        onValueChange = { transfers = it.filter(Char::isDigit).take(2) },
                        label = { Text("Transfers") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = darkTextFieldColors()
                    )
                    OutlinedTextField(
                        value = checkers,
                        onValueChange = { checkers = it.filter(Char::isDigit).take(3) },
                        label = { Text("Checkers") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = darkTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = bwlimit,
                    onValueChange = { bwlimit = it },
                    label = { Text("Bandwidth limit (--bwlimit), e.g. 10M") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = darkTextFieldColors()
                )
            }

            SectionLabel("Schedule")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Run automatically every day", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (canExactAlarm) {
                            "Exact alarm permission ready"
                        } else {
                            "Exact alarm permission unavailable; timing may be approximate"
                        },
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = scheduleEnabled,
                    onCheckedChange = {
                        scheduleEnabled = it
                        if (it && !canExactAlarm) onRequestExactAlarm()
                    }
                )
            }

            if (scheduleEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = darkTextFieldColors()
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = darkTextFieldColors()
                    )
                }
            }

            Button(
                onClick = { onSave(buildCard()) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardIconGraphic(
    customIconPath: String,
    icon: CardIcon,
    tint: Color,
    modifier: Modifier
) {
    val bitmap = remember(customIconPath) {
        customIconPath
            .takeIf { it.isNotBlank() }
            ?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
            ?.asImageBitmap()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(cardIcon(icon), contentDescription = null, tint = tint, modifier = modifier)
    }
}

@Composable
private fun IconChoice(icon: CardIcon, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(43.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Amoled)
            .border(1.dp, if (selected) accent else Outline, RoundedCornerShape(13.dp))
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(cardIcon(icon), null, tint = if (selected) accent else TextSecondary, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun DetailScreen(
    card: TaskCard?,
    job: JobState?,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRun: () -> Unit,
    onMenu: () -> Unit
) {
    val accent = accent(card?.color ?: CardColor.GREEN)
    val state = job ?: JobState(cardId = card?.id.orEmpty(), title = card?.title ?: "Task")

    Column(Modifier.fillMaxSize().background(Amoled)) {
        TopBar(title = card?.let { "${it.title} · ${it.actionLabel}" } ?: "Task Details", onBack = onBack, action = {
            IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, "Menu", tint = TextSecondary) }
        })

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(statusColor(state.status)))
                    Spacer(Modifier.width(8.dp))
                    Text(statusText(state.status), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("%${state.progressPercent}", color = accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = state.progressPercent / 100f,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = accent,
                trackColor = Color(0xFF29292E)
            )
            Spacer(Modifier.height(18.dp))

            StatRow(Icons.Default.Storage, if (state.totalBytes > 0) "${formatBytes(state.bytes)} / ${formatBytes(state.totalBytes)}" else formatBytes(state.bytes))
            StatRow(Icons.Default.DoneAll, if (state.totalTransfers > 0) "${state.transfers} / ${state.totalTransfers} files" else "${state.transfers} files")
            StatRow(Icons.Default.Refresh, formatSpeed(state.speedBytesPerSecond))
            StatRow(Icons.Default.Schedule, formatEta(state.etaSeconds))
            StatRow(Icons.Default.Code, "Elapsed: ${formatDuration(state.elapsedSeconds.toLong())}")
            state.currentFile?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            state.lastError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Red, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(14.dp))
            Text("LIVE LOG", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF080809))
                    .border(1.dp, Color(0xFF17171A), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(state.logs) { line ->
                    Text(line, color = logColor(line), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            when (state.status) {
                JobStatus.RUNNING -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(Modifier.width(6.dp)); Text("Stop")
                    }
                    Button(onClick = onPause, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Amoled)) {
                        Icon(Icons.Default.Pause, null)
                        Spacer(Modifier.width(6.dp)); Text("Pause")
                    }
                }
                JobStatus.PAUSED -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                        Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp)); Text("Stop")
                    }
                    Button(onClick = onResume, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Resume")
                    }
                }
                JobStatus.QUEUED -> OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                    Icon(Icons.Default.Close, null); Spacer(Modifier.width(6.dp)); Text("Remove from Queue")
                }
                else -> Button(onClick = onRun, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Run Again", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Run in Background", color = TextSecondary) }
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = TextDim, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
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
    canExactAlarm: () -> Boolean
) {
    val context = LocalContext.current
    val settingsScope = rememberCoroutineScope()
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val storageOk = remember(resumeTick) { hasAllFilesAccess() }
    val notificationOk = remember(resumeTick) { hasNotificationPermission() }
    val alarmOk = remember(resumeTick) { canExactAlarm() }

    val defaultTransfers by AppSettings.defaultTransfers.collectAsState()
    val defaultCheckers by AppSettings.defaultCheckers.collectAsState()
    val notifyOnCompletion by AppSettings.notifyOnCompletion.collectAsState()

    var rcloneVersion by remember { mutableStateOf("Checking...") }
    var remotes by remember { mutableStateOf<List<String>>(emptyList()) }
    var remotesDialog by remember { mutableStateOf(false) }
    var remoteBusyName by remember { mutableStateOf<String?>(null) }
    var remoteActionMessage by remember { mutableStateOf("") }
    var remoteEditName by remember { mutableStateOf<String?>(null) }
    var remoteEditText by remember { mutableStateOf("") }
    var deleteRemoteName by remember { mutableStateOf<String?>(null) }

    var clearCardsDialog by remember { mutableStateOf(false) }
    var defaultsDialog by remember { mutableStateOf(false) }
    var clearDataDialog by remember { mutableStateOf(false) }

    var driveSetupDialog by remember { mutableStateOf(false) }
    var driveStep by remember { mutableStateOf<RcloneConfigStep?>(null) }
    var driveAnswer by remember { mutableStateOf("") }
    var driveBusy by remember { mutableStateOf(false) }
    var driveWizardError by remember { mutableStateOf<String?>(null) }
    var driveRemoteName by remember { mutableStateOf("gdrive") }

    var termuxConfigDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    var defaultsTransfersText by remember { mutableStateOf(defaultTransfers.toString()) }
    var defaultsCheckersText by remember { mutableStateOf(defaultCheckers.toString()) }

    val serviceAccountPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val dir = File(context.filesDir, "rclone").apply { mkdirs() }
                val dest = File(dir, "service-account.json")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Service Account JSON could not be read.")
                driveAnswer = dest.absolutePath
            }.onFailure {
                driveWizardError = "Service Account import failed: ${it.message}"
            }
        }
    }

    fun refreshRemotes() {
        settingsScope.launch {
            remotes = withContext(Dispatchers.IO) { ConfigManager.driveRemoteNames() }
        }
    }

    LaunchedEffect(Unit) {
        rcloneVersion = withContext(Dispatchers.IO) { RcloneEngine.version(context) }
    }

    Column(Modifier.fillMaxSize().background(Amoled)) {
        TopBar(title = "Settings", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsGroup("Google Drive") {
                SettingsRow(
                    Icons.Default.Cloud,
                    "Google Drive Setup",
                    "Native rclone RC setup with OAuth and live connection verification",
                    onClick = {
                        driveStep = null
                        driveAnswer = ""
                        driveWizardError = null
                        driveSetupDialog = true
                    }
                )
                SettingsRow(
                    Icons.Default.Settings,
                    "Manage Google Drive",
                    "Test, edit or remove configured Drive remotes",
                    onClick = {
                        remoteActionMessage = ""
                        remotesDialog = true
                        refreshRemotes()
                    }
                )
                SettingsRow(
                    Icons.Default.Security,
                    "Supported cloud provider",
                    "Google Drive only in this version"
                )
            }

            SettingsGroup("Rclone") {
                SettingsRow(
                    Icons.Default.FileOpen,
                    "Import rclone.conf",
                    "Import an existing rclone configuration",
                    onClick = onImportConfig
                )
                SettingsRow(
                    Icons.Default.Code,
                    "Import from Termux",
                    "Copy the active Termux rclone.conf into Downloads",
                    onClick = { termuxConfigDialog = true }
                )
                SettingsRow(
                    Icons.Default.FileDownload,
                    "Export rclone.conf",
                    "Export the app-private rclone configuration",
                    onClick = onExportConfig
                )
                SettingsRow(Icons.Default.Code, "Rclone Version", rcloneVersion)
            }

            SettingsGroup("App") {
                SettingsRow(
                    Icons.Default.Notifications,
                    "Completion Notification",
                    if (notifyOnCompletion) "On · notify when a task finishes" else "Off",
                    valueColor = if (notifyOnCompletion) Green else TextDim,
                    onClick = { AppSettings.setNotifyOnCompletion(!notifyOnCompletion) }
                )
                SettingsRow(
                    Icons.Default.Settings,
                    "Default Options",
                    "--transfers=$defaultTransfers, --checkers=$defaultCheckers",
                    onClick = {
                        defaultsTransfersText = defaultTransfers.toString()
                        defaultsCheckersText = defaultCheckers.toString()
                        defaultsDialog = true
                    }
                )
            }

            SettingsGroup("Permissions") {
                SettingsRow(
                    Icons.Default.Storage,
                    "All files access",
                    if (storageOk) "Ready" else "Required for local folders",
                    valueColor = if (storageOk) Green else Amber,
                    onClick = onRequestAllFiles
                )
                SettingsRow(
                    Icons.Default.Notifications,
                    "Notifications",
                    if (notificationOk) "Ready" else "Allow task-status notifications",
                    valueColor = if (notificationOk) Green else Amber,
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
            onDismissRequest = { termuxConfigDialog = false },
            containerColor = Amoled,
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
        val activity = context as? Activity
        val question: RcloneConfigQuestion? = driveStep?.question
        val setupCompleted = driveStep?.done == true

        fun applyStep(step: RcloneConfigStep) {
            driveStep = step
            if (step.done && step.verified) {
                val host = activity
                if (host != null) {
                    settingsScope.launch { RcloneConfigWizard.finish(host) }
                }
                driveSetupDialog = false
                driveStep = null
                driveAnswer = ""
                driveWizardError = null
                toast(context, "${driveRemoteName.ifBlank { "gdrive" }} connected and verified")
            } else if (step.done) {
                driveAnswer = ""
                driveWizardError = step.verificationMessage
            } else {
                driveAnswer = step.question?.defaultValue.orEmpty()
                driveWizardError = step.question?.error?.takeIf { it.isNotBlank() }
            }
        }

        fun startSetup() {
            val host = activity
            if (host == null) {
                driveWizardError = "Unable to access the Android activity."
                return
            }
            driveBusy = true
            driveWizardError = null
            settingsScope.launch {
                RcloneConfigWizard.startDrive(
                    activity = host,
                    remoteName = driveRemoteName
                ).onSuccess(::applyStep)
                    .onFailure { error ->
                        driveWizardError = error.message ?: error.toString()
                    }
                driveBusy = false
            }
        }

        fun continueSetup() {
            val host = activity
            val q = question
            if (host == null || q == null) return

            val answer = driveAnswer.takeIf { it.isNotBlank() } ?: q.defaultValue
            if (q.required && answer.isBlank()) {
                driveWizardError = "This value is required."
                return
            }

            driveBusy = true
            driveWizardError = null
            settingsScope.launch {
                RcloneConfigWizard.answerDrive(
                    activity = host,
                    remoteName = driveRemoteName,
                    state = q.state,
                    result = answer
                ).onSuccess(::applyStep)
                    .onFailure { error ->
                        driveWizardError = error.message ?: error.toString()
                    }
                driveBusy = false
            }
        }

        fun retryVerification() {
            val host = activity ?: return
            driveBusy = true
            driveWizardError = null
            settingsScope.launch {
                RcloneConfigWizard.verifyDrive(
                    activity = host,
                    remoteName = driveRemoteName
                ).onSuccess(::applyStep)
                    .onFailure { error ->
                        driveWizardError = error.message ?: error.toString()
                    }
                driveBusy = false
            }
        }

        fun closeSetup() {
            val host = activity
            if (host != null) {
                settingsScope.launch {
                    if (setupCompleted) {
                        RcloneConfigWizard.finish(host)
                    } else {
                        RcloneConfigWizard.cancel(host)
                    }
                }
            }
            driveSetupDialog = false
            driveStep = null
            driveAnswer = ""
            driveWizardError = null
        }

        AlertDialog(
            onDismissRequest = { if (!driveBusy) closeSetup() },
            containerColor = Amoled,
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
                            "rclone.conf was created, but Basic Rclone Flow does not mark the setup as verified until a real Drive root listing succeeds.",
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
                            setupCompleted -> retryVerification()
                            question == null -> startSetup()
                            else -> continueSetup()
                        }
                    }
                ) {
                    Text(
                        when {
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
                    onClick = { closeSetup() }
                ) { Text("Close") }
            }
        )
    }

    if (remotesDialog) {
        AlertDialog(
            onDismissRequest = { if (remoteBusyName == null) remotesDialog = false },
            containerColor = Amoled,
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
            onDismissRequest = { remoteEditName = null },
            containerColor = Amoled,
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
            onDismissRequest = { deleteRemoteName = null },
            containerColor = Amoled,
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
            onDismissRequest = { aboutDialog = false },
            containerColor = Amoled,
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
            onDismissRequest = { defaultsDialog = false },
            containerColor = Amoled,
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
            onDismissRequest = { clearCardsDialog = false },
            containerColor = Amoled,
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
            onDismissRequest = { clearDataDialog = false },
            containerColor = Amoled,
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

private fun accent(card: TaskCard): Color = customAccent(card.customColorHex) ?: accent(card.color)

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
