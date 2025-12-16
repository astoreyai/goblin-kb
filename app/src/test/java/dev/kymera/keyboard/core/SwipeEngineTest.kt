package dev.kymera.keyboard.core

import android.graphics.RectF
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SwipeEngineTest {

    private lateinit var swipeEngine: SwipeEngine
    private lateinit var keyRects: Map<String, RectF>

    @Before
    fun setup() {
        swipeEngine = SwipeEngine()

        // Create a simple QWERTY-like key layout
        keyRects = mapOf(
            "q" to RectF(0f, 0f, 50f, 50f),
            "w" to RectF(50f, 0f, 100f, 50f),
            "e" to RectF(100f, 0f, 150f, 50f),
            "r" to RectF(150f, 0f, 200f, 50f),
            "t" to RectF(200f, 0f, 250f, 50f),
            "a" to RectF(0f, 50f, 50f, 100f),
            "s" to RectF(50f, 50f, 100f, 100f),
            "d" to RectF(100f, 50f, 150f, 100f),
        )
    }

    @Test
    fun `isValidSwipe returns false for short swipe`() {
        swipeEngine.startSwipe(25f, 25f)
        swipeEngine.addPoint(30f, 25f)

        assertFalse(swipeEngine.isValidSwipe())
    }

    @Test
    fun `isValidSwipe returns true for long enough swipe`() {
        swipeEngine.startSwipe(25f, 25f)
        swipeEngine.addPoint(75f, 25f)
        swipeEngine.addPoint(125f, 25f)

        assertTrue(swipeEngine.isValidSwipe())
    }

    @Test
    fun `endSwipe returns null for invalid swipe`() {
        swipeEngine.startSwipe(25f, 25f)
        swipeEngine.addPoint(30f, 25f)

        val result = swipeEngine.endSwipe(keyRects)
        assertNull(result)
    }

    @Test
    fun `endSwipe returns word for valid horizontal swipe`() {
        // Swipe from q to e (qwe)
        swipeEngine.startSwipe(25f, 25f)  // q
        swipeEngine.addPoint(50f, 25f)
        swipeEngine.addPoint(75f, 25f)    // w
        swipeEngine.addPoint(100f, 25f)
        swipeEngine.addPoint(125f, 25f)   // e

        val result = swipeEngine.endSwipe(keyRects)
        assertNotNull(result)
        assertTrue(result!!.isNotEmpty())
    }

    @Test
    fun `analyzeGesture returns correct stats`() {
        swipeEngine.startSwipe(0f, 0f)
        swipeEngine.addPoint(50f, 0f)
        swipeEngine.addPoint(100f, 0f)

        val analysis = swipeEngine.analyzeGesture()

        assertEquals(3, analysis.pointCount)
        assertEquals(100f, analysis.totalLength, 0.1f)
    }

    @Test
    fun `startSwipe clears previous state`() {
        // First swipe
        swipeEngine.startSwipe(0f, 0f)
        swipeEngine.addPoint(100f, 0f)

        // Second swipe should start fresh
        swipeEngine.startSwipe(200f, 200f)
        swipeEngine.addPoint(250f, 200f)

        val analysis = swipeEngine.analyzeGesture()
        assertEquals(2, analysis.pointCount)
    }
}
