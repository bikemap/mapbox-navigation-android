package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.text.SpannableString
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxStepDistanceTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `render step distance`() {
        val view = MapboxStepDistance(ctx)
        val expected = SpannableString("13 mi")
        val mockStepDistanceRemaining = 45.0
        val mockState = StepDistance(
            mockk {
                every { formatDistance(mockStepDistanceRemaining) } returns SpannableString("13 mi")
            },
            mockStepDistanceRemaining
        )

        view.render(mockState)
        val actual = view.text

        assertEquals(expected, actual)
    }
}
