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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Main Moni window: a sidebar with the pages, a top bar (search, date, notifications, account)
 * and the page itself. The dashboard shows today's spending plan, balances, recent transactions
 * and weekly category budgets.
 */
public class MainApp extends JFrame {

    private static final int RECENT_LIMIT = 300;
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter RANGE_DATE = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // Presentation/demo date. This can be changed from the date button in the top bar.
    private static final LocalDate DEFAULT_DEMO_DATE = LocalDate.of(2026, 9, 24);
    private static LocalDate presentationDate = DEFAULT_DEMO_DATE;

    private static LocalDate appToday() {
        return presentationDate;
    }

    // Values stored in the database.
    private static final String MONEY_IN = TransactionsPanel.MONEY_IN;
    private static final String MONEY_OUT = TransactionsPanel.MONEY_OUT;
    private static final String SAVINGS = "Savings";
    private static final String ALLOWANCE = "Allowance";
    private static final String OTHER_FUNDS = "Other Funds";

    // The three steps that explain what Moni is for (welcome guide + How it works).
    private static final String[][] HOW_IT_WORKS = {
            {"Add your allowance",
             "Whenever you receive your allowance, use Add money so Moni knows what you have."},
            {"Record what you spend",
             "Each time you buy something, add it as an expense and pick a category such as Food."},
            {"Check what's left today",
             "Moni shares your weekly limit across the days left in the week, so you always know "
                     + "how much you can still spend today."}
    };

    /** The pages in the sidebar. */
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

    private final User currentUser;
    private UserSettings settings;

    // Loaded from the database by refreshDashboard(). Filtering and totals work on these copies.
    private double walletBalance;
    private double savingsBalance;
    private double monthSpent;
    private List<TransactionRecord> weekTransactions = new ArrayList<>();
    private List<TransactionRecord> recentTransactions = new ArrayList<>();

    private Page currentPage = Page.DASHBOARD;
    private boolean showMonth; // the dashboard's "This Week" / "This Month" choice

    // Built again by rebuild(). Every widget that shows data registers a refresher that
    // redraws it from the fields above, so refreshDashboard() just runs them all.
    private final Map<Page, NavItem> navItems = new EnumMap<>(Page.class);
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<TransactionsPanel> transactionPanels = new ArrayList<>();
    private CardLayout pageCards;
    private JPanel pageHolder;
    private TransactionsPanel transactionsPage;
    private Theme.SearchField topSearch;
    private Theme.FlatButton bell;

    public MainApp(User user, UserSettings settings) {
        super("Moni \u2014 Student Money Manager");
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

    // =====================================================================
    // Startup
    // =====================================================================

    /**
     * Loads the user's settings, running the setup wizard only if setup was never finished.
     *
     * @return the settings to open the dashboard with, or null to go back to the sign-in screen
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
            // Double-check the save, so this problem can never be silent again.
            if (!MoniDatabase.isSetupCompleted(user)) {
                throw new IllegalStateException("The setup was not stored in user_settings.");
            }
        } catch (Exception e) {
            showError(null, "Your setup could not be saved, so Moni will ask for it again "
                    + "next time you sign in.", e);
            // Still open the dashboard so the database problem can be investigated.
        }
        return chosen;
    }

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
        refreshDashboard();
        revalidate();
        repaint();
        navItems.get(currentPage).requestFocusInWindow(); // keyboard focus starts on the page's own tab
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

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        Theme.stack(words, 0, Theme.text("MONI", Theme.font(Font.BOLD, 30), Color.WHITE));
        Theme.stack(words, 0, Theme.text("Student Money Manager", Theme.font(Font.PLAIN, 12),
                new Color(255, 255, 255, 230)));
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

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        Theme.stack(words, 0, Theme.text("Make your", Theme.font(Font.BOLD, 15), Color.WHITE));
        Theme.stack(words, 2, Theme.text("allowance last", Theme.font(Font.BOLD, 15), Color.WHITE));

        Theme.FlatButton go = new Theme.FlatButton(null, Icons.of(Icons.Name.CHEVRON_RIGHT, 18, Theme.INK),
                Theme.ButtonKind.CIRCLE);
        go.setBorder(new EmptyBorder(0, 0, 0, 0));
        go.setPreferredSize(new Dimension(40, 40));
        go.setToolTipText("How Moni works");
        go.addActionListener(e -> showHowItWorks());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.add(go);

        card.add(words, BorderLayout.NORTH);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    /** One sidebar item. The current page gets a lighter lavender pill. */
    private final class NavItem extends JButton {
        private boolean active;

        NavItem(Page page) {
            super(page.title, Icons.of(page.icon, 22, Color.WHITE));
            setFont(Theme.NAV);
            setForeground(Color.WHITE);
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
                g2.setColor(new Color(255, 255, 255, 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            }
            if (isFocusOwner() && !active) {
                g2.setColor(new Color(255, 255, 255, 150));
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

        Theme.FlatButton date = new Theme.FlatButton(appToday().format(LONG_DATE),
                Icons.of(Icons.Name.CALENDAR, 22, Theme.TEXT), Theme.ButtonKind.PLAIN);
        date.setFont(Theme.font(Font.BOLD, 14));
        date.setIconTextGap(12);
        date.setTrailingIcon(Icons.of(Icons.Name.CHEVRON_DOWN, 16, Theme.TEXT));
        date.setToolTipText("Choose the date Moni treats as today (for presentations)");
        date.addActionListener(e -> changePresentationDate());

        bell = new Theme.FlatButton(null, Icons.of(Icons.Name.BELL, 24, Theme.TEXT), Theme.ButtonKind.PLAIN);
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
        bar.add(bell, c);
        c.gridx = 3;
        c.insets = new Insets(0, 20, 0, 0);
        bar.add(buildUserChip(), c);
        return bar;
    }

    private JComponent buildUserChip() {
        Theme.RoundedPanel chip = new Theme.RoundedPanel(new BorderLayout(12, 0), 18, Theme.CHIP, null);
        chip.setBorder(new EmptyBorder(7, 8, 7, 14));
        chip.add(new JLabel(Icons.avatar(initial(currentUser.getFullName()), 40,
                new Color(0x6A, 0x63, 0x8A), Color.WHITE)), BorderLayout.WEST);

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
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
            @Override public void mouseEntered(MouseEvent e) { chip.setBackground(new Color(0xEC, 0xEB, 0xF2)); }
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
        menu.add(Theme.menuItem("Change presentation date", Icons.Name.CALENDAR, this::changePresentationDate));
        menu.addSeparator();
        menu.add(Theme.menuItem("Log out", Icons.Name.LOGOUT, this::logout));
        menu.show(anchor, anchor.getWidth() - menu.getPreferredSize().width, anchor.getHeight() + 6);
    }

    private void showNotifications(JComponent anchor) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(8, 16, 10, 16));
        Theme.stack(list, 0, Theme.text("Notifications", Theme.font(Font.BOLD, 15), Theme.TEXT));

        List<String[]> alerts = alerts();
        if (alerts.isEmpty()) {
            Theme.stack(list, 10, htmlText("You're all caught up. Moni will let you know here when "
                    + "you get close to a limit.", Theme.MUTED));
        }
        for (String[] a : alerts) {
            Color dot = "red".equals(a[0]) ? Theme.RED : "amber".equals(a[0]) ? Theme.AMBER : Theme.ACCENT;
            JLabel mark = new JLabel(new DotIcon(dot));
            mark.setVerticalAlignment(SwingConstants.TOP);
            mark.setBorder(new EmptyBorder(5, 0, 0, 0));
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.add(mark, BorderLayout.WEST);
            row.add(htmlText(a[1], Theme.TEXT), BorderLayout.CENTER);
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
    // Page layout helpers
    // =====================================================================

    /** Title and subtitle at the top of a page, an optional control on the right, then the body. */
    private static JComponent pageShell(String title, String subtitle, JComponent right, JComponent body) {
        Theme.Page page = new Theme.Page(new BorderLayout(0, 18));
        page.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        Theme.stack(titles, 0, Theme.text(title, Theme.TITLE, Theme.TEXT));
        Theme.stack(titles, 0, Theme.text(subtitle, Theme.font(Font.PLAIN, 16), Theme.MUTED));

        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);
        head.add(titles, BorderLayout.CENTER);
        if (right != null) {
            JPanel holder = new JPanel(new GridBagLayout());
            holder.setOpaque(false);
            holder.add(right);
            head.add(holder, BorderLayout.EAST);
        }

        page.add(head, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private static JPanel clear(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    /** Wraps a component so its preferred width is fixed; GridBag then splits the rest by weight. */
    private static JPanel withWidth(JComponent c, int width) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }

            @Override public Dimension getMinimumSize() {
                return new Dimension(width * 2 / 3, super.getMinimumSize().height);
            }
        };
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    /** Keeps an icon box at the top of its cell instead of stretching it. */
    private static JPanel top(JComponent c) {
        JPanel p = clear(new BorderLayout());
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    /** Vertically centres a component in its cell. */
    private static JPanel centred(JComponent c) {
        JPanel p = clear(new GridBagLayout());
        p.add(c);
        return p;
    }

    private static JLabel cardTitle(String text, Icons.Name icon) {
        JLabel title = Theme.text(text, Theme.font(Font.BOLD, 18), Theme.TEXT);
        title.setIcon(Icons.of(icon, 24, Theme.TEXT));
        title.setIconTextGap(14);
        return title;
    }

    private static Theme.RoundedPanel whiteCard(LayoutManager layout) {
        Theme.RoundedPanel card = new Theme.RoundedPanel(layout, 18, Theme.CARD, Theme.CARD_OUTLINE);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }

    // =====================================================================
    // Dashboard
    // =====================================================================

    private JComponent buildDashboardPage() {
        JPanel body = clear(new BorderLayout(0, 18));

        JPanel stats = clear(new GridLayout(1, 0, 16, 0));
        if (settings.isShowWallet()) stats.add(walletCard());
        if (settings.isShowSavings()) stats.add(savingsCard());
        if (settings.isShowWeekly()) stats.add(spentCard());
        if (settings.isShowDaily()) stats.add(todayCard());
        if (stats.getComponentCount() > 0) body.add(stats, BorderLayout.NORTH);
        body.add(dashboardBottom(), BorderLayout.CENTER);

        JComponent period = null;
        if (settings.isShowWeekly()) {
            Theme.FlatButton b = new Theme.FlatButton(showMonth ? "This Month" : "This Week",
                    Icons.of(Icons.Name.CALENDAR, 20, Theme.TEXT), Theme.ButtonKind.SECONDARY);
            b.setFont(Theme.font(Font.PLAIN, 15));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setIconTextGap(12);
            b.setBorder(new EmptyBorder(11, 16, 11, 16));
            b.setTrailingIcon(Icons.of(Icons.Name.CHEVRON_DOWN, 16, Theme.TEXT));
            b.setPreferredSize(new Dimension(200, b.getPreferredSize().height));
            b.setToolTipText("Show spending for this week or this month");
            b.addActionListener(e -> showPeriodMenu(b));
            period = b;
        }
        return pageShell("Dashboard", "Overview of your finances", period, body);
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

    private JComponent dashboardBottom() {
        boolean tx = settings.isShowTransactions();
        boolean budgets = settings.isShowBudget();
        boolean actions = settings.isShowQuickActions();
        if (!tx && !budgets && !actions) {
            return top(Theme.text("Use Budget Plan \u203A Edit plan to choose what Moni shows here.",
                    Theme.BODY, Theme.MUTED));
        }

        JPanel left = clear(new BorderLayout(0, 14));
        if (tx) left.add(dashboardTransactions(), BorderLayout.CENTER);
        if (actions) left.add(buildActions(), tx ? BorderLayout.SOUTH : BorderLayout.NORTH);
        if (!budgets) return left;

        JComponent budgetCard = buildBudgetCard();
        if (!tx && !actions) return budgetCard;

        JPanel row = clear(new GridBagLayout());
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
        Theme.FlatButton summary = button("This week's summary", Theme.ButtonKind.SECONDARY, this::showWeeklySummary);
        summary.setIcon(Icons.of(Icons.Name.BARS, 16, Theme.TEXT));
        TransactionsPanel panel = new TransactionsPanel("Recent Transactions", Icons.Name.LIST, filterCategories(),
                null, summary, buildWelcome(), "No transactions yet.");
        transactionPanels.add(panel);
        return panel;
    }

    private JPanel buildActions() {
        JPanel row = clear(new GridLayout(1, 3, 14, 0));
        row.add(actionButton("+  Add expense", Icons.Name.RECEIPT, Theme.ButtonKind.PRIMARY, Color.WHITE,
                "Record money you spent", this::expenseDialog));
        row.add(actionButton("+  Add money", Icons.Name.CASH, Theme.ButtonKind.SOFT, Theme.TEXT,
                "Add your allowance or other money you received", this::fundsDialog));
        row.add(actionButton("Move to savings", Icons.Name.PIGGY_LINE, Theme.ButtonKind.SOFT, Theme.TEXT,
                "Move money from your wallet into savings", this::savingsDialog));
        return row;
    }

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

    // ---- the four cards at the top ------------------------------------

    /** A tinted card: icon square, title and big number, then optional bar and caption underneath. */
    private static Theme.RoundedPanel statCard(Theme.Tint tint, Icons.Name icon, JLabel title, JLabel value,
                                               JComponent... below) {
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(0, 10), 18, tint.fill, tint.outline);
        card.setBorder(new EmptyBorder(16, 18, 14, 18));

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        Theme.stack(words, 1, title);
        Theme.stack(words, 0, value);

        JPanel head = clear(new BorderLayout(16, 0));
        head.add(top(new Theme.IconBox(Icons.of(icon, 26, tint.ink), 54, tint.box, 14)), BorderLayout.WEST);
        head.add(words, BorderLayout.CENTER);

        JPanel foot = new JPanel();
        foot.setOpaque(false);
        foot.setLayout(new BoxLayout(foot, BoxLayout.Y_AXIS));
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

    private static Theme.WrapText caption(String text) {
        return new Theme.WrapText(text, Theme.font(Font.PLAIN, 14), Theme.TEXT_SOFT);
    }

    private JPanel walletCard() {
        Theme.FitLabel value = statValue();
        refreshers.add(() -> value.setText(Theme.peso(walletBalance)));
        return statCard(Theme.BLUE, Icons.Name.WALLET, statTitle("Wallet"), value, caption(allowanceSummary()));
    }

    private JPanel savingsCard() {
        Theme.FitLabel value = statValue();
        refreshers.add(() -> value.setText(Theme.peso(savingsBalance)));
        return statCard(Theme.MINT, Icons.Name.PIGGY_LINE, statTitle("Savings"), value,
                caption("Set aside from your wallet"));
    }

    /** "Spent this week" (or this month, from the drop-down above the cards). */
    private JPanel spentCard() {
        JLabel title = statTitle("Spent this week");
        Theme.FitLabel value = statValue();
        Theme.Bar bar = new Theme.Bar(8);
        bar.setTrack(new Color(0xEC, 0xE6, 0xDE));
        Theme.WrapText caption = caption(" ");
        refreshers.add(() -> {
            double limit = showMonth ? monthlyPace() : settings.getWeeklyLimit();
            double spent = showMonth ? monthSpent : spentThisWeek();
            double fraction = limit > 0 ? spent / limit : 0;
            title.setText(showMonth ? "Spent this month" : "Spent this week");
            value.setText(Theme.peso(spent));
            bar.set(fraction, fraction >= 1 ? Theme.RED : fraction >= 0.8 ? Theme.AMBER : Theme.ACCENT);
            if (limit <= 0) caption.setText("No weekly limit set yet");
            else if (showMonth) caption.setText("of about " + Theme.peso(limit) + " at your weekly limit");
            else caption.setText("of your " + Theme.peso(limit) + " weekly limit");
        });
        return statCard(Theme.HONEY, Icons.Name.SWAP, title, value, bar, caption);
    }

    /** The answer to the app's main question: how much can I still spend today? */
    private JPanel todayCard() {
        Theme.FitLabel value = statValue();
        Theme.WrapText status = caption(" ");
        Theme.RoundedPanel card = statCard(Theme.BLUSH, Icons.Name.RECEIPT, statTitle("Left to spend today"),
                value, status);
        refreshers.add(() -> {
            LocalDate today = appToday();
            double plan = todayPlan(today);
            double spent = spentOn(today);
            double fraction = plan > 0 ? spent / plan : (spent > 0 ? 1 : 0);
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

    // ---- weekly budgets -------------------------------------------------

    private JComponent buildBudgetCard() {
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(0, 18), 18, Theme.CARD, Theme.CARD_OUTLINE);
        card.setBorder(new EmptyBorder(18, 20, 16, 10));

        JLabel range = Theme.text(weekRange(appToday()), Theme.font(Font.PLAIN, 13), Theme.TEXT_SOFT);
        range.setBorder(new EmptyBorder(0, 38, 0, 0));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        Theme.stack(head, 0, cardTitle("Weekly Budgets", Icons.Name.PIE));
        Theme.stack(head, 2, range);

        Theme.Column list = new Theme.Column();
        list.setBorder(new EmptyBorder(0, 0, 0, 12));
        JScrollPane scroll = Theme.scroll(list);
        scroll.setPreferredSize(new Dimension(220, 160));
        refreshers.add(() -> fillBudgets(list));

        card.add(head, BorderLayout.NORTH);
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
        double fraction = limit > 0 ? spent / limit : (spent > 0 ? 1 : 0);

        JPanel head = clear(new BorderLayout(8, 0));
        head.add(Theme.text(category, Theme.font(Font.BOLD, 15), Theme.TEXT), BorderLayout.WEST);
        head.add(Theme.text(Theme.peso(spent) + " / " + Theme.peso(limit), Theme.font(Font.PLAIN, 14), Theme.TEXT),
                BorderLayout.EAST);

        Theme.Bar bar = new Theme.Bar(8);
        bar.set(fraction, spent > limit ? Theme.RED : tint.ink);

        JLabel note = spent > limit
                ? Theme.text(Theme.peso(spent - limit) + " over budget", Theme.font(Font.PLAIN, 13), Theme.RED)
                : Theme.text(Theme.peso(limit - spent) + " left", Theme.font(Font.PLAIN, 13), Theme.TEXT_SOFT);

        JPanel words = clear(new BorderLayout(0, 8));
        words.add(head, BorderLayout.NORTH);
        words.add(bar, BorderLayout.CENTER);
        words.add(note, BorderLayout.SOUTH);

        JPanel row = clear(new BorderLayout(16, 0));
        row.add(top(new Theme.IconBox(Icons.of(Icons.forCategory(category), 24, tint.ink), 48, tint.box, 12)),
                BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        return row;
    }

    // ---- welcome guide (shown instead of the table until the first transaction) --

    private JComponent buildWelcome() {
        Theme.Column col = new Theme.Column();
        col.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel title = clear(new BorderLayout(12, 0));
        title.add(Theme.text("Welcome! Here's how Moni works", Theme.H2, Theme.TEXT), BorderLayout.CENTER);
        title.add(button("Add your allowance", Theme.ButtonKind.PRIMARY, this::fundsDialog), BorderLayout.EAST);
        col.addRow(title, 0);
        addSteps(col, 16);
        return Theme.scroll(col);
    }

    private static void addSteps(Theme.Column col, int firstGap) {
        for (int i = 0; i < HOW_IT_WORKS.length; i++) {
            JPanel words = new JPanel();
            words.setOpaque(false);
            words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
            Theme.stack(words, 3, Theme.text(HOW_IT_WORKS[i][0], Theme.BODY_BOLD, Theme.TEXT));
            Theme.stack(words, 2, new Theme.WrapText(HOW_IT_WORKS[i][1], Theme.BODY, Theme.MUTED));

            JPanel row = clear(new BorderLayout(12, 0));
            row.add(top(new Theme.Badge(String.valueOf(i + 1), 26, Theme.ACCENT_SOFT, Theme.ACCENT)), BorderLayout.WEST);
            row.add(words, BorderLayout.CENTER);
            col.addRow(row, i == 0 ? firstGap : 12);
        }
    }

    // =====================================================================
    // Other pages
    // =====================================================================

    private JComponent buildTransactionsPage() {
        Theme.FlatButton summary = button("This week's summary", Theme.ButtonKind.SECONDARY, this::showWeeklySummary);
        summary.setIcon(Icons.of(Icons.Name.BARS, 16, Theme.TEXT));
        transactionsPage = new TransactionsPanel("All transactions", Icons.Name.LIST, filterCategories(), null,
                summary, null, "No transactions yet. Use Add money to record your allowance.");
        transactionPanels.add(transactionsPage);

        JPanel actions = Theme.row(10,
                smallAction("+  Add expense", Icons.Name.RECEIPT, Theme.ButtonKind.PRIMARY, Color.WHITE, this::expenseDialog),
                smallAction("+  Add money", Icons.Name.CASH, Theme.ButtonKind.SOFT, Theme.TEXT, this::fundsDialog));
        return pageShell("Transactions", "Every peso in and out of your wallet", actions, transactionsPage);
    }

    private JComponent buildBudgetPage() {
        String frequency = settings.getAllowanceFrequency().toLowerCase();
        JPanel cards = clear(new GridLayout(1, 3, 16, 0));
        cards.add(statCard(Theme.VIOLET, Icons.Name.COINS, statTitle("Allowance"),
                fixedValue(Theme.peso(settings.getAllowanceAmount())),
                caption("Received " + frequency + ", about " + Theme.peso(settings.getDailyAllowance()) + " a day")));
        cards.add(statCard(Theme.BLUSH, Icons.Name.RECEIPT, statTitle("Daily limit"),
                fixedValue(Theme.peso(settings.getDailyLimit())),
                caption("The most you plan to spend in one day")));
        cards.add(statCard(Theme.HONEY, Icons.Name.CALENDAR, statTitle("Weekly limit"),
                fixedValue(Theme.peso(settings.getWeeklyLimit())),
                caption("Shared across the days left in each week")));

        JPanel body = clear(new BorderLayout(0, 20));
        body.add(cards, BorderLayout.NORTH);
        body.add(buildBudgetCard(), BorderLayout.CENTER);

        Theme.FlatButton edit = smallAction("Edit plan", Icons.Name.SLIDERS, Theme.ButtonKind.PRIMARY, Color.WHITE,
                this::customizeDashboard);
        edit.setToolTipText("Change your allowance, limits, categories and dashboard sections");
        return pageShell("Budget Plan", "Your allowance, spending limits and weekly category budgets", edit, body);
    }

    private static Theme.FitLabel fixedValue(String text) {
        Theme.FitLabel label = statValue();
        label.setText(text);
        return label;
    }

    private JComponent buildSavingsPage() {
        JPanel cards = clear(new GridLayout(1, 2, 16, 0));
        cards.add(savingsCard());
        Theme.FitLabel wallet = statValue();
        refreshers.add(() -> wallet.setText(Theme.peso(walletBalance)));
        cards.add(statCard(Theme.BLUE, Icons.Name.WALLET, statTitle("Wallet"), wallet,
                caption("What you can still spend or move to savings")));

        TransactionsPanel history = new TransactionsPanel("Savings history", Icons.Name.PIGGY_LINE,
                filterCategories(), SAVINGS, null, null,
                "Nothing moved to savings yet. Use Move to savings to start.");
        transactionPanels.add(history);

        JPanel body = clear(new BorderLayout(0, 20));
        body.add(cards, BorderLayout.NORTH);
        body.add(history, BorderLayout.CENTER);

        Theme.FlatButton move = smallAction("Move to savings", Icons.Name.PIGGY_LINE, Theme.ButtonKind.PRIMARY,
                Color.WHITE, this::savingsDialog);
        return pageShell("Savings", "Money you've set aside from your wallet", move, body);
    }

    private JComponent buildReportsPage() {
        Theme.FitLabel in = statValue();
        Theme.FitLabel out = statValue();
        Theme.FitLabel net = statValue();
        JPanel stats = clear(new GridLayout(1, 3, 16, 0));
        stats.add(statCard(Theme.MINT, Icons.Name.CASH, statTitle("Money in"), in,
                caption("Allowance and other money received")));
        stats.add(statCard(Theme.BLUSH, Icons.Name.RECEIPT, statTitle("Money out"), out,
                caption("Spending plus money moved to savings")));
        stats.add(statCard(Theme.BLUE, Icons.Name.SWAP, statTitle("Net"), net,
                caption("Money in minus money out")));
        refreshers.add(() -> {
            double[] totals = weekTotals();
            in.setText(Theme.peso(totals[0]));
            out.setText(Theme.peso(totals[1]));
            net.setText(Theme.signedPeso(totals[0] - totals[1]));
        });

        // Day-by-day chart
        DailyChart chart = new DailyChart();
        refreshers.add(() -> chart.setData(dailySpending(), settings.getDailyLimit(),
                appToday().getDayOfWeek().getValue() - 1));
        Theme.RoundedPanel chartCard = whiteCard(new BorderLayout(0, 12));
        JPanel chartHead = new JPanel();
        chartHead.setOpaque(false);
        chartHead.setLayout(new BoxLayout(chartHead, BoxLayout.Y_AXIS));
        Theme.stack(chartHead, 0, cardTitle("Daily spending", Icons.Name.BARS));
        JLabel chartNote = Theme.text("Each day compared with your daily limit", Theme.font(Font.PLAIN, 13), Theme.MUTED);
        chartNote.setBorder(new EmptyBorder(0, 38, 0, 0));
        Theme.stack(chartHead, 2, chartNote);
        chartCard.add(chartHead, BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);

        // Spending by category
        Theme.Column categories = new Theme.Column();
        categories.setBorder(new EmptyBorder(0, 0, 0, 12));
        refreshers.add(() -> fillCategoryReport(categories));
        Theme.RoundedPanel categoryCard = whiteCard(new BorderLayout(0, 16));
        categoryCard.setBorder(new EmptyBorder(18, 20, 16, 10));
        categoryCard.add(cardTitle("Spending by category", Icons.Name.PIE), BorderLayout.NORTH);
        JScrollPane scroll = Theme.scroll(categories);
        scroll.setPreferredSize(new Dimension(200, 220));
        categoryCard.add(scroll, BorderLayout.CENTER);

        JPanel lower = clear(new GridLayout(1, 2, 16, 0));
        lower.add(chartCard);
        lower.add(categoryCard);

        JPanel body = clear(new BorderLayout(0, 20));
        body.add(stats, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);

        Theme.FlatButton summary = smallAction("This week's summary", Icons.Name.LIST, Theme.ButtonKind.SECONDARY,
                Theme.TEXT, this::showWeeklySummary);
        return pageShell("Reports", "This week at a glance, " + weekRange(appToday()), summary, body);
    }

    private void fillCategoryReport(Theme.Column list) {
        list.clearRows();
        Map<String, Double> spentBy = new LinkedHashMap<>();
        for (String c : settings.getCategoryLimits().keySet()) spentBy.put(c, 0.0);
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (!isSpending(r)) continue;
            spentBy.merge(r.getCategorySource(), Math.abs(r.getAmount()), Double::sum);
            total += Math.abs(r.getAmount());
        }
        if (total <= 0) {
            list.addRow(new Theme.WrapText("No spending recorded this week yet.", Theme.BODY, Theme.MUTED), 0);
        }
        boolean first = true;
        for (Map.Entry<String, Double> e : spentBy.entrySet()) {
            if (total <= 0) break;
            Theme.Tint tint = Theme.tintFor(e.getKey());
            double share = e.getValue() / total;

            JPanel head = clear(new BorderLayout(8, 0));
            head.add(Theme.text(e.getKey(), Theme.font(Font.BOLD, 14), Theme.TEXT), BorderLayout.WEST);
            head.add(Theme.text(Theme.peso(e.getValue()), Theme.font(Font.BOLD, 14), Theme.TEXT), BorderLayout.EAST);
            Theme.Bar bar = new Theme.Bar(8);
            bar.set(share, tint.ink);
            JLabel note = Theme.text(Math.round(share * 100) + "% of this week's spending",
                    Theme.font(Font.PLAIN, 13), Theme.MUTED);

            JPanel words = clear(new BorderLayout(0, 7));
            words.add(head, BorderLayout.NORTH);
            words.add(bar, BorderLayout.CENTER);
            words.add(note, BorderLayout.SOUTH);
            JPanel row = clear(new BorderLayout(14, 0));
            row.add(top(new Theme.IconBox(Icons.of(Icons.forCategory(e.getKey()), 20, tint.ink), 40, tint.box, 10)),
                    BorderLayout.WEST);
            row.add(words, BorderLayout.CENTER);
            list.addRow(row, first ? 0 : 18);
            first = false;
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent buildSettingsPage() {
        Theme.Column col = new Theme.Column();
        col.addRow(settingRow(Icons.Name.SLIDERS, Theme.VIOLET, "Budget plan",
                "Change your allowance, spending limits, categories and which sections the dashboard shows.",
                button("Edit plan", Theme.ButtonKind.SECONDARY, this::customizeDashboard)), 0);
        col.addRow(settingRow(Icons.Name.CALENDAR, Theme.HONEY, "Presentation date",
                "Moni is treating " + appToday().format(LONG_DATE) + " as today. Change it to show a "
                        + "different day; the dates of your transactions stay the same.",
                button("Change date", Theme.ButtonKind.SECONDARY, this::changePresentationDate)), 14);
        col.addRow(settingRow(Icons.Name.INFO, Theme.BLUE, "How Moni works",
                "A short guide to every number on the dashboard.",
                button("Open guide", Theme.ButtonKind.SECONDARY, this::showHowItWorks)), 14);
        col.addRow(settingRow(Icons.Name.LOGOUT, Theme.BLUSH, "Account",
                "Signed in as " + currentUser.getFullName() + ", student number "
                        + currentUser.getStudentNumber() + " (" + currentUser.getEmail() + ").",
                button("Log out", Theme.ButtonKind.SECONDARY, this::logout)), 14);
        return pageShell("Settings", "Your plan, presentation date and account", null, col);
    }

    private static JPanel settingRow(Icons.Name icon, Theme.Tint tint, String title, String description,
                                     JComponent action) {
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(16, 0), 18, Theme.CARD, Theme.CARD_OUTLINE);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        Theme.stack(words, 0, Theme.text(title, Theme.font(Font.BOLD, 15), Theme.TEXT));
        Theme.stack(words, 4, new Theme.WrapText(description, Theme.BODY, Theme.MUTED));
        card.add(top(new Theme.IconBox(Icons.of(icon, 22, tint.ink), 44, tint.box, 12)), BorderLayout.WEST);
        card.add(words, BorderLayout.CENTER);
        card.add(centred(action), BorderLayout.EAST);
        return card;
    }

    private static Theme.FlatButton smallAction(String text, Icons.Name icon, Theme.ButtonKind kind, Color ink,
                                                Runnable action) {
        Theme.FlatButton b = button(text, kind, action);
        b.setIcon(Icons.of(icon, 18, ink));
        b.setBorder(new EmptyBorder(11, 16, 11, 16));
        b.setFont(Theme.font(Font.BOLD, 14));
        return b;
    }

    /** Bar chart of this week's spending, Monday to Sunday, with the daily limit as a dashed line. */
    private static final class DailyChart extends JComponent {
        private double[] values = new double[7];
        private double limit;
        private int todayIndex = -1;

        DailyChart() {
            setPreferredSize(new Dimension(300, 220));
        }

        void setData(double[] dayValues, double dailyLimit, int today) {
            values = dayValues;
            limit = dailyLimit;
            todayIndex = today;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Theme.smoothText(g2);

            int top = 26;
            int bottom = 28;
            int w = getWidth();
            int chartH = Math.max(40, getHeight() - top - bottom);
            double max = limit;
            for (double v : values) max = Math.max(max, v);
            if (max <= 0) max = 1;
            max *= 1.15;

            Font small = Theme.font(Font.PLAIN, 12);
            Font smallBold = Theme.font(Font.BOLD, 12);
            int slot = w / 7;
            int barW = Math.min(38, (int) (slot * 0.55));
            for (int i = 0; i < 7; i++) {
                int x = i * slot + (slot - barW) / 2;
                int bh = (int) Math.round(values[i] / max * chartH);
                int baseY = top + chartH;
                if (values[i] > 0) {
                    boolean over = limit > 0 && values[i] > limit;
                    g2.setColor(over ? Theme.RED : i == todayIndex ? Theme.ACCENT : Theme.ACCENT_MID);
                    g2.fillRoundRect(x, baseY - Math.max(bh, 6), barW, Math.max(bh, 6), 10, 10);
                    g2.setFont(small);
                    g2.setColor(Theme.TEXT_SOFT);
                    String label = Theme.peso(values[i]);
                    int lw = g2.getFontMetrics().stringWidth(label);
                    g2.drawString(label, x + (barW - lw) / 2, baseY - Math.max(bh, 6) - 6);
                } else {
                    g2.setColor(Theme.TRACK);
                    g2.fillRoundRect(x, baseY - 4, barW, 4, 4, 4);
                }
                g2.setFont(i == todayIndex ? smallBold : small);
                g2.setColor(i == todayIndex ? Theme.TEXT : Theme.MUTED);
                String day = i == todayIndex ? "Today" : DAY_NAMES[i];
                int dw = g2.getFontMetrics().stringWidth(day);
                g2.drawString(day, i * slot + (slot - dw) / 2, baseY + 20);
            }

            if (limit > 0) {
                int y = top + chartH - (int) Math.round(limit / max * chartH);
                g2.setColor(Theme.AMBER);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                        new float[]{6f, 5f}, 0f));
                g2.drawLine(0, y, w, y);
                g2.setFont(smallBold);
                String label = "Daily limit " + Theme.peso(limit);
                g2.drawString(label, w - g2.getFontMetrics().stringWidth(label), y - 6);
            }
            g2.dispose();
        }
    }

    // =====================================================================
    // Data refresh
    // =====================================================================

    /** Reloads balances and transactions from the database and redraws every page. */
    private void refreshDashboard() {
        LocalDate today = appToday();
        try {
            MoniDatabase.AccountData account = MoniDatabase.loadAccount(currentUser);
            walletBalance = account.walletBalance;
            savingsBalance = account.savingsBalance;
            weekTransactions = MoniDatabase.getTransactions(
                    currentUser, MoniDatabase.weekStart(today), MoniDatabase.weekEnd(today));
            recentTransactions = MoniDatabase.getRecentTransactions(currentUser, RECENT_LIMIT);
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

    // ---- totals worked out from this week's transactions -----------------

    /** Spending means money out, excluding transfers into savings. */
    private static boolean isSpending(TransactionRecord r) {
        return MONEY_OUT.equals(r.getFlowType()) && !SAVINGS.equals(r.getCategorySource());
    }

    private double spentOn(LocalDate day) {
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (isSpending(r) && LocalDate.parse(r.getDate()).equals(day)) total += Math.abs(r.getAmount());
        }
        return total;
    }

    private double spentThisWeek() {
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (isSpending(r)) total += Math.abs(r.getAmount());
        }
        return total;
    }

    private double spentThisWeek(String category) {
        double total = 0;
        for (TransactionRecord r : weekTransactions) {
            if (isSpending(r) && category.equals(r.getCategorySource())) total += Math.abs(r.getAmount());
        }
        return total;
    }

    /** Spending for each day of this week, Monday first. */
    private double[] dailySpending() {
        double[] days = new double[7];
        for (TransactionRecord r : weekTransactions) {
            if (isSpending(r)) days[LocalDate.parse(r.getDate()).getDayOfWeek().getValue() - 1] += Math.abs(r.getAmount());
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
        return settings.getWeeklyLimit() / 7.0 * appToday().lengthOfMonth();
    }

    /**
     * Today's spending plan: what is left of this week's limit, spread over the remaining days
     * (today included), but never more than the daily limit. Today's own spending is excluded
     * from the pool so it is only subtracted once, on the card.
     */
    private double todayPlan(LocalDate today) {
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

    /** Messages for the bell: {level, text}, where level is "red", "amber" or "info". */
    private List<String[]> alerts() {
        List<String[]> list = new ArrayList<>();
        LocalDate today = appToday();

        if (settings.getAllowanceAmount() <= 0) {
            list.add(new String[]{"info", "Your allowance is set to " + Theme.peso(0)
                    + ". Add it in Budget Plan so Moni can plan your spending."});
        }
        double plan = todayPlan(today);
        double spentToday = spentOn(today);
        if (plan > 0 && spentToday > plan) {
            list.add(new String[]{"red", "You're " + Theme.peso(spentToday - plan) + " over today's plan."});
        }
        double weekly = settings.getWeeklyLimit();
        double week = spentThisWeek();
        if (weekly > 0 && week >= weekly) {
            list.add(new String[]{"red", "You've used all of this week's " + Theme.peso(weekly) + " limit."});
        } else if (weekly > 0 && week >= weekly * 0.8) {
            list.add(new String[]{"amber", "You've used " + Math.round(week / weekly * 100)
                    + "% of this week's " + Theme.peso(weekly) + " limit."});
        }
        for (Map.Entry<String, Double> e : settings.getCategoryLimits().entrySet()) {
            double limit = e.getValue();
            double spent = spentThisWeek(e.getKey());
            if (limit <= 0) continue;
            if (spent > limit) {
                list.add(new String[]{"red", e.getKey() + " is " + Theme.peso(spent - limit) + " over budget this week."});
            } else if (spent >= limit * 0.8) {
                list.add(new String[]{"amber", e.getKey() + ": " + Theme.peso(spent) + " of "
                        + Theme.peso(limit) + " used this week."});
            }
        }
        return list;
    }

    // =====================================================================
    // Actions
    // =====================================================================

    private void expenseDialog() {
        Map<String, Double> limits = settings.getCategoryLimits();
        if (limits.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one spending category in Budget Plan first.",
                    "Add expense", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        FormDialog d = new FormDialog("Add expense", "Record money you spent from your wallet.", "Record expense");
        JTextField amount = Theme.field();
        JComboBox<String> category = new JComboBox<>(limits.keySet().toArray(new String[0]));
        Theme.styleCombo(category);
        JTextField description = Theme.field();
        JLabel budgetNote = Theme.text(" ", Theme.SMALL, Theme.MUTED);

        d.addRow("Amount (\u20B1)", amount);
        d.addRow("Category", category);
        d.addNote(budgetNote);
        d.addRow("Description", description);
        d.addNote(Theme.text("Wallet balance: " + Theme.money(walletBalance), Theme.SMALL, Theme.MUTED));

        Runnable updateNote = () -> {
            String c = (String) category.getSelectedItem();
            double spent = spentThisWeek(c);
            double limit = limits.get(c);
            budgetNote.setText(c + " this week: " + Theme.money(spent) + " of " + Theme.money(limit));
            budgetNote.setForeground(spent >= limit ? Theme.RED : Theme.MUTED);
        };
        category.addActionListener(e -> updateNote.run());
        updateNote.run();

        d.onConfirm(() -> {
            double a = d.readAmount(amount);
            if (a <= 0) return;
            if (a > walletBalance) {
                d.setError("Not enough in your wallet. Available: " + Theme.money(walletBalance));
                return;
            }

            LocalDate today = appToday();
            String c = (String) category.getSelectedItem();
            List<String> over = new ArrayList<>();
            if (settings.getDailyLimit() > 0 && spentOn(today) + a > settings.getDailyLimit()) {
                over.add("your daily limit (" + Theme.money(settings.getDailyLimit()) + ")");
            }
            if (settings.getWeeklyLimit() > 0 && spentThisWeek() + a > settings.getWeeklyLimit()) {
                over.add("your weekly limit (" + Theme.money(settings.getWeeklyLimit()) + ")");
            }
            if (spentThisWeek(c) + a > limits.get(c)) {
                over.add("your " + c + " budget (" + Theme.money(limits.get(c)) + ")");
            }
            if (!over.isEmpty()) {
                StringBuilder message = new StringBuilder("This expense takes you over:\n\n");
                for (String item : over) message.append("  \u2022 ").append(item).append('\n');
                message.append("\nRecord it anyway?");
                int choice = JOptionPane.showConfirmDialog(d, message.toString(), "Over your limit",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
            }

            String text = description.getText().trim().isEmpty() ? "Expense" : description.getText().trim();
            saveTransaction(d, new TransactionRecord(today.toString(), MONEY_OUT, c, text, -a),
                    walletBalance - a, savingsBalance);
        });
        d.open();
    }

    private void fundsDialog() {
        FormDialog d = new FormDialog("Add money", "Add your allowance or other money you received.", "Add money");
        JTextField amount = Theme.field();
        JComboBox<String> source = new JComboBox<>(new String[]{ALLOWANCE, OTHER_FUNDS});
        Theme.styleCombo(source);
        JLabel note = Theme.text(" ", Theme.SMALL, Theme.MUTED);

        d.addRow("Amount (\u20B1)", amount);
        d.addRow("Source", source);
        d.addNote(note);

        String frequency = settings.getAllowanceFrequency();
        String period = periodWord(frequency);

        Runnable updateNote = () -> {
            if (!ALLOWANCE.equals(source.getSelectedItem())) {
                note.setText("Other funds don't count toward your allowance.");
                return;
            }
            try {
                double received = allowanceReceived(frequency);
                note.setText("Allowance received " + period + ": " + Theme.money(received)
                        + " of " + Theme.money(settings.getAllowanceAmount()));
            } catch (Exception ex) {
                note.setText("Couldn't load your allowance status.");
            }
        };
        source.addActionListener(e -> updateNote.run());
        updateNote.run();

        d.onConfirm(() -> {
            double a = d.readAmount(amount);
            if (a <= 0) return;

            String selected = (String) source.getSelectedItem();
            if (ALLOWANCE.equals(selected)) {
                try {
                    double remaining = settings.getAllowanceAmount() - allowanceReceived(frequency);
                    if (a > remaining + 0.005) {
                        d.setError("That's more than your allowance. You can add up to "
                                + Theme.money(Math.max(0, remaining)) + " " + period + ".");
                        return;
                    }
                } catch (Exception ex) {
                    d.setError("Couldn't check your allowance: " + ex.getMessage());
                    return;
                }
            }
            TransactionRecord record = new TransactionRecord(
                    appToday().toString(), MONEY_IN, selected, "Added " + selected, a);
            saveTransaction(d, record, walletBalance + a, savingsBalance);
        });
        d.open();
    }

    private double allowanceReceived(String frequency) throws Exception {
        LocalDate now = appToday();
        return MoniDatabase.getAllowanceReceived(currentUser,
                MoniDatabase.periodStart(now, frequency), MoniDatabase.periodEnd(now, frequency));
    }

    private static String periodWord(String frequency) {
        if ("Weekly".equalsIgnoreCase(frequency)) return "this week";
        if ("Monthly".equalsIgnoreCase(frequency)) return "this month";
        return "today";
    }

    /** Moves wallet money into savings. */
    private void savingsDialog() {
        FormDialog d = new FormDialog("Move to savings", "Move money from your wallet into savings.",
                "Move to savings");
        JTextField amount = Theme.field();
        JTextField note = Theme.field();

        d.addRow("Amount (\u20B1)", amount);
        d.addRow("Note (optional)", note);
        d.addNote(Theme.text("Wallet balance: " + Theme.money(walletBalance), Theme.SMALL, Theme.MUTED));

        d.onConfirm(() -> {
            double a = d.readAmount(amount);
            if (a <= 0) return;
            if (a > walletBalance) {
                d.setError("Not enough in your wallet. Available: " + Theme.money(walletBalance));
                return;
            }
            String description = note.getText().trim().isEmpty() ? "Moved to savings" : note.getText().trim();
            TransactionRecord record = new TransactionRecord(
                    appToday().toString(), MONEY_OUT, SAVINGS, description, -a);
            saveTransaction(d, record, walletBalance - a, savingsBalance + a);
        });
        d.open();
    }

    private void saveTransaction(FormDialog d, TransactionRecord record, double newWallet, double newSavings) {
        try {
            MoniDatabase.saveTransactionAndBalances(currentUser, record, newWallet, newSavings);
            d.dispose();
            refreshDashboard();
        } catch (Exception e) {
            e.printStackTrace();
            d.setError("Could not save: " + e.getMessage());
        }
    }

    private void showWeeklySummary() {
        LocalDate today = appToday();
        JDialog d = new JDialog(this, "This week's summary", true);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(22, 24, 20, 24));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        Theme.stack(head, 0, Theme.text("This week's summary", Theme.H1, Theme.TEXT));
        Theme.stack(head, 4, Theme.text(weekRange(today), Theme.BODY, Theme.MUTED));

        DefaultTableModel model = TransactionsPanel.newModel();
        for (TransactionRecord r : weekTransactions) {
            model.addRow(new Object[]{LocalDate.parse(r.getDate()), r.getFlowType(), r.getCategorySource(),
                    Objects.toString(r.getDescription(), ""), r.getAmount()});
        }
        double[] totals = weekTotals();

        JPanel stats = clear(new GridLayout(1, 3, 12, 0));
        stats.add(miniStat("Money in", Theme.peso(totals[0]), Theme.GREEN));
        stats.add(miniStat("Money out", Theme.peso(totals[1]), Theme.RED));
        stats.add(miniStat("Net", Theme.signedPeso(totals[0] - totals[1]), Theme.TEXT));

        JPanel north = clear(new BorderLayout(0, 16));
        north.add(head, BorderLayout.NORTH);
        north.add(stats, BorderLayout.CENTER);

        Component center;
        if (model.getRowCount() > 0) {
            JTable table = TransactionsPanel.createTable(model);
            table.setRowSorter(new TableRowSorter<>(model)); // click a header to sort
            Theme.RoundedPanel box = Theme.tableBox(table);
            center = box;
        } else {
            center = Theme.text("No transactions this week yet.", Theme.BODY, Theme.MUTED);
        }

        JPanel south = clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.add(button("Close", Theme.ButtonKind.PRIMARY, d::dispose));

        root.add(north, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.setSize(900, 600);
        d.setLocationRelativeTo(this);
        Theme.onEscape(d, d::dispose);
        d.setVisible(true);
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
        JDialog d = new JDialog(this, "How Moni works", true);

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

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(24, 28, 20, 20));
        root.add(Theme.scroll(col), BorderLayout.CENTER);

        JPanel south = clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(button("Got it", Theme.ButtonKind.PRIMARY, d::dispose));
        root.add(south, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.setSize(600, 660);
        d.setLocationRelativeTo(this);
        Theme.onEscape(d, d::dispose);
        d.setVisible(true);
    }

    /**
     * Changes the date used by Moni for the presentation/demo.
     * This does not change the computer date or alter existing database dates.
     * It only changes the date Moni treats as "today" while the app is running.
     */
    private void changePresentationDate() {
        JSpinner dateSpinner = new JSpinner(
                new SpinnerDateModel(
                        java.sql.Date.valueOf(presentationDate),
                        java.sql.Date.valueOf(LocalDate.of(2020, 1, 1)),
                        java.sql.Date.valueOf(LocalDate.of(2099, 12, 31)),
                        java.util.Calendar.DAY_OF_MONTH
                )
        );
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MMM d, yyyy"));
        dateSpinner.setPreferredSize(new Dimension(150, 32));

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(8, 4, 4, 4));
        panel.add(Theme.text(
                "Choose the date Moni should use as \"today\" for this presentation.",
                Theme.BODY,
                Theme.TEXT
        ), BorderLayout.NORTH);
        panel.add(dateSpinner, BorderLayout.CENTER);

        JCheckBox reset = new JCheckBox("Reset to default demo date (Sep 24, 2026)");
        reset.setOpaque(false);
        reset.setFont(Theme.BODY);
        reset.addActionListener(e -> {
            if (reset.isSelected()) {
                dateSpinner.setValue(java.sql.Date.valueOf(DEFAULT_DEMO_DATE));
            }
        });
        panel.add(reset, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Presentation Date",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        java.util.Date selected = (java.util.Date) dateSpinner.getValue();
        presentationDate = selected.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        // Rebuild so every date-dependent section updates immediately.
        rebuild();
    }

    private void customizeDashboard() {
        OnboardingDialog d = new OnboardingDialog(this, currentUser, settings);
        d.setVisible(true);
        UserSettings updated = d.getResult();
        if (updated == null) return;

        try {
            MoniDatabase.saveSettings(currentUser, updated);
            settings = updated;
            rebuild(); // sections may have been shown/hidden, so rebuild the layout
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
    // Helpers
    // =====================================================================

    private static Theme.FlatButton button(String text, Theme.ButtonKind kind, Runnable action) {
        Theme.FlatButton b = new Theme.FlatButton(text, kind);
        b.addActionListener(e -> action.run());
        return b;
    }

    /** The user's categories plus the money sources, for the Category filter. */
    private List<String> filterCategories() {
        List<String> list = new ArrayList<>(settings.getCategoryLimits().keySet());
        for (String c : new String[]{SAVINGS, ALLOWANCE, OTHER_FUNDS}) {
            if (!list.contains(c)) list.add(c);
        }
        return list;
    }

    /** Stops a row from stretching taller inside a vertical BoxLayout. */
    private static void fixHeight(JComponent row) {
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
    }

    private String allowanceSummary() {
        return "Allowance: " + Theme.peso(settings.getAllowanceAmount()) + " "
                + settings.getAllowanceFrequency().toLowerCase();
    }

    private static String weekRange(LocalDate date) {
        return MoniDatabase.weekStart(date).format(RANGE_DATE) + " \u2013 " + MoniDatabase.weekEnd(date).format(RANGE_DATE);
    }

    private static String firstName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        return trimmed.isEmpty() ? "there" : trimmed.split("\\s+")[0];
    }

    private static String initial(String fullName) {
        String first = firstName(fullName);
        return "there".equals(first) ? "?" : first.substring(0, 1).toUpperCase();
    }

    private static void showError(Component parent, String message, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(parent, message + "\n\n" + e.getMessage(), "Moni", JOptionPane.ERROR_MESSAGE);
    }

    /** Small modal form used by the quick actions. Errors show inline instead of in pop-ups. */
    private final class FormDialog extends JDialog {
        private final JPanel form = new JPanel(new GridBagLayout());
        private final JLabel error = Theme.text(" ", Theme.SMALL, Theme.RED);
        private final Theme.FlatButton confirm;
        private int row;

        FormDialog(String title, String subtitle, String confirmText) {
            super(MainApp.this, title, true);

            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setBackground(Theme.CARD);
            root.setBorder(new EmptyBorder(22, 24, 18, 24));

            JPanel head = new JPanel();
            head.setOpaque(false);
            head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
            Theme.stack(head, 0, Theme.text(title, Theme.font(Font.BOLD, 20), Theme.TEXT));
            Theme.stack(head, 4, Theme.text(subtitle, Theme.BODY, Theme.MUTED));

            form.setOpaque(false);
            JPanel center = clear(new BorderLayout(0, 6));
            center.add(form, BorderLayout.CENTER);
            center.add(error, BorderLayout.SOUTH);

            confirm = new Theme.FlatButton(confirmText, Theme.ButtonKind.PRIMARY);
            Theme.FlatButton cancel = new Theme.FlatButton("Cancel", Theme.ButtonKind.SECONDARY);
            cancel.addActionListener(e -> dispose());
            JPanel buttons = clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.add(cancel);
            buttons.add(confirm);

            root.add(head, BorderLayout.NORTH);
            root.add(center, BorderLayout.CENTER);
            root.add(buttons, BorderLayout.SOUTH);
            setContentPane(root);

            getRootPane().setDefaultButton(confirm); // Enter submits
            Theme.onEscape(this, this::dispose);    // Esc cancels
        }

        void addRow(String label, JComponent field) {
            GridBagConstraints l = new GridBagConstraints();
            l.gridx = 0;
            l.gridy = row;
            l.anchor = GridBagConstraints.WEST;
            l.insets = new Insets(6, 0, 6, 16);
            form.add(Theme.text(label, Theme.BODY_BOLD, Theme.TEXT), l);

            GridBagConstraints f = new GridBagConstraints();
            f.gridx = 1;
            f.gridy = row++;
            f.weightx = 1;
            f.fill = GridBagConstraints.HORIZONTAL;
            f.insets = new Insets(6, 0, 6, 0);
            form.add(field, f);
        }

        void addNote(JLabel note) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = row++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 6, 0);
            form.add(note, c);
        }

        void onConfirm(Runnable action) {
            confirm.addActionListener(e -> {
                setError(" ");
                action.run();
            });
        }

        void setError(String message) {
            error.setText(message);
        }

        /** Returns the amount rounded to centavos, or -1 (and shows an error) if it isn't valid. */
        double readAmount(JTextField field) {
            try {
                double value = Math.round(Theme.parseAmount(field.getText()) * 100) / 100.0;
                if (value > 0) return value;
            } catch (NumberFormatException ignored) {
                // handled below
            }
            setError("Enter an amount greater than zero, e.g. 150 or 150.50.");
            field.requestFocusInWindow();
            return -1;
        }

        void open() {
            pack();
            setSize(Math.max(480, getWidth()), getHeight());
            setResizable(false);
            setLocationRelativeTo(MainApp.this);
            setVisible(true);
        }
    }

    // =====================================================================
    // Launch
    // =====================================================================

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default look-and-feel is fine.
            }
            showLogin();
        });
    }

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
            // Setup was not finished: show the sign-in screen again.
        }
    }
}