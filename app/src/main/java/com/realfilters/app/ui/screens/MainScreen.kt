package com.realfilters.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.realfilters.app.domain.engine.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeViewModel: ThemeViewModel,
    viewModel: FilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    var showPresetSheet by rememberSaveable { mutableStateOf(false) }
    var showLayerSheet by rememberSaveable { mutableStateOf(false) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            showEditor = true
            viewModel.loadImage(it)
        }
    }

    val pickDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            showEditor = true
            viewModel.loadImage(it)
        }
    }

    if (showEditor) {
        EditorContent(
            uiState = uiState,
            viewModel = viewModel,
            themeMode = themeMode,
            themeViewModel = themeViewModel,
            onBack = {
                showEditor = false
                showPresetSheet = false
                showLayerSheet = false
                showThemeSheet = false
            },
            showPresetSheet = showPresetSheet,
            showLayerSheet = showLayerSheet,
            showThemeSheet = showThemeSheet,
            onShowPresetSheet = { showPresetSheet = it },
            onShowLayerSheet = { showLayerSheet = it },
            onShowThemeSheet = { showThemeSheet = it }
        )
    } else {
        HomeContent(
            uiState = uiState,
            viewModel = viewModel,
            onPickImage = { pickImage.launch("image/*") },
            onPickDocument = { pickDocument.launch(arrayOf("image/*")) },
            onEditSaved = { id ->
                viewModel.loadSavedFilter(id)
                showEditor = uiState.originalBitmap != null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: FilterUiState,
    viewModel: FilterViewModel,
    onPickImage: () -> Unit,
    onPickDocument: () -> Unit,
    onEditSaved: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Real Filters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .clickable(onClick = onPickImage),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Image",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Tap to load an image",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "JPEG, PNG, GIF, BMP, WebP, TIFF, HEIF, SVG",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PhotoLibrary,
                    title = "Gallery",
                    subtitle = "Pick from gallery",
                    onClick = onPickImage
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FolderOpen,
                    title = "Browse",
                    subtitle = "Open any file",
                    onClick = onPickDocument
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.savedFilters.isNotEmpty()) {
                Text(
                    "Saved Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.savedFilters) { filter ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditSaved(filter.id) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        filter.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        filter.type.replace("_", " ").uppercase(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSavedFilter(filter.id) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorContent(
    uiState: FilterUiState,
    viewModel: FilterViewModel,
    themeMode: ThemeMode,
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
    showPresetSheet: Boolean,
    showLayerSheet: Boolean,
    showThemeSheet: Boolean,
    onShowPresetSheet: (Boolean) -> Unit,
    onShowLayerSheet: (Boolean) -> Unit,
    onShowThemeSheet: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Filter Editor", fontWeight = FontWeight.Bold)
                        if (uiState.layers.isNotEmpty()) {
                            Text(
                                "${uiState.layers.size} layer${if (uiState.layers.size > 1) "s" else ""} active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onShowThemeSheet(true) }) {
                        Icon(Icons.Default.Palette, "Theme")
                    }
                    IconButton(onClick = { viewModel.showImportDialog() }) {
                        Icon(Icons.Default.FileUpload, "Import")
                    }
                    IconButton(onClick = { viewModel.exportCurrentFilter() }) {
                        Icon(Icons.Default.FileDownload, "Export")
                    }
                    IconButton(onClick = { viewModel.showSaveDialog() }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                    IconButton(onClick = { viewModel.resetFilters() }) {
                        Icon(Icons.Default.Refresh, "Reset")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomAction(
                        icon = Icons.Default.GridOn,
                        label = "Presets",
                        onClick = { onShowPresetSheet(true) }
                    )
                    BottomAction(
                        icon = Icons.Default.Layers,
                        label = "Layers",
                        onClick = { onShowLayerSheet(true) },
                        badge = uiState.layers.size
                    )
                    BottomAction(
                        icon = Icons.Default.Tune,
                        label = "Edit",
                        onClick = {
                            if (uiState.editMode == EditMode.COLOR_MATRIX) {
                                viewModel.showMatrixEditor()
                            } else {
                                viewModel.showKernelEditor()
                            }
                        }
                    )
                    BottomAction(
                        icon = Icons.Default.AddCircle,
                        label = "Add",
                        onClick = { viewModel.addLayer() }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = uiState.processedBitmap?.bitmap

                when {
                    displayBitmap != null -> {
                        val imageBitmap = remember(displayBitmap) {
                            displayBitmap.asImageBitmap()
                        }
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Image preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    uiState.isProcessing -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading image...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        EmptyImagePlaceholder()
                    }
                }

                if (uiState.isProcessing && displayBitmap != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Processing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.editMode == EditMode.COLOR_MATRIX,
                        onClick = { viewModel.setEditMode(EditMode.COLOR_MATRIX) },
                        label = { Text("Color Matrix") },
                        leadingIcon = if (uiState.editMode == EditMode.COLOR_MATRIX) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = uiState.editMode == EditMode.CONVOLUTION,
                        onClick = { viewModel.setEditMode(EditMode.CONVOLUTION) },
                        label = { Text("Convolution") },
                        leadingIcon = if (uiState.editMode == EditMode.CONVOLUTION) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            if (uiState.editMode == EditMode.COLOR_MATRIX) {
                                "Current: ${uiState.currentColorMatrix.name}"
                            } else {
                                "Current: ${uiState.currentKernel?.name ?: "None selected"}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (uiState.editMode == EditMode.COLOR_MATRIX) {
                                uiState.currentColorMatrix.toDisplayString()
                            } else {
                                uiState.currentKernel?.toDisplayString() ?: "Select a kernel from presets"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showPresetSheet) {
        PresetSheet(
            uiState = uiState,
            onSelectMatrix = { viewModel.selectPresetMatrix(it) },
            onSelectKernel = { viewModel.selectPresetKernel(it) },
            onDismiss = { onShowPresetSheet(false) }
        )
    }

    if (showLayerSheet) {
        LayerSheet(
            layers = uiState.layers,
            selectedIndex = uiState.selectedLayerIndex,
            onSelect = { viewModel.selectLayer(it) },
            onToggle = { viewModel.toggleLayer(it) },
            onRemove = { viewModel.removeLayer(it) },
            onMove = { from, to -> viewModel.moveLayer(from, to) },
            onOpacityChange = { index, opacity -> viewModel.updateLayerOpacity(index, opacity) },
            onDismiss = { onShowLayerSheet(false) }
        )
    }

    if (showThemeSheet) {
        ThemeSheet(
            currentMode = themeMode,
            onSelect = { themeViewModel.setThemeMode(it) },
            onDismiss = { onShowThemeSheet(false) }
        )
    }

    if (uiState.showMatrixEditor) {
        MatrixEditorDialog(
            matrix = uiState.currentColorMatrix,
            onUpdate = { viewModel.updateColorMatrix(it) },
            onDismiss = { viewModel.hideMatrixEditor() }
        )
    }

    if (uiState.showKernelEditor) {
        KernelEditorDialog(
            kernel = uiState.currentKernel ?: ConvolutionKernel(
                values = FloatArray(9) { 0f },
                width = 3, height = 3, name = "Custom"
            ),
            onUpdate = { viewModel.updateConvolutionKernel(it) },
            onDismiss = { viewModel.hideKernelEditor() }
        )
    }

    if (uiState.showSaveDialog) {
        SaveFilterDialog(
            name = uiState.filterName,
            onNameChange = { viewModel.updateFilterName(it) },
            onSave = { viewModel.saveFilter(uiState.filterName) },
            onDismiss = { viewModel.hideSaveDialog() }
        )
    }

    if (uiState.showExportDialog) {
        ExportDialog(
            json = uiState.exportJson ?: "",
            onDismiss = { viewModel.hideExportDialog() }
        )
    }

    if (uiState.showImportDialog) {
        ImportDialog(
            json = uiState.importJson,
            onJsonChange = { viewModel.updateImportJson(it) },
            onImport = { viewModel.importFilter(uiState.importJson) },
            onDismiss = { viewModel.hideImportDialog() }
        )
    }
}

@Composable
fun EmptyImagePlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "No image loaded",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Go back and pick an image to start editing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    badge: Int = 0
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        BadgedBox(
            badge = {
                if (badge > 0) {
                    Badge {
                        Text("$badge")
                    }
                }
            }
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSheet(
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            ThemeMode.entries.forEach { mode ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onSelect(mode)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mode == currentMode) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            },
                            contentDescription = null,
                            tint = if (mode == currentMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when (mode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                when (mode) {
                                    ThemeMode.LIGHT -> "Always use light theme"
                                    ThemeMode.DARK -> "Always use dark theme"
                                    ThemeMode.SYSTEM -> "Follow system setting"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (mode == currentMode) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSheet(
    uiState: FilterUiState,
    onSelectMatrix: (ColorMatrix) -> Unit,
    onSelectKernel: (ConvolutionKernel) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Preset Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Color Matrices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.presetColorMatrices) { matrix ->
                    FilterChip(
                        selected = uiState.currentColorMatrix.name == matrix.name,
                        onClick = { onSelectMatrix(matrix) },
                        label = { Text(matrix.name, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Convolution Kernels",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.presetKernels) { kernel ->
                    FilterChip(
                        selected = uiState.currentKernel?.name == kernel.name,
                        onClick = { onSelectKernel(kernel) },
                        label = { Text(kernel.name, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerSheet(
    layers: List<FilterLayer>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onOpacityChange: (Int, Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Filter Layers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Applied top to bottom. Tap to select, use arrows to reorder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (layers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No layers yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "Select a preset and tap + to add",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                layers.forEachIndexed { index, layer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelect(index) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = if (index == selectedIndex) {
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        } else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (layer.colorMatrix != null) Icons.Default.Palette else Icons.Default.BlurOn,
                                    contentDescription = null,
                                    tint = if (layer.enabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        layer.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (layer.enabled) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        if (layer.colorMatrix != null) "Color Matrix" else "Convolution",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onToggle(index) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        if (layer.enabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (layer.enabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMove(index, index - 1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (index < layers.size - 1) {
                                    IconButton(
                                        onClick = { onMove(index, index + 1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRemove(index) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Slider(
                                value = layer.opacity,
                                onValueChange = { onOpacityChange(index, it) },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                "Opacity: ${(layer.opacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MatrixEditorDialog(
    matrix: ColorMatrix,
    onUpdate: (ColorMatrix) -> Unit,
    onDismiss: () -> Unit
) {
    val values = remember(matrix) { mutableStateListOf(*matrix.values.toTypedArray()) }
    val labels = listOf("R", "G", "B", "A", "Off")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Color Matrix", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "4×5 color transformation matrix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0 until 5) {
                            OutlinedTextField(
                                value = String.format("%.3f", values[row * 5 + col]),
                                onValueChange = { newVal ->
                                    val parsed = newVal.toFloatOrNull() ?: return@OutlinedTextField
                                    values[row * 5 + col] = parsed
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                label = { Text(labels[col], fontSize = 7.sp) },
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    onUpdate(ColorMatrix(values.toFloatArray(), matrix.name))
                    onDismiss()
                }
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun KernelEditorDialog(
    kernel: ConvolutionKernel,
    onUpdate: (ConvolutionKernel) -> Unit,
    onDismiss: () -> Unit
) {
    var width by remember(kernel) { mutableStateOf(kernel.width.toString()) }
    var height by remember(kernel) { mutableStateOf(kernel.height.toString()) }
    var divisor by remember(kernel) { mutableStateOf(kernel.divisor.toString()) }
    var offset by remember(kernel) { mutableStateOf(kernel.offset.toString()) }
    val w = (width.toIntOrNull() ?: 3).coerceIn(1, 9)
    val h = (height.toIntOrNull() ?: 3).coerceIn(1, 9)
    val values = remember(w, h) { mutableStateListOf(*FloatArray(w * h) { 0f }.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Convolution Kernel", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("W") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("H") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = divisor,
                        onValueChange = { divisor = it },
                        label = { Text("Div") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                for (y in 0 until h) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            OutlinedTextField(
                                value = if (idx < values.size) String.format("%.2f", values[idx]) else "0.00",
                                onValueChange = { newVal ->
                                    val parsed = newVal.toFloatOrNull() ?: return@OutlinedTextField
                                    if (idx < values.size) values[idx] = parsed
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    onUpdate(
                        ConvolutionKernel(
                            values = values.toFloatArray().copyOf(w * h),
                            width = w,
                            height = h,
                            name = "Custom ${w}×$h",
                            divisor = divisor.toFloatOrNull() ?: 1f,
                            offset = offset.toFloatOrNull() ?: 0f
                        )
                    )
                    onDismiss()
                }
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SaveFilterDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Filter", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Filter Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onSave,
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExportDialog(
    json: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Filter", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Copy this JSON to share your filter:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    json,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("filter", json)
                    clipboard.setPrimaryClip(clip)
                    onDismiss()
                }
            ) { Text("Copy") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ImportDialog(
    json: String,
    onJsonChange: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Filter", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Paste a filter JSON to import:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = json,
                    onValueChange = onJsonChange,
                    label = { Text("Filter JSON") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onImport,
                enabled = json.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
