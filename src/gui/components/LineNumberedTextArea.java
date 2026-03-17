package gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

/**
 * A text area with line numbers and breakpoint support
 */
public class LineNumberedTextArea extends JPanel {

    private JTextArea textArea;
    private LineNumberGutter lineNumberArea;
    private JScrollPane scrollPane;
    private Set<Integer> breakpoints;
    private BreakpointClickListener breakpointListener;
    private boolean gutterUpdatePending;
    private BreakpointShiftListener shiftListener;

    /**
     * Interface for breakpoint click events
     */
    public interface BreakpointClickListener {
        void onBreakpointToggled(int lineNumber, boolean isSet);
    }

    public interface BreakpointShiftListener {
        /**
         * Called when line insertions/deletions require breakpoints to be repositioned.
         * 
         * @param fromLine first line affected (1-based, inclusive)
         * @param delta    +N means N lines inserted, -N means N lines deleted
         */
        void onLinesChanged(int fromLine, int delta);
    }

    public LineNumberedTextArea(int rows, int columns) {
        super(new BorderLayout());

        this.breakpoints = new HashSet<>();

        // Main text area
        textArea = new JTextArea(rows, columns);
        textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        // Prevent soft-wrapping: wrapped visual lines would add extra height
        // and cause the gutter (which numbers logical lines) to drift.
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);

        // Line number area
        lineNumberArea = new LineNumberGutter();
        lineNumberArea.setBackground(new Color(240, 240, 240));
        lineNumberArea.setForeground(new Color(100, 100, 100));
        lineNumberArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        // Keep left small; give a bit more room on the right for the breakpoint dot.
        lineNumberArea.setBorder(new EmptyBorder(5, 3, 5, 6));
        lineNumberArea.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Sync scrolling and line numbers
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            private int lastLineCount = 1;

            private void handleChange(DocumentEvent e) {
                updateLineNumbers();

                int currentLineCount = textArea.getLineCount();
                int delta = currentLineCount - lastLineCount;
                if (delta != 0 && shiftListener != null) {
                    // Find which line the edit occurred on
                    try {
                        int affectedLine = textArea.getLineOfOffset(e.getOffset()) + 1; // 1-based
                        // For deletions the line count has already dropped, so clamp
                        shiftListener.onLinesChanged(affectedLine, delta);
                    } catch (BadLocationException ex) {
                        // ignore
                    }
                }
                lastLineCount = currentLineCount;
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                handleChange(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleChange(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
            }
        });

        // Repaint gutter on caret movement (typing can scroll without document
        // changes).
        textArea.addCaretListener(e -> lineNumberArea.repaint());

        // Handle breakpoint clicks
        lineNumberArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleLineNumberClick(e);
            }
        });

        // Create scroll pane
        scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumberArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Ensure gutter repaints when the viewport scrolls.
        scrollPane.getViewport().addChangeListener(e -> lineNumberArea.repaint());

        add(scrollPane, BorderLayout.CENTER);

        updateLineNumbers();
    }

    /**
     * Fine-tunes the vertical alignment of the gutter line numbers.
     * Positive values move numbers down; negative values move them up.
     */
    public void setLineNumberBaselineOffsetPx(int offsetPx) {
        lineNumberArea.setBaselineOffsetPx(offsetPx);
        updateLineNumbers();
    }

    /**
     * Updates the line numbers display
     */
    private void updateLineNumbers() {
        if (gutterUpdatePending) {
            return;
        }
        gutterUpdatePending = true;
        // Coalesce rapid document events and run after Swing updates view/layout.
        SwingUtilities.invokeLater(() -> {
            gutterUpdatePending = false;
            lineNumberArea.updatePreferredWidth();
            lineNumberArea.repaint();
        });
    }

    /**
     * Handles clicks on line numbers (for breakpoint toggling).
     * 
     * If the clicked line is blank or comment-only, the breakpoint snaps
     * forward to the next line that has actual content. If no such line
     * exists, the click is ignored.
     */
    private void handleLineNumberClick(MouseEvent e) {
        try {
            // Map click point into the main text area coordinate space.
            Point pInTextArea = SwingUtilities.convertPoint(lineNumberArea, e.getPoint(), textArea);
            int offset = textArea.viewToModel2D(new Point(0, pInTextArea.y));
            int clickedLine = textArea.getLineOfOffset(offset) + 1; // 1-based
            if (breakpoints.contains(clickedLine)) {
                breakpoints.remove(clickedLine);
                updateLineNumbers();
                if (breakpointListener != null) {
                    breakpointListener.onBreakpointToggled(clickedLine, false);
                }
                return;
            }

            // Resolve to the nearest content-bearing line at or after the click.
            int resolvedLine = resolveToContentLine(clickedLine);
            if (resolvedLine == -1) {
                return; // No content line found — ignore click
            }

            // Toggle on the resolved line.
            boolean wasSet = breakpoints.contains(resolvedLine);
            if (wasSet) {
                breakpoints.remove(resolvedLine);
            } else {
                breakpoints.add(resolvedLine);
            }

            updateLineNumbers();

            // Notify listener with the resolved line so DebugManager stays in sync.
            if (breakpointListener != null) {
                breakpointListener.onBreakpointToggled(resolvedLine, !wasSet);
            }

        } catch (BadLocationException ex) {
            // Ignore click outside text
        }
    }

    /**
     * Finds the first line at or after {@code startLine} that has content
     * (i.e., is not blank and not a pure comment line).
     *
     * @param startLine 1-based line number to start searching from
     * @return the 1-based resolved line number, or -1 if none found
     */
    public int resolveToContentLine(int startLine) {
        int totalLines = textArea.getLineCount();
        for (int line = startLine; line <= totalLines; line++) {
            if (isContentLine(line)) {
                return line;
            }
        }
        return -1;
    }

    /**
     * Returns true if the given 1-based line number has actual assembly content
     * (non-blank after stripping comments).
     */
    private boolean isContentLine(int lineNumber) {
        try {
            int start = textArea.getLineStartOffset(lineNumber - 1);
            int end = textArea.getLineEndOffset(lineNumber - 1);
            String text = textArea.getDocument().getText(start, end - start);

            // Strip comment and surrounding whitespace
            int commentIdx = text.indexOf('#');
            String effective = (commentIdx >= 0 ? text.substring(0, commentIdx) : text).trim();
            return !effective.isEmpty();
        } catch (BadLocationException ex) {
            return false;
        }
    }

    /**
     * Custom-painted gutter for line numbers + breakpoint dots.
     */
    private class LineNumberGutter extends JComponent {
        private static final int MIN_DIGITS = 2;
        private static final int DOT_DIAMETER_PX = 8;
        private static final int DOT_AREA_MIN_PX = 12;
        private static final int DOT_TO_NUMBER_GAP_PX = 6;

        private int baselineOffsetPx = 1;

        LineNumberGutter() {
            setOpaque(true);
        }

        void setBaselineOffsetPx(int offsetPx) {
            this.baselineOffsetPx = offsetPx;
        }

        void updatePreferredWidth() {
            FontMetrics fm = getFontMetrics(getFont());
            int lineCount = Math.max(1, textArea.getLineCount());
            int digits = Math.max(MIN_DIGITS, String.valueOf(lineCount).length());

            Insets insets = getInsets();
            int numbersWidth = fm.charWidth('0') * digits;
            // Dot area on the left, numbers on the right.
            int width = insets.left + DOT_AREA_MIN_PX + DOT_TO_NUMBER_GAP_PX + numbersWidth + insets.right;

            Dimension preferredText = textArea.getPreferredSize();
            Dimension current = getPreferredSize();
            int prefHeight = preferredText != null ? preferredText.height : 1;
            if (current == null || current.width != width || current.height != prefHeight) {
                setPreferredSize(new Dimension(width, prefHeight));
                revalidate();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Insets insets = getInsets();
                int width = getWidth();
                int rightEdge = width - insets.right;

                g2.setColor(getBackground());
                g2.fillRect(0, 0, width, getHeight());

                FontMetrics fm = g2.getFontMetrics();
                FontMetrics textFm = textArea.getFontMetrics(textArea.getFont());
                Rectangle visible = textArea.getVisibleRect();

                // Use the text area's visible region directly. The gutter has its own clip
                // coordinates,
                // so mixing gutter clip.y into text-area coordinates can cause desync during
                // rapid edits.
                int startOffset = textArea.viewToModel2D(new Point(0, visible.y));
                int endOffset = textArea.viewToModel2D(new Point(0, visible.y + visible.height));
                int startLine = textArea.getLineOfOffset(startOffset);
                int endLine = textArea.getLineOfOffset(endOffset);

                g2.setColor(getForeground());
                for (int lineIndex = startLine; lineIndex <= endLine; lineIndex++) {
                    int lineNumber = lineIndex + 1;

                    Rectangle2D r2 = textArea.modelToView2D(textArea.getLineStartOffset(lineIndex));
                    if (r2 == null) {
                        continue;
                    }
                    Rectangle r = r2.getBounds();
                    // Row header view scrolls with the viewport, so gutter coordinates match the
                    // text area's document coordinates. Do not subtract visible.y here.
                    int y = r.y;
                    int lineHeight = r.height;

                    String num = String.valueOf(lineNumber);
                    // Match the text area's baseline: top-of-line + ascent.
                    // (The line rectangle height can include leading; using ascent avoids a slow
                    // drift.)
                    int baseline = y + textFm.getAscent() + baselineOffsetPx;
                    // Right-align numbers so they expand to the left as digits increase.
                    int numberRightX = rightEdge;
                    int numberX = numberRightX - fm.stringWidth(num);
                    g2.drawString(num, numberX, baseline);

                    if (breakpoints.contains(lineNumber)) {
                        // Center the dot within the left-side dot area.
                        int dotAreaLeft = insets.left;
                        int dotAreaRight = dotAreaLeft + DOT_AREA_MIN_PX;
                        int dotCenterX = (dotAreaLeft + dotAreaRight) / 2;
                        int dotCenterY = y + (lineHeight / 2);

                        int radius = DOT_DIAMETER_PX / 2;
                        int dotX = dotCenterX - radius;
                        int dotY = dotCenterY - radius;
                        g2.fillOval(dotX, dotY, DOT_DIAMETER_PX, DOT_DIAMETER_PX);
                    }
                }
            } catch (BadLocationException ex) {
                // ignore painting issues from transient document states
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Sets a breakpoint click listener
     */
    public void setBreakpointListener(BreakpointClickListener listener) {
        this.breakpointListener = listener;
    }

    /**
     * Gets the set of breakpoint line numbers
     */
    public Set<Integer> getBreakpoints() {
        return new HashSet<>(breakpoints);
    }

    /**
     * Sets a breakpoint at a specific line
     */
    public void setBreakpoint(int lineNumber, boolean enabled) {
        if (enabled) {
            breakpoints.add(lineNumber);
        } else {
            breakpoints.remove(lineNumber);
        }
        updateLineNumbers();
    }

    /**
     * Clears all breakpoints
     */
    public void clearBreakpoints() {
        breakpoints.clear();
        updateLineNumbers();
    }

    /**
     * Gets the text content
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * Sets the text content
     */
    public void setText(String text) {
        textArea.setText(text);
        updateLineNumbers();
    }

    /**
     * Gets the underlying text area
     */
    public JTextArea getTextArea() {
        return textArea;
    }

    /**
     * Clears the text
     */
    public void clear() {
        textArea.setText("");
        breakpoints.clear();
        updateLineNumbers();
    }

    /**
     * Sets the caret position
     */
    public void setCaretPosition(int position) {
        textArea.setCaretPosition(position);
    }

    /**
     * Gets the document for undo/redo support
     */
    public Document getDocument() {
        return textArea.getDocument();
    }

    public void setBreakpointShiftListener(BreakpointShiftListener listener) {
        this.shiftListener = listener;
    }
}