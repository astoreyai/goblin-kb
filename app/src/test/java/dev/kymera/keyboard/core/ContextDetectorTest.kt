package dev.kymera.keyboard.core

import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ContextDetectorTest {

    private lateinit var detector: ContextDetector

    @Mock
    private lateinit var editorInfo: EditorInfo

    @Before
    fun setup() {
        detector = ContextDetector()
    }

    @Test
    fun `detectContext returns GENERAL for null EditorInfo`() {
        val result = detector.detectContext(null)
        assertEquals(InputContext.GENERAL, result)
    }

    @Test
    fun `detectContext returns TERMINAL for Termux package`() {
        `when`(editorInfo.packageName).thenReturn("com.termux")

        val result = detector.detectContext(editorInfo)
        assertEquals(InputContext.TERMINAL, result)
    }

    @Test
    fun `detectContext returns CODE for AIDE package`() {
        `when`(editorInfo.packageName).thenReturn("com.aide.ui")

        val result = detector.detectContext(editorInfo)
        assertEquals(InputContext.CODE, result)
    }

    @Test
    fun `detectContext returns KYMERA_CHAT for KYMERA package`() {
        `when`(editorInfo.packageName).thenReturn("dev.kymera.app")
        `when`(editorInfo.hintText).thenReturn(null)

        val result = detector.detectContext(editorInfo)
        assertEquals(InputContext.KYMERA_CHAT, result)
    }

    @Test
    fun `detectContext returns CODE for KYMERA code hint`() {
        `when`(editorInfo.packageName).thenReturn("dev.kymera.app")
        `when`(editorInfo.hintText).thenReturn("Enter code here")

        val result = detector.detectContext(editorInfo)
        assertEquals(InputContext.CODE, result)
    }

    @Test
    fun `detectContext returns TERMINAL for KYMERA terminal hint`() {
        `when`(editorInfo.packageName).thenReturn("dev.kymera.app")
        `when`(editorInfo.hintText).thenReturn("Terminal input")

        val result = detector.detectContext(editorInfo)
        assertEquals(InputContext.TERMINAL, result)
    }

    @Test
    fun `detectContext returns GENERAL for unknown package`() {
        `when`(editorInfo.packageName).thenReturn("com.example.unknown")
        `when`(editorInfo.inputType).thenReturn(0)
        `when`(editorInfo.hintText).thenReturn(null)

        val result = detector.detectContext(editorInfo)
        assertEquals(InputContext.GENERAL, result)
    }

    @Test
    fun `getPackageContext returns correct context for known packages`() {
        assertEquals(InputContext.TERMINAL, detector.getPackageContext("com.termux"))
        assertEquals(InputContext.CODE, detector.getPackageContext("com.aide.ui"))
        assertEquals(InputContext.KYMERA_CHAT, detector.getPackageContext("dev.kymera.app"))
        assertNull(detector.getPackageContext("com.unknown.app"))
    }
}
