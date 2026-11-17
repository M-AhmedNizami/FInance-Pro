package com.mycompany.finance_management_system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;

public class FuturePlansFrame extends JFrame {
    private int userId;
    private DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Plan", "Target (Rs.)", "Spent (Rs.)", "Remaining (Rs.)", "Progress"}, 0
    ) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private JTable table = new JTable(model);
    private JTextField planField = new JTextField(14), targetField = new JTextField(8), spendField = new JTextField(8);
    private JLabel summaryLabel = new JLabel("Select a plan below to log spending.");

    public FuturePlansFrame(int userId) {
        this.userId = userId;
        setTitle("Future Plans & Goals");
        setSize(760, 440);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        add(root);

        // Top: Create New Plan
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        planField.putClientProperty("JTextField.placeholderText", "e.g. New Laptop, Trip");
        targetField.putClientProperty("JTextField.placeholderText", "0.00");
        JButton addPlanBtn = new JButton("+ Add Plan");
        addPlanBtn.setBackground(new Color(59, 130, 246));
        addPlanBtn.setForeground(Color.WHITE);
        addPlanBtn.addActionListener(e -> addPlan());

        top.add(new JLabel("Plan:")); top.add(planField);
        top.add(new JLabel("Target (Rs.):")); top.add(targetField);
        top.add(addPlanBtn);
        root.add(top, BorderLayout.NORTH);

        // Center: Plans Table
        table.setRowHeight(26);
        table.getSelectionModel().addListSelectionListener(e -> updateSuggestion());
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom: Log Spend & Suggestion
        JPanel bottom = new JPanel(new BorderLayout(10, 8));
        summaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        summaryLabel.setForeground(new Color(52, 211, 153));
        bottom.add(summaryLabel, BorderLayout.NORTH);

        JPanel spendBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        spendField.putClientProperty("JTextField.placeholderText", "0.00");
        JButton spendBtn = new JButton("Log Spend / Put Money");
        spendBtn.setBackground(new Color(16, 185, 129));
        spendBtn.setForeground(Color.WHITE);
        spendBtn.addActionListener(e -> logSpend());

        JButton delBtn = new JButton("Delete Plan");
        delBtn.setBackground(new Color(239, 68, 68));
        delBtn.setForeground(Color.WHITE);
        delBtn.addActionListener(e -> deletePlan());

        spendBar.add(new JLabel("Amount (Rs.):")); spendBar.add(spendField);
        spendBar.add(spendBtn); spendBar.add(delBtn);
        bottom.add(spendBar, BorderLayout.SOUTH);

        root.add(bottom, BorderLayout.SOUTH);
        loadPlans();
    }

    private void loadPlans() {
        model.setRowCount(0);
        String sql = "SELECT plan_id, plan_name, target_amount, spent_amount FROM Future_Plans WHERE user_id = ? ORDER BY plan_id DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BigDecimal target = rs.getBigDecimal("target_amount");
                BigDecimal spent = rs.getBigDecimal("spent_amount");
                BigDecimal left = target.subtract(spent).max(BigDecimal.ZERO);
                int pct = target.compareTo(BigDecimal.ZERO) > 0
                        ? spent.multiply(BigDecimal.valueOf(100)).divide(target, 0, RoundingMode.HALF_UP).intValue()
                        : 0;
                model.addRow(new Object[]{
                    rs.getInt("plan_id"), rs.getString("plan_name"), target, spent, left, Math.min(pct, 100) + "%"
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading plans: " + ex.getMessage());
        }
    }

    private void addPlan() {
        String name = planField.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a plan name."); return; }
        try {
            BigDecimal target = new BigDecimal(targetField.getText().trim());
            String sql = "INSERT INTO Future_Plans (user_id, plan_name, target_amount) VALUES (?, ?, ?)";
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, userId); ps.setString(2, name); ps.setBigDecimal(3, target);
                ps.executeUpdate();
                planField.setText(""); targetField.setText("");
                loadPlans();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid target amount.");
        }
    }

    private void logSpend() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a plan from the table first."); return; }
        try {
            int planId = (int) model.getValueAt(row, 0);
            BigDecimal amt = new BigDecimal(spendField.getText().trim());
            String sql = "UPDATE Future_Plans SET spent_amount = spent_amount + ? WHERE plan_id = ? AND user_id = ?";
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBigDecimal(1, amt); ps.setInt(2, planId); ps.setInt(3, userId);
                ps.executeUpdate();
                spendField.setText("");
                loadPlans();
                if (row < model.getRowCount()) table.setRowSelectionInterval(row, row);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount.");
        }
    }

    private void deletePlan() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int planId = (int) model.getValueAt(row, 0);
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM Future_Plans WHERE plan_id = ?")) {
            ps.setInt(1, planId);
            ps.executeUpdate();
            summaryLabel.setText("Plan deleted.");
            loadPlans();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting plan: " + ex.getMessage());
        }
    }

    private void updateSuggestion() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        String name = (String) model.getValueAt(row, 1);
        BigDecimal left = (BigDecimal) model.getValueAt(row, 4);
        if (left.compareTo(BigDecimal.ZERO) <= 0) {
            summaryLabel.setForeground(new Color(52, 211, 153));
            summaryLabel.setText("Goal '" + name + "' is 100% completed! Nothing left to spend.");
        } else {
            summaryLabel.setForeground(new Color(96, 165, 250));
            summaryLabel.setText("For '" + name + "', you still need Rs. " + left + " left.");
        }
    }
}