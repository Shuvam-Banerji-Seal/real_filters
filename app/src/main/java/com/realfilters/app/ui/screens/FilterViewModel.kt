package com.realfilters.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realfilters.app.data.repository.FilterRepository
import com.realfilters.app.domain.engine.*
import com.realfilters.app.domain.filter.PresetFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StableBitmap(val bitmap: Bitmap)

data class FilterUiState(
    val originalBitmap: StableBitmap? = null,
    val processedBitmap: StableBitmap? = null,
    val layers: List<FilterLayer> = emptyList(),
    val selectedLayerIndex: Int = -1,
    val currentColorMatrix: ColorMatrix = PresetFilters.identity,
    val currentKernel: ConvolutionKernel? = null,
    val isProcessing: Boolean = false,
    val presetColorMatrices: List<ColorMatrix> = PresetFilters.colorMatrices,
    val presetKernels: List<ConvolutionKernel> = PresetFilters.convolutionKernels,
    val savedFilters: List<com.realfilters.app.data.model.SavedFilter> = emptyList(),
    val showMatrixEditor: Boolean = false,
    val showKernelEditor: Boolean = false,
    val editMode: EditMode = EditMode.COLOR_MATRIX,
    val exportJson: String? = null,
    val importJson: String = "",
    val error: String? = null,
    val imageFormat: ImageLoader.ImageFormat = ImageLoader.ImageFormat.UNKNOWN,
    val showSaveDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val filterName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class EditMode {
    COLOR_MATRIX, CONVOLUTION
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@HiltViewModel
class FilterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: ImageProcessingEngine,
    private val imageLoader: ImageLoader,
    private val repository: FilterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilterUiState())
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()

    private var applyJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getAllSavedFilters().collect { filters ->
                _uiState.update { it.copy(savedFilters = filters) }
            }
        }
    }

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val format = imageLoader.detectFormat(context, uri)
                val bitmap = imageLoader.loadImage(context, uri)
                if (bitmap != null) {
                    val stable = StableBitmap(bitmap)
                    _uiState.update {
                        it.copy(
                            originalBitmap = stable,
                            processedBitmap = stable,
                            layers = emptyList(),
                            selectedLayerIndex = -1,
                            imageFormat = format,
                            isProcessing = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Failed to load image", isProcessing = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isProcessing = false) }
            }
        }
    }

    fun selectPresetMatrix(matrix: ColorMatrix) {
        _uiState.update { it.copy(currentColorMatrix = matrix) }
    }

    fun selectPresetKernel(kernel: ConvolutionKernel) {
        _uiState.update { it.copy(currentKernel = kernel) }
    }

    fun updateColorMatrix(matrix: ColorMatrix) {
        _uiState.update { it.copy(currentColorMatrix = matrix) }
    }

    fun updateConvolutionKernel(kernel: ConvolutionKernel) {
        _uiState.update { it.copy(currentKernel = kernel) }
    }

    fun setEditMode(mode: EditMode) {
        _uiState.update { it.copy(editMode = mode) }
    }

    fun addLayer() {
        val state = _uiState.value
        val layer = when (state.editMode) {
            EditMode.COLOR_MATRIX -> FilterLayer(
                colorMatrix = state.currentColorMatrix,
                name = state.currentColorMatrix.name
            )
            EditMode.CONVOLUTION -> {
                val kernel = state.currentKernel ?: return
                FilterLayer(
                    convolutionKernel = kernel,
                    name = kernel.name
                )
            }
        }
        val newLayers = state.layers + layer
        _uiState.update { it.copy(layers = newLayers, selectedLayerIndex = newLayers.size - 1) }
        applyFilters()
    }

    fun removeLayer(index: Int) {
        val state = _uiState.value
        if (index < 0 || index >= state.layers.size) return
        val newLayers = state.layers.toMutableList().apply { removeAt(index) }
        val newSelected = when {
            newLayers.isEmpty() -> -1
            state.selectedLayerIndex >= newLayers.size -> newLayers.size - 1
            else -> state.selectedLayerIndex
        }
        _uiState.update { it.copy(layers = newLayers, selectedLayerIndex = newSelected) }
        applyFilters()
    }

    fun toggleLayer(index: Int) {
        val state = _uiState.value
        if (index < 0 || index >= state.layers.size) return
        val newLayers = state.layers.toMutableList().apply {
            this[index] = this[index].copy(enabled = !this[index].enabled)
        }
        _uiState.update { it.copy(layers = newLayers) }
        applyFilters()
    }

    fun updateLayerOpacity(index: Int, opacity: Float) {
        val state = _uiState.value
        if (index < 0 || index >= state.layers.size) return
        val newLayers = state.layers.toMutableList().apply {
            this[index] = this[index].copy(opacity = opacity)
        }
        _uiState.update { it.copy(layers = newLayers) }
        applyFilters()
    }

    fun selectLayer(index: Int) {
        _uiState.update { it.copy(selectedLayerIndex = index) }
    }

    fun moveLayer(from: Int, to: Int) {
        val state = _uiState.value
        if (from < 0 || from >= state.layers.size || to < 0 || to >= state.layers.size) return
        val newLayers = state.layers.toMutableList().apply {
            add(to, removeAt(from))
        }
        _uiState.update { it.copy(layers = newLayers, selectedLayerIndex = to) }
        applyFilters()
    }

    fun multiplySelectedWithPreset(preset: ColorMatrix) {
        val state = _uiState.value
        if (state.selectedLayerIndex < 0 || state.selectedLayerIndex >= state.layers.size) return
        val layer = state.layers[state.selectedLayerIndex]
        if (layer.colorMatrix == null) return
        val result = engine.multiplyMatrices(layer.colorMatrix, preset)
        val newLayers = state.layers.toMutableList().apply {
            this[state.selectedLayerIndex] = layer.copy(colorMatrix = result, name = result.name)
        }
        _uiState.update { it.copy(layers = newLayers) }
        applyFilters()
    }

    fun applyFilters() {
        val state = _uiState.value
        val original = state.originalBitmap?.bitmap ?: return

        applyJob?.cancel()
        applyJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val result = if (state.layers.isEmpty()) {
                    original
                } else {
                    engine.applyFilterLayers(original, state.layers)
                }
                _uiState.update {
                    it.copy(
                        processedBitmap = StableBitmap(result),
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isProcessing = false) }
            }
        }
    }

    fun resetFilters() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                processedBitmap = state.originalBitmap,
                layers = emptyList(),
                selectedLayerIndex = -1,
                currentColorMatrix = PresetFilters.identity,
                currentKernel = null
            )
        }
    }

    fun saveFilter(name: String) {
        val state = _uiState.value
        if (state.layers.isEmpty()) return
        viewModelScope.launch {
            repository.saveFilter(name, state.layers)
            _uiState.update { it.copy(showSaveDialog = false, filterName = "") }
        }
    }

    fun loadSavedFilter(id: Long) {
        viewModelScope.launch {
            val layers = repository.loadFilter(id)
            if (layers != null) {
                _uiState.update { it.copy(layers = layers, selectedLayerIndex = layers.size - 1) }
                applyFilters()
            }
        }
    }

    fun deleteSavedFilter(id: Long) {
        viewModelScope.launch {
            repository.deleteFilter(id)
        }
    }

    fun exportCurrentFilter(): String? {
        val state = _uiState.value
        if (state.layers.isEmpty()) return null
        val json = repository.exportFilterToJson("Exported Filter", state.layers)
        _uiState.update { it.copy(exportJson = json, showExportDialog = true) }
        return json
    }

    fun importFilter(json: String) {
        val layers = repository.importFilterFromJson(json)
        if (layers != null) {
            _uiState.update {
                it.copy(
                    layers = it.layers + layers,
                    showImportDialog = false,
                    importJson = ""
                )
            }
            applyFilters()
        } else {
            _uiState.update { it.copy(error = "Invalid filter format") }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun showMatrixEditor() = _uiState.update { it.copy(showMatrixEditor = true) }
    fun hideMatrixEditor() = _uiState.update { it.copy(showMatrixEditor = false) }
    fun showKernelEditor() = _uiState.update { it.copy(showKernelEditor = true) }
    fun hideKernelEditor() = _uiState.update { it.copy(showKernelEditor = false) }
    fun showSaveDialog() = _uiState.update { it.copy(showSaveDialog = true) }
    fun hideSaveDialog() = _uiState.update { it.copy(showSaveDialog = false, filterName = "") }
    fun showImportDialog() = _uiState.update { it.copy(showImportDialog = true) }
    fun hideImportDialog() = _uiState.update { it.copy(showImportDialog = false, importJson = "") }
    fun hideExportDialog() = _uiState.update { it.copy(showExportDialog = false, exportJson = null) }
    fun updateFilterName(name: String) = _uiState.update { it.copy(filterName = name) }
    fun updateImportJson(json: String) = _uiState.update { it.copy(importJson = json) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
