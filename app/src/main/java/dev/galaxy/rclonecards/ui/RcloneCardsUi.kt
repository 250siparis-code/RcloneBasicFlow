@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.galaxy.rclonecards.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.galaxy.rclonecards.BuildConfig
import dev.galaxy.rclonecards.data.AppSettings
import dev.galaxy.rclonecards.data.CardStore
import dev.galaxy.rclonecards.data.ConfigManager
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
import java.util.Locale

private val Amoled = Color.Black
private val Surface = Color(0xFF111113)
private val Surface2 = Color(0xFF161619)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcloneCardsRoot(
    externalNavigation: StateFlow<String?>,
    onImportConfig: () -> Unit,
    onExportConfig: () -> Unit,
    onImportCards: () -> Unit,
    onExportCards: () -> Unit,
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
        if (value.startsWith("detail:")) {
            screen = Screen.Detail(value.substringAfter("detail:"))
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
                        toast(context, "Önce ‘Tüm dosyalara erişim’ iznini ver.")
                        onRequestAllFiles()
                    } else {
                        if (!hasNotificationPermission()) onRequestNotification()
                        sendService(context, RcloneService.ACTION_START, card.id)
                        screen = Screen.Detail(card.id)
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
                    screen = Screen.Detail(menuCard.id)
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
                    toast(context, "Ana ekran kısayolu isteği gönderildi")
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
                containerColor = Surface,
                title = { Text("Kart silinsin mi?") },
                text = { Text("${deleteCard.title} kartı ve ayarları silinecek.", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        TaskScheduler.cancel(context, deleteCard.id)
                        CardStore.delete(deleteCard.id)
                        deleteCardId = null
                    }) { Text("Sil", color = Red) }
                },
                dismissButton = { TextButton(onClick = { deleteCardId = null }) { Text("Vazgeç") } }
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 94.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Görev Kartları", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    val active = jobs.values.count { it.status == JobStatus.RUNNING || it.status == JobStatus.PAUSED || it.status == JobStatus.QUEUED }
                    if (active > 0) Text("$active aktif", color = Green, fontSize = 11.sp)
                }
            }
            items(cards, key = { it.id }) { card ->
                val job = jobs[card.id]
                TaskCardItem(
                    card = card,
                    job = job,
                    onClick = {
                        if (job?.status == JobStatus.RUNNING || job?.status == JobStatus.PAUSED || job?.status == JobStatus.QUEUED) {
                            onOpenDetail(card)
                        } else onRun(card)
                    },
                    onLongPress = { onLongPress(card) },
                    onRun = { onRun(card) }
                )
            }
            if (cards.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Henüz görev kartı yok", color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text("Sağ alttaki + ile ilk kartı ekle.", color = TextDim, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DimActionButton(icon = Icons.Default.Settings, contentDescription = "Ayarlar", onClick = onSettings)
            DimActionButton(icon = Icons.Default.Add, contentDescription = "Yeni görev", onClick = onAdd)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCardItem(
    card: TaskCard,
    job: JobState?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRun: () -> Unit
) {
    val accent = accent(card.color)
    val active = job?.status == JobStatus.RUNNING || job?.status == JobStatus.PAUSED
    val queued = job?.status == JobStatus.QUEUED
    val completed = job?.status == JobStatus.COMPLETED
    val error = job?.status == JobStatus.ERROR

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (active) Color(0xFF101412) else Surface)
            .border(if (active) 1.5.dp else 1.dp, if (active) accent else Outline, RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(cardIcon(card.icon), null, tint = accent, modifier = Modifier.size(25.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(card.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        active && job?.status == JobStatus.PAUSED -> "Duraklatıldı"
                        active -> "${card.actionLabel} · çalışıyor"
                        queued -> "Sırada"
                        completed -> "Tamamlandı"
                        error -> job?.lastError?.take(55) ?: "Hata"
                        else -> card.actionLabel
                    },
                    color = when {
                        completed -> Green
                        error -> Red
                        active || queued -> accent
                        else -> TextDim
                    },
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            when {
                active -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                        CircularProgressIndicator(
                            progress = (job?.progressPercent ?: 0) / 100f,
                            modifier = Modifier.size(46.dp),
                            color = accent,
                            trackColor = Color(0xFF29292E),
                            strokeWidth = 3.dp
                        )
                        Text("%${job?.progressPercent ?: 0}", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                completed -> Icon(Icons.Default.DoneAll, null, tint = Green, modifier = Modifier.size(28.dp))
                error -> Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(28.dp))
                else -> {
                    IconButton(
                        onClick = onRun,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent.copy(alpha = 0.14f))
                    ) {
                        Icon(Icons.Default.PlayArrow, "Çalıştır", tint = accent)
                    }
                }
            }
        }

        if (active && job != null) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = job.progressPercent / 100f,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = accent,
                trackColor = Color(0xFF29292E)
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${formatBytes(job.bytes)} / ${formatBytes(job.totalBytes)}", color = TextSecondary, fontSize = 10.sp)
                Text("${formatSpeed(job.speedBytesPerSecond)}  ·  ${formatEta(job.etaSeconds)}", color = TextSecondary, fontSize = 10.sp)
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                commandSummary(card.command),
                color = TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DimActionButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .alpha(0.72f)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0xFF101012))
            .border(1.dp, Outline, RoundedCornerShape(17.dp))
    ) {
        Icon(icon, contentDescription, tint = TextSecondary, modifier = Modifier.size(23.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121214),
        contentColor = TextPrimary,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent(card.color).copy(alpha = .16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(cardIcon(card.icon), null, tint = accent(card.color), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(card.subtitle, color = TextSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Kapat", tint = TextSecondary) }
            }
            HorizontalDivider(color = Outline)
            Spacer(Modifier.height(6.dp))
            MenuRow(Icons.Default.PlayArrow, "Çalıştır", accent(card.color), onRun)
            MenuRow(Icons.Default.Edit, "Düzenle", TextSecondary, onEdit)
            MenuRow(Icons.Default.ContentCopy, "Kopyala", TextSecondary, onCopy)
            MenuRow(Icons.Default.Edit, "Yeniden Adlandır", TextSecondary, onEdit)
            MenuRow(Icons.Default.Settings, "Renk / Simge Değiştir", Purple, onEdit)
            MenuRow(Icons.Default.PlaylistAdd, "Sıraya Ekle", Cyan, onQueue)
            MenuRow(Icons.Default.Schedule, "Zamanlayıcı", Amber, onEdit)
            MenuRow(Icons.Default.AddToHomeScreen, "Kısayol Oluştur", TextSecondary, onShortcut)
            if (job?.status == JobStatus.RUNNING || job?.status == JobStatus.PAUSED) {
                Text("Aktif işlem sürerken kart silinemez.", color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(14.dp))
            } else {
                MenuRow(Icons.Default.Delete, "Sil", Red, onDelete)
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, color = if (tint == Red) Red else TextPrimary, fontSize = 13.sp)
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
    var title by remember(card.id) { mutableStateOf(card.title) }
    var subtitle by remember(card.id) { mutableStateOf(card.subtitle) }
    var actionLabel by remember(card.id) { mutableStateOf(card.actionLabel) }
    var command by remember(card.id) { mutableStateOf(card.command) }
    var workDir by remember(card.id) { mutableStateOf(card.workDir) }
    var color by remember(card.id) { mutableStateOf(card.color) }
    var icon by remember(card.id) { mutableStateOf(card.icon) }
    var transfers by remember(card.id) { mutableStateOf(card.transfers?.toString().orEmpty()) }
    var checkers by remember(card.id) { mutableStateOf(card.checkers?.toString().orEmpty()) }
    var bwlimit by remember(card.id) { mutableStateOf(card.bwlimit) }
    var advancedExpanded by remember(card.id) { mutableStateOf(false) }
    var scheduleEnabled by remember(card.id) { mutableStateOf(card.scheduleEnabled) }
    var hour by remember(card.id) { mutableStateOf((card.scheduleHour ?: 3).toString()) }
    var minute by remember(card.id) { mutableStateOf((card.scheduleMinute ?: 0).toString().padStart(2, '0')) }

    Column(Modifier.fillMaxSize().background(Amoled)) {
        TopBar(title = "Kartı Düzenle", onBack = onBack, action = {
            IconButton(onClick = {
                val h = hour.toIntOrNull()?.coerceIn(0, 23)
                val m = minute.toIntOrNull()?.coerceIn(0, 59)
                onSave(
                    card.copy(
                        title = title.ifBlank { "Görev" },
                        subtitle = subtitle,
                        actionLabel = actionLabel.ifBlank { "Çalıştır" },
                        command = command.trim(),
                        workDir = workDir.trim().ifBlank { "/storage/emulated/0/" },
                        color = color,
                        icon = icon,
                        transfers = transfers.toIntOrNull()?.coerceIn(1, 64),
                        checkers = checkers.toIntOrNull()?.coerceIn(1, 128),
                        bwlimit = bwlimit.trim(),
                        scheduleEnabled = scheduleEnabled && h != null && m != null,
                        scheduleHour = if (scheduleEnabled) h else null,
                        scheduleMinute = if (scheduleEnabled) m else null
                    )
                )
            }) { Icon(Icons.Default.Check, "Kaydet", tint = Green) }
        })

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(62.dp).clip(RoundedCornerShape(19.dp)).background(accent(color).copy(alpha = .18f)).border(1.dp, accent(color).copy(alpha = .45f), RoundedCornerShape(19.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(cardIcon(icon), null, tint = accent(color), modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Kart Adı") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = darkTextFieldColors()
                )
            }

            OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Alt Başlık") }, modifier = Modifier.fillMaxWidth(), colors = darkTextFieldColors())
            OutlinedTextField(value = actionLabel, onValueChange = { actionLabel = it }, label = { Text("İşlem adı") }, modifier = Modifier.fillMaxWidth(), colors = darkTextFieldColors())

            SectionLabel("Simge")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardIcon.entries.forEach { candidate ->
                    IconChoice(candidate, selected = icon == candidate, accent = accent(color)) { icon = candidate }
                }
            }

            SectionLabel("Renk")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CardColor.entries.forEach { candidate ->
                    val c = accent(candidate)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(if (color == candidate) 3.dp else 0.dp, if (color == candidate) Color.White else Color.Transparent, CircleShape)
                            .combinedClickable(onClick = { color = candidate })
                    )
                }
            }

            SectionLabel("Çalıştırılacak Komut")
            Text(
                "Komutu normal Termux biçiminde yaz. Tırnaklı yollar ve tüm rclone alt komutları desteklenir.",
                color = TextDim,
                fontSize = 10.sp
            )
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth().height(170.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color(0xFF86EFAC)),
                colors = darkTextFieldColors(),
                label = { Text("rclone ...") }
            )

            OutlinedTextField(
                value = workDir,
                onValueChange = { workDir = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                colors = darkTextFieldColors(),
                label = { Text("Çalışma klasörü") }
            )

            OutlinedButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (advancedExpanded) "Gelişmiş Seçenekleri Gizle" else "Gelişmiş Seçenekler")
            }
            if (advancedExpanded) {
                Text(
                    "Boş bırakırsan Ayarlar’daki varsayılan değer kullanılır. Komutun içinde aynı bayrak varsa komut değeri önceliklidir.",
                    color = TextDim,
                    fontSize = 10.sp
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
                    label = { Text("Bandwidth sınırı (--bwlimit), örn. 10M") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = darkTextFieldColors()
                )
            }

            SectionLabel("Zamanlayıcı")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Her gün otomatik çalıştır", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (canExactAlarm) "Kesin alarm izni hazır" else "Kesin alarm izni yok; saat yaklaşık olabilir", color = TextDim, fontSize = 10.sp)
                }
                Switch(checked = scheduleEnabled, onCheckedChange = {
                    scheduleEnabled = it
                    if (it && !canExactAlarm) onRequestExactAlarm()
                })
            }
            if (scheduleEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Saat") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = darkTextFieldColors()
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Dakika") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = darkTextFieldColors()
                    )
                }
            }

            Button(
                onClick = {
                    val h = hour.toIntOrNull()?.coerceIn(0, 23)
                    val m = minute.toIntOrNull()?.coerceIn(0, 59)
                    onSave(
                        card.copy(
                            title = title.ifBlank { "Görev" },
                            subtitle = subtitle,
                            actionLabel = actionLabel.ifBlank { "Çalıştır" },
                            command = command.trim(),
                            workDir = workDir.trim().ifBlank { "/storage/emulated/0/" },
                            color = color,
                            icon = icon,
                            transfers = transfers.toIntOrNull()?.coerceIn(1, 64),
                            checkers = checkers.toIntOrNull()?.coerceIn(1, 128),
                            bwlimit = bwlimit.trim(),
                            scheduleEnabled = scheduleEnabled && h != null && m != null,
                            scheduleHour = if (scheduleEnabled) h else null,
                            scheduleMinute = if (scheduleEnabled) m else null
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IconChoice(icon: CardIcon, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(43.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Surface)
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
    val state = job ?: JobState(cardId = card?.id.orEmpty(), title = card?.title ?: "Görev")

    Column(Modifier.fillMaxSize().background(Amoled)) {
        TopBar(title = card?.let { "${it.title} · ${it.actionLabel}" } ?: "İşlem Detayı", onBack = onBack, action = {
            IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, "Menü", tint = TextSecondary) }
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
            StatRow(Icons.Default.DoneAll, if (state.totalTransfers > 0) "${state.transfers} / ${state.totalTransfers} dosya" else "${state.transfers} dosya")
            StatRow(Icons.Default.Refresh, formatSpeed(state.speedBytesPerSecond))
            StatRow(Icons.Default.Schedule, formatEta(state.etaSeconds))
            StatRow(Icons.Default.Code, "Geçen süre: ${formatDuration(state.elapsedSeconds.toLong())}")
            state.currentFile?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            state.lastError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Red, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(14.dp))
            Text("CANLI LOG", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Spacer(Modifier.width(6.dp)); Text("Durdur")
                    }
                    Button(onClick = onPause, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Surface2)) {
                        Icon(Icons.Default.Pause, null)
                        Spacer(Modifier.width(6.dp)); Text("Duraklat")
                    }
                }
                JobStatus.PAUSED -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                        Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp)); Text("Durdur")
                    }
                    Button(onClick = onResume, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Devam")
                    }
                }
                JobStatus.QUEUED -> OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                    Icon(Icons.Default.Close, null); Spacer(Modifier.width(6.dp)); Text("Kuyruktan Çıkar")
                }
                else -> Button(onClick = onRun, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Tekrar Çalıştır", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Arka Planda Çalış", color = TextSecondary) }
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
    onRequestAllFiles: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    hasAllFilesAccess: () -> Boolean,
    hasNotificationPermission: () -> Boolean,
    canExactAlarm: () -> Boolean
) {
    val context = LocalContext.current
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumeTick++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val storageOk = remember(resumeTick) { hasAllFilesAccess() }
    val notificationOk = remember(resumeTick) { hasNotificationPermission() }
    val alarmOk = remember(resumeTick) { canExactAlarm() }

    val defaultTransfers by AppSettings.defaultTransfers.collectAsState()
    val defaultCheckers by AppSettings.defaultCheckers.collectAsState()
    val notifyOnCompletion by AppSettings.notifyOnCompletion.collectAsState()

    var rcloneVersion by remember { mutableStateOf("Kontrol ediliyor...") }
    var remotes by remember { mutableStateOf<List<String>>(emptyList()) }
    var remotesDialog by remember { mutableStateOf(false) }
    var configDialog by remember { mutableStateOf(false) }
    var configText by remember { mutableStateOf("") }
    var resetCardsDialog by remember { mutableStateOf(false) }
    var defaultsDialog by remember { mutableStateOf(false) }
    var clearDataDialog by remember { mutableStateOf(false) }
    var defaultsTransfersText by remember { mutableStateOf(defaultTransfers.toString()) }
    var defaultsCheckersText by remember { mutableStateOf(defaultCheckers.toString()) }

    LaunchedEffect(Unit) {
        rcloneVersion = withContext(Dispatchers.IO) { RcloneEngine.version(context) }
    }

    Column(Modifier.fillMaxSize().background(Amoled)) {
        TopBar(title = "Ayarlar", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsGroup("Rclone") {
                SettingsRow(Icons.Default.FileOpen, "rclone.conf", "İçe aktar", onClick = onImportConfig)
                SettingsRow(Icons.Default.FileDownload, "rclone.conf", "Dışa aktar", onClick = onExportConfig)
                SettingsRow(Icons.Default.Code, "rclone.conf düzenle", "Uygulama içindeki yapılandırma", onClick = {
                    configText = ConfigManager.readText()
                    configDialog = true
                })
                SettingsRow(Icons.Default.Cloud, "Remote’ları Yönet", "Bağlı servisleri gör / config üzerinden düzenle", onClick = {
                    remotesDialog = true
                })
                SettingsRow(Icons.Default.Code, "Rclone Sürümü", rcloneVersion)
            }

            SettingsGroup("Uygulama") {
                SettingsRow(Icons.Default.Security, "Tema", "AMOLED saf siyah")
                SettingsRow(
                    Icons.Default.Notifications,
                    "Tamamlanma Bildirimi",
                    if (notifyOnCompletion) "Açık · işlem bitince sonuç bildir" else "Kapalı",
                    valueColor = if (notifyOnCompletion) Green else TextDim,
                    onClick = { AppSettings.setNotifyOnCompletion(!notifyOnCompletion) }
                )
                SettingsRow(
                    Icons.Default.Settings,
                    "Varsayılan Seçenekler",
                    "--transfers=$defaultTransfers, --checkers=$defaultCheckers",
                    onClick = {
                        defaultsTransfersText = defaultTransfers.toString()
                        defaultsCheckersText = defaultCheckers.toString()
                        defaultsDialog = true
                    }
                )
            }

            SettingsGroup("İzinler") {
                SettingsRow(
                    Icons.Default.Storage,
                    "Tüm dosyalara erişim",
                    if (storageOk) "Hazır" else "Gerekli: yerel klasörler için",
                    valueColor = if (storageOk) Green else Amber,
                    onClick = onRequestAllFiles
                )
                SettingsRow(
                    Icons.Default.Notifications,
                    "Bildirimler",
                    if (notificationOk) "Hazır" else "İşlem durumunu göstermek için izin ver",
                    valueColor = if (notificationOk) Green else Amber,
                    onClick = onRequestNotification
                )
                SettingsRow(
                    Icons.Default.Schedule,
                    "Kesin zamanlayıcı",
                    if (alarmOk) "Hazır" else "Günlük görevlerin tam saatinde çalışması için",
                    valueColor = if (alarmOk) Green else Amber,
                    onClick = onRequestExactAlarm
                )
            }

            SettingsGroup("Veri") {
                SettingsRow(Icons.Default.FileDownload, "Kartları Yedekle", "Tüm görev kartlarını JSON olarak dışa aktar", onClick = onExportCards)
                SettingsRow(Icons.Default.FileOpen, "Kartları Geri Yükle", "Daha önce alınmış JSON yedeğini içe aktar", onClick = onImportCards)
                SettingsRow(Icons.Default.Refresh, "Varsayılan Kartları Geri Getir", "Mevcut kart listesini örnek kartlarla değiştir", valueColor = Amber, onClick = {
                    resetCardsDialog = true
                })
            }

            SettingsGroup("Diğer") {
                SettingsRow(Icons.Default.Storage, "Kart verisi", "Yalnızca cihazda saklanır")
                SettingsRow(Icons.Default.Delete, "Uygulama Verisini Temizle", "Kartlar, config ve uygulama ayarlarını sıfırla", valueColor = Red, onClick = {
                    clearDataDialog = true
                })
                SettingsRow(Icons.Default.Code, "Hakkında", "Rclone Cards ${BuildConfig.VERSION_NAME} · gerçek rclone motoru")
            }

            Text(
                "Not: Android 11+ sistem kısıtları nedeniyle /Android/data ve /Android/obb klasörleri, ‘tüm dosyalara erişim’ izni olsa bile normal uygulamalara kapalı olabilir.",
                color = TextDim,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }

    if (defaultsDialog) {
        AlertDialog(
            onDismissRequest = { defaultsDialog = false },
            containerColor = Surface,
            title = { Text("Varsayılan rclone seçenekleri") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kartın gelişmiş seçenekleri boşsa bu değerler uygulanır. Komut satırında açıkça yazılan değer her zaman önceliklidir.", color = TextSecondary, fontSize = 11.sp)
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
                    toast(context, "Varsayılan rclone seçenekleri kaydedildi")
                }) { Text("Kaydet", color = Green) }
            },
            dismissButton = { TextButton(onClick = { defaultsDialog = false }) { Text("Vazgeç") } }
        )
    }

    if (clearDataDialog) {
        AlertDialog(
            onDismissRequest = { clearDataDialog = false },
            containerColor = Surface,
            title = { Text("Uygulama verileri sıfırlansın mı?") },
            text = { Text("Kartlar varsayılana döner, rclone.conf temizlenir ve uygulama ayarları sıfırlanır. Drive’daki veya telefondaki dosyalar silinmez.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    CardStore.cards.value.forEach { TaskScheduler.cancel(context, it.id) }
                    ConfigManager.writeText("")
                    AppSettings.reset()
                    CardStore.resetDefaults()
                    clearDataDialog = false
                    toast(context, "Uygulama verileri sıfırlandı")
                }) { Text("Sıfırla", color = Red) }
            },
            dismissButton = { TextButton(onClick = { clearDataDialog = false }) { Text("Vazgeç") } }
        )
    }

    if (configDialog) {
        AlertDialog(
            onDismissRequest = { configDialog = false },
            containerColor = Surface,
            title = { Text("rclone.conf") },
            text = {
                OutlinedTextField(
                    value = configText,
                    onValueChange = { configText = it },
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    colors = darkTextFieldColors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ConfigManager.writeText(configText)
                    configDialog = false
                    toast(context, "rclone.conf kaydedildi")
                }) { Text("Kaydet", color = Green) }
            },
            dismissButton = { TextButton(onClick = { configDialog = false }) { Text("Vazgeç") } }
        )
    }

    if (remotesDialog) {
        LaunchedEffect(remotesDialog) {
            remotes = withContext(Dispatchers.IO) { RcloneEngine.listRemotes(context) }
        }
        AlertDialog(
            onDismissRequest = { remotesDialog = false },
            containerColor = Surface,
            title = { Text("Remote’lar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (remotes.isEmpty()) Text("Remote bulunamadı veya config henüz içe aktarılmadı.", color = TextSecondary, fontSize = 12.sp)
                    remotes.forEach { remote ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A0A0C)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, null, tint = Cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(remote, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    remotesDialog = false
                    configText = ConfigManager.readText()
                    configDialog = true
                }) { Text("Config Düzenle", color = Green) }
            },
            dismissButton = { TextButton(onClick = { remotesDialog = false }) { Text("Kapat") } }
        )
    }

    if (resetCardsDialog) {
        AlertDialog(
            onDismissRequest = { resetCardsDialog = false },
            containerColor = Surface,
            title = { Text("Varsayılan kartlara dönülsün mü?") },
            text = { Text("Mevcut görev kartları değiştirilecek. İstersen önce ‘Kartları Yedekle’ ile JSON yedeği al.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    CardStore.cards.value.forEach { TaskScheduler.cancel(context, it.id) }
                    CardStore.resetDefaults()
                    resetCardsDialog = false
                    toast(context, "Varsayılan kartlar geri getirildi")
                }) { Text("Geri Getir", color = Amber) }
            },
            dismissButton = { TextButton(onClick = { resetCardsDialog = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(Locale.getDefault()), color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface).border(1.dp, Outline, RoundedCornerShape(20.dp)),
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
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri", tint = TextSecondary) }
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
    focusedContainerColor = Color(0xFF0C0C0E),
    unfocusedContainerColor = Color(0xFF0C0C0E),
    focusedBorderColor = Green,
    unfocusedBorderColor = Outline,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextDim,
    cursorColor = Green
)

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
    JobStatus.RUNNING -> "Çalışıyor"
    JobStatus.PAUSED -> "Duraklatıldı"
    JobStatus.COMPLETED -> "Tamamlandı"
    JobStatus.ERROR -> "Hata"
    JobStatus.QUEUED -> "Sırada"
    JobStatus.STOPPED -> "Durduruldu"
    JobStatus.IDLE -> "Boşta"
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
    return "${formatDuration(seconds)} kaldı"
}

private fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "%d sa %02d dk".format(h, m)
        m > 0 -> "%d dk %02d sn".format(m, sec)
        else -> "$sec sn"
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
