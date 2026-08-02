package com.jediterm.terminal.ui;

import com.jediterm.terminal.CursorShape;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.model.CharBuffer;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TerminalPanelHighlightPaintingTest {
  private static final TerminalColor DEFAULT_FOREGROUND = TerminalColor.rgb(220, 220, 220);
  private static final TerminalColor DEFAULT_BACKGROUND = TerminalColor.rgb(20, 20, 20);
  private static final TerminalColor ANSI_FOREGROUND = TerminalColor.rgb(200, 40, 40);
  private static final TerminalColor ANSI_BACKGROUND = TerminalColor.rgb(30, 40, 50);
  private static final TerminalColor HIGHLIGHT_FOREGROUND = TerminalColor.rgb(20, 220, 80);
  private static final TextStyle TERMINAL_STYLE = new TextStyle.Builder()
    .setForeground(ANSI_FOREGROUND)
    .setBackground(ANSI_BACKGROUND)
    .setOption(TextStyle.Option.BOLD, true)
    .build();
  private static final TextStyle HIGHLIGHT_STYLE = new TextStyle.Builder()
    .setForeground(HIGHLIGHT_FOREGROUND)
    .setOption(TextStyle.Option.ITALIC, true)
    .setOption(TextStyle.Option.UNDERLINED, true)
    .build();
  private static final TextStyle SEARCH_STYLE = new TextStyle(
    TerminalColor.rgb(1, 2, 3), TerminalColor.rgb(240, 220, 20));
  private static final TextStyle SELECTION_STYLE = new TextStyle(
    TerminalColor.rgb(250, 250, 250), TerminalColor.rgb(40, 80, 160));

  @Test
  public void composesMergeAndOverrideWithTerminalStyle() throws Exception {
    onEdt(() -> {
      Fixture fixture = fixture();

      fixture.panel.setCoordinateHighlightResult(highlight(fixture.buffer, false));
      assertEquals(List.of(TERMINAL_STYLE, mergedHighlightStyle()), renderStyles(fixture.panel));

      fixture.panel.setCoordinateHighlightResult(highlight(fixture.buffer, true));
      assertEquals(List.of(TERMINAL_STYLE, HIGHLIGHT_STYLE), renderStyles(fixture.panel));
    });
  }

  @Test
  public void paintsHighlightBeforeSearchSelectionAndCursor() throws Exception {
    onEdt(() -> {
      Fixture fixture = fixture();
      long revision = fixture.buffer.getMainBufferRevision();
      fixture.panel.setCoordinateHighlightResult(highlight(fixture.buffer, false));
      fixture.panel.setCoordinateFindResult(new TerminalSearchResult(revision, List.of(
        new TerminalSearchResult.Match(List.of(new TerminalSearchResult.Span(0, 0, 1)))
      )));
      fixture.panel.selectVisible();
      fixture.panel.setCursorVisible(true);
      fixture.panel.setDefaultCursorShape(CursorShape.STEADY_BLOCK);
      fixture.panel.setCursor(0, 1);

      TextStyle search = TERMINAL_STYLE.toBuilder()
        .setForeground(SEARCH_STYLE.getForeground())
        .setBackground(SEARCH_STYLE.getBackground())
        .build();
      TextStyle selection = TERMINAL_STYLE.toBuilder()
        .setForeground(SELECTION_STYLE.getForeground())
        .setBackground(SELECTION_STYLE.getBackground())
        .build();
      TextStyle cursor = selection.toBuilder().setOption(TextStyle.Option.INVERSE, true).build();

      assertEquals(
        List.of(TERMINAL_STYLE, mergedHighlightStyle(), search, selection, cursor),
        renderStyles(fixture.panel)
      );
    });
  }

  @Test
  public void suppressesStaleAndAlternateMainBufferHighlights() throws Exception {
    onEdt(() -> {
      Fixture fixture = fixture();
      TerminalHighlightResult result = highlight(fixture.buffer, false);
      fixture.panel.setCoordinateHighlightResult(result);
      assertEquals(List.of(TERMINAL_STYLE, mergedHighlightStyle()), renderStyles(fixture.panel));

      fixture.buffer.setLineWrapped(0, true);
      assertSame(result, fixture.panel.getCoordinateHighlightResult());
      assertEquals(List.of(TERMINAL_STYLE), renderStyles(fixture.panel));

      fixture.buffer.setLineWrapped(0, false);
      result = highlight(fixture.buffer, false);
      fixture.panel.setCoordinateHighlightResult(result);
      long revision = fixture.buffer.getMainBufferRevision();
      fixture.buffer.useAlternateBuffer(true);
      fixture.state.setCurrent(TERMINAL_STYLE);
      fixture.buffer.writeString(0, 1, new CharBuffer("X"));
      assertEquals(revision, fixture.buffer.getMainBufferRevision());
      assertEquals(List.of(TERMINAL_STYLE), renderStyles(fixture.panel));

      fixture.buffer.useAlternateBuffer(false);
      assertEquals(revision, fixture.buffer.getMainBufferRevision());
      assertEquals(List.of(TERMINAL_STYLE, mergedHighlightStyle()), renderStyles(fixture.panel));
    });
  }

  private static Fixture fixture() {
    StyleState state = new StyleState();
    state.setDefaultStyle(new TextStyle(DEFAULT_FOREGROUND, DEFAULT_BACKGROUND));
    state.setCurrent(TERMINAL_STYLE);
    TerminalTextBuffer buffer = new TerminalTextBuffer(1, 1, state);
    buffer.writeString(0, 1, new CharBuffer("X"));
    RecordingTerminalPanel panel = new RecordingTerminalPanel(buffer, state);
    panel.initializeForPainting();
    return new Fixture(state, buffer, panel);
  }

  private static TerminalHighlightResult highlight(TerminalTextBuffer buffer, boolean override) {
    return new TerminalHighlightResult(buffer.getMainBufferRevision(), List.of(
      new TerminalHighlightResult.Span(0, 0, 1, HIGHLIGHT_STYLE, override)
    ));
  }

  private static TextStyle mergedHighlightStyle() {
    return TERMINAL_STYLE.toBuilder()
      .setForeground(HIGHLIGHT_FOREGROUND)
      .setOption(TextStyle.Option.ITALIC, true)
      .setOption(TextStyle.Option.UNDERLINED, true)
      .build();
  }

  private static List<TextStyle> renderStyles(RecordingTerminalPanel panel) {
    panel.clearRenderedStyles();
    BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      panel.paintComponent(graphics);
      return panel.renderedStyles();
    }
    finally {
      graphics.dispose();
    }
  }

  private static void onEdt(Runnable action) throws Exception {
    SwingUtilities.invokeAndWait(action);
  }

  private static final class Fixture {
    private final StyleState state;
    private final TerminalTextBuffer buffer;
    private final RecordingTerminalPanel panel;

    private Fixture(StyleState state, TerminalTextBuffer buffer, RecordingTerminalPanel panel) {
      this.state = state;
      this.buffer = buffer;
      this.panel = panel;
    }
  }

  private static final class RecordingTerminalPanel extends TerminalPanel {
    private final List<TextStyle> renderedStyles = new ArrayList<>();

    private RecordingTerminalPanel(TerminalTextBuffer buffer, StyleState state) {
      super(new TestSettingsProvider(), buffer, state);
    }

    private void initializeForPainting() {
      initFont();
      setSize(getPixelWidth(), getPixelHeight());
    }

    private void clearRenderedStyles() {
      renderedStyles.clear();
    }

    private List<TextStyle> renderedStyles() {
      return List.copyOf(renderedStyles);
    }

    @Override
    protected @NotNull Font getFontToDisplay(char[] text, int start, int end, @NotNull TextStyle style) {
      renderedStyles.add(style);
      return super.getFontToDisplay(text, start, end, style);
    }

    @Override
    public boolean isFocusOwner() {
      return true;
    }
  }

  private static final class TestSettingsProvider extends DefaultSettingsProvider {
    @Override
    public Font getTerminalFont() {
      return new Font(Font.MONOSPACED, Font.PLAIN, 12);
    }

    @Override
    public boolean useAntialiasing() {
      return false;
    }

    @Override
    public boolean DECCompatibilityMode() {
      return false;
    }

    @Override
    public boolean useInverseSelectionColor() {
      return false;
    }

    @Override
    public @NotNull TextStyle getFoundPatternColor() {
      return SEARCH_STYLE;
    }

    @Override
    public @NotNull TextStyle getSelectionColor() {
      return SELECTION_STYLE;
    }
  }
}
