package org.odk.collect.android.widgets.utilities

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import org.javarosa.core.model.data.GeoPointData
import org.junit.Test
import org.junit.runner.RunWith
import org.odk.collect.android.R
import org.odk.collect.android.widgets.support.GeoWidgetHelpers
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils.convertCoordinatesIntoDegreeFormat
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils.floor
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils.getGeoPointAnswerToDisplay
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils.getLocationParamsFromStringAnswer
import org.odk.collect.android.widgets.utilities.GeoWidgetUtils.truncateDouble

@RunWith(AndroidJUnit4::class)
class GeoWidgetUtilsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val answer = GeoPointData(GeoWidgetHelpers.getRandomDoubleArray())

    @Test
    fun getAnswerToDisplay_whenAnswerIsNull_returnsEmptyString() {
        assertEquals(getGeoPointAnswerToDisplay(context, null), "")
    }

    @Test
    fun getAnswerToDisplay_whenAnswerIsNotConvertible_returnsEmptyString() {
        assertEquals(getGeoPointAnswerToDisplay(context, "blah"), "")
    }

    @Test
    fun getAnswerToDisplay_whenAnswerIsNotNullAndConvertible_returnsAnswer() {
        val stringAnswer = answer.displayText
        val parts = stringAnswer.split(" ".toRegex()).toTypedArray()
        assertEquals(
            getGeoPointAnswerToDisplay(context, stringAnswer),
            context.getString(
                R.string.gps_result,
                convertCoordinatesIntoDegreeFormat(context, parts[0].toDouble(), "lat"),
                convertCoordinatesIntoDegreeFormat(context, parts[1].toDouble(), "lon"),
                truncateDouble(parts[2]),
                truncateDouble(parts[3])
            )
        )
    }

    @Test // Results confirmed with https://www.sunearthtools.com/dp/tools/conversion.php
    fun convertCoordinatesIntoDegreeFormatTest() {
        assertEquals(
            "N 37°27'5\"",
            convertCoordinatesIntoDegreeFormat(context, 37.45153333333334, "lat")
        )
        assertEquals(
            "W 122°9'19\"",
            convertCoordinatesIntoDegreeFormat(context, -122.15539166666667, "lon")
        )
        assertEquals(
            "N 3°51'4\"",
            convertCoordinatesIntoDegreeFormat(context, 3.8513583333333337, "lat")
        )
        assertEquals(
            "W 70°2'11\"",
            convertCoordinatesIntoDegreeFormat(context, -70.03650333333333, "lon")
        )
        assertEquals(
            "S 31°8'40\"",
            convertCoordinatesIntoDegreeFormat(context, -31.144546666666663, "lat")
        )
        assertEquals(
            "E 138°16'15\"",
            convertCoordinatesIntoDegreeFormat(context, 138.27083666666667, "lon")
        )
        assertEquals(
            "N 61°23'15\"",
            convertCoordinatesIntoDegreeFormat(context, 61.38757333333333, "lat")
        )
        assertEquals(
            "W 150°55'37\"",
            convertCoordinatesIntoDegreeFormat(context, -150.92708666666667, "lon")
        )
        assertEquals("N 0°0'0\"", convertCoordinatesIntoDegreeFormat(context, 0.0, "lat"))
        assertEquals("E 0°0'0\"", convertCoordinatesIntoDegreeFormat(context, 0.0, "lon"))
    }

    @Test
    fun floorTest() {
        assertEquals("5", floor("5"))
        assertEquals("-5", floor("-5"))
        assertEquals("5", floor("5.55"))
        assertEquals("-5", floor("-5.55"))
        assertEquals("", floor(""))
        assertEquals("", floor(null))
        assertEquals("qwerty", floor("qwerty"))
    }

    @Test
    fun getLocationParamsFromStringAnswerTest() {
        var gp =
            getLocationParamsFromStringAnswer("37.45153333333334 -122.15539166666667 0.0 20.0")
        assertEquals(37.45153333333334, gp[0])
        assertEquals(-122.15539166666667, gp[1])
        assertEquals(0.0, gp[2])
        assertEquals(20.0, gp[3])

        gp = getLocationParamsFromStringAnswer("37.45153333333334")
        assertEquals(37.45153333333334, gp[0])
        assertEquals(0.0, gp[1])
        assertEquals(0.0, gp[2])
        assertEquals(0.0, gp[3])

        gp = getLocationParamsFromStringAnswer("")
        assertEquals(0.0, gp[0])
        assertEquals(0.0, gp[1])
        assertEquals(0.0, gp[2])
        assertEquals(0.0, gp[3])

        gp = getLocationParamsFromStringAnswer(null)
        assertEquals(0.0, gp[0])
        assertEquals(0.0, gp[1])
        assertEquals(0.0, gp[2])
        assertEquals(0.0, gp[3])

        gp =
            getLocationParamsFromStringAnswer("37.45153333333334 -122.15539166666667 0.0 qwerty")
        assertEquals(37.45153333333334, gp[0])
        assertEquals(-122.15539166666667, gp[1])
        assertEquals(0.0, gp[2])
        assertEquals(0.0, gp[3])

        gp = getLocationParamsFromStringAnswer(" 37.45153333333334 -122.15539166666667 0.0 ")
        assertEquals(37.45153333333334, gp[0])
        assertEquals(-122.15539166666667, gp[1])
        assertEquals(0.0, gp[2])
        assertEquals(0.0, gp[3])
    }

    @Test
    fun truncateDoubleTest() {
        assertEquals("5", truncateDouble("5"))
        assertEquals("-5", truncateDouble("-5"))
        assertEquals("5.12", truncateDouble("5.12"))
        assertEquals("-5.12", truncateDouble("-5.12"))
        assertEquals("5.12", truncateDouble("5.1234"))
        assertEquals("-5.12", truncateDouble("-5.1234"))
        assertEquals("", truncateDouble(""))
        assertEquals("", truncateDouble(null))
        assertEquals("", truncateDouble("qwerty"))
    }
}
