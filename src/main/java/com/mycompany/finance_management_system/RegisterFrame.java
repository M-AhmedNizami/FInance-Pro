package com.mycompany.finance_management_system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class RegisterFrame extends JFrame {
    private JTextField nameField = new JTextField(), emailField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JComboBox<String> profileCombo = new JComboBox<>(new String[]{"Student", "Family", "Small Business"});

    public RegisterFrame() {
        setTitle("Finance Manager - Sign Up");
        setSize(380, 440);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(25, 35, 25, 35));
        add(p);

        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        p.add(title);
        p.add(Box.createVerticalStrut(18));

        addInput(p, "Full Name", nameField, "e.g. Ahmed Nizami");
        addInput(p, "Email", emailField, "Enter your email");
        addInput(p, "Password", passwordField, "Create a password");

        p.add(new JLabel("Profile Type"));
        p.add(Box.createVerticalStrut(4));
        profileCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.add(profileCombo);
        p.add(Box.createVerticalStrut(18));

        JButton regBtn = new JButton("Sign Up");
        regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        regBtn.setBackground(new Color(59, 130, 246));
        regBtn.setForeground(Color.WHITE);
        regBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        regBtn.addActionListener(e -> handleRegister());
        p.add(regBtn);

        JButton backBtn = new JButton("Already have an account? Sign In");
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setForeground(new Color(96, 165, 250));
        backBtn.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        p.add(Box.createVerticalStrut(8));
        p.add(backBtn);
    }

    private void addInput(JPanel p, String label, JTextField field, String hint) {
        p.add(new JLabel(label));
        p.add(Box.createVerticalStrut(4));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.putClientProperty("JTextField.placeholderText", hint);
        p.add(field);
        p.add(Box.createVerticalStrut(10));
    }

    private void handleRegister() {
        String name = nameField.getText().trim(), email = emailField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }
        String sql = "INSERT INTO Users (full_name, email, password_hash, profile_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, pass);
            ps.setString(4, (String) profileCombo.getSelectedItem());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Account created! You can now sign in.");
            dispose();
            new LoginFrame().setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getErrorCode() == 1062 ? "Email already exists." : "Error: " + ex.getMessage());
        }
    }
}