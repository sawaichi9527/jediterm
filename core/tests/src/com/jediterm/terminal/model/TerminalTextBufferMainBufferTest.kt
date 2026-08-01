package com.jediterm.terminal.model

import com.jediterm.JediTerminal
import com.jediterm.terminal.StyleState
import com.jediterm.util.BackBufferDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TerminalTextBufferMainBufferTest {
  @Test
  fun `snapshot remains detached from later main buffer changes`() {
    val styleState = StyleState()
    val buffer = TerminalTextBuffer(8, 2, styleState)
    val terminal = JediTerminal(BackBufferDisplay(buffer), buffer, styleState)
    terminal.writeString("before")

    val snapshot = buffer.getMainBufferSnapshot()
    terminal.writeString(" after")

    assertEquals("before", snapshot.screenLines.first().text)
  }

  @Test
  fun `processes retained main buffer while alternate buffer is active`() {
    val styleState = StyleState()
    val buffer = TerminalTextBuffer(8, 2, styleState)
    val terminal = JediTerminal(BackBufferDisplay(buffer), buffer, styleState)
    terminal.writeString("main")

    val expected = buffer.mainBufferText()
    terminal.useAlternateBuffer(true)
    terminal.writeString("alternate")

    assertEquals(expected, buffer.mainBufferText())
    assertFalse(buffer.mainBufferText().contains("alternate"))
  }

  private fun TerminalTextBuffer.mainBufferText(): String {
    val snapshot = getMainBufferSnapshot()
    return (snapshot.historyLines + snapshot.screenLines).joinToString("\n") { it.text }
  }
}
