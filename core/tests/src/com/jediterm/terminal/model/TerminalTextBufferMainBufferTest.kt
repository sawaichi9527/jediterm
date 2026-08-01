package com.jediterm.terminal.model

import com.jediterm.util.BackBufferDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TerminalTextBufferMainBufferTest {
  @Test
  fun `tracks main wrap changes but not alternate transitions`() {
    val buffer = TerminalTextBuffer(8, 2, StyleState())
    val initialRevision = buffer.getMainBufferRevision()

    buffer.setLineWrapped(0, true)
    assertEquals(initialRevision + 1, buffer.getMainBufferRevision())

    val wrappedRevision = buffer.getMainBufferRevision()
    buffer.useAlternateBuffer(true)
    buffer.useAlternateBuffer(false)
    assertEquals(wrappedRevision, buffer.getMainBufferRevision())
  }

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
    val bounds = requireNotNull(buffer.getMainBufferSelectionBounds())
    val mainRevision = buffer.getMainBufferRevision()
    terminal.useAlternateBuffer(true)
    terminal.writeString("alternate")

    assertEquals(expected, buffer.mainBufferText())
    assertFalse(buffer.mainBufferText().contains("alternate"))
    assertEquals(mainRevision, buffer.getMainBufferRevision())
    assertEquals(0, bounds.startRow)
    assertEquals(0, bounds.endRow)
    assertEquals(3, bounds.endColumn)
  }

  private fun TerminalTextBuffer.mainBufferText(): String {
    val snapshot = getMainBufferSnapshot()
    return (snapshot.historyLines + snapshot.screenLines).joinToString("\n") { it.text }
  }
}
