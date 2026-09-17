package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AuthDialog extends JDialog {
    private User authenticatedUser = null;

    public AuthDialog(Frame parent) {
        super(parent, "Student Portal - Authentication", true);
        setSize(420, 380);
        setLocationRelativeTo(parent);
        setResizable(false);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel loginPanel = new JPanel(new GridLayout(4, 2, 10, 12));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        loginPanel.add(new JLabel("Student Number:"));
        JTextField loginStudentNumField = new JTextField("2026-1001");
        loginPanel.add(loginStudentNumField);

        loginPanel.add(new JLabel("Password:"));
        JPasswordField loginPassField = new JPasswordField("pass123");
        loginPanel.add(loginPassField);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 13));
        loginPanel.add(new JLabel());
        loginPanel.add(loginBtn);

        tabbedPane.addTab("Login", loginPanel);

        JPanel registerPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        registerPanel.add(new JLabel("Student Number:"));
        JTextField regStudentNumField = new JTextField();
        registerPanel.add(regStudentNumField);

        registerPanel.add(new JLabel("Full Name:"));
        JTextField regNameField = new JTextField();
        registerPanel.add(regNameField);

        registerPanel.add(new JLabel("Email Address:"));
        JTextField regEmailField = new JTextField();
        registerPanel.add(regEmailField);

        registerPanel.add(new JLabel("Password:"));
        JPasswordField regPassField = new JPasswordField();
        registerPanel.add(regPassField);

        JButton registerBtn = new JButton("Create Account");
        registerBtn.setFont(new Font("Arial", Font.BOLD, 13));
        registerPanel.add(new JLabel());
        registerPanel.add(registerBtn);

        tabbedPane.addTab("Register", registerPanel);
        add(tabbedPane);

        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String studNum = loginStudentNumField.getText().trim();
                String pass = new String(loginPassField.getPassword()).trim();

                User user = AuthManager.login(studNum, pass);
                if (user != null) {
                    authenticatedUser = user;
                    JOptionPane.showMessageDialog(AuthDialog.this, 
                        "Welcome back, " + user.getFullName() + "!", 
                        "Login Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(AuthDialog.this, 
                        "Invalid Student Number or Password.", 
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        registerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String studNum = regStudentNumField.getText().trim();
                String name = regNameField.getText().trim();
                String email = regEmailField.getText().trim();
                String pass = new String(regPassField.getPassword()).trim();

                if (studNum.isEmpty() || name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(AuthDialog.this, 
                        "All fields are required.", 
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean success = AuthManager.register(studNum, name, email, pass);
                if (success) {
                    JOptionPane.showMessageDialog(AuthDialog.this, 
                        "Account registered! You can now log in.", 
                        "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                    tabbedPane.setSelectedIndex(0);
                    loginStudentNumField.setText(studNum);
                    loginPassField.setText("");
                } else {
                    JOptionPane.showMessageDialog(AuthDialog.this, 
                        "Registration failed: Student Number already exists.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}