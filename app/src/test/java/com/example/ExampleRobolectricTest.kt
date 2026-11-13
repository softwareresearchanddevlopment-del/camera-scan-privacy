package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PageFormat
import com.example.ui.util.AppLanguage
import com.example.ui.util.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Camera Scan: All Doc, PDF Tools", appName)
    }

    @Test
    fun `verify bilingual localization`() {
        Strings.setLanguage(AppLanguage.ENGLISH)
        assertEquals("Camera Scan: All Doc, PDF Tools", Strings.appName())
        assertEquals("Home", Strings.tabHome())

        Strings.setLanguage(AppLanguage.HINDI)
        assertEquals("होम", Strings.tabHome())
        assertEquals("दस्तावेज़ स्कैन करें", Strings.scanDocument())

        // Reset to English
        Strings.setLanguage(AppLanguage.ENGLISH)
    }

    @Test
    fun `verify standard PDF page formats`() {
        assertEquals(595, PageFormat.A4.widthPt)
        assertEquals(842, PageFormat.A4.heightPt)
        assertEquals(612, PageFormat.LETTER.widthPt)
        assertEquals(792, PageFormat.LETTER.heightPt)
    }

    @Test
    fun `verify quadrilateral perspective crop algorithm`() {
        val srcBitmap = android.graphics.Bitmap.createBitmap(400, 600, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(srcBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val cropped = com.example.data.pdf.PdfEngine.cropQuadrilateral(
            srcBitmap = srcBitmap,
            tl = android.graphics.PointF(0.1f, 0.1f),
            tr = android.graphics.PointF(0.9f, 0.15f),
            br = android.graphics.PointF(0.85f, 0.85f),
            bl = android.graphics.PointF(0.15f, 0.8f)
        )

        assertNotNull(cropped)
        org.junit.Assert.assertTrue(cropped.width > 0)
        org.junit.Assert.assertTrue(cropped.height > 0)
    }

    @Test
    fun `verify bitmap rotation`() {
        val srcBitmap = android.graphics.Bitmap.createBitmap(200, 400, android.graphics.Bitmap.Config.ARGB_8888)
        val rotated = com.example.data.pdf.PdfEngine.rotateBitmap90(srcBitmap, clockwise = true)
        assertEquals(400, rotated.width)
        assertEquals(200, rotated.height)
    }

    @Test
    fun `verify PDF validity check handles non-existent or empty files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val emptyFile = java.io.File(context.cacheDir, "test_empty.pdf")
        emptyFile.writeText("")
        val isValid = com.example.data.pdf.PdfEngine.isPdfValid(emptyFile)
        org.junit.Assert.assertFalse(isValid)
        emptyFile.delete()
    }
}

