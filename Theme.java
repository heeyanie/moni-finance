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
import java.util.prefs.Preferences;

/**
 * Shared colours, fonts, formatting and small custom components used by every Moni window.
 *
 * Buttons, cards, fields and progress bars are painted by hand so they look the same on
 * Windows, macOS and Linux (the system look-and-feel ignores setBackground() on buttons).
 */
final class Theme {
    private Theme() {}

    // ---------------------------------------------------------------- colours
    // Moni's palette is olive green and beige cream. Every other colour below is a shade of one
    // of those two, apart from green / amber / red for money in, warnings and money out.
    //
    // The fields aren't final because dark mode swaps them (see setDarkMode). Components read
    // them when they are built, so after switching modes the window has to be built again.

    static final Color OLIVE = new Color(0x4D694E);
    static final Color CREAM = new Color(0xFFF3D5);

    private static final String DARK_MODE_KEY = "darkMode";
    private static boolean darkMode;

    static Color BG;              // page background
    static Color CARD;            // cards, fields, top bar
    static Color TEXT;            // main text
    static Color TEXT_SOFT;       // captions on tinted cards
    static Color MUTED;           // secondary text
    static Color BORDER;          // field outlines
    static Color CARD_OUTLINE;    // card outlines
    static Color LINE;            // table row separators
    static Color ACCENT;          // primary buttons, links, focus outlines
    static Color ACCENT_HOVER;
    static Color ACCENT_PRESSED;
    static Color ON_ACCENT;       // text and icons on a primary button
    static Color ACCENT_SOFT;     // table header, selected rows, tips
    static Color ACCENT_MID;      // the other days in the daily chart
    static Color INK;             // sidebar and the sign-in panel
    static Color INK_DEEP;        // bottom of the sidebar gradient
    static Color ON_INK;          // text on the sidebar
    static Color ON_INK_MUTED;
    static Color NAV_SELECTED;    // selected sidebar item
    static Color TIP_CARD;        // card at the bottom of the sidebar
    static Color LEAF;            // leaves on the sidebar card
    static Color GREEN;           // money in
    static Color GREEN_SOFT;
    static Color AMBER;           // near a limit
    static Color RED;             // money out / over a limit
    static Color RED_SOFT;
    static Color ALERT;           // notification dot
    static Color TRACK;           // empty part of progress bars
    static Color SOFT_BUTTON;     // secondary quick-action buttons
    static Color SOFT_BUTTON_LINE;
    static Color CHIP;            // user chip in the top bar
    static Color CHIP_HOVER;

    /** A family of colours for one tinted card, icon square or category tag, all made from one ink colour. */
    static final class Tint {
        final Color fill;     // card background
        final Color outline;  // card outline
        final Color box;      // icon square
        final Color ink;      // icon, bar and tag text
        final Color soft;     // tag background

        private Tint(int lightInk) {
            // Dark mode lightens the ink so it stays readable on dark cards.
            ink = darkMode ? mix(new Color(lightInk), CREAM, 0.55) : new Color(lightInk);
            fill = mix(ink, CARD, darkMode ? 0.10 : 0.07);
            outline = mix(ink, CARD, 0.16);
            box = mix(ink, CARD, 0.18);
            soft = mix(ink, CARD, 0.14);
        }
    }

    // Tints for the stat cards. Earthy shades so they sit well next to olive and cream.
    static Tint MOSS;
    static Tint FERN;
    static Tint OCHRE;
    static Tint CLAY;
    static Tint SLATE;

    private static Map<String, Tint> categoryTints;

    static {
        darkMode = loadDarkModePreference();
        applyPalette();
    }

    static boolean isDarkMode() {
        return darkMode;
    }

    /** Switches between light and dark colours and remembers the choice for next time. */
    static void setDarkMode(boolean dark) {
        darkMode = dark;
        applyPalette();
        try {
            Preferences.userNodeForPackage(Theme.class).putBoolean(DARK_MODE_KEY, dark);
        } catch (SecurityException e) {
            // The choice just won't be remembered.
        }
    }

    private static boolean loadDarkModePreference() {
        try {
            return Preferences.userNodeForPackage(Theme.class).getBoolean(DARK_MODE_KEY, false);
        } catch (SecurityException e) {
            return false;
        }
    }

    private static void applyPalette() {
        if (darkMode) {
            BG = new Color(0x171C17);
            CARD = new Color(0x212821);
            TEXT = new Color(0xF3EBD6);
            TEXT_SOFT = new Color(0xD6CFBA);
            MUTED = new Color(0xA3A796);
            BORDER = new Color(0x3D483D);
            CARD_OUTLINE = new Color(0x2E372E);
            LINE = new Color(0x2B332B);
            ACCENT = new Color(0x7E9F7F);
            ACCENT_HOVER = new Color(0x8BAC8C);
            ACCENT_PRESSED = new Color(0x6F906F);
            ON_ACCENT = new Color(0x141A14);
            ACCENT_MID = new Color(0x4F6A52);
            INK = new Color(0x2B392C);
            INK_DEEP = new Color(0x1E281F);
            NAV_SELECTED = new Color(0x435844);
            TIP_CARD = new Color(0x354636);
            LEAF = new Color(0xCDBB8E);
            GREEN = new Color(0x7DC08A);
            AMBER = new Color(0xE2AA4F);
            RED = new Color(0xEE8273);
            ALERT = new Color(0xF0645A);
            TRACK = new Color(0x343E34);
            SOFT_BUTTON = new Color(0x2A322A);
            SOFT_BUTTON_LINE = new Color(0x3A443A);
            CHIP = new Color(0x2A322A);
            CHIP_HOVER = new Color(0x343D34);
        } else {
            BG = CREAM;
            CARD = new Color(0xFFFDF8);
            TEXT = new Color(0x263226);
            TEXT_SOFT = new Color(0x3D4A3D);
            MUTED = new Color(0x6B7566);
            BORDER = new Color(0xDAD3BE);
            CARD_OUTLINE = new Color(0xEDE3C8);
            LINE = new Color(0xF1EBDA);
            ACCENT = OLIVE;
            ACCENT_HOVER = new Color(0x435C44);
            ACCENT_PRESSED = new Color(0x394F3A);
            ON_ACCENT = CREAM;
            ACCENT_MID = new Color(0x9DB09D);
            INK = OLIVE;
            INK_DEEP = new Color(0x405841);
            NAV_SELECTED = new Color(0x6B866C);
            TIP_CARD = new Color(0x5A775B);
            LEAF = new Color(0xEBDDB5);
            GREEN = new Color(0x2F7D46);
            AMBER = new Color(0xB7791F);
            RED = new Color(0xB83B32);
            ALERT = new Color(0xD93A2F);
            TRACK = new Color(0xE9E4D4);
            SOFT_BUTTON = new Color(0xF8EDD0);
            SOFT_BUTTON_LINE = new Color(0xE8DBB8);
            CHIP = new Color(0xF4F2EA);
            CHIP_HOVER = new Color(0xEAE8DD);
        }
        ACCENT_SOFT = mix(ACCENT, CARD, darkMode ? 0.18 : 0.10);
        GREEN_SOFT = mix(GREEN, CARD, 0.14);
        RED_SOFT = mix(RED, CARD, 0.14);
        ON_INK = CREAM;
        ON_INK_MUTED = mix(CREAM, INK, 0.72);

        MOSS = new Tint(0x4D694E);
        FERN = new Tint(0x2F7A55);
        OCHRE = new Tint(0xB07A1E);
        CLAY = new Tint(0xB5543A);
        SLATE = new Tint(0x4E6A80);

        categoryTints = Map.ofEntries(
                Map.entry("food", CLAY),
                Map.entry("transportation", SLATE),
                Map.entry("school", new Tint(0x7A5A8C)),
                Map.entry("entertainment", new Tint(0x5E7D2A)),
                Map.entry("shopping", OCHRE),
                Map.entry("bills", new Tint(0x2F7A78)),
                Map.entry("health", new Tint(0xA8486A)),
                Map.entry("other", new Tint(0x7A7062)),
                Map.entry("allowance", MOSS),
                Map.entry("other funds", new Tint(0x4F6F7F)),
                Map.entry("savings", FERN));

        installSwingDefaults();
    }

    /**
     * Colours for the parts Moni doesn't paint itself: message boxes, tooltips, menus and the
     * date spinner. These only affect components created after this runs.
     */
    private static void installSwingDefaults() {
        String[] backgrounds = {"Panel.background", "OptionPane.background", "CheckBox.background",
                "PopupMenu.background", "MenuItem.background", "Viewport.background"};
        for (String key : backgrounds) UIManager.put(key, CARD);
        String[] foregrounds = {"Label.foreground", "OptionPane.messageForeground", "CheckBox.foreground",
                "MenuItem.foreground", "ToolTip.foreground"};
        for (String key : foregrounds) UIManager.put(key, TEXT);
        UIManager.put("ToolTip.background", CARD);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("MenuItem.selectionBackground", ACCENT_SOFT);
        UIManager.put("MenuItem.selectionForeground", TEXT);
        UIManager.put("Separator.foreground", LINE);
        UIManager.put("ComboBox.selectionBackground", ACCENT_SOFT);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        for (String field : new String[]{"TextField", "FormattedTextField", "PasswordField"}) {
            UIManager.put(field + ".background", CARD);
            UIManager.put(field + ".foreground", TEXT);
            UIManager.put(field + ".caretForeground", TEXT);
            UIManager.put(field + ".selectionBackground", ACCENT_SOFT);
            UIManager.put(field + ".selectionForeground", TEXT);
        }
    }

    /** Blends two colours: amount 1 gives a, 0 gives b. */
    static Color mix(Color a, Color b, double amount) {
        double rest = 1 - amount;
        return new Color(
                (int) Math.round(a.getRed() * amount + b.getRed() * rest),
                (int) Math.round(a.getGreen() * amount + b.getGreen() * rest),
                (int) Math.round(a.getBlue() * amount + b.getBlue() * rest));
    }

    /** The same colour with transparency, e.g. for focus rings and scroll bar thumbs. */
    static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Colours for a category, so its icon, bar and tag match on every screen. */
    static Tint tintFor(String category) {
        String key = category == null ? "other" : category.trim().toLowerCase();
        return categoryTints.getOrDefault(key, categoryTints.get("other"));
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
        PRIMARY,    // filled olive, cream text
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
                    setForeground(ON_ACCENT);
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
                    fill = !isEnabled() ? mix(ACCENT, CARD, 0.45)
                            : m.isPressed() ? ACCENT_PRESSED : m.isRollover() ? ACCENT_HOVER : ACCENT;
                    break;
                case SECONDARY:
                    fill = m.isPressed() ? mix(ACCENT, CARD, 0.2) : m.isRollover() ? ACCENT_SOFT : CARD;
                    line = BORDER;
                    break;
                case SOFT:
                    fill = m.isPressed() ? mix(ACCENT, SOFT_BUTTON, 0.16)
                            : m.isRollover() ? mix(ACCENT, SOFT_BUTTON, 0.08) : SOFT_BUTTON;
                    line = SOFT_BUTTON_LINE;
                    break;
                case PLAIN:
                    if (m.isRollover() || m.isPressed()) fill = m.isPressed() ? CHIP_HOVER : CHIP;
                    break;
                case CIRCLE:
                    fill = m.isRollover() ? mix(CREAM, Color.WHITE, 0.4) : CREAM;
                    break;
                case CURRENT:
                    fill = ACCENT;
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
                g2.setColor(withAlpha(ACCENT, 120));
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

    /** Colour for a progress fraction: olive when fine, amber near the limit, red when over. */
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

    /** Moni's logo: a cream peso coin with an olive sign, the same in light and dark mode. */
    static Badge logo(int size) {
        return new Badge("\u20B1", size, CREAM, OLIVE);
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
            thumbColor = withAlpha(TEXT, 55);
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
            g2.setColor(isThumbRollover() ? withAlpha(TEXT, 90) : thumbColor);
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
