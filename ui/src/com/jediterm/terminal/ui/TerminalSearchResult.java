package com.jediterm.terminal.ui;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerminalSearchResult {
  private final long myRevision;
  private final List<Match> myMatches;
  private final Map<Integer, List<Span>> mySpansByRow;

  public TerminalSearchResult(long revision, @NotNull List<Match> matches) {
    myRevision = revision;
    myMatches = List.copyOf(matches);
    Map<Integer, List<Span>> spansByRow = new HashMap<>();
    for (Match match : matches) {
      for (Span span : match.getSpans()) {
        spansByRow.computeIfAbsent(span.getRow(), ignored -> new ArrayList<>()).add(span);
      }
    }
    spansByRow.replaceAll((ignored, spans) -> List.copyOf(spans));
    mySpansByRow = Collections.unmodifiableMap(spansByRow);
  }

  public long getRevision() {
    return myRevision;
  }

  public @NotNull List<Match> getMatches() {
    return myMatches;
  }

  public @NotNull List<Span> getSpans(int row) {
    return mySpansByRow.getOrDefault(row, Collections.emptyList());
  }

  public static final class Match {
    private final List<Span> mySpans;

    public Match(@NotNull List<Span> spans) {
      if (spans.isEmpty()) {
        throw new IllegalArgumentException("A search match must contain at least one span");
      }
      mySpans = List.copyOf(spans);
    }

    public @NotNull List<Span> getSpans() {
      return mySpans;
    }
  }

  public static final class Span {
    private final int myRow;
    private final int myStartCell;
    private final int myEndCell;

    public Span(int row, int startCell, int endCell) {
      if (startCell < 0 || endCell <= startCell) {
        throw new IllegalArgumentException("Invalid search span: " + startCell + ".." + endCell);
      }
      myRow = row;
      myStartCell = startCell;
      myEndCell = endCell;
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
  }
}
