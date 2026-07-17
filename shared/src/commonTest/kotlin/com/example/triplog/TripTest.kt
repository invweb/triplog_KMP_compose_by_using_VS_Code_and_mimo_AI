package com.example.triplog

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TripTest {
    @Test
    fun testStartDateBeforeEndDate() {
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 5)
        assertTrue(startDate <= endDate)
    }

    @Test
    fun testStartDateAfterEndDate() {
        val startDate = LocalDate(2024, 1, 10)
        val endDate = LocalDate(2024, 1, 5)
        assertFalse(startDate <= endDate)
    }

    @Test
    fun testStartDateEqualsEndDate() {
        val startDate = LocalDate(2024, 6, 15)
        val endDate = LocalDate(2024, 6, 15)
        assertTrue(startDate <= endDate)
    }
}