package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Sign-in / create-account window shown before the dashboard. */
public class AuthDialog extends JDialog {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final CardLayout formCards = new CardLayout();
    private final JPanel forms = new JPanel(formCards);
    private final Theme.FlatButton signInTab = new Theme.FlatButton("Sign in", Theme.ButtonKind.SECONDARY);
    private final Theme.FlatButton createTab = new Theme.FlatButton("Create account", Theme.ButtonKind.TAB);

    private final JTextField loginStudent = Theme.field();
    private final JPasswordField loginPassword = new JPasswordField(18);
    private final JLabel loginError = Theme.text(" ", Theme.SMALL, Theme.RED);
    private final Theme.FlatButton loginButton = new Theme.FlatButton("Sign in", Theme.ButtonKind.PRIMARY);

    private final JTextField regStudent = Theme.field();
    private final JTextField regName = Theme.field();
    private final JTextField regEmail = Theme.field();
    private final JPasswordField regPassword = new JPasswordField(18);
    private final JPasswordField regConfirm = new JPasswordField(18);
    private final JLabel registerError = Theme.text(" ", Theme.SMALL, Theme.RED);
    private final Theme.FlatButton registerButton = new Theme.FlatButton("Create account", Theme.ButtonKind.PRIMARY);

    private User authenticatedUser;

    public AuthDialog(Frame parent) {
        super(parent, "Moni", true);
        setSize(840, 680);
        setMinimumSize(new Dimension(720, 560));
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CARD);
        root.add(createBrandPanel(), BorderLayout.WEST);
        root.add(createFormPanel(), BorderLayout.CENTER);
        setContentPane(root);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());

        signInTab.addActionListener(e -> showForm(true));
        createTab.addActionListener(e -> showForm(false));
        showForm(true);
    }

    private void showForm(boolean signIn) {
        formCards.show(forms, signIn ? "login" : "register");
        signInTab.setKind(signIn ? Theme.ButtonKind.SECONDARY : Theme.ButtonKind.TAB);
        createTab.setKind(signIn ? Theme.ButtonKind.TAB : Theme.ButtonKind.SECONDARY);
        getRootPane().setDefaultButton(signIn ? loginButton : registerButton);
        (signIn ? loginStudent : regStudent).requestFocusInWindow();
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(280, 0));
        panel.setBackground(Theme.INK);
        panel.setBorder(new EmptyBorder(56, 32, 32, 32));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(Theme.logo(40));
        JLabel name = Theme.text("MONI", Theme.font(Font.BOLD, 34), Color.WHITE);
        name.setBorder(new EmptyBorder(0, 12, 0, 0));
        brand.add(name);
        fixHeight(brand);
        Theme.stack(panel, 0, brand);

        Theme.stack(panel, 16, new Theme.WrapText("Know how much you can spend today, so your allowance "
                + "lasts until the next one arrives.", Theme.font(Font.PLAIN, 15), Color.WHITE));

        Theme.stack(panel, 32, Theme.text("How it works", Theme.LABEL, Theme.ON_INK_MUTED));
        String[] steps = {"Add your allowance", "Record what you spend", "See what's left for today"};
        for (int i = 0; i < steps.length; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.add(new Theme.Badge(String.valueOf(i + 1), 24, Theme.SAGE, Theme.TEXT));
            JLabel step = Theme.text(steps[i], Theme.BODY_BOLD, Color.WHITE);
            step.setBorder(new EmptyBorder(0, 12, 0, 0));
            row.add(step);
            fixHeight(row);
            Theme.stack(panel, i == 0 ? 12 : 10, row);
        }

        panel.add(Box.createVerticalGlue());
        Theme.stack(panel, 0, Theme.text("Student money manager", Theme.SMALL, Theme.ON_INK_MUTED));
        return panel;
    }

    private static void fixHeight(JPanel row) {
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(Theme.CARD);
        panel.setBorder(new EmptyBorder(40, 44, 20, 44));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        Theme.stack(heading, 0, Theme.text("Welcome to Moni", Theme.H1, Theme.TEXT));
        Theme.stack(heading, 6, Theme.text("Sign in or create your student money account.", Theme.BODY, Theme.MUTED));

        JPanel switcher = new Theme.RoundedPanel(new GridLayout(1, 2, 4, 0), 14, Theme.ACCENT_SOFT, null);
        switcher.setBorder(new EmptyBorder(4, 4, 4, 4));
        switcher.add(signInTab);
        switcher.add(createTab);
        Theme.stack(heading, 22, switcher);

        forms.setOpaque(false);
        forms.add(Theme.scroll(createLoginForm()), "login");
        forms.add(Theme.scroll(createRegisterForm()), "register");

        panel.add(heading, BorderLayout.NORTH);
        panel.add(forms, BorderLayout.CENTER);
        return panel;
    }

    private Theme.Column createLoginForm() {
        Theme.styleField(loginPassword);
        Theme.Column col = form();
        addField(col, "Student number", loginStudent, 0);
        addField(col, "Password", loginPassword, 14);
        col.addRow(loginError, 10);
        col.addRow(loginButton, 6);
        col.addRow(Theme.text("New to Moni? Choose Create account above.", Theme.SMALL, Theme.MUTED), 14);
        return col;
    }

    private Theme.Column createRegisterForm() {
        Theme.styleField(regPassword);
        Theme.styleField(regConfirm);
        Theme.Column col = form();
        addField(col, "Student number", regStudent, 0);
        addField(col, "Full name", regName, 10);
        addField(col, "Email address", regEmail, 10);
        addField(col, "Password", regPassword, 10);
        addField(col, "Confirm password", regConfirm, 10);
        col.addRow(registerError, 10);
        col.addRow(registerButton, 6);
        return col;
    }

    private static Theme.Column form() {
        Theme.Column col = new Theme.Column();
        col.setBorder(new EmptyBorder(4, 0, 12, 4));
        return col;
    }

    private static void addField(Theme.Column col, String label, JTextField field, int gapAbove) {
        col.addRow(Theme.text(label, Theme.BODY_BOLD, Theme.TEXT), gapAbove);
        col.addRow(field, 6);
    }

    private void login() {
        String student = loginStudent.getText().trim();
        String password = new String(loginPassword.getPassword()).trim();
        if (student.isEmpty() || password.isEmpty()) {
            loginError.setText("Enter your student number and password.");
            return;
        }

        User user = AuthManager.login(student, password);
        if (user == null) {
            loginError.setText("Incorrect student number or password.");
            loginPassword.setText("");
            loginPassword.requestFocusInWindow();
            return;
        }
        authenticatedUser = user;
        dispose();
    }

    private void register() {
        String student = regStudent.getText().trim();
        String name = regName.getText().trim();
        String email = regEmail.getText().trim();
        String password = new String(regPassword.getPassword()).trim();
        String confirm = new String(regConfirm.getPassword()).trim();

        if (student.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            registerError.setText("Please fill in every field.");
            return;
        }
        if (!email.matches(EMAIL_PATTERN)) {
            registerError.setText("That email address doesn't look right.");
            regEmail.requestFocusInWindow();
            return;
        }
        if (!password.equals(confirm)) {
            registerError.setText("The two passwords don't match.");
            regConfirm.setText("");
            regConfirm.requestFocusInWindow();
            return;
        }
        if (!AuthManager.register(student, name, email, password)) {
            registerError.setText("That student number or email is already registered, "
                    + "or the database is unavailable.");
            return;
        }

        JOptionPane.showMessageDialog(this, "Your account is ready. Sign in to set up Moni.",
                "Account created", JOptionPane.INFORMATION_MESSAGE);

        for (JTextField f : new JTextField[]{regStudent, regName, regEmail, regPassword, regConfirm}) f.setText("");
        registerError.setText(" ");

        showForm(true);
        loginStudent.setText(student);
        loginError.setText(" ");
        loginPassword.requestFocusInWindow();
    }
}