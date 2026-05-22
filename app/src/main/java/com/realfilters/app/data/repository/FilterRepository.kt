package com.realfilters.app.data.repository

import com.realfilters.app.data.db.FilterDao
import com.realfilters.app.data.model.FilterExport
import com.realfilters.app.data.model.FilterLayerExport
import com.realfilters.app.data.model.FilterSerializer
import com.realfilters.app.data.model.SavedFilter
import com.realfilters.app.domain.engine.ColorMatrix
import com.realfilters.app.domain.engine.ConvolutionKernel
import com.realfilters.app.domain.engine.FilterLayer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val filterDao: FilterDao
) {
    fun getAllSavedFilters(): Flow<List<SavedFilter>> = filterDao.getAllFilters()

    suspend fun saveFilter(name: String, layers: List<FilterLayer>): Long {
        val export = layersToExport(name, layers)
        val json = FilterSerializer.toJson(export)
        return filterDao.insertFilter(
            SavedFilter(
                name = name,
                type = export.type,
                matrixData = json
            )
        )
    }

    suspend fun deleteFilter(id: Long) = filterDao.deleteFilterById(id)

    suspend fun loadFilter(id: Long): List<FilterLayer>? {
        val saved = filterDao.getFilterById(id) ?: return null
        return exportToLayers(FilterSerializer.fromJson(saved.matrixData))
    }

    fun exportFilterToJson(name: String, layers: List<FilterLayer>): String {
        val export = layersToExport(name, layers)
        return FilterSerializer.toJson(export)
    }

    fun importFilterFromJson(json: String): List<FilterLayer>? {
        val export = FilterSerializer.fromJson(json) ?: return null
        return exportToLayers(export)
    }

    private fun layersToExport(name: String, layers: List<FilterLayer>): FilterExport {
        val layerExports = layers.map { layer ->
            when {
                layer.colorMatrix != null -> FilterLayerExport(
                    name = layer.name,
                    type = "color_matrix",
                    enabled = layer.enabled,
                    matrixValues = layer.colorMatrix.values,
                    opacity = layer.opacity
                )
                layer.convolutionKernel != null -> FilterLayerExport(
                    name = layer.name,
                    type = "convolution",
                    enabled = layer.enabled,
                    kernelValues = layer.convolutionKernel.values,
                    kernelWidth = layer.convolutionKernel.width,
                    kernelHeight = layer.convolutionKernel.height,
                    kernelDivisor = layer.convolutionKernel.divisor,
                    kernelOffset = layer.convolutionKernel.offset,
                    opacity = layer.opacity
                )
                else -> FilterLayerExport(name = layer.name, type = "identity", enabled = layer.enabled)
            }
        }

        val type = when {
            layers.size > 1 -> "composite"
            layers.any { it.colorMatrix != null } -> "color_matrix"
            else -> "convolution"
        }

        return FilterExport(name = name, type = type, layers = layerExports)
    }

    private fun exportToLayers(export: FilterExport?): List<FilterLayer>? {
        export ?: return null
        return export.layers.map { layerExport ->
            when (layerExport.type) {
                "color_matrix" -> FilterLayer(
                    colorMatrix = layerExport.matrixValues?.let {
                        ColorMatrix(it, layerExport.name)
                    },
                    name = layerExport.name,
                    enabled = layerExport.enabled,
                    opacity = layerExport.opacity
                )
                "convolution" -> FilterLayer(
                    convolutionKernel = layerExport.kernelValues?.let {
                        ConvolutionKernel(
                            values = it,
                            width = layerExport.kernelWidth,
                            height = layerExport.kernelHeight,
                            name = layerExport.name,
                            divisor = layerExport.kernelDivisor,
                            offset = layerExport.kernelOffset
                        )
                    },
                    name = layerExport.name,
                    enabled = layerExport.enabled,
                    opacity = layerExport.opacity
                )
                else -> FilterLayer(name = layerExport.name, enabled = layerExport.enabled)
            }
        }
    }
}
