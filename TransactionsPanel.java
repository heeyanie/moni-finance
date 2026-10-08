package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The "Recent Transactions" card: search and filters, a table with In/Out and category tags,
 * and page buttons underneath. The dashboard, the Transactions page and the Savings page
 * each use their own copy.
 *
 * Filtering, sorting and paging all happen in memory on the list passed to {@link #setData},
 * so typing in the search box never queries the database.
 */
final class TransactionsPanel extends Theme.RoundedPanel {

    // Filter choices.
    static final String ALL_CATEGORIES = "All categories";
    static final String ALL_TYPES = "All types";
    static final String ALL_TIME = "All time";
    static final String TODAY = "Today";
    static final String THIS_WEEK = "This week";
    static final String THIS_MONTH = "This month";

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final int ROW_HEIGHT = 44;
    private static final int HEADER_HEIGHT = 42;
    private static final int MIN_ROWS = 3;

    private final DefaultTableModel model = newModel();
    private final JTable table = createTable(model);
    private final Theme.SearchField search = new Theme.SearchField("Search transactions...", null);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{ALL_TYPES, TransactionRecord.MONEY_IN, TransactionRecord.MONEY_OUT});
    private final JComboBox<String> periodFilter =
            new JComboBox<>(new String[]{ALL_TIME, TODAY, THIS_WEEK, THIS_MONTH});
    private final JPanel filters = new JPanel(new GridBagLayout());
    private final CardLayout views = new CardLayout();
    private final JPanel holder = new JPanel(views);
    private final JLabel emptyLabel = Theme.text(" ", Theme.BODY, Theme.MUTED);
    private final JLabel showing = Theme.text(" ", Theme.font(Font.PLAIN, 13), Theme.MUTED);
    private final JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JPanel footer = new JPanel(new BorderLayout(12, 0));

    private final String lockedCategory;
    private final boolean hasWelcome;
    private final String emptyMessage;

    private List<TransactionRecord> all = new ArrayList<>();
    private List<TransactionRecord> filtered = new ArrayList<>();
    private LocalDate today = LocalDate.now();
    private int page;
    private int pageSize = MIN_ROWS;
    private int sortColumn = -1;
    private boolean ascending;

    /**
     * @param title          card title, e.g. "Recent Transactions"
     * @param categories     the user's spending categories, for the Category filter
     * @param lockedCategory show only this category and hide the Category filter (or null)
     * @param headerAction   button shown on the right of the title (or null)
     * @param welcome        shown instead of the table before the first transaction (or null)
     * @param emptyMessage   shown when there are no transactions at all and there's no welcome
     */
    TransactionsPanel(String title, Icons.Name titleIcon, List<String> categories, String lockedCategory,
                      JComponent headerAction, JComponent welcome, String emptyMessage) {
        super(new BorderLayout(0, 12), 18, Theme.CARD, Theme.CARD_OUTLINE);
        this.lockedCategory = lockedCategory;
        this.hasWelcome = welcome != null;
        this.emptyMessage = emptyMessage;
        setBorder(new EmptyBorder(16, 20, 14, 20));

        // ---- title row
        JLabel titleLabel = Theme.text(title, Theme.font(Font.BOLD, 18), Theme.TEXT);
        titleLabel.setIcon(Icons.of(titleIcon, 24, Theme.TEXT));
        titleLabel.setIconTextGap(14);
        JPanel head = new JPanel(new BorderLayout(12, 0));
        head.setOpaque(false);
        head.add(titleLabel, BorderLayout.WEST);
        if (headerAction != null) head.add(headerAction, BorderLayout.EAST);

        // ---- filters
        categoryFilter.addItem(ALL_CATEGORIES);
        for (String c : categories) categoryFilter.addItem(c);
        filters.setOpaque(false);
        addFilter(0, null, search, 1.0, 110);
        int x = 1;
        if (lockedCategory == null) addFilter(x++, "Category", categoryFilter, 0.3, 150);
        addFilter(x++, "Type", typeFilter, 0.3, 112);
        addFilter(x, "Date", periodFilter, 0.3, 120);

        search.getDocument().addDocumentListener(Theme.onChange(this::firstPage));
        categoryFilter.addActionListener(e -> firstPage());
        typeFilter.addActionListener(e -> firstPage());
        periodFilter.addActionListener(e -> firstPage());

        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(filters, BorderLayout.CENTER);

        // ---- table, empty message and welcome guide share one spot
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        Theme.RoundedPanel empty = new Theme.RoundedPanel(new BorderLayout(), 14, Theme.CARD, Theme.CARD_OUTLINE);
        empty.add(emptyLabel, BorderLayout.CENTER);

        holder.setOpaque(false);
        holder.add(Theme.tableBox(table), "table");
        holder.add(empty, "empty");
        if (welcome != null) {
            Theme.RoundedPanel box = new Theme.RoundedPanel(new BorderLayout(), 14, Theme.CARD, Theme.CARD_OUTLINE);
            box.setBorder(new EmptyBorder(1, 1, 1, 1));
            box.add(welcome, BorderLayout.CENTER);
            holder.add(box, "welcome");
        }
        // Small preferred height: the card grows to fill the window and fits more rows per page.
        holder.setPreferredSize(new Dimension(200, HEADER_HEIGHT + ROW_HEIGHT * MIN_ROWS + 4));
        holder.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { fitRowsToHeight(); }
        });

        // ---- footer: "Showing 1 to 3 of 3 transactions" and page buttons
        pager.setOpaque(false);
        footer.setOpaque(false);
        footer.add(showing, BorderLayout.WEST);
        footer.add(pager, BorderLayout.EAST);

        // Click a column heading to sort all matching transactions, not just this page.
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column < 0) return;
                ascending = column == sortColumn ? !ascending : column != 0 && column != 4;
                sortColumn = column;
                table.putClientProperty(Theme.HeaderRenderer.SORT_COLUMN, sortColumn);
                table.putClientProperty(Theme.HeaderRenderer.SORT_ASCENDING, ascending);
                table.getTableHeader().repaint();
                firstPage();
            }
        });
        table.getTableHeader().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        table.getTableHeader().setToolTipText("Click a column to sort by it");

        add(north, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    // =====================================================================
    // Public API
    // =====================================================================

    /** Replaces the transactions (newest first) and redraws. */
    void setData(List<TransactionRecord> records, LocalDate appToday) {
        all = new ArrayList<>(records);
        today = appToday;
        render();
    }

    /** Used by the search box in the top bar. */
    void setSearchText(String text) {
        if (!Objects.equals(search.getText(), text)) search.setText(text);
    }

    // =====================================================================
    // Filtering, sorting and paging
    // =====================================================================

    private void firstPage() {
        page = 0;
        render();
    }

    private void fitRowsToHeight() {
        int rows = Math.max(MIN_ROWS, (holder.getHeight() - HEADER_HEIGHT - 4) / ROW_HEIGHT);
        if (rows != pageSize) {
            TransactionRecord firstShown = page * pageSize < filtered.size() ? filtered.get(page * pageSize) : null;
            pageSize = rows;
            // Stay on the page that contains the row that was at the top.
            page = firstShown == null ? 0 : filtered.indexOf(firstShown) / pageSize;
            render();
        }
    }

    private void render() {
        String text = search.getText().trim().toLowerCase();
        String category = lockedCategory != null ? lockedCategory : String.valueOf(categoryFilter.getSelectedItem());
        String type = String.valueOf(typeFilter.getSelectedItem());
        String period = String.valueOf(periodFilter.getSelectedItem());

        LocalDate start = null;
        if (TODAY.equals(period)) start = today;
        else if (THIS_WEEK.equals(period)) start = MoniDatabase.weekStart(today);
        else if (THIS_MONTH.equals(period)) start = MoniDatabase.monthStart(today);

        filtered = new ArrayList<>();
        for (TransactionRecord r : all) {
            LocalDate date = r.getDate();
            if (start != null && (date.isBefore(start) || date.isAfter(today))) continue;
            if (!ALL_CATEGORIES.equals(category) && !category.equals(r.getCategorySource())) continue;
            if (!ALL_TYPES.equals(type) && !type.equals(r.getFlowType())) continue;
            if (!text.isEmpty() && !(r.getCategorySource() + " " + r.getDescription()).toLowerCase().contains(text)) continue;
            filtered.add(r);
        }
        if (sortColumn >= 0) {
            Comparator<TransactionRecord> order = comparator(sortColumn);
            filtered.sort(ascending ? order : order.reversed());
        }

        int pages = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * pageSize;
        int to = Math.min(filtered.size(), from + pageSize);

        model.setRowCount(0);
        for (TransactionRecord r : filtered.subList(from, to)) addRow(model, r);

        showing.setText(filtered.isEmpty() ? " "
                : "Showing " + (from + 1) + " to " + to + " of " + filtered.size()
                  + (filtered.size() == 1 ? " transaction" : " transactions"));
        buildPager(pages);

        // "Nothing yet" means nothing this card could ever show (e.g. no savings moves on the Savings page).
        boolean nothingYet = true;
        for (TransactionRecord r : all) {
            if (lockedCategory == null || lockedCategory.equals(r.getCategorySource())) {
                nothingYet = false;
                break;
            }
        }
        filters.setVisible(!nothingYet); // nothing to filter yet
        footer.setVisible(!filtered.isEmpty());
        if (nothingYet && hasWelcome) {
            views.show(holder, "welcome"); // brand-new user: explain how Moni works
        } else if (filtered.isEmpty()) {
            emptyLabel.setText(nothingYet ? emptyMessage : "No transactions match these filters.");
            views.show(holder, "empty");
        } else {
            views.show(holder, "table");
        }
        revalidate();
        repaint();
    }

    private static Comparator<TransactionRecord> comparator(int column) {
        switch (column) {
            case 0: return Comparator.comparing(TransactionRecord::getDate);
            case 1: return Comparator.comparing(r -> Objects.toString(r.getFlowType(), ""));
            case 2: return Comparator.comparing(r -> Objects.toString(r.getCategorySource(), "").toLowerCase());
            case 3: return Comparator.comparing(r -> r.getDescription().toLowerCase());
            default: return Comparator.comparingDouble(TransactionRecord::getAmount);
        }
    }

    /** Previous / numbered / next buttons. At most five numbers are shown, around the current page. */
    private void buildPager(int pages) {
        pager.removeAll();
        Theme.FlatButton prev = pageButton(null, Icons.Name.CHEVRON_LEFT, page > 0, () -> goTo(page - 1));
        prev.setToolTipText("Previous page");
        pager.add(prev);

        int first = Math.max(0, Math.min(page - 2, pages - 5));
        int last = Math.min(pages - 1, first + 4);
        for (int p = first; p <= last; p++) {
            final int target = p;
            Theme.FlatButton b = pageButton(String.valueOf(p + 1), null, true, () -> goTo(target));
            if (p == page) b.setKind(Theme.ButtonKind.CURRENT);
            pager.add(b);
        }

        Theme.FlatButton next = pageButton(null, Icons.Name.CHEVRON_RIGHT, page < pages - 1, () -> goTo(page + 1));
        next.setToolTipText("Next page");
        pager.add(next);
    }

    private void goTo(int newPage) {
        page = newPage;
        render();
    }

    private static Theme.FlatButton pageButton(String text, Icons.Name icon, boolean enabled, Runnable action) {
        Theme.FlatButton b = new Theme.FlatButton(text,
                icon == null ? null : Icons.of(icon, 16, enabled ? Theme.TEXT : Theme.BORDER), Theme.ButtonKind.SECONDARY);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
        b.setRadius(10);
        Dimension d = new Dimension(34, 34);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setEnabled(enabled);
        if (enabled) b.addActionListener(e -> action.run());
        else b.setCursor(Cursor.getDefaultCursor());
        return b;
    }

    /** @param minWidth narrowest the input may get before the text in it is cut off */
    private void addFilter(int x, String label, JComponent input, double weight, int minWidth) {
        if (input instanceof JComboBox) Theme.styleCombo((JComboBox<?>) input);
        input.setPreferredSize(new Dimension(minWidth + 20, 38));
        input.setMinimumSize(new Dimension(minWidth, 38));

        JPanel cell = new JPanel(new BorderLayout(0, 5));
        cell.setOpaque(false);
        if (label != null) cell.add(Theme.text(label, Theme.font(Font.BOLD, 13), Theme.TEXT), BorderLayout.NORTH);
        cell.add(input, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = 0;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, x == 0 ? 0 : 12, 0, 0);
        filters.add(cell, c);
    }

    // =====================================================================
    // Table (also used by the weekly summary window)
    // =====================================================================

    static void addRow(DefaultTableModel model, TransactionRecord r) {
        model.addRow(new Object[]{r.getDate(), r.getFlowType(), r.getCategorySource(), r.getDescription(), r.getAmount()});
    }

    static DefaultTableModel newModel() {
        return new DefaultTableModel(new String[]{"Date", "Type", "Category", "Description", "Amount"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }

            @Override public Class<?> getColumnClass(int column) {
                if (column == 0) return LocalDate.class; // sorts by real date
                if (column == 4) return Double.class;    // sorts numerically
                return String.class;
            }
        };
    }

    static JTable createTable(DefaultTableModel model) {
        JTable table = Theme.table(model);
        TableColumnModel columns = table.getColumnModel();

        columns.getColumn(0).setCellRenderer(new Theme.CellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof LocalDate ? ((LocalDate) value).format(LONG_DATE) : String.valueOf(value));
            }
        });
        columns.getColumn(1).setCellRenderer(new PillRenderer(true));
        columns.getColumn(2).setCellRenderer(new PillRenderer(false));

        Theme.CellRenderer amount = new Theme.CellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof Number ? Theme.signedPeso(((Number) value).doubleValue()) : "");
            }

            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                     boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                boolean positive = value instanceof Number && ((Number) value).doubleValue() >= 0;
                setForeground(positive ? Theme.GREEN : Theme.RED);
                setFont(Theme.TABLE_BOLD);
                return this;
            }
        };
        amount.setHorizontalAlignment(SwingConstants.RIGHT);
        columns.getColumn(4).setCellRenderer(amount);

        // Minimum widths stop the short columns from being cut to "..."; Description takes the rest.
        int[] widths = {130, 80, 140, 260, 110};
        int[] minimum = {118, 74, 112, 120, 92};
        for (int i = 0; i < widths.length; i++) {
            columns.getColumn(i).setPreferredWidth(widths[i]);
            columns.getColumn(i).setMinWidth(minimum[i]);
        }
        return table;
    }

    /** Shows the Type ("In" / "Out") or the category as a coloured tag. */
    private static final class PillRenderer extends JPanel implements TableCellRenderer {
        private final boolean typeColumn;
        private final Theme.Pill pill = new Theme.Pill(" ", Theme.ACCENT_SOFT, Theme.TEXT);

        PillRenderer(boolean typeColumn) {
            super(new GridBagLayout());
            this.typeColumn = typeColumn;
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 14, 0, 6);
            add(pill, c);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            String text = String.valueOf(value);
            if (typeColumn) {
                boolean in = TransactionRecord.MONEY_IN.equals(value);
                pill.set(in ? "In" : TransactionRecord.MONEY_OUT.equals(value) ? "Out" : text,
                        in ? Theme.GREEN_SOFT : Theme.RED_SOFT, in ? Theme.GREEN : Theme.RED);
            } else {
                Theme.Tint tint = Theme.tintFor(text);
                pill.set(text, tint.soft, tint.ink);
            }
            setBackground(selected ? table.getSelectionBackground() : Theme.CARD);
            return this;
        }
    }
}