package com.realfilters.app

import com.realfilters.app.domain.engine.ColorMatrix
import com.realfilters.app.domain.engine.ConvolutionKernel
import org.junit.Assert.*
import org.junit.Test

class EngineDataClassesTest {

    @Test
    fun `ColorMatrix default constructor produces identity`() {
        val m = ColorMatrix()
        assertEquals(20, m.values.size)
        for (i in 0 until 4) {
            assertEquals(1f, m[i, i], 0f)
            for (j in 0 until 5) {
                if (i != j) assertEquals(0f, m[i, j], 0f)
            }
        }
        assertEquals("Identity", m.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ColorMatrix rejects wrong value count`() {
        ColorMatrix(values = FloatArray(15))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ColorMatrix rejects empty values`() {
        ColorMatrix(values = FloatArray(0))
    }

    @Test
    fun `ColorMatrix clone is independent`() {
        val m = ColorMatrix()
        val c = m.clone()
        c[0, 0] = 99f
        assertNotEquals(m[0, 0], c[0, 0])
    }

    @Test
    fun `ColorMatrix equality compares values only`() {
        val a = ColorMatrix(name = "A")
        val b = ColorMatrix(name = "B")
        assertEquals(a, b)
        b[0, 0] = 5f
        assertNotEquals(a, b)
    }

    @Test
    fun `ConvolutionKernel valid constructor succeeds`() {
        val k = ConvolutionKernel(values = floatArrayOf(1f, 0f, 0f, 1f), width = 2, height = 2)
        assertEquals(2, k.width)
        assertEquals(2, k.height)
        assertEquals(1f, k[0, 0], 0f)
        assertEquals(1f, k[1, 1], 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ConvolutionKernel rejects zero width`() {
        ConvolutionKernel(values = FloatArray(0), width = 0, height = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ConvolutionKernel rejects zero height`() {
        ConvolutionKernel(values = FloatArray(0), width = 1, height = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ConvolutionKernel rejects size mismatch`() {
        ConvolutionKernel(values = floatArrayOf(1f, 2f, 3f), width = 2, height = 2)
    }

    @Test
    fun `ConvolutionKernel clone is independent`() {
        val k = ConvolutionKernel(values = floatArrayOf(1f, 0f, 0f, 1f), width = 2, height = 2)
        val c = k.clone()
        c.values[0] = 99f
        assertEquals(1f, k[0, 0], 0f)
    }

    @Test
    fun `ConvolutionKernel equality includes divisor and offset`() {
        val a = ConvolutionKernel(values = floatArrayOf(1f, 0f, 0f, 1f), width = 2, height = 2, divisor = 1f, offset = 0f)
        val b = ConvolutionKernel(values = floatArrayOf(1f, 0f, 0f, 1f), width = 2, height = 2, divisor = 2f, offset = 0f)
        assertNotEquals(a, b)
    }

    @Test
    fun `ConvolutionKernel hashCode consistent with equals`() {
        val a = ConvolutionKernel(values = floatArrayOf(1f, 0f, 0f, 1f), width = 2, height = 2, divisor = 2f, offset = 1f)
        val b = ConvolutionKernel(values = floatArrayOf(1f, 0f, 0f, 1f), width = 2, height = 2, divisor = 2f, offset = 1f)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `toDisplayString contains all rows`() {
        val m = ColorMatrix()
        val s = m.toDisplayString()
        // 4 rows joined by \n => 3 newlines
        assertEquals(3, s.count { it == '\n' })
    }
}
