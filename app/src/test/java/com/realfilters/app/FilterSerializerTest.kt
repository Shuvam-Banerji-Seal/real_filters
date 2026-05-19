package com.realfilters.app

import com.realfilters.app.data.model.FilterExport
import com.realfilters.app.data.model.FilterLayerExport
import com.realfilters.app.data.model.FilterSerializer
import com.realfilters.app.domain.engine.ColorMatrix
import com.realfilters.app.domain.engine.ConvolutionKernel
import com.realfilters.app.domain.engine.FilterLayer
import com.realfilters.app.domain.filter.PresetFilters
import org.junit.Assert.*
import org.junit.Test

class FilterSerializerTest {

    @Test
    fun `serialize and deserialize color matrix filter`() {
        val export = FilterExport(
            name = "Test Filter",
            type = "color_matrix",
            layers = listOf(
                FilterLayerExport(
                    name = "Sepia",
                    type = "color_matrix",
                    matrixValues = PresetFilters.sepia.values,
                    opacity = 1f
                )
            )
        )

        val json = FilterSerializer.toJson(export)
        assertNotNull(json)
        assertTrue(json.contains("Sepia"))

        val deserialized = FilterSerializer.fromJson(json)
        assertNotNull(deserialized)
        assertEquals("Test Filter", deserialized!!.name)
        assertEquals(1, deserialized.layers.size)
        assertEquals("color_matrix", deserialized.layers[0].type)
        assertArrayEquals(PresetFilters.sepia.values, deserialized.layers[0].matrixValues!!, 0.001f)
    }

    @Test
    fun `serialize and deserialize convolution filter`() {
        val export = FilterExport(
            name = "Sharpen",
            type = "convolution",
            layers = listOf(
                FilterLayerExport(
                    name = "Sharpen",
                    type = "convolution",
                    kernelValues = PresetFilters.sharpen.values,
                    kernelWidth = 3,
                    kernelHeight = 3,
                    kernelDivisor = 1f
                )
            )
        )

        val json = FilterSerializer.toJson(export)
        val deserialized = FilterSerializer.fromJson(json)

        assertNotNull(deserialized)
        assertEquals("convolution", deserialized!!.layers[0].type)
        assertEquals(3, deserialized.layers[0].kernelWidth)
        assertEquals(3, deserialized.layers[0].kernelHeight)
    }

    @Test
    fun `serialize composite filter with multiple layers`() {
        val export = FilterExport(
            name = "Composite",
            type = "composite",
            layers = listOf(
                FilterLayerExport(
                    name = "Grayscale",
                    type = "color_matrix",
                    matrixValues = PresetFilters.grayscale.values
                ),
                FilterLayerExport(
                    name = "Sharpen",
                    type = "convolution",
                    kernelValues = PresetFilters.sharpen.values,
                    kernelWidth = 3,
                    kernelHeight = 3
                )
            )
        )

        val json = FilterSerializer.toJson(export)
        val deserialized = FilterSerializer.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(2, deserialized!!.layers.size)
        assertEquals("color_matrix", deserialized.layers[0].type)
        assertEquals("convolution", deserialized.layers[1].type)
    }

    @Test
    fun `invalid json returns null`() {
        val result = FilterSerializer.fromJson("not valid json{{{")
        assertNull(result)
    }

    @Test
    fun `empty layers list serializes correctly`() {
        val export = FilterExport(
            name = "Empty",
            type = "color_matrix",
            layers = emptyList()
        )

        val json = FilterSerializer.toJson(export)
        val deserialized = FilterSerializer.fromJson(json)

        assertNotNull(deserialized)
        assertTrue(deserialized!!.layers.isEmpty())
    }
}
