package com.mycompany.finance_management_system;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField emailField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();

    public LoginFrame() {
        setTitle("Finance Manager");
        setSize(380, 340);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(25, 35, 25, 35));
        add(p);

        JLabel title = new JLabel("Welcome Back");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        
        JLabel sub = new JLabel("Sign in to manage your finances");
        sub.setForeground(Color.GRAY);
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        addInput(p, "Email", emailField, "Enter your email");
        addInput(p, "Password", passwordField, "Enter your password");

        JButton loginBtn = new JButton("Sign In");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginBtn.setBackground(new Color(59, 130, 246));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.addActionListener(e -> handleLogin());
        p.add(loginBtn);
        getRootPane().setDefaultButton(loginBtn);

        JButton signupLink = new JButton("Don't have an account? Sign Up");
        signupLink.setBorderPainted(false);
        signupLink.setContentAreaFilled(false);
        signupLink.setForeground(new Color(96, 165, 250));
        signupLink.addActionListener(e -> { dispose(); new RegisterFrame().setVisible(true); });
        p.add(Box.createVerticalStrut(10));
        p.add(signupLink);
    }

    private void addInput(JPanel p, String label, JTextField field, String hint) {
        p.add(new JLabel(label));
        p.add(Box.createVerticalStrut(4));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.putClientProperty("JTextField.placeholderText", hint);
        p.add(field);
        p.add(Box.createVerticalStrut(12));
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }
        String sql = "SELECT user_id, full_name, password_hash FROM Users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && pass.equals(rs.getString("password_hash"))) {
                JOptionPane.showMessageDialog(this, "Welcome back, " + rs.getString("full_name") + "!");
                dispose();
                new DashboardFrame(rs.getInt("user_id"), rs.getString("full_name")).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid email or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}