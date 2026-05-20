package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.SimpleMathParser
import com.example.ui.WorkViewModel
import com.example.ui.theme.Typography
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Elegant Slate Premium Colors overriding MaterialTheme
            val SlateDarkPalette = darkColorScheme(
                primary = Color(0xFF38BDF8),       // Sky accent
                secondary = Color(0xFF94A3B8),     // Muted text gray
                tertiary = Color(0xFF34D399),      // Safe Emerald
                background = Color(0xFF0F172A),    // Rich deep slate
                surface = Color(0xFF1E293B),       // Slate-800 Cards
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFF0F172A),
                onBackground = Color(0xFFF1F5F9),  // Light off-white text
                onSurface = Color(0xFFF1F5F9)
            )

            MaterialTheme(
                colorScheme = SlateDarkPalette,
                typography = Typography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: WorkViewModel = viewModel()
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        viewModel.init(context)
                    }

                    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()

                    Crossfade(targetState = activeScreen, label = "screen_trans") { screen ->
                        when (screen) {
                            WorkViewModel.Screen.Main -> {
                                MainAppScreen(viewModel = viewModel)
                            }
                            WorkViewModel.Screen.PdfReport -> {
                                PdfReportScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Particle class for native Confetti drawing
data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float,
    var alpha: Float = 1f
)

@Composable
fun ConfettiCanvas(
    trigger: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (!trigger) return

    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val colors = listOf(
        Color(0xFFA78BFA), // Purple
        Color(0xFF38BDF8), // Blue
        Color(0xFF34D399), // Green
        Color(0xFFFBBF24)  // Yellow
    )

    LaunchedEffect(trigger) {
        particles.clear()
        for (i in 0 until 120) {
            val angleDeg = 90.0 + (Math.random() * 80.0 - 40.0) // 80 deg spread
            val angleRad = Math.toRadians(angleDeg)
            val speed = 10f + (Math.random() * 25f).toFloat()
            val vx = (Math.cos(angleRad) * speed).toFloat()
            val vy = (-Math.sin(angleRad) * speed).toFloat()
            
            particles.add(
                ConfettiParticle(
                    x = 0.5f,
                    y = 0.6f,
                    vx = vx,
                    vy = vy,
                    color = colors.random(),
                    radius = 6f + (Math.random() * 8f).toFloat()
                )
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tick")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick_anim"
    )

    var elapsed by remember { mutableStateOf(0) }
    LaunchedEffect(tick) {
        elapsed++
        if (elapsed > 45) { // Stop after approx ~1.2s (45 frames)
            onAnimationEnd()
            elapsed = 0
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            p.x += p.vx / w
            p.y += p.vy / h
            p.y += 0.35f / h
            
            p.alpha = maxOf(0f, p.alpha - 0.022f)

            if (p.alpha > 0f) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.radius,
                    center = Offset(p.x * w, p.y * h)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: WorkViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isMobile = LocalConfiguration.current.screenWidthDp <= 768

    val state by viewModel.appState.collectAsStateWithLifecycle()
    val curMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val selectedRows by viewModel.selectedRows.collectAsStateWithLifecycle()
    val confirmDeleteRowId by viewModel.confirmDeleteRowId.collectAsStateWithLifecycle()

    var showBulkAddModal by remember { mutableStateOf(false) }
    var showPdfRangeModal by remember { mutableStateOf(false) }
    var calcExpanded by remember { mutableStateOf(false) }
    var calculatorInput by remember { mutableStateOf("0") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val density = LocalDensity.current
    var appBarOffsetHeightPx by remember { mutableStateOf(0f) }
    val appBarHeight = 64.dp
    val appBarHeightPx = with(density) { appBarHeight.roundToPx().toFloat() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = appBarOffsetHeightPx + delta
                appBarOffsetHeightPx = newOffset.coerceIn(-appBarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    val backupReminderNeeded by viewModel.showBackupReminder.collectAsStateWithLifecycle()

    var triggerConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.showConfettiEvent) {
        viewModel.showConfettiEvent.collectLatest {
            triggerConfetti = true
        }
    }

    val currentMonthData = state.monthlyData[curMonth] ?: MonthData()

    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val keyboardHeightDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val reader = InputStreamReader(input).buffered()
                val text = reader.use { r -> r.readText() }
                val ok = viewModel.importBackupJson(text, context)
                if (ok) {
                    Toast.makeText(context, "Backup successfully imported!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid backup json file format.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading backup file.", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (backupReminderNeeded) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackupReminder(context) },
            title = { Text("Backup Reminder", fontWeight = FontWeight.Bold) },
            text = { Text("It has been 5 days since your last backup. Would you like to download a JSON backup of your logs now?") },
            confirmButton = {
                Button(
                    onClick = {
                        val text = viewModel.getExportJson()
                        shareJsonBackup(context, text)
                        viewModel.updateBackupTimestamp(context)
                    }
                ) {
                    Text("Backup (JSON)")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBackupReminder(context) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPdfRangeModal) {
        Dialog(onDismissRequest = { showPdfRangeModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PDF Log Report Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select data range to compile into printable PDF:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.selectPdfRange("current")
                            showPdfRangeModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Current Month (Today)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.selectPdfRange("previous")
                            showPdfRangeModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Previous Month")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.selectPdfRange("all")
                            showPdfRangeModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("All Months Combined")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showPdfRangeModal = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    var isFabMenuOpen by remember { mutableStateOf(false) }

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileSettingsDrawer(
                viewModel = viewModel,
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            topBar = {
                val isMobile = LocalConfiguration.current.screenWidthDp <= 768
                val yOffset = if (isMobile) appBarOffsetHeightPx else 0f
                Column(
                    modifier = Modifier
                        .offset { IntOffset(0, yOffset.roundToInt()) }
                        .fillMaxWidth()
                ) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Profile")
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WorkDevForce",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "· $curMonth",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (currentMonthData.isClosed) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "CLOSED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .padding(bottom = if (isKeyboardOpen) keyboardHeightDp else 0.dp)
                ) {
                    RadialFabMenu(
                        isOpen = isFabMenuOpen,
                        onToggle = { isFabMenuOpen = !isFabMenuOpen },
                        onAction = { action ->
                            isFabMenuOpen = false
                            when (action) {
                                "export_json" -> {
                                    val text = viewModel.getExportJson()
                                    shareJsonBackup(context, text)
                                    viewModel.updateBackupTimestamp(context)
                                }
                                "export_pdf" -> {
                                    showPdfRangeModal = true
                                }
                                "import" -> {
                                    fileImportLauncher.launch("application/json")
                                }
                                "bulk_add" -> {
                                    showBulkAddModal = true
                                }
                                "add_row" -> {
                                    viewModel.addSingleRow(context)
                                    scope.launch {
                                        if (currentMonthData.rows.isNotEmpty()) {
                                            listState.animateScrollToItem(currentMonthData.rows.size - 1)
                                        }
                                    }
                                }
                                "tasks" -> {
                                    scope.launch {
                                        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                                    }
                                }
                            }
                        },
                        isMonthClosed = currentMonthData.isClosed
                    )
                }
            }
        ) { innerPadding ->
            if (showBulkAddModal) {
                Dialog(onDismissRequest = { showBulkAddModal = false }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Bulk Add Days",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Add sequential work log rows to the current viewed month:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(onClick = {
                                    viewModel.handleBulkAdd("5", context)
                                    showBulkAddModal = false
                                }) { Text("+5 Days") }
                                Button(onClick = {
                                    viewModel.handleBulkAdd("10", context)
                                    showBulkAddModal = false
                                }) { Text("+10 Days") }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(onClick = {
                                    viewModel.handleBulkAdd("15", context)
                                    showBulkAddModal = false
                                }) { Text("+15 Days") }
                                Button(onClick = {
                                    viewModel.handleBulkAdd("full", context)
                                    showBulkAddModal = false
                                }) { Text("Full Month") }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { showBulkAddModal = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x0C38BDF8),
                                        Color(0x00000000)
                                    ),
                                    center = Offset(this.size.width * 0.8f, this.size.height * 0.2f),
                                    radius = this.size.width * 0.8f
                                )
                            )
                        }
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // Month Navigation Dashboard Node
                    item {
                        MonthNavigatorWidget(
                            curMonth = curMonth,
                            isClosed = currentMonthData.isClosed,
                            defaultOff = state.defaultOff,
                            onPrev = { viewModel.navigatePrevMonth(context) },
                            onNext = { viewModel.navigateNextMonth(context) },
                            onToday = {
                                viewModel.navigateToday(context) {
                                    scope.launch {
                                        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        val idx = currentMonthData.rows.indexOfFirst { it.date == todayStr }
                                        if (idx != -1) {
                                            listState.animateScrollToItem(idx + 3)
                                        }
                                    }
                                }
                            },
                            onToggleLock = { viewModel.toggleMonthLock(context) },
                            onToggleDefaultOff = { viewModel.toggleDefaultOff(context) },
                            isTodayMonth = viewModel.isTodayMonth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Defaults Panels and Global Core Configs
                    item {
                        DefaultsAndFinanceWidget(
                            defaults = state.defaults,
                            rateValue = state.rate,
                            globalBonus = currentMonthData.globalBonus ?: "",
                            globalDeduction = currentMonthData.globalDeduction ?: "",
                            isMonthClosed = currentMonthData.isClosed,
                            onDefaultsChanged = { s, e, b -> viewModel.updateDefaults(s, e, b, context) },
                            onRateChanged = { r -> viewModel.updateRate(r, context) },
                            onBonusLossChanged = { b, l -> viewModel.updateGlobalMonthlyValues(b, l, context) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Work Logs Render
                    if (currentMonthData.rows.isEmpty()) {
                        item {
                            EmptyLogsWidget(
                                isClosed = currentMonthData.isClosed,
                                onAddRow = { viewModel.addSingleRow(context) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        if (isMobile) {
                            itemsIndexed(currentMonthData.rows, key = { _, r -> r.id }) { index, row ->
                                WorkRowMobileCard(
                                    row = row,
                                    rate = state.rate,
                                    isMonthClosed = currentMonthData.isClosed,
                                    confirmDeleteId = confirmDeleteRowId,
                                    onFieldChange = { field, newVal ->
                                        viewModel.handleInputChange(row.id, field, newVal, context)
                                    },
                                    onOffToggle = { off ->
                                        viewModel.handleOffToggled(row.id, off, context)
                                    },
                                    onDeleteRequested = {
                                        viewModel.requestDeleteRow(row.id)
                                    },
                                    onDeleteConfirmed = {
                                        viewModel.confirmDeleteRow(row.id, context)
                                    },
                                    onDeleteCancelled = {
                                        viewModel.requestDeleteRow(null)
                                    },
                                    isSelected = selectedRows.contains(row.id),
                                    onSelectToggled = { viewModel.toggleSelectRow(row.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            item {
                                WorkLogsTableWidget(
                                    rows = currentMonthData.rows,
                                    rate = state.rate,
                                    selectedRows = selectedRows,
                                    confirmDeleteId = confirmDeleteRowId,
                                    isMonthClosed = currentMonthData.isClosed,
                                    onSelectToggled = { viewModel.toggleSelectRow(it) },
                                    onSelectAll = { viewModel.selectAllRows() },
                                    onDeselectAll = { viewModel.deselectAllRows() },
                                    onFieldChange = { id, field, newVal ->
                                        viewModel.handleInputChange(id, field, newVal, context)
                                    },
                                    onOffToggle = { id, off ->
                                        viewModel.handleOffToggled(id, off, context)
                                    },
                                    onDeleteRequested = { viewModel.requestDeleteRow(it) },
                                    onDeleteConfirmed = { viewModel.confirmDeleteRow(it, context) },
                                    onDeleteCancelled = { viewModel.requestDeleteRow(null) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    // Bulk Check action bar
                    if (selectedRows.isNotEmpty()) {
                        item {
                            BulkActionBarWidget(
                                selectedCount = selectedRows.size,
                                onMarkOff = { viewModel.bulkMarkOff(context) },
                                onDelete = { viewModel.bulkDelete(context) },
                                onNewCompanyStart = {
                                    viewModel.triggerNewCompanyStart(context) {
                                        scope.launch { drawerState.open() }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Total Summaries & Deductions Dashboard Node
                    val finances = viewModel.getMonthlyFinances()
                    item {
                        MonthlyFinancesSummary(
                            finances = finances,
                            deductions = currentMonthData.deductions,
                            isMonthClosed = currentMonthData.isClosed,
                            onAddDeduction = { viewModel.addDeduction(context) },
                            onUpdateDeduction = { dedId, amt, note ->
                                viewModel.updateDeduction(dedId, amt, note, context)
                            },
                            onDeleteDeduction = { dedId ->
                                viewModel.deleteDeduction(dedId, context)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Built-in expandable Calculator Card Widget
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { calcExpanded = !calcExpanded }
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = "Calc", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Embedded Quick Calculator",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        if (calcExpanded) Icons.Default.Close else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand"
                                    )
                                }

                                if (calcExpanded) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CalculatorPanel(
                                        expressionInput = calculatorInput,
                                        onInputChange = { calculatorInput = it },
                                        onCalculatePressed = {
                                            calculatorInput = SimpleMathParser.calculate(calculatorInput)
                                        },
                                        onClear = { calculatorInput = "0" }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Tasks Kanban Board Node (drag and drop capability)
                    item {
                        TasksKanbanBoardWidget(
                            notes = state.notes,
                            netPay = finances.netPay,
                            onAddTask = { viewModel.addTask(it, context) },
                            onPriceToggled = { id, enabled -> viewModel.toggleTaskPriceEnabled(id, enabled, context) },
                            onPriceChanged = { id, price -> viewModel.updateTaskPrice(id, price, context) },
                            onStatusChanged = { id, stat -> viewModel.updateTaskStatus(id, stat, context) },
                            onDeleteTask = { id -> viewModel.deleteTask(id, context) },
                            onReordered = { newList -> viewModel.reorderTasks(newList, context) }
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                ConfettiCanvas(
                    trigger = triggerConfetti,
                    onAnimationEnd = { triggerConfetti = false }
                )
            }
        }
    }
}

fun shareJsonBackup(context: Context, jsonText: String) {
    try {
        val cacheFile = File(context.cacheDir, "worklog_data.json")
        cacheFile.writeText(jsonText)

        val authority = "com.aistudio.workdevforce.whlzpk.fileprovider"
        val uri: Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WorkDevForce Log Backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Save Backup Via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun triggerNativePrint(context: Context, rows: List<WorkRow>, title: String, periodLabel: String) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val todayLabel = LocalDate.now().atTime(java.time.LocalTime.now()).format(formatter)

    val html = generatePdfHtml(rows, title, periodLabel, todayLabel)

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = title.replace("/", "-")
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val printAttributes = android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

fun generatePdfHtml(rows: List<WorkRow>, title: String, periodLabel: String, todayLabel: String): String {
    val tableRowsHtml = StringBuilder()
    var totalHours = 0.0

    for (row in rows) {
        val calculated = LocalStore.calcHours(row)
        totalHours += calculated

        val localDate = try { LocalDate.parse(row.date) } catch (e: Exception) { null }
        val weekday = localDate?.let { LocalStore.getDayOfWeekIndex(it) } ?: -1

        val formattedDate = try {
            LocalDate.parse(row.date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            row.date
        }

        if (weekday == 0) {
            tableRowsHtml.append("""
                <tr class="sunday-row" style="background-color: #f1f5f9;">
                    <td>${row.dayName}</td>
                    <td>$formattedDate</td>
                    <td colspan="4" class="span-off" style="font-style: italic; color: #64748b;">Sunday - Off</td>
                </tr>
            """.trimIndent())
        } else if (row.off) {
            tableRowsHtml.append("""
                <tr class="off-row" style="background-color: #f8fafc;">
                    <td>${row.dayName}</td>
                    <td>$formattedDate</td>
                    <td colspan="4" class="span-off" style="font-weight: bold; color: #475569;">NO WORK</td>
                </tr>
            """.trimIndent())
        } else {
            tableRowsHtml.append("""
                <tr>
                    <td>${row.dayName}</td>
                    <td>$formattedDate</td>
                    <td>${row.start}</td>
                    <td>${row.end}</td>
                    <td>${row.breakDuration}m</td>
                    <td class="bold" style="font-weight: bold;">${String.format(Locale.US, "%.2f", calculated)}</td>
                </tr>
            """.trimIndent())
        }
    }

    val totalHoursStr = String.format(Locale.US, "%.2f", totalHours)

    return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    margin: 40px;
                    color: #0f172a;
                    direction: rtl;
                }
                .header {
                    margin-bottom: 25px;
                    border-bottom: 2px solid #0f172a;
                    padding-bottom: 12px;
                }
                .header-meta {
                    display: flex;
                    justify-content: space-between;
                    font-size: 14px;
                    color: #475569;
                    margin-top: 5px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 15px;
                }
                th, td {
                    border: 1px solid #94a3b8;
                    padding: 8px 10px;
                    text-align: center;
                    font-size: 13px;
                }
                th {
                    background-color: #f1f5f9;
                    font-weight: bold;
                }
                .bold { font-weight: bold; }
                .footer {
                    margin-top: 20px;
                    font-size: 18px;
                    font-weight: bold;
                    display: flex;
                    justify-content: flex-end;
                }
                @media print {
                    body { margin: 10mm; }
                }
            </style>
        </head>
        <body>
            <div class="header">
                <h1 style="font-size: 22px; margin: 0;">$title</h1>
                <div class="header-meta">
                    <span><strong>الفترة:</strong> $periodLabel</span>
                    <span style="float: left;"><strong>تاريخ الاستخراج:</strong> $todayLabel</span>
                </div>
            </div>
            
            <table>
                <thead>
                    <tr>
                        <th style="width: 18%;">اليوم</th>
                        <th style="width: 22%;">التاريخ</th>
                        <th style="width: 15%;">الدخول</th>
                        <th style="width: 15%;">الخروج</th>
                        <th style="width: 15%;">الاستراحة</th>
                        <th style="width: 15%;">مجموع الساعات</th>
                    </tr>
                </thead>
                <tbody>
                    $tableRowsHtml
                </tbody>
            </table>
            
            <div class="footer">
                TOTAL WORK HOURS: <span style="font-size: 22px; margin-right: 15px; border-bottom: 2px solid #000;">$totalHoursStr</span>
            </div>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun ProfileSettingsDrawer(
    viewModel: WorkViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.appState.collectAsStateWithLifecycle()
    val profile = state.profile

    var showWipeConfirmFirst by remember { mutableStateOf(false) }
    var showWipeConfirmSecond by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "My Staff Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = profile.name,
            onValueChange = {
                viewModel.updateProfile(it, profile.companyId, profile.companyName, profile.startDate, context)
            },
            label = { Text("My Full Name") },
            placeholder = { Text("E.g. समीर (Samir)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.companyName,
            onValueChange = {
                viewModel.updateProfile(profile.name, profile.companyId, it, profile.startDate, context)
            },
            label = { Text("Company Name") },
            placeholder = { Text("E.g. Tech Corp") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.companyId,
            onValueChange = {
                viewModel.updateProfile(profile.name, it, profile.companyName, profile.startDate, context)
            },
            label = { Text("Company ID / My Number") },
            placeholder = { Text("E.g. EMP-12345") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.startDate,
            onValueChange = {
                viewModel.updateProfile(profile.name, profile.companyId, profile.companyName, it, context)
            },
            label = { Text("Employment Start Date") },
            placeholder = { Text("yyyy-MM-dd") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = { onClose() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Close")
        }
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showWipeConfirmFirst = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clean Delete Data")
        }

        if (showWipeConfirmFirst) {
            AlertDialog(
                onDismissRequest = { showWipeConfirmFirst = false },
                title = { Text("Wipe All Data?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to COMPLETELY WIPE ALL DATA? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showWipeConfirmFirst = false
                            showWipeConfirmSecond = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirmFirst = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showWipeConfirmSecond) {
            AlertDialog(
                onDismissRequest = { showWipeConfirmSecond = false },
                title = { Text("Double Checking!", fontWeight = FontWeight.Bold) },
                text = { Text("Double checking: Delete EVERYTHING?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.cleanWipeData(context)
                            showWipeConfirmSecond = false
                            Toast.makeText(context, "All local data wiped.", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete EVERYTHING")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirmSecond = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PdfReportScreen(viewModel: WorkViewModel) {
    val context = LocalContext.current
    val rows = viewModel.getPdfRows()
    val period = viewModel.getPdfPeriodLabel()
    val title = viewModel.getPdfReportTitle()

    Scaffold(
        topBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.closePdfScreen() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PDF Print Layout Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { triggerNativePrint(context, rows, title, period) }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الفترة: $period", fontSize = 12.sp, color = Color(0xFF475569))
                        Text("معاينة الطباعة", fontSize = 12.sp, color = Color(0xFF94A3B8), fontStyle = FontStyle.Italic)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(16.dp))

                    rows.forEach { r ->
                        val hours = LocalStore.calcHours(r)
                        val isSun = try { LocalDate.parse(r.date).dayOfWeek == java.time.DayOfWeek.SUNDAY } catch(e:Exception){false}
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${r.dayName} (${r.date})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSun) {
                                Text(
                                    "Sunday - Off",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            } else if (r.off) {
                                Text(
                                    "NO WORK",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            } else {
                                Text(
                                    "${r.start} - ${r.end} (${r.breakDuration}m)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "${String.format("%.2f", hours)} hrs",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    val tot = rows.sumOf { LocalStore.calcHours(it) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "TOTAL HOURS: ${String.format("%.2f", tot)} hrs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthNavigatorWidget(
    curMonth: String,
    isClosed: Boolean,
    defaultOff: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleDefaultOff: () -> Unit,
    isTodayMonth: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev Month")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = curMonth,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onToday,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isTodayMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Jump to Today")
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isClosed,
                        onCheckedChange = { onToggleLock() }
                    )
                    Text("Lock Month", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = defaultOff,
                        onCheckedChange = { onToggleDefaultOff() }
                    )
                    Text("Default Off Day", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DefaultsAndFinanceWidget(
    defaults: Defaults,
    rateValue: Double,
    globalBonus: String,
    globalDeduction: String,
    isMonthClosed: Boolean,
    onDefaultsChanged: (String, String, String) -> Unit,
    onRateChanged: (Double) -> Unit,
    onBonusLossChanged: (String, String) -> Unit
) {
    var defStart by remember(defaults) { mutableStateOf(defaults.start) }
    var defEnd by remember(defaults) { mutableStateOf(defaults.end) }
    var defBreak by remember(defaults) { mutableStateOf(defaults.brk) }

    var localRate by remember(rateValue) { mutableStateOf(rateValue.toString()) }
    var localBonus by remember(globalBonus) { mutableStateOf(globalBonus) }
    var localLoss by remember(globalDeduction) { mutableStateOf(globalDeduction) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Defaults for New Days (Settings)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = defStart,
                        onValueChange = {
                            defStart = it
                            onDefaultsChanged(it, defEnd, defBreak)
                        },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("06:30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = defEnd,
                        onValueChange = {
                            defEnd = it
                            onDefaultsChanged(defStart, it, defBreak)
                        },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("15:30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = defBreak,
                        onValueChange = {
                            defBreak = it
                            onDefaultsChanged(defStart, defEnd, it)
                        },
                        label = { Text("Break (m)") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        placeholder = { Text("-30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Monthly Finances (MAD)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = localRate,
                        onValueChange = {
                            localRate = it
                            it.toDoubleOrNull()?.let { r -> onRateChanged(r) }
                        },
                        label = { Text("MAD/hr (Rate)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = localBonus,
                        onValueChange = {
                            localBonus = it
                            onBonusLossChanged(it, localLoss)
                        },
                        label = { Text("Bonus (+)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isMonthClosed,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = localLoss,
                        onValueChange = {
                            localLoss = it
                            onBonusLossChanged(localBonus, it)
                        },
                        label = { Text("Loss (-)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isMonthClosed,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLogsWidget(
    isClosed: Boolean,
    onAddRow: () -> Unit
) {
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No logs logged for this month yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Click below or use the radial Floating Action Button menu in the corners to start adding days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddRow,
                enabled = !isClosed
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log First Day")
            }
        }
    }
}

@Composable
fun WorkRowMobileCard(
    row: WorkRow,
    rate: Double,
    isMonthClosed: Boolean,
    confirmDeleteId: String?,
    onFieldChange: (String, String) -> Unit,
    onOffToggle: (Boolean) -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    isSelected: Boolean,
    onSelectToggled: () -> Unit
) {
    val isSunday = try { LocalDate.parse(row.date).dayOfWeek == java.time.DayOfWeek.SUNDAY } catch(e:Exception){false}
    val cardColor = when {
        isSunday -> Color(0xFF1E293B).copy(alpha = 0.5f)
        row.off -> Color(0xFF1E293B).copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }

    val isConfirmingDelete = confirmDeleteId == row.id

    val isFuture = try {
        LocalDate.parse(row.date).isAfter(LocalDate.now())
    } catch(e: Exception) { false }

    val disableInputs = row.off || isSunday || isMonthClosed
    val disableDateNotes = isFuture || isMonthClosed

    val cardBorder = if (row.isNewCompanyStart) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    } else {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    }

    Card(
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggled() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${row.dayName} (${row.date})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (row.isNewCompanyStart) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                    if (row.isNewCompanyStart) {
                        Text(
                            "NEW COMPANY START",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!isMonthClosed) {
                    AnimatedContent(targetState = isConfirmingDelete, label = "del_anim") { c ->
                        if (c) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sure?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                TextButton(onClick = onDeleteConfirmed, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                    Text("Yes")
                                }
                                TextButton(onClick = onDeleteCancelled) {
                                    Text("No")
                                }
                            }
                        } else {
                            IconButton(onClick = onDeleteRequested) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Row", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = row.off,
                        onCheckedChange = { onOffToggle(it) },
                        enabled = !isSunday && !isFuture && !isMonthClosed
                    )
                    Text("Day Off", style = MaterialTheme.typography.bodyMedium)
                }

                val hrs = LocalStore.calcHours(row)
                val earnings = hrs * rate

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", hrs)} hrs",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", earnings)} MAD",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = row.start,
                    onValueChange = { onFieldChange("start", it) },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !disableInputs,
                    placeholder = { Text("06:30") }
                )
                OutlinedTextField(
                    value = row.end,
                    onValueChange = { onFieldChange("end", it) },
                    label = { Text("End") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !disableInputs,
                    placeholder = { Text("15:30") }
                )
                OutlinedTextField(
                    value = row.breakDuration,
                    onValueChange = { onFieldChange("break", it) },
                    label = { Text("Break") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    enabled = !disableInputs,
                    placeholder = { Text("-30") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = row.note,
                onValueChange = { onFieldChange("note", it) },
                label = { Text("Note / Memo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !disableDateNotes,
                placeholder = { Text("E.g. Overtime details") }
            )
        }
    }
}

@Composable
fun WorkLogsTableWidget(
    rows: List<WorkRow>,
    rate: Double,
    selectedRows: Set<String>,
    confirmDeleteId: String?,
    isMonthClosed: Boolean,
    onSelectToggled: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onFieldChange: (String, String, String) -> Unit,
    onOffToggle: (String, Boolean) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onDeleteConfirmed: (String) -> Unit,
    onDeleteCancelled: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Timesheet Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    TextButton(onClick = onSelectAll) { Text("Select All") }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onDeselectAll) { Text("Deselect All") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                rows.forEach { r ->
                    val isSunday = try { LocalDate.parse(r.date).dayOfWeek == java.time.DayOfWeek.SUNDAY } catch(e:Exception){false}
                    val isConfirmingDelete = confirmDeleteId == r.id
                    val isFuture = try {
                        LocalDate.parse(r.date).isAfter(LocalDate.now())
                    } catch(e: Exception) { false }

                    val disableInputs = r.off || isSunday || isMonthClosed
                    val disableDateNotes = isFuture || isMonthClosed

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSunday) Color(0xFF1E293B).copy(alpha = 0.4f)
                                else if (r.off) Color(0xFF1E293B).copy(alpha = 0.6f)
                                else Color.Transparent
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedRows.contains(r.id),
                            onCheckedChange = { onSelectToggled(r.id) }
                        )

                        Text(
                            text = "${r.dayName} ${r.date.substring(5)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(90.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Checkbox(
                            checked = r.off,
                            onCheckedChange = { onOffToggle(r.id, it) },
                            enabled = !isSunday && !isFuture && !isMonthClosed,
                            modifier = Modifier.width(36.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedTextField(
                            value = r.start,
                            onValueChange = { onFieldChange(r.id, "start", it) },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            enabled = !disableInputs,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = r.end,
                            onValueChange = { onFieldChange(r.id, "end", it) },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            enabled = !disableInputs,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = r.breakDuration,
                            onValueChange = { onFieldChange(r.id, "break", it) },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            enabled = !disableInputs,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = r.note,
                            onValueChange = { onFieldChange(r.id, "note", it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !disableDateNotes,
                            textStyle = MaterialTheme.typography.bodySmall,
                            placeholder = { Text("Memos", fontSize = 10.sp) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val hrs = LocalStore.calcHours(r)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", hrs)}h",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(45.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!isMonthClosed) {
                            AnimatedContent(targetState = isConfirmingDelete, label = "del_anim") { c ->
                                if (c) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { onDeleteConfirmed(r.id) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                            Text("Sure?", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = onDeleteCancelled) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                } else {
                                    IconButton(onClick = { onDeleteRequested(r.id) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete Row", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun BulkActionBarWidget(
    selectedCount: Int,
    onMarkOff: () -> Unit,
    onDelete: () -> Unit,
    onNewCompanyStart: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$selectedCount row(s) selected",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row {
                    if (selectedCount == 1) {
                        Button(
                            onClick = onNewCompanyStart,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text("New Company Start", fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = onMarkOff,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text("Mark Off", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Days", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete the selected days?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MonthlyFinancesSummary(
    finances: WorkViewModel.MonthlyFinances,
    deductions: List<DeductionItem>,
    isMonthClosed: Boolean,
    onAddDeduction: () -> Unit,
    onUpdateDeduction: (String, String, String) -> Unit,
    onDeleteDeduction: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Financial Overviews",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TOTAL HOURS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("${String.format(Locale.US, "%.2f", finances.totalHours)} hrs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text("HOURLY PAY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("${String.format(Locale.US, "%.2f", finances.totalPay)} MAD", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                }
                Column {
                    Text("DEDUCTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("${String.format(Locale.US, "%.2f", finances.totalDeductions)} MAD", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Text("NET EARNINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))

            val netPayStr = String.format(Locale.US, "%.2f", finances.netPay)
            Row(verticalAlignment = Alignment.Bottom) {
                netPayStr.forEachIndexed { index, char ->
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                        },
                        label = "net_pay_char_$index"
                    ) { activeChar ->
                        Text(
                            text = activeChar.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MAD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Salary Deductions & Loans advances",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Button(
                    onClick = onAddDeduction,
                    enabled = !isMonthClosed,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (deductions.isEmpty()) {
                Text(
                    "No loans or deductions reported this month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                deductions.forEach { ded ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ded.amount,
                            onValueChange = { onUpdateDeduction(ded.id, it, ded.note) },
                            placeholder = { Text("0 dh") },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            enabled = !isMonthClosed,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = ded.note,
                            onValueChange = { onUpdateDeduction(ded.id, ded.amount, it) },
                            placeholder = { Text("Advance repayment detail") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isMonthClosed
                        )
                        if (!isMonthClosed) {
                            IconButton(onClick = { onDeleteDeduction(ded.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Deduction", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorPanel(
    expressionInput: String,
    onInputChange: (String) -> Unit,
    onCalculatePressed: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = expressionInput,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val buttons = listOf(
                listOf("C", "(", ")", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { char ->
                        val btnWeight = if (char == "0") 2f else 1f
                        Button(
                            onClick = {
                                when (char) {
                                    "C" -> onClear()
                                    "=" -> onCalculatePressed()
                                    else -> {
                                        val newInput = if (expressionInput == "0" && char != ".") char else expressionInput + char
                                        onInputChange(newInput)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (char) {
                                    "C" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    "=", "÷", "×", "−", "+" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                contentColor = when (char) {
                                    "C" -> MaterialTheme.colorScheme.error
                                    "=", "÷", "×", "−", "+" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(btnWeight)
                                .height(48.dp)
                        ) {
                            Text(char, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksKanbanBoardWidget(
    notes: List<TaskItem>,
    netPay: Double,
    onAddTask: (String) -> Unit,
    onPriceToggled: (String, Boolean) -> Unit,
    onPriceChanged: (String, String) -> Unit,
    onStatusChanged: (String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onReordered: (List<TaskItem>) -> Unit
) {
    var taskInputText by remember { mutableStateOf("") }

    val todoTasks = notes.filter { it.status == "todo" }
    val doneTasks = notes.filter { it.status == "done" }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Workspace Kanban Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = taskInputText,
                        onValueChange = { taskInputText = it },
                        placeholder = { Text("Write a task... (Enter to save)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (taskInputText.isNotBlank()) {
                                onAddTask(taskInputText)
                                taskInputText = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (taskInputText.isNotBlank()) {
                                onAddTask(taskInputText)
                                taskInputText = ""
                            }
                        },
                        enabled = taskInputText.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val isMobile = LocalConfiguration.current.screenWidthDp <= 768
        if (isMobile) {
            Column(modifier = Modifier.fillMaxWidth()) {
                KanbanColumn(
                    title = "To Do",
                    tasks = todoTasks,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    onStatusChanged = onStatusChanged,
                    onPriceToggled = onPriceToggled,
                    onPriceChanged = onPriceChanged,
                    onDeleteTask = onDeleteTask,
                    netPay = netPay,
                    oppositeStatus = "done"
                )
                Spacer(modifier = Modifier.height(16.dp))
                KanbanColumn(
                    title = "Done",
                    tasks = doneTasks,
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    onStatusChanged = onStatusChanged,
                    onPriceToggled = onPriceToggled,
                    onPriceChanged = onPriceChanged,
                    onDeleteTask = onDeleteTask,
                    netPay = netPay,
                    oppositeStatus = "todo"
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    KanbanColumn(
                        title = "To Do",
                        tasks = todoTasks,
                        badgeColor = MaterialTheme.colorScheme.primary,
                        onStatusChanged = onStatusChanged,
                        onPriceToggled = onPriceToggled,
                        onPriceChanged = onPriceChanged,
                        onDeleteTask = onDeleteTask,
                        netPay = netPay,
                        oppositeStatus = "done"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    KanbanColumn(
                        title = "Done",
                        tasks = doneTasks,
                        badgeColor = MaterialTheme.colorScheme.tertiary,
                        onStatusChanged = onStatusChanged,
                        onPriceToggled = onPriceToggled,
                        onPriceChanged = onPriceChanged,
                        onDeleteTask = onDeleteTask,
                        netPay = netPay,
                        oppositeStatus = "todo"
                    )
                }
            }
        }
    }
}

@Composable
fun KanbanColumn(
    title: String,
    tasks: List<TaskItem>,
    badgeColor: Color,
    oppositeStatus: String,
    onStatusChanged: (String, String) -> Unit,
    onPriceToggled: (String, Boolean) -> Unit,
    onPriceChanged: (String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    netPay: Double
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeColor),
                    shape = CircleShape
                ) {
                    Text(
                        text = tasks.size.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                tasks.forEach { item ->
                    KanbanTaskCard(
                        item = item,
                        oppositeStatus = oppositeStatus,
                        onStatusChanged = onStatusChanged,
                        onPriceToggled = onPriceToggled,
                        onPriceChanged = onPriceChanged,
                        onDeleteTask = onDeleteTask,
                        netPay = netPay
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun KanbanTaskCard(
    item: TaskItem,
    oppositeStatus: String,
    onStatusChanged: (String, String) -> Unit,
    onPriceToggled: (String, Boolean) -> Unit,
    onPriceChanged: (String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    netPay: Double
) {
    var offsetX by remember { mutableStateOf(0f) }
    var scaleFactor by remember { mutableStateOf(1f) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { scaleFactor = 1.05f },
                    onDragEnd = {
                        scaleFactor = 1f
                        if (offsetX > 250f || offsetX < -250f) {
                            onStatusChanged(item.id, oppositeStatus)
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        scaleFactor = 1f
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Swipe to move column",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.text,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onPriceToggled(item.id, !item.hasPrice) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (item.hasPrice) Icons.Default.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = "Toggle Pricing",
                                tint = if (item.hasPrice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("Has Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { onStatusChanged(item.id, oppositeStatus) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(if (oppositeStatus == "done") "Complete" else "To Do", fontSize = 11.sp)
                            Icon(
                                if (oppositeStatus == "done") Icons.Default.CheckCircle else Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).padding(start = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onDeleteTask(item.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove task",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (item.hasPrice) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = item.price,
                        onValueChange = { onPriceChanged(item.id, it) },
                        placeholder = { Text("Price (dh)", fontSize = 11.sp) },
                        modifier = Modifier
                            .width(110.dp)
                            .height(44.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    val itemPrice = item.price.toDoubleOrNull() ?: 0.0
                    val remaining = netPay - itemPrice
                    val isGreen = remaining >= 0

                    Column {
                        Text("REMAINING NET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontSize = 9.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", remaining)} MAD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isGreen) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RadialFabMenu(
    isOpen: Boolean,
    onToggle: () -> Unit,
    onAction: (String) -> Unit,
    isMonthClosed: Boolean
) {
    val rotation by animateFloatAsState(targetValue = if (isOpen) 45f else 0f, label = "rot")

    Box(contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + expandIn(expandFrom = Alignment.BottomEnd),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .width(200.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { onAction("export_json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("JSON Backup", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(
                        onClick = { onAction("export_pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("PDF Print Log", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(
                        onClick = { onAction("import") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Import Backup", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(
                        onClick = { onAction("bulk_add") },
                        enabled = !isMonthClosed,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Bulk Add Days", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(
                        onClick = { onAction("add_row") },
                        enabled = !isMonthClosed,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Log Day", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(
                        onClick = { onAction("tasks") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tasks List", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Toggle Menu actions",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}
