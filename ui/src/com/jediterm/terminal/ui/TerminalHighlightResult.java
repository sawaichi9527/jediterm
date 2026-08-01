package com.jediterm.terminal.ui;

import com.jediterm.terminal.TextStyle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerminalHighlightResult {
  private final long myRevision;
  private final Map<Integer, List<Span>> mySpansByRow;

  public TerminalHighlightResult(long revision, @NotNull List<Span> spans) {
    myRevision = revision;
    Map<Integer, List<Span>> spansByRow = new HashMap<>();
    for (Span span : spans) {
      spansByRow.computeIfAbsent(span.getRow(), ignored -> new ArrayList<>()).add(span);
    }
    spansByRow.replaceAll((ignored, rowSpans) -> List.copyOf(rowSpans));
    mySpansByRow = Collections.unmodifiableMap(spansByRow);
  }

  public long getRevision() {
    return myRevision;
  }

  public @NotNull List<Span> getSpans(int row) {
    return mySpansByRow.getOrDefault(row, Collections.emptyList());
  }

  public static final class Span {
    private final int myRow;
    private final int myStartCell;
    private final int myEndCell;
    private final TextStyle myStyle;
    private final boolean myOverrideTerminalStyle;

    public Span(int row, int startCell, int endCell, @NotNull TextStyle style) {
      this(row, startCell, endCell, style, false);
    }

    public Span(int row, int startCell, int endCell, @NotNull TextStyle style, boolean overrideTerminalStyle) {
      if (startCell < 0 || endCell <= startCell) {
        throw new IllegalArgumentException("Invalid highlight span: " + startCell + ".." + endCell);
      }
      myRow = row;
      myStartCell = startCell;
      myEndCell = endCell;
      myStyle = style;
      myOverrideTerminalStyle = overrideTerminalStyle;
    }

    public int getRow() {
      return myRow;
    }

    public int getStartCell() {
      return myStartCell;
    }

    public int getEndCell() {
      return myEndCell;
    }

    public @NotNull TextStyle getStyle() {
      return myStyle;
    }

    public boolean isOverrideTerminalStyle() {
      return myOverrideTerminalStyle;
    }
  }
}
