package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Moni window: a sidebar with the pages, a top bar (search, date, notifications, account)
 * and the current page. The dashboard shows today's spending plan, balances, recent transactions
 * and weekly category budgets.
 */
public class MainApp extends JFrame {

    private static final int RECENT_TRANSACTION_LIMIT = 300;
    // Spending at 80% of a limit or more counts as "close to the limit".
    private static final double NEAR_LIMIT = 0.8;

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter RANGE_DATE = DateTimeFormatter.ofPattern("EEE, MMM d");

    private static final String MONEY_IN = TransactionRecord.MONEY_IN;
    private static final String MONEY_OUT = TransactionRecord.MONEY_OUT;
    private static final String SAVINGS = TransactionRecord.SAVINGS;
    private static final String ALLOWANCE = TransactionRecord.ALLOWANCE;
    private static final String OTHER_FUNDS = TransactionRecord.OTHER_FUNDS;

    // Shown on the welcome card and in the "How Moni works" guide.
    private static final String[][] HOW_IT_WORKS = {
            {"Add your allowance",
             "Whenever you receive your allowance, use Add money so Moni knows what you have."},
            {"Record what you spend",
             "Each time you buy something, add it as an expense and pick a category such as Food."},
            {"Check what's left today",
             "Moni shares your weekly limit across the days left in the week, so you always know "
                     + "how much you can still spend today."}
    };

    private enum Page {
        DASHBOARD("Dashboard", Icons.Name.HOME),
        TRANSACTIONS("Transactions", Icons.Name.LIST),
        BUDGET("Budget Plan", Icons.Name.PIE),
        SAVINGS("Savings", Icons.Name.PIGGY),
        REPORTS("Reports", Icons.Name.BARS),
        SETTINGS("Settings", Icons.Name.GEAR);

        final String title;
        final Icons.Name icon;

        Page(String title, Icons.Name icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    /** One message in the notifications pop-up. */
    private static final class Alert {
        final Color color;
        final String message;

        Alert(Color color, String message) {
            this.color = color;
            this.message = message;
        }
    }

    private final User currentUser;
    private UserSettings settings;

    // The day Moni treats as "today". The user can change it from the date button in the top bar
    // to look at (or add transactions on) another day.
    private LocalDate selectedDate = LocalDate.now();

    // Loaded from the database by reloadData(). The totals below are worked out from these.
    private double walletBalance;
    private double savingsBalance;
    private double monthSpent;
    private List<TransactionRecord> weekTransactions = new ArrayList<>();
    private List<TransactionRecord> recentTransactions = new ArrayList<>();

    private Page currentPage = Page.DASHBOARD;
    private boolean showMonth; // the dashboard's "This Week" / "This Month" choice

    // Filled again every time rebuild() runs. Each widget that shows data adds a refresher
    // that redraws it from the fields above, so reloadData() can just run them all.
    private final Map<Page, NavItem> navItems = new EnumMap<>(Page.class);
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<TransactionsPanel> transactionPanels = new ArrayList<>();
    private CardLayout pageCards;
    private JPanel pageHolder;
    private TransactionsPanel transactionsPage;
    private Theme.SearchField topSearch;

    public MainApp(User user, UserSettings settings) {
        super("Moni — Student Money Manager");
        currentUser = user;
        this.settings = settings;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1360, screen.width), Math.min(880, screen.height));
        setMinimumSize(new Dimension(Math.min(1080, screen.width), Math.min(680, screen.height)));
        setLocationRelativeTo(null);
        if (screen.width < 1280 || screen.height < 760) setExtendedState(JFrame.MAXIMIZED_BOTH);

        rebuild();
    }

    private LocalDate today() {
        return selectedDate;
    }

    // =====================================================================
    // Window setup
    // =====================================================================

    /** Builds the whole window from the current settings, then loads data into it. */
    private void rebuild() {
        navItems.clear();
        refreshers.clear();
        transactionPanels.clear();

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(buildTopBar(), BorderLayout.NORTH);

        pageCards = new CardLayout();
        pageHolder = new JPanel(pageCards);
        pageHolder.setBackground(Theme.BG);
        pageHolder.add(pageScroll(buildDashboardPage()), Page.DASHBOARD.name());
        pageHolder.add(pageScroll(buildTransactionsPage()), Page.TRANSACTIONS.name());
        pageHolder.add(pageScroll(buildBudgetPage()), Page.BUDGET.name());
        pageHolder.add(pageScroll(buildSavingsPage()), Page.SAVINGS.name());
        pageHolder.add(pageScroll(buildReportsPage()), Page.REPORTS.name());
        pageHolder.add(pageScroll(buildSettingsPage()), Page.SETTINGS.name());
        main.add(pageHolder, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
        installShortcuts(root);

        showPage(currentPage);
        reloadData();
        revalidate();
        repaint();
        navItems.get(currentPage).requestFocusInWindow();
    }

    private void showPage(Page page) {
        currentPage = page;
        pageCards.show(pageHolder, page.name());
        for (Map.Entry<Page, NavItem> e : navItems.entrySet()) e.getValue().setActive(e.getKey() == page);
    }

    /** Ctrl + K jumps to the search box; Esc in the search box clears it. */
    private void installShortcuts(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "moni.search");
        root.getActionMap().put("moni.search", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                topSearch.requestFocusInWindow();
                topSearch.selectAll();
            }
        });
        topSearch.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "moni.clear");
        topSearch.getActionMap().put("moni.clear", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                topSearch.setText("");
            }
        });
    }

    private static JScrollPane pageScroll(JComponent page) {
        JScrollPane scroll = new JScrollPane(page,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        Theme.thinScrollBars(scroll);
        return scroll;
    }

    // =====================================================================
    // Sidebar
    // =====================================================================

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.INK, 0, getHeight(), Theme.INK_DEEP));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        side.setPreferredSize(new Dimension(248, 0));
        side.setBorder(new EmptyBorder(24, 16, 18, 16));

        JPanel top = verticalPanel();

        JPanel words = verticalPanel();
        Theme.stack(words, 0, Theme.text("MONI", Theme.font(Font.BOLD, 30), Theme.ON_INK));
        Theme.stack(words, 0, Theme.text("Student Money Manager", Theme.font(Font.PLAIN, 12), Theme.ON_INK_MUTED));
        JPanel brand = Theme.row(12, Theme.logo(50), words);
        brand.setBorder(new EmptyBorder(0, 6, 0, 0));
        fixHeight(brand);
        Theme.stack(top, 0, brand);

        for (Page p : Page.values()) {
            NavItem item = new NavItem(p);
            navItems.put(p, item);
            Theme.stack(top, p.ordinal() == 0 ? 32 : 10, item);
        }

        side.add(top, BorderLayout.NORTH);
        side.add(buildTipCard(), BorderLayout.SOUTH);
        return side;
    }

    /** "Make your allowance last" card at the bottom of the sidebar. It opens the guide. */
    private JComponent buildTipCard() {
        Icon big = Icons.of(Icons.Name.SPROUT, 64, Theme.LEAF);
        Icon small = Icons.of(Icons.Name.SPROUT, 34, Theme.LEAF);
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(), 22, Theme.TIP_CARD, null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                big.paintIcon(this, g, 4, getHeight() - 70);
                small.paintIcon(this, g, getWidth() - 46, 14);
            }
        };
        card.setBorder(new EmptyBorder(24, 38, 14, 14));
        card.setPreferredSize(new Dimension(0, 150));

        JPanel words = verticalPanel();
        Theme.stack(words, 0, Theme.text("Make your", Theme.font(Font.BOLD, 15), Theme.ON_INK));
        Theme.stack(words, 2, Theme.text("allowance last", Theme.font(Font.BOLD, 15), Theme.ON_INK));

        Theme.FlatButton go = new Theme.FlatButton(null, Icons.of(Icons.Name.CHEVRON_RIGHT, 18, Theme.OLIVE),
                Theme.ButtonKind.CIRCLE);
        go.setBorder(new EmptyBorder(0, 0, 0, 0));
        go.setPreferredSize(new Dimension(40, 40));
        go.setToolTipText("How Moni works");
        go.addActionListener(e -> showHowItWorks());
        JPanel south = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.add(go);

        card.add(words, BorderLayout.NORTH);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    /** One sidebar item. The current page gets a lighter olive pill. */
    private final class NavItem extends JButton {
        private boolean active;

        NavItem(Page page) {
            super(page.title, Icons.of(page.icon, 22, Theme.ON_INK));
            setFont(Theme.NAV);
            setForeground(Theme.ON_INK);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(18);
            setBorder(new EmptyBorder(11, 18, 11, 12));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
            addActionListener(e -> showPage(page));
        }

        void setActive(boolean value) {
            active = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(Theme.NAV_SELECTED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            } else if (getModel().isRollover()) {
                g2.setColor(Theme.withAlpha(Theme.ON_INK, 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            }
            if (isFocusOwner() && !active) {
                g2.setColor(Theme.withAlpha(Theme.ON_INK, 150));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =====================================================================
    // Top bar
    // =====================================================================

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.CARD);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.LINE);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        bar.setBackground(Theme.CARD); // the search box's rounded corners blend into this
        bar.setBorder(new EmptyBorder(14, 28, 14, 24));

        topSearch = new Theme.SearchField("Search transactions, categories...", "Ctrl + K");
        topSearch.setPreferredSize(new Dimension(400, 44));
        topSearch.setMinimumSize(new Dimension(220, 44));
        topSearch.setToolTipText("Search all your transactions (Ctrl + K)");
        topSearch.getDocument().addDocumentListener(Theme.onChange(this::searchEverywhere));

        Theme.FlatButton date = new Theme.FlatButton(today().format(LONG_DATE),
                Icons.of(Icons.Name.CALENDAR, 22, Theme.TEXT), Theme.ButtonKind.PLAIN);
        date.setFont(Theme.font(Font.BOLD, 14));
        date.setIconTextGap(12);
        date.setTrailingIcon(Icons.of(Icons.Name.CHEVRON_DOWN, 16, Theme.TEXT));
        date.setToolTipText("Choose which day Moni treats as today");
        date.addActionListener(e -> changeDate());

        Theme.FlatButton mode = new Theme.FlatButton(null,
                Icons.of(Theme.isDarkMode() ? Icons.Name.SUN : Icons.Name.MOON, 22, Theme.TEXT), Theme.ButtonKind.PLAIN);
        mode.setBorder(new EmptyBorder(0, 0, 0, 0));
        mode.setPreferredSize(new Dimension(44, 44));
        mode.setToolTipText(Theme.isDarkMode() ? "Switch to light mode" : "Switch to dark mode");
        mode.addActionListener(e -> toggleDarkMode());

        Theme.FlatButton bell = new Theme.FlatButton(null, Icons.of(Icons.Name.BELL, 24, Theme.TEXT),
                Theme.ButtonKind.PLAIN);
        bell.setBorder(new EmptyBorder(0, 0, 0, 0));
        bell.setPreferredSize(new Dimension(44, 44));
        bell.setToolTipText("Notifications");
        bell.addActionListener(e -> showNotifications(bell));
        refreshers.add(() -> bell.setDot(!alerts().isEmpty()));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        bar.add(topSearch, c);
        c.weightx = 0;
        c.gridx = 1;
        bar.add(date, c);
        c.gridx = 2;
        c.insets = new Insets(0, 14, 0, 0);
        bar.add(mode, c);
        c.gridx = 3;
        c.insets = new Insets(0, 4, 0, 0);
        bar.add(bell, c);
        c.gridx = 4;
        c.insets = new Insets(0, 20, 0, 0);
        bar.add(buildUserChip(), c);
        return bar;
    }

    private JComponent buildUserChip() {
        Theme.RoundedPanel chip = new Theme.RoundedPanel(new BorderLayout(12, 0), 18, Theme.CHIP, null);
        chip.setBorder(new EmptyBorder(7, 8, 7, 14));
        chip.add(new JLabel(Icons.avatar(initial(currentUser.getFullName()), 40,
                Theme.ACCENT, Theme.ON_ACCENT)), BorderLayout.WEST);

        JPanel words = verticalPanel();
        words.add(Box.createVerticalGlue());
        Theme.stack(words, 0, Theme.text("Hi, " + firstName(currentUser.getFullName()),
                Theme.font(Font.BOLD, 14), Theme.TEXT));
        Theme.stack(words, 1, Theme.text("Student", Theme.font(Font.PLAIN, 12), Theme.MUTED));
        words.add(Box.createVerticalGlue());
        chip.add(words, BorderLayout.CENTER);
        chip.add(new JLabel(Icons.of(Icons.Name.CHEVRON_DOWN, 16, Theme.TEXT)), BorderLayout.EAST);

        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText("Your account");
        chip.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { chip.setBackground(Theme.CHIP_HOVER); }
            @Override public void mouseExited(MouseEvent e) { chip.setBackground(Theme.CHIP); }
            @Override public void mousePressed(MouseEvent e) { showUserMenu(chip); }
        });
        return chip;
    }

    /** Typing in the top search box shows the Transactions page, filtered as you type. */
    private void searchEverywhere() {
        if (transactionsPage == null) return;
        String text = topSearch.getText();
        transactionsPage.setSearchText(text);
        if (!text.trim().isEmpty() && currentPage != Page.TRANSACTIONS) showPage(Page.TRANSACTIONS);
    }

    private void showUserMenu(JComponent anchor) {
        JPopupMenu menu = Theme.menu();
        menu.add(Theme.menuItem("How Moni works", Icons.Name.INFO, this::showHowItWorks));
        menu.add(Theme.menuItem("Edit plan", Icons.Name.SLIDERS, this::customizeDashboard));
        menu.add(Theme.menuItem("Change date", Icons.Name.CALENDAR, this::changeDate));
        menu.addSeparator();
        menu.add(Theme.menuItem("Log out", Icons.Name.LOGOUT, this::logout));
        menu.show(anchor, anchor.getWidth() - menu.getPreferredSize().width, anchor.getHeight() + 6);
    }

    private void showNotifications(JComponent anchor) {
        JPanel list = verticalPanel();
        list.setBorder(new EmptyBorder(8, 16, 10, 16));
        Theme.stack(list, 0, Theme.text("Notifications", Theme.font(Font.BOLD, 15), Theme.TEXT));

        List<Alert> alerts = alerts();
        if (alerts.isEmpty()) {
            Theme.stack(list, 10, htmlText("You're all caught up. Moni will let you know here when "
                    + "you get close to a limit.", Theme.MUTED));
        }
        for (Alert alert : alerts) {
            JLabel mark = new JLabel(new DotIcon(alert.color));
            mark.setVerticalAlignment(SwingConstants.TOP);
            mark.setBorder(new EmptyBorder(5, 0, 0, 0));
            JPanel row = transparentPanel(new BorderLayout(10, 0));
            row.add(mark, BorderLayout.WEST);
            row.add(htmlText(alert.message, Theme.TEXT), BorderLayout.CENTER);
            Theme.stack(list, 12, row);
        }

        JPopupMenu menu = Theme.menu();
        menu.add(list);
        menu.show(anchor, anchor.getWidth() - menu.getPreferredSize().width, anchor.getHeight() + 6);
    }

    /** Wrapped text for pop-ups, where the width has to be known before layout. */
    private static JLabel htmlText(String text, Color color) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return Theme.text("<html><div style='width:250px'>" + safe + "</div></html>", Theme.BODY, color);
    }

    private static final class DotIcon implements Icon {
        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, 8, 8);
            g2.dispose();
        }

        @Override public int getIconWidth() { return 8; }
        @Override public int getIconHeight() { return 8; }
    }

    // =====================================================================
    // Shared page pieces
    // =====================================================================

    /** Title and subtitle at the top of a page, an optional control on the right, then the body. */
    private static JComponent pageShell(String title, String subtitle, JComponent right, JComponent body) {
        Theme.Page page = new Theme.Page(new BorderLayout(0, 18));
        page.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel titles = verticalPanel();
        Theme.stack(titles, 0, Theme.text(title, Theme.TITLE, Theme.TEXT));
        Theme.stack(titles, 0, Theme.text(subtitle, Theme.font(Font.PLAIN, 16), Theme.MUTED));

        JPanel head = transparentPanel(new BorderLayout(16, 0));
        head.add(titles, BorderLayout.CENTER);
        if (right != null) head.add(centred(right), BorderLayout.EAST);

        page.add(head, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    /** A tinted card: icon square, title and big number, then optional bar and caption underneath. */
    private static Theme.RoundedPanel statCard(Theme.Tint tint, Icons.Name icon, JLabel title, JLabel value,
                                               JComponent... below) {
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(0, 10), 18, tint.fill, tint.outline);
        card.setBorder(new EmptyBorder(16, 18, 14, 18));

        JPanel words = verticalPanel();
        Theme.stack(words, 1, title);
        Theme.stack(words, 0, value);

        JPanel head = transparentPanel(new BorderLayout(16, 0));
        head.add(alignTop(new Theme.IconBox(Icons.of(icon, 26, tint.ink), 54, tint.box, 14)), BorderLayout.WEST);
        head.add(words, BorderLayout.CENTER);

        JPanel foot = verticalPanel();
        for (int i = 0; i < below.length; i++) Theme.stack(foot, i == 0 ? 0 : 8, below[i]);

        card.add(head, BorderLayout.NORTH);
        card.add(foot, BorderLayout.CENTER);
        return card;
    }

    private static JLabel statTitle(String text) {
        return new Theme.FitLabel(text, Theme.font(Font.BOLD, 14), Theme.TEXT);
    }

    private static Theme.FitLabel statValue() {
        return new Theme.FitLabel(Theme.peso(0), Theme.STAT, Theme.TEXT);
    }

    private static Theme.FitLabel fixedValue(String text) {
        Theme.FitLabel label = statValue();
        label.setText(text);
        return label;
    }

    private static Theme.WrapText caption(String text) {
        return new Theme.WrapText(text, Theme.font(Font.PLAIN, 14), Theme.TEXT_SOFT);
    }

    private static JLabel cardTitle(String text, Icons.Name icon) {
        JLabel title = Theme.text(text, Theme.font(Font.BOLD, 18), Theme.TEXT);
        title.setIcon(Icons.of(icon, 24, Theme.TEXT));
        title.setIconTextGap(14);
        return title;
    }

    /** Card title with a smaller line of text underneath, lined up with the title text. */
    private static JPanel cardTitle(String text, Icons.Name icon, String note, Color noteColor) {
        JLabel noteLabel = Theme.text(note, Theme.font(Font.PLAIN, 13), noteColor);
        noteLabel.setBorder(new EmptyBorder(0, 38, 0, 0));
        JPanel head = verticalPanel();
        Theme.stack(head, 0, cardTitle(text, icon));
        Theme.stack(head, 2, noteLabel);
        return head;
    }

    // =====================================================================
    // Dashboard
    // =====================================================================

    private JComponent buildDashboardPage() {
        JPanel body = transparentPanel(new BorderLayout(0, 18));

        JPanel stats = transparentPanel(new GridLayout(1, 0, 16, 0));
        if (settings.isShowWallet()) stats.add(walletCard());
        if (settings.isShowSavings()) stats.add(savingsCard());
        if (settings.isShowWeekly()) stats.add(spentCard());
        if (settings.isShowDaily()) stats.add(todayCard());
        if (stats.getComponentCount() > 0) body.add(stats, BorderLayout.NORTH);
        body.add(dashboardBottom(), BorderLayout.CENTER);

        // The week/month switch only matters for the "Spent this week" card.
        JComponent periodButton = settings.isShowWeekly() ? buildPeriodButton() : null;
        return pageShell("Dashboard", "Overview of your finances", periodButton, body);
    }

    private JComponent buildPeriodButton() {
        Theme.FlatButton button = new Theme.FlatButton(showMonth ? "This Month" : "This Week",
                Icons.of(Icons.Name.CALENDAR, 20, Theme.TEXT), Theme.ButtonKind.SECONDARY);
        button.setFont(Theme.font(Font.PLAIN, 15));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(12);
        button.setBorder(new EmptyBorder(11, 16, 11, 16));
        button.setTrailingIcon(Icons.of(Icons.Name.CHEVRON_DOWN, 16, Theme.TEXT));
        button.setPreferredSize(new Dimension(200, button.getPreferredSize().height));
        button.setToolTipText("Show spending for this week or this month");
        button.addActionListener(e -> showPeriodMenu(button));
        return button;
    }

    private void showPeriodMenu(Theme.FlatButton anchor) {
        JPopupMenu menu = Theme.menu();
        menu.add(Theme.menuItem("This Week", null, () -> setMonthView(false, anchor)));
        menu.add(Theme.menuItem("This Month", null, () -> setMonthView(true, anchor)));
        menu.setPreferredSize(new Dimension(anchor.getWidth(), menu.getPreferredSize().height));
        menu.show(anchor, 0, anchor.getHeight() + 4);
    }

    private void setMonthView(boolean month, Theme.FlatButton button) {
        showMonth = month;
        button.setText(month ? "This Month" : "This Week");
        runRefreshers();
    }

    /** Transactions and quick actions on the left, weekly budgets on the right. */
    private JComponent dashboardBottom() {
        boolean showTransactions = settings.isShowTransactions();
        boolean showBudgets = settings.isShowBudget();
        boolean showActions = settings.isShowQuickActions();
        if (!showTransactions && !showBudgets && !showActions) {
            return alignTop(Theme.text("Use Budget Plan › Edit plan to choose what Moni shows here.",
                    Theme.BODY, Theme.MUTED));
        }

        JPanel left = transparentPanel(new BorderLayout(0, 14));
        if (showTransactions) left.add(dashboardTransactions(), BorderLayout.CENTER);
        if (showActions) left.add(buildActions(), showTransactions ? BorderLayout.SOUTH : BorderLayout.NORTH);
        if (!showBudgets) return left;

        JComponent budgetCard = buildBudgetCard();
        if (!showTransactions && !showActions) return budgetCard;

        JPanel row = transparentPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.66;
        row.add(withWidth(left, 470), c);
        c.gridx = 1;
        c.weightx = 0.34;
        c.insets = new Insets(0, 16, 0, 0);
        row.add(withWidth(budgetCard, 250), c);
        return row;
    }

    private JComponent dashboardTransactions() {
        TransactionsPanel panel = new TransactionsPanel("Recent Transactions", Icons.Name.LIST, filterCategories(),
                null, summaryButton(), buildWelcome(), "No transactions yet.");
        transactionPanels.add(panel);
        return panel;
    }

    private JPanel buildActions() {
        JPanel row = transparentPanel(new GridLayout(1, 3, 14, 0));
        row.add(actionButton("+  Add expense", Icons.Name.RECEIPT, Theme.ButtonKind.PRIMARY, Theme.ON_ACCENT,
                "Record money you spent", this::expenseDialog));
        row.add(actionButton("+  Add money", Icons.Name.CASH, Theme.ButtonKind.SOFT, Theme.TEXT,
                "Add your allowance or other money you received", this::fundsDialog));
        row.add(actionButton("Move to savings", Icons.Name.PIGGY_LINE, Theme.ButtonKind.SOFT, Theme.TEXT,
                "Move money from your wallet into savings", this::savingsDialog));
        return row;
    }

    // ---- the four cards at the top ----

    private JPanel walletCard() {
        return statCard(Theme.MOSS, Icons.Name.WALLET, statTitle("Wallet"), walletValue(), caption(allowanceSummary()));
    }

    private JPanel savingsCard() {
        Theme.FitLabel value = statValue();
        refreshers.add(() -> value.setText(Theme.peso(savingsBalance)));
        return statCard(Theme.FERN, Icons.Name.PIGGY_LINE, statTitle("Savings"), value,
                caption("Set aside from your wallet"));
    }

    /** Big number that always shows the current wallet balance. */
    private Theme.FitLabel walletValue() {
        Theme.FitLabel value = statValue();
        refreshers.add(() -> value.setText(Theme.peso(walletBalance)));
        return value;
    }

    /** "Spent this week" (or this month, from the drop-down above the cards). */
    private JPanel spentCard() {
        JLabel title = statTitle("Spent this week");
        Theme.FitLabel value = statValue();
        Theme.Bar bar = new Theme.Bar(8);
        Theme.WrapText caption = caption(" ");
        refreshers.add(() -> {
            double limit = showMonth ? monthlyPace() : settings.getWeeklyLimit();
            double spent = showMonth ? monthSpent : spentThisWeek();
            double fraction = limit > 0 ? spent / limit : 0;
            title.setText(showMonth ? "Spent this month" : "Spent this week");
            value.setText(Theme.peso(spent));
            bar.set(fraction, Theme.progressColor(fraction));
            if (limit <= 0) caption.setText("No weekly limit set yet");
            else if (showMonth) caption.setText("of about " + Theme.peso(limit) + " at your weekly limit");
            else caption.setText("of your " + Theme.peso(limit) + " weekly limit");
        });
        return statCard(Theme.OCHRE, Icons.Name.SWAP, title, value, bar, caption);
    }

    /** The app's main question: how much can I still spend today? */
    private JPanel todayCard() {
        Theme.FitLabel value = statValue();
        Theme.WrapText status = caption(" ");
        Theme.RoundedPanel card = statCard(Theme.CLAY, Icons.Name.RECEIPT, statTitle("Left to spend today"),
                value, status);
        refreshers.add(() -> {
            double plan = todayPlan();
            double spent = spentOn(today());
            double fraction = usedFraction(spent, plan);
            value.setText(Theme.peso(Math.max(0, plan - spent)));
            card.setToolTipText(Theme.peso(spent) + " of " + Theme.peso(plan) + " spent today");

            status.setForeground(Theme.TEXT_SOFT);
            if (spent == 0) {
                status.setText("Nothing spent yet today.");
            } else if (fraction < 0.5) {
                status.setText("You're well within today's plan.");
            } else if (fraction < 1) {
                status.setText("You're within today's plan.");
            } else {
                status.setText(spent > plan
                        ? "You're " + Theme.peso(spent - plan) + " over today's plan."
                        : "You've used up today's plan.");
                status.setForeground(Theme.RED);
            }
        });
        return card;
    }

    // ---- weekly budgets card (also on the Budget Plan page) ----

    private JComponent buildBudgetCard() {
        Theme.RoundedPanel card = Theme.card(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(18, 20, 16, 10));

        Theme.Column list = new Theme.Column();
        list.setBorder(new EmptyBorder(0, 0, 0, 12));
        JScrollPane scroll = Theme.scroll(list);
        scroll.setPreferredSize(new Dimension(220, 160));
        refreshers.add(() -> fillBudgets(list));

        card.add(cardTitle("Weekly Budgets", Icons.Name.PIE, weekRange(today()), Theme.TEXT_SOFT), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void fillBudgets(Theme.Column list) {
        list.clearRows();
        Map<String, Double> limits = settings.getCategoryLimits();
        if (limits.isEmpty()) {
            list.addRow(new Theme.WrapText("No category budgets yet. Add some in Budget Plan.",
                    Theme.BODY, Theme.MUTED), 0);
        }
        boolean first = true;
        for (Map.Entry<String, Double> entry : limits.entrySet()) {
            String category = entry.getKey();
            list.addRow(budgetRow(category, spentThisWeek(category), entry.getValue()), first ? 0 : 20);
            first = false;
        }
        list.revalidate();
        list.repaint();
    }

    private static JPanel budgetRow(String category, double spent, double limit) {
        Theme.Tint tint = Theme.tintFor(category);
        boolean over = spent > limit;

        JPanel head = transparentPanel(new BorderLayout(8, 0));
        head.add(Theme.text(category, Theme.font(Font.BOLD, 15), Theme.TEXT), BorderLayout.WEST);
        head.add(Theme.text(Theme.peso(spent) + " / " + Theme.peso(limit), Theme.font(Font.PLAIN, 14), Theme.TEXT),
                BorderLayout.EAST);

        Theme.Bar bar = new Theme.Bar(8);
        bar.set(usedFraction(spent, limit), over ? Theme.RED : tint.ink);

        JLabel note = over
                ? Theme.text(Theme.peso(spent - limit) + " over budget", Theme.font(Font.PLAIN, 13), Theme.RED)
                : Theme.text(Theme.peso(limit - spent) + " left", Theme.font(Font.PLAIN, 13), Theme.TEXT_SOFT);

        JPanel words = transparentPanel(new BorderLayout(0, 8));
        words.add(head, BorderLayout.NORTH);
        words.add(bar, BorderLayout.CENTER);
        words.add(note, BorderLayout.SOUTH);

        JPanel row = transparentPanel(new BorderLayout(16, 0));
        row.add(alignTop(new Theme.IconBox(Icons.of(Icons.forCategory(category), 24, tint.ink), 48, tint.box, 12)),
                BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        return row;
    }

    // ---- welcome guide, shown instead of the table until the first transaction ----

    private JComponent buildWelcome() {
        Theme.Column col = new Theme.Column();
        col.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel title = transparentPanel(new BorderLayout(12, 0));
        title.add(Theme.text("Welcome! Here's how Moni works", Theme.H2, Theme.TEXT), BorderLayout.CENTER);
        title.add(button("Add your allowance", Theme.ButtonKind.PRIMARY, this::fundsDialog), BorderLayout.EAST);
        col.addRow(title, 0);
        addSteps(col, 16);
        return Theme.scroll(col);
    }

    private static void addSteps(Theme.Column col, int firstGap) {
        for (int i = 0; i < HOW_IT_WORKS.length; i++) {
            JPanel words = verticalPanel();
            Theme.stack(words, 3, Theme.text(HOW_IT_WORKS[i][0], Theme.BODY_BOLD, Theme.TEXT));
            Theme.stack(words, 2, new Theme.WrapText(HOW_IT_WORKS[i][1], Theme.BODY, Theme.MUTED));

            JPanel row = transparentPanel(new BorderLayout(12, 0));
            row.add(alignTop(new Theme.Badge(String.valueOf(i + 1), 26, Theme.ACCENT_SOFT, Theme.ACCENT)),
                    BorderLayout.WEST);
            row.add(words, BorderLayout.CENTER);
            col.addRow(row, i == 0 ? firstGap : 12);
        }
    }

    // =====================================================================
    // Transactions, Budget Plan and Savings pages
    // =====================================================================

    private JComponent buildTransactionsPage() {
        transactionsPage = new TransactionsPanel("All transactions", Icons.Name.LIST, filterCategories(), null,
                summaryButton(), null, "No transactions yet. Use Add money to record your allowance.");
        transactionPanels.add(transactionsPage);

        JPanel actions = Theme.row(10,
                smallAction("+  Add expense", Icons.Name.RECEIPT, Theme.ButtonKind.PRIMARY, Theme.ON_ACCENT, this::expenseDialog),
                smallAction("+  Add money", Icons.Name.CASH, Theme.ButtonKind.SOFT, Theme.TEXT, this::fundsDialog));
        return pageShell("Transactions", "Every peso in and out of your wallet", actions, transactionsPage);
    }

    private JComponent buildBudgetPage() {
        String frequency = settings.getAllowanceFrequency().toLowerCase();
        JPanel cards = transparentPanel(new GridLayout(1, 3, 16, 0));
        cards.add(statCard(Theme.MOSS, Icons.Name.COINS, statTitle("Allowance"),
                fixedValue(Theme.peso(settings.getAllowanceAmount())),
                caption("Received " + frequency + ", about " + Theme.peso(settings.getDailyAllowance()) + " a day")));
        cards.add(statCard(Theme.CLAY, Icons.Name.RECEIPT, statTitle("Daily limit"),
                fixedValue(Theme.peso(settings.getDailyLimit())),
                caption("The most you plan to spend in one day")));
        cards.add(statCard(Theme.OCHRE, Icons.Name.CALENDAR, statTitle("Weekly limit"),
                fixedValue(Theme.peso(settings.getWeeklyLimit())),
                caption("Shared across the days left in each week")));

        JPanel body = transparentPanel(new BorderLayout(0, 20));
        body.add(cards, BorderLayout.NORTH);
        body.add(buildBudgetCard(), BorderLayout.CENTER);

        Theme.FlatButton edit = smallAction("Edit plan", Icons.Name.SLIDERS, Theme.ButtonKind.PRIMARY, Theme.ON_ACCENT,
                this::customizeDashboard);
        edit.setToolTipText("Change your allowance, limits, categories and dashboard sections");
        return pageShell("Budget Plan", "Your allowance, spending limits and weekly category budgets", edit, body);
    }

    private JComponent buildSavingsPage() {
        JPanel cards = transparentPanel(new GridLayout(1, 2, 16, 0));
        cards.add(savingsCard());
        cards.add(statCard(Theme.MOSS, Icons.Name.WALLET, statTitle("Wallet"), walletValue(),
                caption("What you can still spend or move to savings")));

        TransactionsPanel history = new TransactionsPanel("Savings history", Icons.Name.PIGGY_LINE,
                filterCategories(), SAVINGS, null, null,
                "Nothing moved to savings yet. Use Move to savings to start.");
        transactionPanels.add(history);

        JPanel body = transparentPanel(new BorderLayout(0, 20));
        body.add(cards, BorderLayout.NORTH);
        body.add(history, BorderLayout.CENTER);

        Theme.FlatButton move = smallAction("Move to savings", Icons.Name.PIGGY_LINE, Theme.ButtonKind.PRIMARY,
                Theme.ON_ACCENT, this::savingsDialog);
        return pageShell("Savings", "Money you've set aside from your wallet", move, body);
    }

    // =====================================================================
    // Reports page
    // =====================================================================

    private JComponent buildReportsPage() {
        JPanel lower = transparentPanel(new GridLayout(1, 2, 16, 0));
        lower.add(buildDailyChartCard());
        lower.add(buildCategoryReportCard());

        JPanel body = transparentPanel(new BorderLayout(0, 20));
        body.add(buildReportTotals(), BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);

        Theme.FlatButton summary = smallAction("This week's summary", Icons.Name.LIST, Theme.ButtonKind.SECONDARY,
                Theme.TEXT, this::showWeeklySummary);
        return pageShell("Reports", "This week at a glance, " + weekRange(today()), summary, body);
    }

    private JPanel buildReportTotals() {
        Theme.FitLabel in = statValue();
        Theme.FitLabel out = statValue();
        Theme.FitLabel net = statValue();
        refreshers.add(() -> {
            double[] totals = weekTotals();
            in.setText(Theme.peso(totals[0]));
            out.setText(Theme.peso(totals[1]));
            net.setText(Theme.signedPeso(totals[0] - totals[1]));
        });

        JPanel stats = transparentPanel(new GridLayout(1, 3, 16, 0));
        stats.add(statCard(Theme.FERN, Icons.Name.CASH, statTitle("Money in"), in,
                caption("Allowance and other money received")));
        stats.add(statCard(Theme.CLAY, Icons.Name.RECEIPT, statTitle("Money out"), out,
                caption("Spending plus money moved to savings")));
        stats.add(statCard(Theme.SLATE, Icons.Name.SWAP, statTitle("Net"), net,
                caption("Money in minus money out")));
        return stats;
    }

    private JComponent buildDailyChartCard() {
        DailyChart chart = new DailyChart();
        refreshers.add(() -> chart.setData(dailySpending(), settings.getDailyLimit(),
                today().getDayOfWeek().getValue() - 1));

        Theme.RoundedPanel card = Theme.card(new BorderLayout(0, 12));
        card.add(cardTitle("Daily spending", Icons.Name.BARS, "Each day compared with your daily limit", Theme.MUTED),
                BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildCategoryReportCard() {
        Theme.Column categories = new Theme.Column();
        categories.setBorder(new EmptyBorder(0, 0, 0, 12));
        refreshers.add(() -> fillCategoryReport(categories));
        JScrollPane scroll = Theme.scroll(categories);
        scroll.setPreferredSize(new Dimension(200, 220));

        Theme.RoundedPanel card = Theme.card(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(18, 20, 16, 10));
        card.add(cardTitle("Spending by category", Icons.Name.PIE), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void fillCategoryReport(Theme.Column list) {
        list.clearRows();

        // Start with every budget category at zero so they keep the user's order.
        Map<String, Double> spentByCategory = new LinkedHashMap<>();
        for (String category : settings.getCategoryLimits().keySet()) spentByCategory.put(category, 0.0);
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (!r.isSpending()) continue;
            spentByCategory.merge(r.getCategorySource(), Math.abs(r.getAmount()), Double::sum);
            total += Math.abs(r.getAmount());
        }

        if (total <= 0) {
            list.addRow(new Theme.WrapText("No spending recorded this week yet.", Theme.BODY, Theme.MUTED), 0);
        } else {
            boolean first = true;
            for (Map.Entry<String, Double> e : spentByCategory.entrySet()) {
                list.addRow(categoryShareRow(e.getKey(), e.getValue(), e.getValue() / total), first ? 0 : 18);
                first = false;
            }
        }
        list.revalidate();
        list.repaint();
    }

    private static JPanel categoryShareRow(String category, double spent, double share) {
        Theme.Tint tint = Theme.tintFor(category);

        JPanel head = transparentPanel(new BorderLayout(8, 0));
        head.add(Theme.text(category, Theme.font(Font.BOLD, 14), Theme.TEXT), BorderLayout.WEST);
        head.add(Theme.text(Theme.peso(spent), Theme.font(Font.BOLD, 14), Theme.TEXT), BorderLayout.EAST);
        Theme.Bar bar = new Theme.Bar(8);
        bar.set(share, tint.ink);
        JLabel note = Theme.text(Math.round(share * 100) + "% of this week's spending",
                Theme.font(Font.PLAIN, 13), Theme.MUTED);

        JPanel words = transparentPanel(new BorderLayout(0, 7));
        words.add(head, BorderLayout.NORTH);
        words.add(bar, BorderLayout.CENTER);
        words.add(note, BorderLayout.SOUTH);

        JPanel row = transparentPanel(new BorderLayout(14, 0));
        row.add(alignTop(new Theme.IconBox(Icons.of(Icons.forCategory(category), 20, tint.ink), 40, tint.box, 10)),
                BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        return row;
    }

    // =====================================================================
    // Settings page
    // =====================================================================

    private JComponent buildSettingsPage() {
        Theme.Column col = new Theme.Column();
        col.addRow(settingRow(Icons.Name.SLIDERS, Theme.MOSS, "Budget plan",
                "Change your allowance, spending limits, categories and which sections the dashboard shows.",
                button("Edit plan", Theme.ButtonKind.SECONDARY, this::customizeDashboard)), 0);
        col.addRow(settingRow(Icons.Name.CALENDAR, Theme.OCHRE, "Date",
                "Moni is treating " + today().format(LONG_DATE) + " as today. Change it to see a "
                        + "different day; the dates of your transactions stay the same.",
                button("Change date", Theme.ButtonKind.SECONDARY, this::changeDate)), 14);
        boolean dark = Theme.isDarkMode();
        col.addRow(settingRow(dark ? Icons.Name.SUN : Icons.Name.MOON, Theme.FERN, "Appearance",
                dark ? "Moni is using dark colours, which are easier on the eyes at night."
                     : "Moni is using light colours. Dark mode is easier on the eyes at night.",
                button(dark ? "Use light mode" : "Use dark mode", Theme.ButtonKind.SECONDARY,
                        this::toggleDarkMode)), 14);
        col.addRow(settingRow(Icons.Name.INFO, Theme.SLATE, "How Moni works",
                "A short guide to every number on the dashboard.",
                button("Open guide", Theme.ButtonKind.SECONDARY, this::showHowItWorks)), 14);
        col.addRow(settingRow(Icons.Name.LOGOUT, Theme.CLAY, "Account",
                "Signed in as " + currentUser.getFullName() + ", student number "
                        + currentUser.getStudentNumber() + " (" + currentUser.getEmail() + ").",
                button("Log out", Theme.ButtonKind.SECONDARY, this::logout)), 14);
        return pageShell("Settings", "Your plan, date, appearance and account", null, col);
    }

    private static JPanel settingRow(Icons.Name icon, Theme.Tint tint, String title, String description,
                                     JComponent action) {
        Theme.RoundedPanel card = Theme.card(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel words = verticalPanel();
        Theme.stack(words, 0, Theme.text(title, Theme.font(Font.BOLD, 15), Theme.TEXT));
        Theme.stack(words, 4, new Theme.WrapText(description, Theme.BODY, Theme.MUTED));
        card.add(alignTop(new Theme.IconBox(Icons.of(icon, 22, tint.ink), 44, tint.box, 12)), BorderLayout.WEST);
        card.add(words, BorderLayout.CENTER);
        card.add(centred(action), BorderLayout.EAST);
        return card;
    }

    // =====================================================================
    // Loading data and spending calculations
    // =====================================================================

    /** Reloads balances and transactions from the database and redraws every page. */
    private void reloadData() {
        LocalDate today = today();
        try {
            MoniDatabase.AccountData account = MoniDatabase.loadAccount(currentUser);
            walletBalance = account.walletBalance;
            savingsBalance = account.savingsBalance;
            weekTransactions = MoniDatabase.getTransactions(
                    currentUser, MoniDatabase.weekStart(today), MoniDatabase.weekEnd(today));
            recentTransactions = MoniDatabase.getRecentTransactions(currentUser, RECENT_TRANSACTION_LIMIT);
            monthSpent = MoniDatabase.getSpent(currentUser, MoniDatabase.monthStart(today), MoniDatabase.monthEnd(today));
        } catch (Exception e) {
            showError(this, "Could not load your latest data from the database.", e);
        }
        for (TransactionsPanel panel : transactionPanels) panel.setData(recentTransactions, today);
        runRefreshers();
    }

    private void runRefreshers() {
        for (Runnable r : refreshers) r.run();
    }

    private double spentOn(LocalDate day) {
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (r.isSpending() && r.getDate().equals(day)) total += Math.abs(r.getAmount());
        }
        return total;
    }

    private double spentThisWeek() {
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (r.isSpending()) total += Math.abs(r.getAmount());
        }
        return total;
    }

    private double spentThisWeek(String category) {
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (r.isSpending() && category.equals(r.getCategorySource())) total += Math.abs(r.getAmount());
        }
        return total;
    }

    /** Spending for each day of this week, Monday first. */
    private double[] dailySpending() {
        double[] days = new double[7];
        for (TransactionRecord r : weekTransactions) {
            if (!r.isSpending()) continue;
            int dayIndex = r.getDate().getDayOfWeek().getValue() - 1;
            days[dayIndex] += Math.abs(r.getAmount());
        }
        return days;
    }

    /** {money in, money out} for this week. Money out includes moves to savings. */
    private double[] weekTotals() {
        double in = 0;
        double out = 0;
        for (TransactionRecord r : weekTransactions) {
            if (r.getAmount() >= 0) in += r.getAmount();
            else out += Math.abs(r.getAmount());
        }
        return new double[]{in, out};
    }

    /** The weekly limit spread over this whole month, for the "This Month" view. */
    private double monthlyPace() {
        return settings.getWeeklyLimit() / 7.0 * today().lengthOfMonth();
    }

    /**
     * Today's spending plan: what's left of the weekly limit, split over the days left in the
     * week (today included), but never more than the daily limit. Today's own spending is left
     * out here because the "Left to spend today" card subtracts it.
     */
    private double todayPlan() {
        LocalDate today = today();
        double dailyLimit = settings.getDailyLimit();
        double weeklyLimit = settings.getWeeklyLimit();
        double spentBeforeToday = spentThisWeek() - spentOn(today);
        long daysLeft = Math.max(1, ChronoUnit.DAYS.between(today, MoniDatabase.weekEnd(today)) + 1);

        double plan = weeklyLimit > 0
                ? Math.max(0, weeklyLimit - spentBeforeToday) / daysLeft
                : dailyLimit;
        if (dailyLimit > 0) plan = Math.min(plan, dailyLimit);
        return plan;
    }

    /** spent / limit, treating any spending against a zero limit as fully used. */
    private static double usedFraction(double spent, double limit) {
        if (limit > 0) return spent / limit;
        return spent > 0 ? 1 : 0;
    }

    /** Messages for the notification bell. */
    private List<Alert> alerts() {
        List<Alert> list = new ArrayList<>();

        if (settings.getAllowanceAmount() <= 0) {
            list.add(new Alert(Theme.ACCENT, "Your allowance is set to " + Theme.peso(0)
                    + ". Add it in Budget Plan so Moni can plan your spending."));
        }

        double plan = todayPlan();
        double spentToday = spentOn(today());
        if (plan > 0 && spentToday > plan) {
            list.add(new Alert(Theme.RED, "You're " + Theme.peso(spentToday - plan) + " over today's plan."));
        }

        double weeklyLimit = settings.getWeeklyLimit();
        double spentWeek = spentThisWeek();
        if (weeklyLimit > 0 && spentWeek >= weeklyLimit) {
            list.add(new Alert(Theme.RED, "You've used all of this week's " + Theme.peso(weeklyLimit) + " limit."));
        } else if (weeklyLimit > 0 && spentWeek >= weeklyLimit * NEAR_LIMIT) {
            list.add(new Alert(Theme.AMBER, "You've used " + Math.round(spentWeek / weeklyLimit * 100)
                    + "% of this week's " + Theme.peso(weeklyLimit) + " limit."));
        }

        for (Map.Entry<String, Double> e : settings.getCategoryLimits().entrySet()) {
            double limit = e.getValue();
            double spent = spentThisWeek(e.getKey());
            if (limit <= 0) continue;
            if (spent > limit) {
                list.add(new Alert(Theme.RED, e.getKey() + " is " + Theme.peso(spent - limit)
                        + " over budget this week."));
            } else if (spent >= limit * NEAR_LIMIT) {
                list.add(new Alert(Theme.AMBER, e.getKey() + ": " + Theme.peso(spent) + " of "
                        + Theme.peso(limit) + " used this week."));
            }
        }
        return list;
    }

    // =====================================================================
    // Add expense / Add money / Move to savings
    // =====================================================================

    private void expenseDialog() {
        Map<String, Double> limits = settings.getCategoryLimits();
        if (limits.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one spending category in Budget Plan first.",
                    "Add expense", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        FormDialog dialog = new FormDialog(this, "Add expense", "Record money you spent from your wallet.",
                "Record expense");
        JTextField amountField = Theme.field();
        JComboBox<String> categoryBox = new JComboBox<>(limits.keySet().toArray(new String[0]));
        Theme.styleCombo(categoryBox);
        JTextField descriptionField = Theme.field();
        JLabel budgetNote = Theme.text(" ", Theme.SMALL, Theme.MUTED);

        dialog.addRow("Amount (₱)", amountField);
        dialog.addRow("Category", categoryBox);
        dialog.addNote(budgetNote);
        dialog.addRow("Description", descriptionField);
        dialog.addNote(Theme.text("Wallet balance: " + Theme.money(walletBalance), Theme.SMALL, Theme.MUTED));

        Runnable updateNote = () -> {
            String category = (String) categoryBox.getSelectedItem();
            double spent = spentThisWeek(category);
            double limit = limits.get(category);
            budgetNote.setText(category + " this week: " + Theme.money(spent) + " of " + Theme.money(limit));
            budgetNote.setForeground(spent >= limit ? Theme.RED : Theme.MUTED);
        };
        categoryBox.addActionListener(e -> updateNote.run());
        updateNote.run();

        dialog.onConfirm(() -> {
            double amount = dialog.readAmount(amountField);
            if (amount <= 0) return;
            if (amount > walletBalance) {
                dialog.setError("Not enough in your wallet. Available: " + Theme.money(walletBalance));
                return;
            }

            String category = (String) categoryBox.getSelectedItem();
            List<String> over = limitsExceeded(category, amount);
            if (!over.isEmpty() && !confirmOverLimit(dialog, over)) return;

            String description = descriptionField.getText().trim();
            if (description.isEmpty()) description = "Expense";
            TransactionRecord record = new TransactionRecord(today(), MONEY_OUT, category,
                    description, -amount);
            saveTransaction(dialog, record, walletBalance - amount, savingsBalance);
        });
        dialog.open();
    }

    /** Which limits (daily, weekly, category) a new expense would go over. */
    private List<String> limitsExceeded(String category, double amount) {
        List<String> over = new ArrayList<>();
        double dailyLimit = settings.getDailyLimit();
        double weeklyLimit = settings.getWeeklyLimit();
        double categoryLimit = settings.getCategoryLimits().get(category);

        if (dailyLimit > 0 && spentOn(today()) + amount > dailyLimit) {
            over.add("your daily limit (" + Theme.money(dailyLimit) + ")");
        }
        if (weeklyLimit > 0 && spentThisWeek() + amount > weeklyLimit) {
            over.add("your weekly limit (" + Theme.money(weeklyLimit) + ")");
        }
        if (spentThisWeek(category) + amount > categoryLimit) {
            over.add("your " + category + " budget (" + Theme.money(categoryLimit) + ")");
        }
        return over;
    }

    private static boolean confirmOverLimit(Component parent, List<String> over) {
        StringBuilder message = new StringBuilder("This expense takes you over:\n\n");
        for (String item : over) message.append("  • ").append(item).append('\n');
        message.append("\nRecord it anyway?");
        int choice = JOptionPane.showConfirmDialog(parent, message.toString(), "Over your limit",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void fundsDialog() {
        FormDialog dialog = new FormDialog(this, "Add money", "Add your allowance or other money you received.",
                "Add money");
        JTextField amountField = Theme.field();
        JComboBox<String> sourceBox = new JComboBox<>(new String[]{ALLOWANCE, OTHER_FUNDS});
        Theme.styleCombo(sourceBox);
        JLabel note = Theme.text(" ", Theme.SMALL, Theme.MUTED);

        dialog.addRow("Amount (₱)", amountField);
        dialog.addRow("Source", sourceBox);
        dialog.addNote(note);

        String frequency = settings.getAllowanceFrequency();
        String period = periodWord(frequency);

        Runnable updateNote = () -> {
            if (!ALLOWANCE.equals(sourceBox.getSelectedItem())) {
                note.setText("Other funds don't count toward your allowance.");
                return;
            }
            try {
                double received = allowanceReceived(frequency);
                note.setText("Allowance received " + period + ": " + Theme.money(received)
                        + " of " + Theme.money(settings.getAllowanceAmount()));
            } catch (Exception e) {
                note.setText("Couldn't load your allowance status.");
            }
        };
        sourceBox.addActionListener(e -> updateNote.run());
        updateNote.run();

        dialog.onConfirm(() -> {
            double amount = dialog.readAmount(amountField);
            if (amount <= 0) return;

            // Allowance can't go over the amount set in the budget plan for this period.
            String source = (String) sourceBox.getSelectedItem();
            if (ALLOWANCE.equals(source)) {
                try {
                    double remaining = settings.getAllowanceAmount() - allowanceReceived(frequency);
                    if (amount > remaining + 0.005) { // allow for rounding
                        dialog.setError("That's more than your allowance. You can add up to "
                                + Theme.money(Math.max(0, remaining)) + " " + period + ".");
                        return;
                    }
                } catch (Exception e) {
                    dialog.setError("Couldn't check your allowance: " + e.getMessage());
                    return;
                }
            }
            TransactionRecord record = new TransactionRecord(today(), MONEY_IN, source,
                    "Added " + source, amount);
            saveTransaction(dialog, record, walletBalance + amount, savingsBalance);
        });
        dialog.open();
    }

    private double allowanceReceived(String frequency) throws Exception {
        LocalDate today = today();
        return MoniDatabase.getAllowanceReceived(currentUser,
                MoniDatabase.periodStart(today, frequency), MoniDatabase.periodEnd(today, frequency));
    }

    private static String periodWord(String frequency) {
        if ("Weekly".equalsIgnoreCase(frequency)) return "this week";
        if ("Monthly".equalsIgnoreCase(frequency)) return "this month";
        return "today";
    }

    private void savingsDialog() {
        FormDialog dialog = new FormDialog(this, "Move to savings", "Move money from your wallet into savings.",
                "Move to savings");
        JTextField amountField = Theme.field();
        JTextField noteField = Theme.field();

        dialog.addRow("Amount (₱)", amountField);
        dialog.addRow("Note (optional)", noteField);
        dialog.addNote(Theme.text("Wallet balance: " + Theme.money(walletBalance), Theme.SMALL, Theme.MUTED));

        dialog.onConfirm(() -> {
            double amount = dialog.readAmount(amountField);
            if (amount <= 0) return;
            if (amount > walletBalance) {
                dialog.setError("Not enough in your wallet. Available: " + Theme.money(walletBalance));
                return;
            }
            String description = noteField.getText().trim();
            if (description.isEmpty()) description = "Moved to savings";
            TransactionRecord record = new TransactionRecord(today(), MONEY_OUT, SAVINGS,
                    description, -amount);
            saveTransaction(dialog, record, walletBalance - amount, savingsBalance + amount);
        });
        dialog.open();
    }

    /** Saves the transaction and the new balances together, then closes the dialog. */
    private void saveTransaction(FormDialog dialog, TransactionRecord record, double newWallet, double newSavings) {
        try {
            MoniDatabase.saveTransactionAndBalances(currentUser, record, newWallet, newSavings);
            dialog.dispose();
            reloadData();
        } catch (Exception e) {
            System.err.println("Could not save transaction: " + e);
            dialog.setError("Could not save: " + e.getMessage());
        }
    }

    // =====================================================================
    // Other windows: weekly summary, guide, date, plan, log out
    // =====================================================================

    private void showWeeklySummary() {
        JDialog dialog = new JDialog(this, "This week's summary", true);

        JPanel head = verticalPanel();
        Theme.stack(head, 0, Theme.text("This week's summary", Theme.H1, Theme.TEXT));
        Theme.stack(head, 4, Theme.text(weekRange(today()), Theme.BODY, Theme.MUTED));

        double[] totals = weekTotals();
        JPanel stats = transparentPanel(new GridLayout(1, 3, 12, 0));
        stats.add(miniStat("Money in", Theme.peso(totals[0]), Theme.GREEN));
        stats.add(miniStat("Money out", Theme.peso(totals[1]), Theme.RED));
        stats.add(miniStat("Net", Theme.signedPeso(totals[0] - totals[1]), Theme.TEXT));

        JPanel north = transparentPanel(new BorderLayout(0, 16));
        north.add(head, BorderLayout.NORTH);
        north.add(stats, BorderLayout.CENTER);

        DefaultTableModel model = TransactionsPanel.newModel();
        for (TransactionRecord r : weekTransactions) TransactionsPanel.addRow(model, r);
        Component center;
        if (model.getRowCount() > 0) {
            JTable table = TransactionsPanel.createTable(model);
            table.setRowSorter(new TableRowSorter<>(model)); // click a header to sort
            center = Theme.tableBox(table);
        } else {
            center = Theme.text("No transactions this week yet.", Theme.BODY, Theme.MUTED);
        }

        JPanel south = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.add(button("Close", Theme.ButtonKind.PRIMARY, dialog::dispose));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(22, 24, 20, 24));
        root.add(north, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        showDialog(dialog, root, 900, 600);
    }

    private static JPanel miniStat(String title, String value, Color color) {
        JPanel card = Theme.card(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        Theme.stack(card, 0, Theme.text(title, Theme.font(Font.BOLD, 13), Theme.TEXT_SOFT));
        Theme.stack(card, 6, Theme.text(value, Theme.font(Font.BOLD, 22), color));
        return card;
    }

    /** Plain-language guide to the app and to every number on the dashboard. */
    private void showHowItWorks() {
        JDialog dialog = new JDialog(this, "How Moni works", true);

        Theme.Column col = new Theme.Column();
        col.setBorder(new EmptyBorder(0, 0, 8, 8));
        col.addRow(Theme.text("How Moni works", Theme.H1, Theme.TEXT), 0);
        col.addRow(new Theme.WrapText("Moni is a student money manager. Its job is to answer one question: "
                + "how much can I spend today and still have allowance left for the rest of the week?",
                Theme.BODY, Theme.MUTED), 6);
        addSteps(col, 18);

        col.addRow(Theme.text("What the dashboard shows", Theme.H2, Theme.TEXT), 26);
        String[][] terms = {
                {"Left to spend today", "What's left of your weekly limit, shared across the days left in "
                        + "the week (never more than your daily limit), minus what you've spent today."},
                {"Wallet", "The money you have right now."},
                {"Savings", "Money you've moved out of your wallet to keep."},
                {"Spent this week", "Everything you've spent since Monday, compared with your weekly limit. "
                        + "Switch the drop-down above the cards to This Month to see the whole month."},
                {"Weekly Budgets", "How much of each category's budget you've used this week."},
                {"Recent Transactions", "Every peso in and out. Search, filter, or click a column to sort. "
                        + "The search box at the top searches all your transactions."},
                {"Notifications", "The bell shows a red dot when there's something to check, such as a "
                        + "budget that's nearly used up."},
                {"Budget Plan", "Change your allowance, limits, categories, or which sections you see."},
                {"Reports", "This week's money in and out, day by day and by category."}
        };
        for (String[] term : terms) {
            col.addRow(Theme.text(term[0], Theme.BODY_BOLD, Theme.TEXT), 12);
            col.addRow(new Theme.WrapText(term[1], Theme.BODY, Theme.MUTED), 2);
        }

        JPanel south = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(button("Got it", Theme.ButtonKind.PRIMARY, dialog::dispose));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(24, 28, 20, 20));
        root.add(Theme.scroll(col), BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        showDialog(dialog, root, 600, 660);
    }

    private void showDialog(JDialog dialog, JComponent content, int width, int height) {
        dialog.setContentPane(content);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(this);
        Theme.onEscape(dialog, dialog::dispose);
        dialog.setVisible(true);
    }

    /**
     * Lets the user pick which day Moni treats as today. Only the app's view changes:
     * the computer's clock and the dates already saved in the database stay the same.
     */
    private void changeDate() {
        LocalDate realToday = LocalDate.now();
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(
                java.sql.Date.valueOf(selectedDate),
                java.sql.Date.valueOf(LocalDate.of(2020, 1, 1)),
                java.sql.Date.valueOf(LocalDate.of(2099, 12, 31)),
                Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MMM d, yyyy"));
        dateSpinner.setPreferredSize(new Dimension(150, 32));

        JCheckBox reset = new JCheckBox("Go back to today (" + realToday.format(LONG_DATE) + ")");
        reset.setOpaque(false);
        reset.setFont(Theme.BODY);
        reset.addActionListener(e -> {
            if (reset.isSelected()) dateSpinner.setValue(java.sql.Date.valueOf(realToday));
        });

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(8, 4, 4, 4));
        panel.add(Theme.text("Choose the date Moni should use as \"today\".", Theme.BODY, Theme.TEXT),
                BorderLayout.NORTH);
        panel.add(dateSpinner, BorderLayout.CENTER);
        panel.add(reset, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change date",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        java.util.Date picked = (java.util.Date) dateSpinner.getValue();
        selectedDate = picked.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        rebuild(); // every page shows date-based numbers, so build them all again
    }

    /** Every component reads its colours when it is built, so switching modes rebuilds the window. */
    private void toggleDarkMode() {
        Theme.setDarkMode(!Theme.isDarkMode());
        rebuild();
    }

    private void customizeDashboard() {
        OnboardingDialog dialog = new OnboardingDialog(this, currentUser, settings);
        dialog.setVisible(true);
        UserSettings updated = dialog.getResult();
        if (updated == null) return;

        try {
            MoniDatabase.saveSettings(currentUser, updated);
            settings = updated;
            rebuild(); // sections may have been shown or hidden
        } catch (Exception e) {
            showError(this, "Could not save your changes.", e);
        }
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out of Moni?", "Log out", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            showLogin();
        }
    }

    // =====================================================================
    // Small helpers
    // =====================================================================

    private static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** Transparent panel that stacks its children top to bottom (use with Theme.stack). */
    private static JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /** Wraps a component so its preferred width is fixed; GridBag then splits the rest by weight. */
    private static JPanel withWidth(JComponent c, int width) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }

            @Override public Dimension getMinimumSize() {
                return new Dimension(width * 2 / 3, super.getMinimumSize().height);
            }
        };
        panel.setOpaque(false);
        panel.add(c);
        return panel;
    }

    /** Keeps a component at the top of its cell instead of stretching it. */
    private static JPanel alignTop(JComponent c) {
        JPanel panel = transparentPanel(new BorderLayout());
        panel.add(c, BorderLayout.NORTH);
        return panel;
    }

    /** Vertically centres a component in its cell. */
    private static JPanel centred(JComponent c) {
        JPanel panel = transparentPanel(new GridBagLayout());
        panel.add(c);
        return panel;
    }

    /** Stops a row from stretching taller inside a vertical BoxLayout. */
    private static void fixHeight(JComponent row) {
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
    }

    private static Theme.FlatButton button(String text, Theme.ButtonKind kind, Runnable action) {
        Theme.FlatButton b = new Theme.FlatButton(text, kind);
        b.addActionListener(e -> action.run());
        return b;
    }

    /** The large buttons under the dashboard table. */
    private static Theme.FlatButton actionButton(String text, Icons.Name icon, Theme.ButtonKind kind, Color ink,
                                                 String tip, Runnable action) {
        Theme.FlatButton b = button(text, kind, action);
        b.setIcon(Icons.of(icon, 22, ink));
        b.setIconTextGap(12);
        b.setFont(Theme.font(Font.BOLD, 15));
        b.setBorder(new EmptyBorder(13, 14, 13, 14));
        b.setToolTipText(tip);
        return b;
    }

    /** The smaller buttons in the top-right corner of a page. */
    private static Theme.FlatButton smallAction(String text, Icons.Name icon, Theme.ButtonKind kind, Color ink,
                                                Runnable action) {
        Theme.FlatButton b = button(text, kind, action);
        b.setIcon(Icons.of(icon, 18, ink));
        b.setBorder(new EmptyBorder(11, 16, 11, 16));
        b.setFont(Theme.font(Font.BOLD, 14));
        return b;
    }

    private Theme.FlatButton summaryButton() {
        Theme.FlatButton summary = button("This week's summary", Theme.ButtonKind.SECONDARY, this::showWeeklySummary);
        summary.setIcon(Icons.of(Icons.Name.BARS, 16, Theme.TEXT));
        return summary;
    }

    /** The user's categories plus the money sources, for the Category filter. */
    private List<String> filterCategories() {
        List<String> list = new ArrayList<>(settings.getCategoryLimits().keySet());
        for (String c : new String[]{SAVINGS, ALLOWANCE, OTHER_FUNDS}) {
            if (!list.contains(c)) list.add(c);
        }
        return list;
    }

    private String allowanceSummary() {
        return "Allowance: " + Theme.peso(settings.getAllowanceAmount()) + " "
                + settings.getAllowanceFrequency().toLowerCase();
    }

    private static String weekRange(LocalDate date) {
        return MoniDatabase.weekStart(date).format(RANGE_DATE) + " – " + MoniDatabase.weekEnd(date).format(RANGE_DATE);
    }

    private static String firstName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        return trimmed.isEmpty() ? "there" : trimmed.split("\\s+")[0];
    }

    private static String initial(String fullName) {
        String first = firstName(fullName);
        return "there".equals(first) ? "?" : first.substring(0, 1).toUpperCase();
    }

    /** Shows a database (or other) error to the user and prints the details for debugging. */
    private static void showError(Component parent, String message, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(parent, message + "\n\n" + e.getMessage(), "Moni", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    // Starting the app
    // =====================================================================

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to the default look and feel.
            }
            showLogin();
        });
    }

    /** Shows the sign-in window until someone signs in (and finishes setup) or closes it. */
    private static void showLogin() {
        while (true) {
            AuthDialog login = new AuthDialog(null);
            login.setVisible(true);
            User user = login.getAuthenticatedUser();
            if (user == null) {
                System.exit(0);
                return;
            }
            UserSettings settings = loadOrRunSetup(user);
            if (settings != null) {
                new MainApp(user, settings).setVisible(true);
                return;
            }
            // Setup wasn't finished, so go back to the sign-in window.
        }
    }

    /**
     * Loads the user's settings, running the setup wizard first if setup was never finished.
     *
     * @return the settings to open the dashboard with, or null to go back to the sign-in window
     */
    private static UserSettings loadOrRunSetup(User user) {
        UserSettings loaded;
        try {
            loaded = MoniDatabase.loadSettings(user);
        } catch (Exception e) {
            showError(null, "Moni couldn't load your settings from the database.", e);
            return null;
        }
        if (loaded.isSetupCompleted()) return loaded;

        OnboardingDialog setup = new OnboardingDialog(null, user);
        setup.setVisible(true);
        UserSettings chosen = setup.getResult();
        if (chosen == null) {
            JOptionPane.showMessageDialog(null,
                    "Finish the four setup steps to open your dashboard.\n"
                            + "You can change everything later in Budget Plan.",
                    "Setup not finished", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        try {
            MoniDatabase.saveSettings(user, chosen);
        } catch (Exception e) {
            // Still open the dashboard with the new plan; setup will run again at the next sign-in.
            showError(null, "Your setup could not be saved, so Moni will ask for it again "
                    + "next time you sign in.", e);
        }
        return chosen;
    }
}
