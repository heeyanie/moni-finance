package app;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shared colours, fonts, formatting and small custom components used by every Moni window.
 *
 * Buttons, cards, fields and progress bars are painted by hand so they look the same on
 * Windows, macOS and Linux (the system look-and-feel ignores setBackground() on buttons).
 */
final class Theme {
    private Theme() {}

    // ---------------------------------------------------------------- colours
    static final Color BG = new Color(0xF8, 0xF7, 0xFA);            // page background
    static final Color CARD = Color.WHITE;                           // cards, fields, top bar
    static final Color TEXT = new Color(0x1F, 0x1D, 0x2B);          // main text
    static final Color TEXT_SOFT = new Color(0x3E, 0x3B, 0x4A);     // captions on tinted cards
    static final Color MUTED = new Color(0x6E, 0x6B, 0x7B);         // secondary text
    static final Color BORDER = new Color(0xE4, 0xE2, 0xEA);        // field outlines
    static final Color CARD_OUTLINE = new Color(0xEC, 0xEA, 0xF0);  // white card outlines
    static final Color LINE = new Color(0xF0, 0xEF, 0xF4);          // table row separators
    static final Color ACCENT = new Color(0x5E, 0x57, 0x81);        // primary buttons
    static final Color ACCENT_HOVER = new Color(0x53, 0x4D, 0x74);
    static final Color ACCENT_PRESSED = new Color(0x47, 0x41, 0x63);
    static final Color ACCENT_SOFT = new Color(0xF0, 0xEF, 0xFA);   // pale lavender (table header)
    static final Color ACCENT_MID = new Color(0x95, 0x90, 0xB4);    // current page in the pager
    static final Color INK = new Color(0x55, 0x4E, 0x74);           // sidebar and login panel
    static final Color INK_DEEP = new Color(0x4C, 0x46, 0x69);      // bottom of the sidebar gradient
    static final Color NAV_SELECTED = new Color(0x97, 0x90, 0xC5);  // selected sidebar item
    static final Color TIP_CARD = new Color(0x63, 0x5B, 0x83);      // card at the bottom of the sidebar
    static final Color ON_INK_MUTED = new Color(214, 209, 228);
    static final Color LEAF = new Color(0xE8, 0xD3, 0xAE);          // cream leaves on the sidebar card
    static final Color CREAM = new Color(0xEB, 0xE1, 0xC6);         // the logo coin
    static final Color SAGE = new Color(0x91, 0x9D, 0x85);          // step numbers on the sign-in window
    static final Color GREEN = new Color(0x2F, 0x7D, 0x46);         // money in
    static final Color GREEN_SOFT = new Color(0xE6, 0xF4, 0xEA);
    static final Color AMBER = new Color(0xB7, 0x79, 0x1F);         // near a limit
    static final Color RED = new Color(0xD0, 0x28, 0x4F);           // money out / over a limit
    static final Color RED_SOFT = new Color(0xFC, 0xE3, 0xE8);
    static final Color ALERT = new Color(0xE8, 0x25, 0x30);         // notification dot
    static final Color TRACK = new Color(0xE9, 0xE7, 0xEE);         // empty part of progress bars
    static final Color SOFT_BUTTON = new Color(0xF9, 0xF4, 0xEE);   // cream secondary buttons
    static final Color SOFT_BUTTON_LINE = new Color(0xEE, 0xE5, 0xD9);
    static final Color CHIP = new Color(0xF4, 0xF3, 0xF7);          // user chip in the top bar

    /** A family of colours for one tinted card, icon square or category tag. */
    static final class Tint {
        final Color fill;     // card background
        final Color outline;  // card outline
        final Color box;      // icon square
        final Color ink;      // icon, bar and tag text
        final Color soft;     // tag background

        Tint(int fill, int outline, int box, int ink, int soft) {
            this.fill = new Color(fill);
            this.outline = new Color(outline);
            this.box = new Color(box);
            this.ink = new Color(ink);
            this.soft = new Color(soft);
        }
    }

    static final Tint BLUE = new Tint(0xEEF1FE, 0xE0E6FB, 0xDCE3FC, 0x2E4BB4, 0xE6EBFD);
    static final Tint MINT = new Tint(0xEEF7F1, 0xDDEEE3, 0xD6EEDD, 0x2D6E3E, 0xE3F3E8);
    static final Tint HONEY = new Tint(0xFDF6EC, 0xF6E8D3, 0xFBE7C4, 0xD48214, 0xFCEFD9);
    static final Tint BLUSH = new Tint(0xFCEAEE, 0xF7D9E0, 0xF8D4DD, 0xB7294B, 0xFCE3E8);
    static final Tint VIOLET = new Tint(0xF3EFFC, 0xE8E1F8, 0xEEE6FB, 0x6A3FC8, 0xEFEAFC);

    private static final Map<String, Tint> CATEGORY_TINTS = Map.ofEntries(
            Map.entry("food", new Tint(0xFDEFF2, 0xF8DDE4, 0xFADFE6, 0xD9295A, 0xFDEBEF)),
            Map.entry("transportation", new Tint(0xEEF2FE, 0xDFE6FB, 0xE6ECFD, 0x2F56D0, 0xE8EEFD)),
            Map.entry("school", VIOLET),
            Map.entry("entertainment", new Tint(0xEDF7F0, 0xDAEEDF, 0xDFF2E6, 0x2F8446, 0xE3F3E8)),
            Map.entry("shopping", new Tint(0xFEF4EA, 0xF8E5D0, 0xFDEBD8, 0xD0741A, 0xFDEFE0)),
            Map.entry("bills", new Tint(0xECF7F8, 0xD6ECEF, 0xDDF1F3, 0x1F8595, 0xE2F3F5)),
            Map.entry("health", new Tint(0xFDEFF6, 0xF6DCE9, 0xFBE1EE, 0xC0307A, 0xFCE8F2)),
            Map.entry("other", new Tint(0xF5F4F1, 0xE8E6E1, 0xECEAE6, 0x726B5E, 0xEFEDE8)),
            Map.entry("allowance", new Tint(0xF1F0FD, 0xE3E1FA, 0xE5E3FB, 0x5B53C2, 0xECEBFD)),
            Map.entry("other funds", new Tint(0xEEF5FA, 0xDAE8F2, 0xDCEBF5, 0x2F6B92, 0xE2EFF7)),
            Map.entry("savings", MINT));

    /** Colours for a category, so its icon, bar and tag match on every screen. */
    static Tint tintFor(String category) {
        String key = category == null ? "other" : category.trim().toLowerCase();
        return CATEGORY_TINTS.getOrDefault(key, CATEGORY_TINTS.get("other"));
    }

    // ---------------------------------------------------------------- fonts
    private static final String FAMILY = pickFontFamily();

    static final Font BODY = font(Font.PLAIN, 13);
    static final Font BODY_BOLD = font(Font.BOLD, 13);
    static final Font SMALL = font(Font.PLAIN, 12);
    static final Font LABEL = font(Font.BOLD, 12);
    static final Font H2 = font(Font.BOLD, 17);
    static final Font H1 = font(Font.BOLD, 26);
    static final Font TITLE = font(Font.BOLD, 30);      // page titles such as "Dashboard"
    static final Font STAT = font(Font.BOLD, 28);       // big numbers on the cards
    static final Font NAV = font(Font.PLAIN, 15);       // sidebar items
    static final Font TABLE = font(Font.PLAIN, 14);
    static final Font TABLE_BOLD = font(Font.BOLD, 14);

    static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    /** Uses a modern system font when one is installed that can draw the peso sign. */
    private static String pickFontFamily() {
        String[] preferred = {"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Noto Sans", "Ubuntu"};
        try {
            Set<String> installed = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String name : preferred) {
                if (installed.contains(name) && new Font(name, Font.PLAIN, 13).canDisplay('\u20B1')) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // Fall through to the logical font.
        }
        return Font.SANS_SERIF;
    }

    /** Turns on the same text smoothing the operating system uses, for hand-painted text. */
    static void smoothText(Graphics2D g2) {
        Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (hints instanceof Map) {
            g2.addRenderingHints((Map<?, ?>) hints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    // ---------------------------------------------------------------- money
    // Locale.US keeps "." as the decimal point so parseAmount() can always read it back.
    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat WHOLE =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    /** 1234.5 becomes "₱1,234.50". */
    static String money(double value) {
        return (value < 0 ? "-" : "") + "\u20B1" + MONEY.format(Math.abs(value));
    }

    /** Dashboard style: whole pesos drop the ".00" ("₱850"), but centavos are kept ("₱285.71"). */
    static String peso(double value) {
        return (value < -0.004 ? "-" : "") + "\u20B1" + amountText(Math.abs(value));
    }

    /** Like {@link #peso} but always signed: "+₱400" or "-₱150". */
    static String signedPeso(double value) {
        return (value < 0 ? "-" : "+") + "\u20B1" + amountText(Math.abs(value));
    }

    private static String amountText(double v) {
        double rounded = Math.round(v * 100) / 100.0;
        return Math.abs(rounded - Math.rint(rounded)) < 0.005 ? WHOLE.format(Math.rint(rounded)) : MONEY.format(rounded);
    }

    /** Plain number for text fields, e.g. "1500.00". */
    static String plain(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    /** Accepts "1500", "1,500.50" or "₱1,500". Throws NumberFormatException for anything else. */
    static double parseAmount(String text) {
        String clean = text == null ? "" : text.replace(",", "").replace("\u20B1", "").trim();
        return Double.parseDouble(clean);
    }

    static double parseAmount(JTextField field, double fallback) {
        try {
            return parseAmount(field.getText());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---------------------------------------------------------------- simple builders
    static JLabel text(String value, Font font, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** A rounded white card with a thin outline. */
    static RoundedPanel card(LayoutManager layout) {
        RoundedPanel card = new RoundedPanel(layout, 18, CARD, CARD_OUTLINE);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }

    /** Adds a component to a vertical BoxLayout panel, left-aligned, with a gap above it. */
    static void stack(JPanel boxY, int gapAbove, JComponent component) {
        if (gapAbove > 0) {
            JComponent gap = (JComponent) Box.createRigidArea(new Dimension(0, gapAbove));
            gap.setAlignmentX(Component.LEFT_ALIGNMENT);
            boxY.add(gap);
        }
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        boxY.add(component);
    }

    /** A transparent row that lays its children out left to right without extra gaps. */
    static JPanel row(int gap, JComponent... parts) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        for (int i = 0; i < parts.length; i++) {
            if (i > 0 && gap > 0) row.add(Box.createRigidArea(new Dimension(gap, 0)));
            parts[i].setAlignmentY(Component.CENTER_ALIGNMENT);
            row.add(parts[i]);
        }
        return row;
    }

    static JScrollPane scroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        thinScrollBars(scroll);
        return scroll;
    }

    /** Replaces the chunky system scroll bars with a thin rounded thumb. */
    static void thinScrollBars(JScrollPane scroll) {
        for (JScrollBar bar : new JScrollBar[]{scroll.getVerticalScrollBar(), scroll.getHorizontalScrollBar()}) {
            bar.setUI(new ThinScrollBarUI());
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(10, 10));
        }
    }

    static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    /** Pressing Esc in the dialog runs the action (usually closing it). */
    static void onEscape(JDialog dialog, Runnable action) {
        dialog.getRootPane().registerKeyboardAction(e -> action.run(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ---------------------------------------------------------------- inputs
    static JTextField field() {
        JTextField field = new JTextField(18);
        styleField(field);
        return field;
    }

    static void styleField(JTextField field) {
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(CARD);
        field.setCaretColor(TEXT);
        field.setBorder(new RoundBorder(BORDER, 10, new Insets(8, 12, 8, 12)));
        highlightOnFocus(field);
    }

    /** Draws the rounded outline in the accent colour while the component has focus. */
    private static void highlightOnFocus(JComponent c) {
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { setOutline(c, ACCENT); }
            @Override public void focusLost(FocusEvent e) { setOutline(c, BORDER); }
        });
    }

    private static void setOutline(JComponent c, Color color) {
        if (c.getBorder() instanceof RoundBorder) {
            ((RoundBorder) c.getBorder()).line = color;
            c.repaint();
        }
    }

    /** Disabled fields get a tinted background so they clearly look inactive. */
    static void setFieldEnabled(JTextField field, boolean enabled) {
        field.setEnabled(enabled);
        field.setBackground(enabled ? CARD : BG);
    }

    static void styleCombo(JComboBox<?> combo) {
        combo.setUI(new FlatComboUI());
        combo.setFont(BODY);
        combo.setForeground(TEXT);
        combo.setBackground(CARD);
        combo.setOpaque(true);
        combo.setBorder(new RoundBorder(BORDER, 10, new Insets(2, 8, 2, 4)));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, false);
                setBorder(new EmptyBorder(6, index < 0 ? 4 : 10, 6, 10));
                setFont(BODY);
                if (index < 0) setOpaque(false); // the closed box shows the combo's own background
                if (!selected || index < 0) setForeground(TEXT);
                return this;
            }
        });
        highlightOnFocus(combo);
        // Same height as a text field so rows line up.
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 38));
    }

    static void styleCheck(AbstractButton check) {
        check.setOpaque(false);
        check.setFont(BODY_BOLD);
        check.setForeground(TEXT);
        check.setFocusPainted(false);
    }

    /**
     * Rounded outline for text fields and combo boxes. The square corners of the field's
     * background are painted over in the colour behind the field, so the field looks rounded.
     */
    static final class RoundBorder extends AbstractBorder {
        Color line;
        private final int radius;
        private final Insets pad;

        RoundBorder(Color line, int radius, Insets pad) {
            this.line = line;
            this.radius = radius;
            this.pad = pad;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color behind = visibleBackground(c.getParent());
            if (behind != null) {
                Area corners = new Area(new Rectangle(x, y, w, h));
                corners.subtract(new Area(new RoundRectangle2D.Double(x, y, w - 1, h - 1, radius * 2, radius * 2)));
                g2.setColor(behind);
                g2.fill(corners);
            }
            g2.setColor(line);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius * 2, radius * 2);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) {
            return new Insets(pad.top + 1, pad.left + 1, pad.bottom + 1, pad.right + 1);
        }

        @Override public Insets getBorderInsets(Component c, Insets insets) {
            Insets i = getBorderInsets(c);
            insets.set(i.top, i.left, i.bottom, i.right);
            return insets;
        }
    }

    /** The colour actually showing behind a component (skipping transparent panels). */
    private static Color visibleBackground(Component c) {
        while (c != null) {
            if (c instanceof RoundedPanel || c.isOpaque()) return c.getBackground();
            c = c.getParent();
        }
        return null;
    }

    /** Combo box with a flat body and a chevron instead of the system arrow button. */
    static final class FlatComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton(Icons.of(Icons.Name.CHEVRON_DOWN, 16, MUTED));
            arrow.setContentAreaFilled(false);
            arrow.setBorderPainted(false);
            arrow.setFocusPainted(false);
            arrow.setOpaque(false);
            arrow.setBorder(new EmptyBorder(0, 4, 0, 8));
            return arrow;
        }

        // The system UI paints a blue "selected" block behind the value in the closed box.
        // These two overrides skip it so the combo looks like the text fields.
        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            super.paintCurrentValue(g, bounds, false);
        }
    }

    /**
     * Text field with a magnifying glass, grey placeholder text and an optional keyboard hint
     * (for example "Ctrl + K") on the right.
     */
    static final class SearchField extends JTextField {
        private final String placeholder;
        private final String hint;

        SearchField(String placeholder, String hint) {
            super(18);
            this.placeholder = placeholder;
            this.hint = hint;
            styleField(this);
            int right = hint == null ? 12 : 12 + hintWidth() + 10;
            setBorder(new RoundBorder(BORDER, 10, new Insets(9, 40, 9, right)));
        }

        private int hintWidth() {
            return getFontMetrics(LABEL).stringWidth(hint) + 16;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            smoothText(g2);
            int h = getHeight();
            Icons.of(Icons.Name.SEARCH, 18, MUTED).paintIcon(this, g2, 14, (h - 18) / 2);
            if (getText().isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, getInsets().left, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            if (hint != null) {
                g2.setFont(LABEL);
                FontMetrics fm = g2.getFontMetrics();
                int w = hintWidth();
                int bh = fm.getHeight() + 6;
                int x = getWidth() - w - 10;
                int y = (h - bh) / 2;
                g2.setColor(CARD);
                g2.fillRoundRect(x, y, w, bh, 10, 10);
                g2.setColor(BORDER);
                g2.drawRoundRect(x, y, w, bh, 10, 10);
                g2.setColor(TEXT_SOFT);
                g2.drawString(hint, x + 8, y + 3 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- tables
    static JTable table(TableModel model) {
        JTable table = new JTable(model);
        table.setFont(TABLE);
        table.setForeground(TEXT);
        table.setRowHeight(44);
        table.setBackground(CARD);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(ACCENT_SOFT);
        table.setSelectionForeground(TEXT);
        table.setDefaultRenderer(Object.class, new CellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setBackground(ACCENT_SOFT);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(null);
        return table;
    }

    private static JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        thinScrollBars(scroll);
        return scroll;
    }

    /** A table inside a rounded outline, with the header's top corners rounded too. */
    static RoundedPanel tableBox(JTable table) {
        RoundedPanel box = new RoundedPanel(new BorderLayout(), 14, CARD, CARD_OUTLINE);
        box.setMaskColor(CARD);
        box.add(tableScroll(table), BorderLayout.CENTER);
        return box;
    }

    /** Padded cells. Subclass it to format or colour a column. */
    static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setFont(TABLE);
            if (!selected) {
                setBackground(CARD);
                setForeground(TEXT);
            }
            return this;
        }
    }

    static final class HeaderRenderer extends DefaultTableCellRenderer {
        /** Table client properties that the transaction list uses to show which column it sorts by. */
        static final String SORT_COLUMN = "moni.sortColumn";
        static final String SORT_ASCENDING = "moni.sortAscending";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            String text = value == null ? "" : value.toString();
            Object sorted = table.getClientProperty(SORT_COLUMN);
            if (sorted instanceof Integer && (Integer) sorted == column) {
                text += Boolean.TRUE.equals(table.getClientProperty(SORT_ASCENDING)) ? "  \u2191" : "  \u2193";
            }
            setText(text);
            setFont(TABLE_BOLD);
            setForeground(TEXT);
            setBackground(ACCENT_SOFT);
            setOpaque(true);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            boolean numeric = Number.class.isAssignableFrom(table.getColumnClass(column));
            setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }

    // ---------------------------------------------------------------- custom components

    /**
     * Panel with rounded corners and an optional outline. With a mask colour, the corners
     * outside the curve are painted over after the children, so square children (like a
     * table header) still look rounded.
     */
    static class RoundedPanel extends JPanel {
        private final int radius;
        private Color outline;
        private Color mask;

        RoundedPanel(LayoutManager layout, int radius, Color fill, Color outline) {
            super(layout);
            this.radius = radius;
            this.outline = outline;
            setBackground(fill);
            setOpaque(false);
        }

        void setMaskColor(Color color) {
            mask = color;
            if (mask != null) setBorder(new EmptyBorder(1, 1, 1, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (outline != null && mask == null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (mask == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Area corners = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
            corners.subtract(new Area(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radius, radius)));
            g2.setColor(mask);
            g2.fill(corners);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
        }
    }

    /**
     * A page that fills the scroll pane's height when there's room, so cards can stretch to the
     * bottom of the window, and scrolls when the window is too small.
     */
    static final class Page extends JPanel implements Scrollable {
        Page(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }

        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    enum ButtonKind {
        PRIMARY,    // filled lavender, white text
        SECONDARY,  // white with an outline
        SOFT,       // cream, for the quick actions next to a primary button
        LINK,       // text only
        TAB,        // unselected tab on the sign-in window
        PLAIN,      // no box until the pointer is over it (top bar)
        CIRCLE,     // round icon button on the sidebar card
        CURRENT     // the current page number in the pager
    }

    /** A flat, rounded button that keeps its colours on every operating system. */
    static class FlatButton extends JButton {
        private ButtonKind kind;
        private Icon trailing;
        private boolean dot;
        private int radius = 12;

        FlatButton(String text, ButtonKind kind) {
            this(text, null, kind);
        }

        FlatButton(String text, Icon icon, ButtonKind kind) {
            super(text, icon);
            setFont(BODY_BOLD);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setIconTextGap(8);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(kind == ButtonKind.LINK ? new EmptyBorder(4, 0, 4, 0) : new EmptyBorder(9, 16, 9, 16));
            setKind(kind);
        }

        /** Lets a button switch style, e.g. the selected segment of a tab switcher. */
        void setKind(ButtonKind newKind) {
            kind = newKind;
            switch (kind) {
                case PRIMARY:
                case CURRENT:
                    setForeground(Color.WHITE);
                    break;
                case LINK:
                    setForeground(ACCENT);
                    break;
                case TAB:
                    setForeground(MUTED);
                    break;
                default:
                    setForeground(TEXT);
            }
            repaint();
        }

        /** A second icon on the right, e.g. the chevron of a drop-down button. */
        void setTrailingIcon(Icon icon) {
            trailing = icon;
            Insets i = getBorder().getBorderInsets(this);
            setBorder(new EmptyBorder(i.top, i.left, i.bottom, i.right + icon.getIconWidth() + 8));
        }

        /** Shows a small red dot, e.g. on the notification bell. */
        void setDot(boolean show) {
            dot = show;
            repaint();
        }

        void setRadius(int r) {
            radius = r;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel m = getModel();
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            Color fill = null;
            Color line = null;
            switch (kind) {
                case PRIMARY:
                    fill = !isEnabled() ? new Color(185, 180, 201)
                            : m.isPressed() ? ACCENT_PRESSED : m.isRollover() ? ACCENT_HOVER : ACCENT;
                    break;
                case SECONDARY:
                    fill = m.isPressed() ? new Color(0xE6, 0xE4, 0xF0) : m.isRollover() ? ACCENT_SOFT : CARD;
                    line = BORDER;
                    break;
                case SOFT:
                    fill = m.isPressed() ? new Color(0xEF, 0xE6, 0xD9) : m.isRollover() ? new Color(0xF5, 0xEE, 0xE4) : SOFT_BUTTON;
                    line = SOFT_BUTTON_LINE;
                    break;
                case PLAIN:
                    if (m.isRollover() || m.isPressed()) fill = m.isPressed() ? new Color(0xE9, 0xE7, 0xF0) : CHIP;
                    break;
                case CIRCLE:
                    fill = m.isRollover() ? Color.WHITE : new Color(0xE6, 0xE5, 0xF7);
                    break;
                case CURRENT:
                    fill = ACCENT_MID;
                    break;
                case TAB:
                    setForeground(m.isRollover() ? TEXT : MUTED);
                    break;
                case LINK:
                    setForeground(m.isRollover() ? ACCENT_PRESSED : ACCENT);
                    break;
                default:
                    break;
            }
            if (fill != null) {
                g2.setColor(fill);
                if (kind == ButtonKind.CIRCLE) g2.fillOval(0, 0, w, h);
                else g2.fillRoundRect(0, 0, w, h, radius, radius);
            }
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, w, h, radius, radius);
            }
            if (isFocusOwner() && kind != ButtonKind.LINK && kind != ButtonKind.TAB) {
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 120));
                if (kind == ButtonKind.CIRCLE) g2.drawOval(2, 2, w - 4, h - 4);
                else g2.drawRoundRect(2, 2, w - 4, h - 4, Math.max(4, radius - 2), Math.max(4, radius - 2));
            }
            g2.dispose();

            super.paintComponent(g);

            if (trailing != null || dot) {
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (trailing != null) {
                    // setTrailingIcon() widened the right inset to make room, so the icon starts there.
                    trailing.paintIcon(this, g3, getWidth() - getInsets().right,
                            (getHeight() - trailing.getIconHeight()) / 2);
                }
                if (dot) {
                    int cx = getWidth() / 2 + 6;
                    int cy = getHeight() / 2 - 8;
                    g3.setColor(CARD);
                    g3.fillOval(cx - 5, cy - 5, 10, 10);
                    g3.setColor(ALERT);
                    g3.fillOval(cx - 4, cy - 4, 8, 8);
                }
                g3.dispose();
            }
        }
    }

    /** Thin rounded progress bar. The fraction is clamped to 0..1. */
    static final class Bar extends JComponent {
        private double fraction;
        private Color color = ACCENT;
        private Color track = TRACK;

        Bar(int height) {
            setPreferredSize(new Dimension(160, height));
            setMinimumSize(new Dimension(20, height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        void setTrack(Color trackColor) {
            track = trackColor;
            repaint();
        }

        void set(double value, Color barColor) {
            fraction = Double.isNaN(value) ? 0 : Math.max(0, Math.min(1, value));
            color = barColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g2.setColor(track);
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            int filled = (int) Math.round(getWidth() * fraction);
            if (filled > 0) {
                g2.setColor(color);
                g2.fillRoundRect(0, 0, Math.max(filled, h), h, h, h);
            }
            g2.dispose();
        }
    }

    /** Colour for a progress fraction: lavender when fine, amber near the limit, red when over. */
    static Color progressColor(double fraction) {
        if (fraction >= 1) return RED;
        if (fraction >= 0.8) return AMBER;
        return ACCENT;
    }

    /** A rounded square with an icon in the middle, as on the dashboard cards. */
    static final class IconBox extends JComponent {
        private final Icon icon;
        private final Color fill;
        private final int radius;

        IconBox(Icon icon, int size, Color fill, int radius) {
            this.icon = icon;
            this.fill = fill;
            this.radius = radius;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);
            icon.paintIcon(this, g2, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);
            g2.dispose();
        }
    }

    /** A small rounded tag such as "In", "Out" or "Food". */
    static final class Pill extends JLabel {
        Pill(String text, Color background, Color foreground) {
            super(text);
            setFont(BODY);
            setForeground(foreground);
            setBackground(background);
            setOpaque(false);
            setBorder(new EmptyBorder(3, 11, 3, 11));
        }

        void set(String text, Color background, Color foreground) {
            setText(text);
            setBackground(background);
            setForeground(foreground);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A big number that shrinks its font instead of being cut off when the card is narrow. */
    static final class FitLabel extends JLabel {
        private final Font base;

        FitLabel(String text, Font font, Color color) {
            super(text);
            base = font;
            setFont(font);
            setForeground(color);
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.min(d.width, 80), d.height);
        }

        @Override public Dimension getMinimumSize() { return new Dimension(20, super.getPreferredSize().height); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, super.getPreferredSize().height); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smoothText(g2);
            Font f = base;
            FontMetrics fm = g2.getFontMetrics(f);
            int available = getWidth() - getInsets().left - getInsets().right;
            while (fm.stringWidth(getText()) > available && f.getSize2D() > base.getSize2D() * 0.6f) {
                f = f.deriveFont(f.getSize2D() - 1f);
                fm = g2.getFontMetrics(f);
            }
            FontMetrics baseMetrics = g2.getFontMetrics(base);
            int baseline = getInsets().top + (getHeight() - getInsets().top - getInsets().bottom
                    - baseMetrics.getHeight()) / 2 + baseMetrics.getAscent();
            g2.setFont(f);
            g2.setColor(getForeground());
            g2.drawString(getText(), getInsets().left, baseline);
            g2.dispose();
        }
    }

    /** A filled circle with a short text in the middle: the ₱ logo and numbered steps. */
    static final class Badge extends JComponent {
        private final String value;
        private final int size;
        private final Color fill;
        private final Color ink;

        Badge(String value, int size, Color fill, Color ink) {
            this.value = value;
            this.size = size;
            this.fill = fill;
            this.ink = ink;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setFont(font(Font.BOLD, Math.max(10, Math.round(size * 0.5f))));
            FontMetrics fm = g2.getFontMetrics();
            int x = (size - fm.stringWidth(value)) / 2;
            int y = (size - fm.getAscent() - fm.getDescent()) / 2 + fm.getAscent();
            g2.setColor(ink);
            g2.drawString(value, x, y);
            g2.dispose();
        }
    }

    /** Moni's logo: a vintage cream peso coin. */
    static Badge logo(int size) {
        return new Badge("\u20B1", size, CREAM, INK);
    }

    /**
     * Multi-line text that wraps to the width it is given (unlike a JLabel, which never wraps
     * unless you hard-code a pixel width in HTML).
     */
    static final class WrapText extends JTextArea {
        WrapText(String value, Font font, Color color) {
            super(value);
            setFont(font);
            setForeground(color);
            setLineWrap(true);
            setWrapStyleWord(true);
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setBorder(null);
            setHighlighter(null);
            // Stop the caret from scrolling the page down to this text when it is created.
            ((DefaultCaret) getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            // After the first layout the real width is known, so recompute the height once.
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    if (getPreferredSize().height != getHeight()) revalidate();
                }
            });
        }

        // Without this, Swing scrolls the (hidden) text cursor into view, so a page with
        // WrapText on it opens scrolled to the bottom. Read-only text never needs that.
        @Override public void scrollRectToVisible(Rectangle r) {
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(40, super.getPreferredSize().height);
        }

        @Override public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /**
     * A vertical list of full-width rows. Put it inside {@link #scroll} and it tracks the
     * viewport width, so nothing gets cut off on the right and long text wraps.
     */
    static final class Column extends JPanel implements Scrollable {
        private final GridBagConstraints next = new GridBagConstraints();
        private Component filler;

        Column() {
            super(new GridBagLayout());
            setOpaque(false);
            clearRows();
        }

        void clearRows() {
            removeAll();
            filler = null;
            next.gridx = 0;
            next.gridy = 0;
            next.weightx = 1;
            next.fill = GridBagConstraints.HORIZONTAL;
            next.anchor = GridBagConstraints.NORTHWEST;
        }

        Column addRow(Component component, int gapAbove) {
            if (filler != null) remove(filler);
            next.insets = new Insets(gapAbove, 0, 0, 0);
            next.weighty = 0;
            add(component, next.clone());
            next.gridy++;

            // Empty glue at the bottom pushes the rows to the top.
            GridBagConstraints glue = (GridBagConstraints) next.clone();
            glue.weighty = 1;
            glue.insets = new Insets(0, 0, 0, 0);
            filler = Box.createGlue();
            add(filler, glue);
            return this;
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }

        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** Scroll bar with a thin rounded thumb and no arrow buttons. */
    static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(0x1F, 0x1D, 0x2B, 55);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override protected JButton createDecreaseButton(int orientation) { return noButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return noButton(); }

        private static JButton noButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // No track, only the thumb.
        }

        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? new Color(0x1F, 0x1D, 0x2B, 90) : thumbColor);
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            int t = 6;
            if (vertical) g2.fillRoundRect(r.x + (r.width - t) / 2, r.y + 2, t, r.height - 4, t, t);
            else g2.fillRoundRect(r.x + 2, r.y + (r.height - t) / 2, r.width - 4, t, t, t);
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- menus

    /** A white pop-up menu with a light outline, used by the top bar. */
    static JPopupMenu menu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(CARD);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(6, 0, 6, 0)));
        return menu;
    }

    static JMenuItem menuItem(String text, Icons.Name icon, Runnable action) {
        JMenuItem item = new JMenuItem(text, icon == null ? null : Icons.of(icon, 18, TEXT_SOFT));
        item.setFont(BODY);
        item.setForeground(TEXT);
        item.setBackground(CARD);
        item.setIconTextGap(10);
        item.setBorder(new EmptyBorder(8, 14, 8, 24));
        item.addActionListener(e -> action.run());
        return item;
    }
}