package com.realfilters.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realfilters.app.data.repository.FilterRepository
import com.realfilters.app.domain.engine.*
import com.realfilters.app.domain.filter.PresetFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class StableBitmap(val bitmap: Bitmap) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StableBitmap) return false
        return bitmap === other.bitmap
    }

    override fun hashCode(): Int = System.identityHashCode(bitmap)
}

data class FilterUiState(
    val originalBitmap: StableBitmap? = null,
    val processedBitmap: StableBitmap? = null,
    val layers: List<FilterLayer> = emptyList(),
    val selectedLayerIndex: Int = -1,
    val currentColorMatrix: ColorMatrix = PresetFilters.identity.clone(),
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
    val filterName: String = ""
)

enum class EditMode {
    COLOR_MATRIX, CONVOLUTION
}

@HiltViewModel
class FilterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: ImageProcessingEngine,
    private val imageLoader: ImageLoader,
    private val repository: FilterRepository
) : ViewModel() {

    companion object {
        private const val TAG = "FilterViewModel"
    }

    private val _uiState = MutableStateFlow(FilterUiState())
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()

    private var applyJob: Job? = null
    private var loadImageJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getAllSavedFilters().collect { filters ->
                _uiState.update { it.copy(savedFilters = filters) }
            }
        }
    }

    fun loadImage(uri: Uri) {
        loadImageJob?.cancel()
        applyJob?.cancel()
        loadImageJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val format = imageLoader.detectFormat(context, uri)
                val bitmap = imageLoader.loadImage(context, uri)
                if (bitmap != null) {
                    val stable = StableBitmap(bitmap)
                    val oldOriginal = _uiState.value.originalBitmap?.bitmap
                    val oldProcessed = _uiState.value.processedBitmap?.bitmap
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
                    recycleIfDifferent(oldOriginal, bitmap)
                    recycleIfDifferent(oldProcessed, oldOriginal)
                    Log.d(TAG, "loadImage success: ${bitmap.width}x${bitmap.height}")
                } else {
                    _uiState.update { it.copy(error = "Failed to load image", isProcessing = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                _uiState.update { it.copy(error = "Image is too large to load", isProcessing = false) }
                Log.e(TAG, "loadImage OOM", e)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load image", isProcessing = false) }
                Log.e(TAG, "loadImage failed", e)
            }
        }
    }

    fun selectPresetMatrix(matrix: ColorMatrix) {
        _uiState.update { it.copy(currentColorMatrix = matrix.clone()) }
    }

    fun selectPresetKernel(kernel: ConvolutionKernel) {
        _uiState.update { it.copy(currentKernel = kernel.clone()) }
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
        if (state.originalBitmap == null) return
        if (state.layers.size >= ImageProcessingEngine.MAX_LAYERS) {
            _uiState.update { it.copy(error = "Maximum ${ImageProcessingEngine.MAX_LAYERS} layers reached") }
            return
        }
        val layer = when (state.editMode) {
            EditMode.COLOR_MATRIX -> FilterLayer(
                colorMatrix = state.currentColorMatrix.clone(),
                name = state.currentColorMatrix.name
            )
            EditMode.CONVOLUTION -> {
                val kernel = state.currentKernel ?: return
                FilterLayer(
                    convolutionKernel = kernel.clone(),
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
        val clampedOpacity = opacity.coerceIn(0f, 1f)
        val newLayers = state.layers.toMutableList().apply {
            this[index] = this[index].copy(opacity = clampedOpacity)
        }
        _uiState.update { it.copy(layers = newLayers) }
        applyFilters()
    }

    fun selectLayer(index: Int) {
        val state = _uiState.value
        if (index < -1 || index >= state.layers.size) return
        _uiState.update { it.copy(selectedLayerIndex = index) }
    }

    fun moveLayer(from: Int, to: Int) {
        val state = _uiState.value
        if (from < 0 || from >= state.layers.size || to < 0 || to >= state.layers.size) return
        val newLayers = state.layers.toMutableList().apply {
            add(to, removeAt(from))
        }
        val newSelected = when (state.selectedLayerIndex) {
            from -> to
            in minOf(from, to)..maxOf(from, to) -> {
                if (from < to) state.selectedLayerIndex - 1 else state.selectedLayerIndex + 1
            }
            else -> state.selectedLayerIndex
        }
        _uiState.update { it.copy(layers = newLayers, selectedLayerIndex = newSelected) }
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
                val currentState = _uiState.value
                val layersToApply = currentState.layers
                val result = if (layersToApply.isEmpty()) {
                    original
                } else {
                    engine.applyFilterLayers(original, layersToApply)
                }
                val oldProcessed = _uiState.value.processedBitmap?.bitmap
                _uiState.update {
                    it.copy(
                        processedBitmap = StableBitmap(result),
                        isProcessing = false
                    )
                }
                if (oldProcessed != null && oldProcessed !== original) {
                    oldProcessed.recycle()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                _uiState.update { it.copy(error = "Image is too large to process", isProcessing = false) }
                Log.e(TAG, "applyFilters OOM", e)
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(error = e.message ?: "Invalid filter", isProcessing = false) }
                Log.e(TAG, "applyFilters invalid arg", e)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Filter processing failed", isProcessing = false) }
                Log.e(TAG, "applyFilters failed", e)
            }
        }
    }

    fun resetFilters() {
        applyJob?.cancel()
        val state = _uiState.value
        val oldProcessed = state.processedBitmap?.bitmap
        _uiState.update {
            it.copy(
                processedBitmap = state.originalBitmap,
                layers = emptyList(),
                selectedLayerIndex = -1,
                currentColorMatrix = PresetFilters.identity.clone(),
                currentKernel = null
            )
        }
        if (oldProcessed != null && oldProcessed !== state.originalBitmap?.bitmap) {
            oldProcessed.recycle()
        }
    }

    fun saveFilter(name: String) {
        val state = _uiState.value
        if (state.layers.isEmpty()) return
        _uiState.update { it.copy(showSaveDialog = false, filterName = "") }
        viewModelScope.launch {
            try {
                repository.saveFilter(name, state.layers)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save: ${e.message}") }
                Log.e(TAG, "saveFilter failed", e)
            }
        }
    }

    fun loadSavedFilter(id: Long) {
        viewModelScope.launch {
            try {
                val layers = repository.loadFilter(id)
                if (layers != null) {
                    _uiState.update { it.copy(layers = layers, selectedLayerIndex = layers.size - 1) }
                    applyFilters()
                } else {
                    _uiState.update { it.copy(error = "Failed to load saved filter") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load: ${e.message}") }
                Log.e(TAG, "loadSavedFilter failed", e)
            }
        }
    }

    fun deleteSavedFilter(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteFilter(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
                Log.e(TAG, "deleteSavedFilter failed", e)
            }
        }
    }

    fun exportCurrentFilter(): String? {
        val state = _uiState.value
        if (state.layers.isEmpty()) {
            _uiState.update { it.copy(error = "No layers to export") }
            return null
        }
        val json = repository.exportFilterToJson("Exported Filter", state.layers)
        _uiState.update { it.copy(exportJson = json, showExportDialog = true) }
        return json
    }

    fun importFilter(json: String) {
        try {
            val layers = repository.importFilterFromJson(json)
            if (layers != null && layers.isNotEmpty()) {
                val state = _uiState.value
                val availableSlots = ImageProcessingEngine.MAX_LAYERS - state.layers.size
                if (availableSlots <= 0) {
                    _uiState.update { it.copy(error = "Maximum ${ImageProcessingEngine.MAX_LAYERS} layers reached", showImportDialog = false, importJson = "") }
                    return
                }
                val layersToAdd = if (layers.size > availableSlots) {
                    _uiState.update { it.copy(error = "Only $availableSlots layer slots available; truncating import") }
                    layers.take(availableSlots)
                } else layers
                val newLayers = state.layers + layersToAdd
                _uiState.update {
                    it.copy(
                        layers = newLayers,
                        selectedLayerIndex = newLayers.size - 1,
                        showImportDialog = false,
                        importJson = ""
                    )
                }
                applyFilters()
            } else if (layers != null) {
                _uiState.update { it.copy(error = "Imported filter has no layers") }
            } else {
                _uiState.update { it.copy(error = "Invalid filter format") }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Import failed: ${e.message ?: "unknown error"}") }
            Log.e(TAG, "importFilter failed", e)
        }
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

    private fun recycleIfDifferent(bitmap: Bitmap?, other: Bitmap?) {
        if (bitmap != null && bitmap !== other && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    override fun onCleared() {
        super.onCleared()
        applyJob?.cancel()
        loadImageJob?.cancel()
        // Recycle any held bitmaps to free native memory promptly.
        val state = _uiState.value
        state.originalBitmap?.bitmap?.takeIf { !it.isRecycled }?.recycle()
        val processed = state.processedBitmap?.bitmap
        if (processed != null && processed !== state.originalBitmap?.bitmap && !processed.isRecycled) {
            processed.recycle()
        }
    }
}
