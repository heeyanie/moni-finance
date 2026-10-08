package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Small modal form used by Add expense, Add money and Move to savings.
 * Errors are shown inside the dialog instead of in a separate pop-up.
 */
class FormDialog extends JDialog {
    private final JPanel form = new JPanel(new GridBagLayout());
    private final JLabel error = Theme.text(" ", Theme.SMALL, Theme.RED);
    private final Theme.FlatButton confirm;
    private int row;

    FormDialog(Frame owner, String title, String subtitle, String confirmText) {
        super(owner, title, true);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.CARD);
        root.setBorder(new EmptyBorder(22, 24, 18, 24));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        Theme.stack(head, 0, Theme.text(title, Theme.font(Font.BOLD, 20), Theme.TEXT));
        Theme.stack(head, 4, Theme.text(subtitle, Theme.BODY, Theme.MUTED));

        form.setOpaque(false);
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(form, BorderLayout.CENTER);
        center.add(error, BorderLayout.SOUTH);

        confirm = new Theme.FlatButton(confirmText, Theme.ButtonKind.PRIMARY);
        Theme.FlatButton cancel = new Theme.FlatButton("Cancel", Theme.ButtonKind.SECONDARY);
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
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
        }
        setError("Enter an amount greater than zero, e.g. 150 or 150.50.");
        field.requestFocusInWindow();
        return -1;
    }

    void open() {
        pack();
        setSize(Math.max(480, getWidth()), getHeight());
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }
}
