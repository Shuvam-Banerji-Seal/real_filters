package com.realfilters.app

import com.realfilters.app.data.model.FilterExport
import com.realfilters.app.data.model.FilterLayerExport
import com.realfilters.app.data.model.FilterSerializer
import com.realfilters.app.domain.engine.ColorMatrix
import com.realfilters.app.domain.engine.ConvolutionKernel
import org.junit.Assert.*
import org.junit.Test

/**
 * These tests verify the JSON shape produced/consumed by FilterSerializer,
 * which is the boundary the repository validates against. The repository
 * enforces values.size, kernel size, and per-layer correctness.
 */
class FilterImportValidationTest {

    @Test
    fun `valid color matrix filter round-trips through serializer`() {
        val json = """
            {
              "name": "Test",
              "type": "color_matrix",
              "layers": [
                {
                  "name": "Brightness",
                  "type": "color_matrix",
                  "enabled": true,
                  "opacity": 1.0,
                  "matrix_values": [1.2,0,0,0,0.1, 0,1.2,0,0,0.1, 0,0,1.2,0,0.1, 0,0,0,1,0]
                }
              ]
            }
        """.trimIndent()
        val parsed = FilterSerializer.fromJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.layers.size)
        assertEquals("Brightness", parsed.layers[0].name)
        assertEquals(20, parsed.layers[0].matrixValues?.size)
    }

    @Test
    fun `ColorMatrix rejects 3-element matrix_values from repository validation`() {
        // Simulate the repository's validation: ColorMatrix init requires 20 elements.
        assertThrows(IllegalArgumentException::class.java) {
            ColorMatrix(values = floatArrayOf(1f, 0f, 0f), name = "X")
        }
    }

    @Test
    fun `ConvolutionKernel rejects 3-element values for 3x3 layout`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConvolutionKernel(values = floatArrayOf(1f, 2f, 3f), width = 3, height = 3)
        }
    }

    @Test
    fun `garbage JSON returns null without throwing`() {
        assertNull(FilterSerializer.fromJson("not even json"))
    }

    @Test
    fun `empty name and empty layers is treated as invalid`() {
        val json = """{"name": "", "type": "composite", "layers": []}"""
        assertNull(FilterSerializer.fromJson(json))
    }

    @Test
    fun `toJson and fromJson round-trip preserves name and layer count`() {
        val original = FilterExport(
            name = "Round Trip",
            type = "composite",
            layers = listOf(
                FilterLayerExport(
                    name = "L1",
                    type = "color_matrix",
                    enabled = true,
                    matrixValues = FloatArray(20) { 1f },
                    opacity = 0.5f
                )
            )
        )
        val json = FilterSerializer.toJson(original)
        val parsed = FilterSerializer.fromJson(json)
        assertNotNull(parsed)
        assertEquals(original.name, parsed!!.name)
        assertEquals(1, parsed.layers.size)
        assertEquals(original.layers[0].opacity, parsed.layers[0].opacity, 0f)
    }
}
