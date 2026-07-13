package com.devicemonitor.utils

import android.content.Context
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RecordingHelperTest {

    private val context: Context = mockk(relaxed = true)
    private val recordingHelper = RecordingHelper(context)

    @Test
    fun `test initial state is not recording`() {
        assertFalse(recordingHelper.isRecording())
        assertFalse(recordingHelper.isPaused())
        assertEquals(0L, recordingHelper.getRecordingProgressSeconds())
    }

    // Since MediaRecorder and MediaProjection involve heavy Android dependencies,
    // we would typically use Robolectric or integration tests for full logic.
    // Here we test the utility functions that are testable in plain JUnit.
    
    @Test
    fun `test constants are correct`() {
        assertEquals(18000L, RecordingHelper.MAX_RECORDING_SECONDS)
        assertEquals(44100, RecordingHelper.HIGH_QUALITY_SAMPLE_RATE)
        assertEquals(192000, RecordingHelper.HIGH_QUALITY_ENCODING_BIT_RATE)
    }
}
