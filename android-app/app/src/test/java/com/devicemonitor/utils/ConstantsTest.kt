package com.devicemonitor.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConstantsTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
    }

    @Test
    fun `test getDeviceId returns existing id`() {
        every { sharedPreferences.getString("device_id", null) } returns "existing-id"
        
        val deviceId = Constants.getDeviceId(context)
        
        assertEquals("existing-id", deviceId)
    }

    @Test
    fun `test getDeviceId generates and saves new id if none exists`() {
        every { sharedPreferences.getString("device_id", null) } returns null
        
        val deviceId = Constants.getDeviceId(context)
        
        verify { editor.putString("device_id", any()) }
        verify { editor.apply() }
    }

    @Test
    fun `test theme mode storage`() {
        Constants.setThemeMode(context, 2)
        verify { editor.putInt("theme_mode", 2) }
        
        every { sharedPreferences.getInt("theme_mode", 0) } returns 2
        assertEquals(2, Constants.getThemeMode(context))
    }

    @Test
    fun `test language storage`() {
        Constants.setLanguage(context, "bn")
        verify { editor.putString("language_code", "bn") }
        
        every { sharedPreferences.getString("language_code", "en") } returns "bn"
        assertEquals("bn", Constants.getLanguage(context))
    }
}
