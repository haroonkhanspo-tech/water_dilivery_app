package com.example.canetracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun GlassBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                Color(0xFFE3F2FD),
                Color(0xFFF1F8E9),
                Color(0xFFFFF3E0),
                Color(0xFFF3E5F5)
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .blur(80.dp)
        )
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1565C0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CLASSIFIED",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: WaterDeliveryViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Modern Back Navigation: Return to Home instead of exiting
    BackHandler(enabled = viewModel.currentScreen != AppScreen.HOME) {
        viewModel.navigateTo(AppScreen.HOME)
    }
    
    var showFileSelectionDialog by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var fileToDownload by remember { mutableStateOf<String?>(null) }
    var showEntryDialog by remember { mutableStateOf(false) }
    
    var fileToDelete by remember { mutableStateOf<Pair<String, String>?>(null) } // uri to fileName
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMonthlySheet by remember { mutableStateOf(false) }

    var showProfileDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            selectedUris.take(2).forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: Exception) { }
            }
            viewModel.uploadFiles(selectedUris.take(2).map { it.toString() })
        }
    }

    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { destinationUri ->
            fileToDownload?.let { sourceUri ->
                viewModel.performDownload(sourceUri, destinationUri)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "CLASSIFIED",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                
                // --- THEME SELECTOR ---
                Text(
                    "Appearance",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        text = "System",
                        selected = viewModel.themeMode == 0,
                        onClick = { viewModel.updateThemeMode(0) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        text = "Light",
                        selected = viewModel.themeMode == 1,
                        onClick = { viewModel.updateThemeMode(1) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        text = "Dark",
                        selected = viewModel.themeMode == 2,
                        onClick = { viewModel.updateThemeMode(2) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // --- NAVIGATION ITEMS ---
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Home & History") },
                    selected = viewModel.currentScreen == AppScreen.HOME,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.HOME)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PersonSearch, contentDescription = null) },
                    label = { Text("Customer Profiles") },
                    selected = viewModel.currentScreen == AppScreen.CUSTOMER_PROFILES,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.CUSTOMER_PROFILES)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("View History") },
                    selected = viewModel.currentScreen == AppScreen.HISTORY,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(AppScreen.HISTORY)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    label = { Text("Upload Excel Sheets") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel"))
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    label = { Text("Download Excel Sheet") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (viewModel.files.size == 1) {
                            fileToDownload = viewModel.files[0]
                            downloadLauncher.launch(getUriFileName(context, Uri.parse(fileToDownload!!)))
                        } else if (viewModel.files.size > 1) {
                            showFileSelectionDialog = true
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = { Text("Reset Data") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showResetConfirmation = true
                    }
                )

                if (viewModel.files.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Text(
                        "Active Workbooks",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    viewModel.files.forEach { uriString ->
                        val fileName = getUriFileName(context, Uri.parse(uriString))
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF1565C0)) },
                            label = { 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(fileName, maxLines = 1, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { 
                                            fileToDelete = uriString to fileName
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            selected = false,
                            onClick = {}
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (viewModel.currentScreen) {
                                AppScreen.HOME -> "CLASSIFIED"
                                AppScreen.CUSTOMER_PROFILES -> "Customer Profiles"
                                AppScreen.HISTORY -> "Delivery History"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        UserProfileIcon(
                            viewModel = viewModel,
                            onClick = { showProfileDialog = true }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                GlassBackground()
                
                when (viewModel.currentScreen) {
                    AppScreen.HOME -> {
                        Column {
                            if (viewModel.hasInaccessibleFiles) {
                                InaccessibleFilesBanner { viewModel.dismissFileAlert() }
                            }
                            
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                item {
                                    InlineSearchBar(viewModel) { showEntryDialog = true }
                                }

                                item {
                                    Text(
                                        "Recent Activity",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Show recent entries grouped by date
                                val recentHistory = viewModel.history.take(15)
                                val groupedRecent = recentHistory.groupBy { it.fullDate }

                                if (recentHistory.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                            Text("No recent activity.", color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                } else {
                                    groupedRecent.forEach { (date, entries) ->
                                        stickyHeader {
                                            DateSeparator(date, viewModel.getDailyTotal(date))
                                        }
                                        items(entries) { entry ->
                                            ChatBubbleEntry(entry) { viewModel.deleteEntry(entry) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    AppScreen.CUSTOMER_PROFILES -> {
                        CustomerProfilesScreen(viewModel)
                    }
                    AppScreen.HISTORY -> {
                        HistoryScreen(viewModel)
                    }
                }
                
                if (viewModel.isRestoringData) {
                    RestorationOverlay()
                }
            }
        }
    }

    if (showEntryDialog && viewModel.selectedCustomer != null) {
        FastEntryDialog(
            customer = viewModel.selectedCustomer!!,
            summary = viewModel.customerSummary,
            onDismiss = { 
                showEntryDialog = false
                viewModel.clearSelection() 
            },
            onSave = { received, returned, note ->
                val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                viewModel.addEntry(viewModel.selectedCustomer!!, day, received, returned, note)
                showEntryDialog = false
                viewModel.clearSelection()
            }
        )
    }

    if (showFileSelectionDialog) {
        FileSelectionDialog(
            files = viewModel.files,
            context = context,
            onDismiss = { showFileSelectionDialog = false },
            onFileSelected = { file ->
                fileToDownload = file
                showFileSelectionDialog = false
                downloadLauncher.launch(getUriFileName(context, Uri.parse(file)))
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Confirm Reset") },
            text = { Text("Are you sure you want to reset? This may remove the current working data.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    showResetConfirmation = false
                }) { Text("Reset", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showMonthlySheet && viewModel.selectedMonthlyCustomer != null) {
        MonthlyRecordSheet(
            viewModel = viewModel,
            sheetState = sheetState,
            onDismiss = { 
                showMonthlySheet = false
                viewModel.clearMonthlySelection() 
            }
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false }
        )
    }

    fileToDelete?.let { (uri, name) ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Workbook?") },
            text = { Text("Are you sure you want to delete '$name'? This will remove all associated records from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFile(uri, name)
                    fileToDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }
}



@Composable
fun InaccessibleFilesBanner(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Permission Lost",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Restored Excel files cannot be accessed. Please re-upload them to fix search suggestions.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RestorationOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Restoring your records...", fontWeight = FontWeight.Bold)
                Text(
                    "Downloading data from your Google account",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun DateSeparator(date: String, dailyTotal: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Total Deliveries: $dailyTotal",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ChatBubbleEntry(entry: HistoryEntry, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    shadowElevation = 1.dp.toPx()
                    shape = RoundedCornerShape(24.dp)
                    clip = true
                },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.customer,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingBag, "", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Delivered: ${entry.received}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.AutoMirrored.Filled.Undo, "", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Returned: ${entry.returned}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Balance: ${entry.balance.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (entry.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = entry.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 16.sp
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun InlineSearchBar(viewModel: WaterDeliveryViewModel, onSelect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    shadowElevation = 2.dp.toPx()
                    shape = RoundedCornerShape(28.dp)
                    clip = true
                },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { 
                    Text(
                        "Search customer...", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    ) 
                },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (viewModel.isLoadingCustomers) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                singleLine = true
            )
        }

        // Suggestions
        if (viewModel.searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(20.dp)
                        clip = true
                    },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                if (viewModel.filteredCustomers.isEmpty() && !viewModel.isLoadingCustomers) {
                    Text(
                        text = "No customers found matching \"${viewModel.searchQuery}\"",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(viewModel.filteredCustomers) { customer ->
                            Text(
                                text = customer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCustomer(customer)
                                        onSelect()
                                    }
                                    .padding(16.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FastEntryDialog(
    customer: String,
    summary: CustomerSummary?,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String) -> Unit
) {
    var received by remember { mutableStateOf("") }
    var returned by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(customer, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                summary?.let {
                    Text("Current Balance: ${it.currentBalance.toInt()}", fontSize = 14.sp, color = Color(0xFF2E7D32))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = received,
                        onValueChange = { if (it.all { char -> char.isDigit() }) received = it },
                        label = { Text("Delivered") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = returned,
                        onValueChange = { if (it.all { char -> char.isDigit() }) returned = it },
                        label = { Text("Returned") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(received.toIntOrNull() ?: 0, returned.toIntOrNull() ?: 0, note) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE ENTRY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun FileSelectionDialog(files: List<String>, context: Context, onDismiss: () -> Unit, onFileSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select File to Download") },
        text = {
            LazyColumn {
                items(files) { file ->
                    TextButton(
                        onClick = { onFileSelected(file) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = getUriFileName(context, Uri.parse(file)), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: WaterDeliveryViewModel) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Full Activity Archive",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )
            if (viewModel.history.isNotEmpty()) {
                TextButton(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CLEAR ALL")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (viewModel.history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Your delivery history is empty.", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                viewModel.groupedHistory.forEach { (date, entries) ->
                    stickyHeader {
                        DateSeparator(date, viewModel.getDailyTotal(date))
                    }
                    items(entries) { entry ->
                        ChatBubbleEntry(entry) { viewModel.deleteEntry(entry) }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently delete every entry in your history. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearConfirm = false
                }) { Text("CLEAR ALL", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun CustomerProfilesScreen(viewModel: WaterDeliveryViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    shadowElevation = 2.dp.toPx()
                    shape = RoundedCornerShape(28.dp)
                    clip = true
                },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            OutlinedTextField(
                value = viewModel.menuSearchQuery,
                onValueChange = { viewModel.updateMenuSearchQuery(it) },
                placeholder = { 
                    Text(
                        "Search customer for monthly records...", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    ) 
                },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (viewModel.menuSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateMenuSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                singleLine = true
            )
        }

        if (viewModel.menuFilteredCustomers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                LazyColumn {
                    items(viewModel.menuFilteredCustomers) { customer ->
                        Text(
                            text = customer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectMonthlyCustomer(customer) }
                                .padding(16.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        this@Column.AnimatedVisibility(
            visible = viewModel.selectedMonthlyCustomer != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.weight(1f)
        ) {
            MonthlyRecordContent(viewModel)
        }

        if (viewModel.selectedMonthlyCustomer == null && viewModel.menuSearchQuery.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Search for a customer to view their monthly record",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyRecordSheet(
    viewModel: WaterDeliveryViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            MonthlyRecordContent(viewModel, onDismiss)
        }
    }
}

@Composable
fun MonthlyRecordContent(
    viewModel: WaterDeliveryViewModel,
    onClose: (() -> Unit)? = null
) {
    val customer = viewModel.selectedMonthlyCustomer ?: ""
    val records = viewModel.monthlyRecords
    val isLoading = viewModel.isLoadingMonthly
    
    val monthName = remember {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
            .format(Calendar.getInstance().time)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Report for $monthName",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No records found for this month.", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    title = "Delivered",
                    value = viewModel.monthlyTotalReceived.toInt().toString(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    title = "Returned",
                    value = viewModel.monthlyTotalReturned.toInt().toString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    title = "Balance",
                    value = viewModel.monthlyNetBalance.toInt().toString(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Daily Activity Feed",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records) { record ->
                    MonthlyRecordCard(record)
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            Text(value, fontSize = 20.sp, color = contentColor, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MonthlyRecordCard(record: MonthlyRecordEntry) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day Indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.day.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, "", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Delivered: ${record.received.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Icon(Icons.AutoMirrored.Filled.Undo, "", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Returned: ${record.returned.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                if (record.description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = record.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                Text(
                    text = record.balance.toInt().toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun UserProfileIcon(
    viewModel: WaterDeliveryViewModel,
    onClick: () -> Unit
) {
    val user = viewModel.currentUser
    
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (user?.photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = user?.email?.firstOrNull()?.uppercase() ?: "?"
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = initial.toString(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun ProfileDialog(
    viewModel: WaterDeliveryViewModel,
    onDismiss: () -> Unit
) {
    val user = viewModel.currentUser

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserProfileIcon(viewModel = viewModel, onClick = {})
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user?.displayName ?: "Guest User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (user?.email != null) {
                        Text(
                            text = user.email ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                if (viewModel.lastSyncTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudDone, 
                            contentDescription = null, 
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cloud Backup Active", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Last synced: ${viewModel.lastSyncTime}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else if (user != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sync, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing data to cloud...", fontSize = 14.sp)
                    }
                } else {
                    Text(
                        "Sign in to backup your history and tracked workbooks to your Google account.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    viewModel.signOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (user != null) "Sign Out" else "Go to Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ThemeOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

fun getUriFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result ?: "Unknown.xlsx"
}
