package com.stillhere.app.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    private val day = UpdatePolicy.INTERVAL_MS

    @Test
    fun neverCheckedIsDue() = assertTrue(UpdatePolicy.isDue(null, 1_000))

    @Test
    fun withinADayIsNot() = assertFalse(UpdatePolicy.isDue(10_000, 10_000 + day - 1))

    @Test
    fun aDayLaterIs() = assertTrue(UpdatePolicy.isDue(10_000, 10_000 + day))

    @Test
    fun aClockThatWentBackwardsIsDue() = assertTrue(UpdatePolicy.isDue(10_000, 5_000))
}
