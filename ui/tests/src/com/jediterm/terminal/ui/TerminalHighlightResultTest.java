package com.jediterm.terminal.ui;

import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TerminalHighlightResultTest {
  @Test
  public void indexesAnImmutableCopyOfSpansByRow() {
    TextStyle style = new TextStyle(TerminalColor.WHITE, TerminalColor.BLACK);
    List<TerminalHighlightResult.Span> spans = new ArrayList<>();
    spans.add(new TerminalHighlightResult.Span(-1, 2, 5, style));

    TerminalHighlightResult result = new TerminalHighlightResult(17, spans);
    spans.clear();

    assertEquals(17, result.getRevision());
    assertEquals(1, result.getSpans(-1).size());
    assertEquals(style, result.getSpans(-1).get(0).getStyle());
    assertEquals(false, result.getSpans(-1).get(0).isOverrideTerminalStyle());
    assertThrows(UnsupportedOperationException.class,
      () -> result.getSpans(-1).add(new TerminalHighlightResult.Span(-1, 6, 7, style)));
  }

  @Test
  public void retainsTerminalStyleOverrideMode() {
    TerminalHighlightResult.Span span = new TerminalHighlightResult.Span(0, 0, 1, TextStyle.EMPTY, true);

    assertEquals(true, span.isOverrideTerminalStyle());
  }

  @Test
  public void rejectsEmptySpans() {
    assertThrows(IllegalArgumentException.class,
      () -> new TerminalHighlightResult.Span(0, 2, 2, TextStyle.EMPTY));
  }
}
