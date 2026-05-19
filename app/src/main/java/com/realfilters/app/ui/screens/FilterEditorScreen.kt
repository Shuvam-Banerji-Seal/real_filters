package com.realfilters.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.realfilters.app.domain.engine.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: FilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPresetSheet by remember { mutableStateOf(false) }
    var showLayerSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filter Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
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
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomAction(
                        icon = Icons.Default.GridOn,
                        label = "Matrix",
                        onClick = { showPresetSheet = true }
                    )
                    BottomAction(
                        icon = Icons.Default.Layers,
                        label = "Layers",
                        onClick = { showLayerSheet = true }
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
                        icon = Icons.Default.Add,
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
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.processedBitmap != null) {
                    Image(
                        bitmap = uiState.processedBitmap!!.asImageBitmap(),
                        contentDescription = "Processed Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (uiState.originalBitmap != null) {
                    Image(
                        bitmap = uiState.originalBitmap!!.asImageBitmap(),
                        contentDescription = "Original Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No image loaded",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
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
                        } else null
                    )
                    FilterChip(
                        selected = uiState.editMode == EditMode.CONVOLUTION,
                        onClick = { viewModel.setEditMode(EditMode.CONVOLUTION) },
                        label = { Text("Convolution") },
                        leadingIcon = if (uiState.editMode == EditMode.CONVOLUTION) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            if (uiState.editMode == EditMode.COLOR_MATRIX) {
                                "Current Matrix: ${uiState.currentColorMatrix.name}"
                            } else {
                                "Current Kernel: ${uiState.currentKernel?.name ?: "None"}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (uiState.editMode == EditMode.COLOR_MATRIX) {
                                uiState.currentColorMatrix.toDisplayString()
                            } else {
                                uiState.currentKernel?.toDisplayString() ?: "Select a kernel"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Layers: ${uiState.layers.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPresetSheet) {
        PresetSheet(
            uiState = uiState,
            onSelectMatrix = { viewModel.selectPresetMatrix(it) },
            onSelectKernel = { viewModel.selectPresetKernel(it) },
            onDismiss = { showPresetSheet = false }
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
            onMultiply = { viewModel.multiplySelectedWithPreset(it) },
            onDismiss = { showLayerSheet = false }
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
fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Color Matrices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.presetColorMatrices) { matrix ->
                    AssistChip(
                        onClick = { onSelectMatrix(matrix) },
                        label = { Text(matrix.name, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Convolution Kernels",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.presetKernels) { kernel ->
                    AssistChip(
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
    onMultiply: (ColorMatrix) -> Unit,
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Layers are applied top to bottom. Tap to select, long-press for options.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (layers.isEmpty()) {
                Text(
                    "No layers added yet. Use the + button to add filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                layers.forEachIndexed { index, layer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(index) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
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
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        layer.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (layer.colorMatrix != null) "Color Matrix" else "Convolution Kernel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onToggle(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (layer.enabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMove(index, index - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (index < layers.size - 1) {
                                    IconButton(
                                        onClick = { onMove(index, index + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRemove(index) },
                                    modifier = Modifier.size(32.dp)
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
                                valueRange = 0f..1f
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
    val values = remember { mutableStateListOf(*matrix.values.toTypedArray()) }
    val labels = listOf("R", "G", "B", "A", "Offset")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Color Matrix") },
        text = {
            Column {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                    .height(56.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                label = { Text(labels[col], fontSize = 8.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
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
    var width by remember { mutableStateOf(kernel.width.toString()) }
    var height by remember { mutableStateOf(kernel.height.toString()) }
    var divisor by remember { mutableStateOf(kernel.divisor.toString()) }
    var offset by remember { mutableStateOf(kernel.offset.toString()) }
    val w = width.toIntOrNull() ?: 3
    val h = height.toIntOrNull() ?: 3
    val values = remember { mutableStateListOf(*kernel.values.toTypedArray()) }

    LaunchedEffect(w, h) {
        if (values.size != w * h) {
            values.clear()
            values.addAll(FloatArray(w * h) { 0f }.toTypedArray())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Convolution Kernel") },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("Width") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = divisor,
                        onValueChange = { divisor = it },
                        label = { Text("Divisor") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = offset,
                        onValueChange = { offset = it },
                        label = { Text("Offset") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                for (y in 0 until h) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                    .height(52.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUpdate(
                        ConvolutionKernel(
                            values = values.toFloatArray().copyOf(w * h),
                            width = w,
                            height = h,
                            name = "Custom ${w}x$h",
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
        title = { Text("Save Filter") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Filter Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
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
        title = { Text("Export Filter") },
        text = {
            Column {
                Text("Filter exported as JSON. Copy and share:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    json,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
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
        title = { Text("Import Filter") },
        text = {
            OutlinedTextField(
                value = json,
                onValueChange = onJsonChange,
                label = { Text("Paste filter JSON") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 10
            )
        },
        confirmButton = {
            TextButton(
                onClick = onImport,
                enabled = json.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
