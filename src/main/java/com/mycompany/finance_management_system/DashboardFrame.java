package com.mycompany.finance_management_system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class DashboardFrame extends JFrame {
    private int userId;
    private YearMonth currentMonth = YearMonth.now();
    private JLabel monthLabel = new JLabel(currentMonth.toString());
    private JLabel balanceValue = new JLabel("Rs. 0.00");
    private JLabel expenseValue = new JLabel("Rs. 0.00");
    private JLabel budgetStatusLabel = new JLabel("No budget set");
    private JProgressBar budgetBar = new JProgressBar(0, 100);

    private JPanel notificationBar = new JPanel(new BorderLayout());
    private JLabel notificationLabel = new JLabel("No active warnings for this month.");

    private DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Date", "Type", "Category", "Amount", "Description"}, 0
    ) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    private JTextField searchField = new JTextField();
    private JTextField amountField = new JTextField(8), descField = new JTextField(14);
    private JComboBox<String> typeBox = new JComboBox<>(new String[]{"Expense", "Income"});
    private JComboBox<String> catBox = new JComboBox<>();
    private DonutChartPanel chartPanel = new DonutChartPanel();
    private DailyExpenseCardPanel dailyGraphPanel = new DailyExpenseCardPanel();

    private Stack<Tx> undoStack = new Stack<>(), redoStack = new Stack<>();
    private static class Tx {
        int id, catId; BigDecimal amt; String type, desc; Date date;
        Tx(int id, int catId, BigDecimal amt, String type, String desc, Date date) {
            this.id = id; this.catId = catId; this.amt = amt; this.type = type; this.desc = desc; this.date = date;
        }
    }

    public DashboardFrame(int userId, String fullName) {
        this.userId = userId;
        setTitle("Finance Manager - Dashboard");
        setSize(1240, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        add(root);

        // --- LEFT SIDEBAR ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(215, 0));
        sidebar.setBackground(new Color(30, 32, 36));
        sidebar.setBorder(new EmptyBorder(20, 14, 18, 14));
        sidebar.putClientProperty("FlatLaf.style", "arc: 16");

        JLabel brand = new JLabel("FINANCE PRO");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brand.setForeground(new Color(96, 165, 250));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userLbl = new JLabel("Hi, " + fullName);
        userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLbl.setForeground(new Color(156, 163, 175));
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(userLbl);
        sidebar.add(Box.createVerticalStrut(20));

        // Section 1: Quick Actions
        JLabel actionsHeader = new JLabel("QUICK ACTIONS");
        actionsHeader.setFont(new Font("Segoe UI", Font.BOLD, 10));
        actionsHeader.setForeground(new Color(107, 114, 128));
        actionsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(actionsHeader);
        sidebar.add(Box.createVerticalStrut(8));

        JButton pocketBtn = createSideBtn("+ Pocket Money", new Color(16, 185, 129));
        pocketBtn.addActionListener(e -> addPocketMoney());

        JButton budgetBtn = createSideBtn("Set Monthly Budget", new Color(59, 130, 246));
        budgetBtn.addActionListener(e -> setMonthlyBudget());

        sidebar.add(pocketBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(budgetBtn);
        sidebar.add(Box.createVerticalStrut(16));

        JSeparator sep1 = new JSeparator();
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep1.setForeground(new Color(55, 58, 64));
        sidebar.add(sep1);
        sidebar.add(Box.createVerticalStrut(14));

        // Section 2: Tools
        JLabel toolsHeader = new JLabel("TOOLS");
        toolsHeader.setFont(new Font("Segoe UI", Font.BOLD, 10));
        toolsHeader.setForeground(new Color(107, 114, 128));
        toolsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(toolsHeader);
        sidebar.add(Box.createVerticalStrut(8));

        JButton calcBtn = createSideBtn("Calculator & Pad", new Color(55, 65, 81));
        calcBtn.addActionListener(e -> new CalculatorFrame().setVisible(true));

        JButton plansBtn = createSideBtn("Future Plans", new Color(55, 65, 81));
        plansBtn.addActionListener(e -> new FuturePlansFrame(userId).setVisible(true));

        sidebar.add(calcBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(plansBtn);
        sidebar.add(Box.createVerticalStrut(16));

        JSeparator sep2 = new JSeparator();
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep2.setForeground(new Color(55, 58, 64));
        sidebar.add(sep2);
        sidebar.add(Box.createVerticalStrut(14));

        // Section 3: Smart Features
        JLabel smartHeader = new JLabel("SMART FEATURES");
        smartHeader.setFont(new Font("Segoe UI", Font.BOLD, 10));
        smartHeader.setForeground(new Color(107, 114, 128));
        smartHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(smartHeader);
        sidebar.add(Box.createVerticalStrut(8));

        JButton noddyBtn = createSideBtn("Noddy AI", new Color(109, 40, 217));
        noddyBtn.setIcon(createRobotIcon(22));
        noddyBtn.setIconTextGap(10);
        noddyBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Noddy AI Personal Assistant is coming soon!", "Coming Soon", JOptionPane.INFORMATION_MESSAGE));

        JButton bankSyncBtn = createSideBtn("Bank Sync", new Color(55, 65, 81));
        bankSyncBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Bank Sync is coming soon!", "Coming Soon", JOptionPane.INFORMATION_MESSAGE));

        JButton investBtn = createSideBtn("Smart Investing", new Color(55, 65, 81));
        investBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Smart Investing is coming soon!", "Coming Soon", JOptionPane.INFORMATION_MESSAGE));

        sidebar.add(noddyBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(bankSyncBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(investBtn);

        sidebar.add(Box.createVerticalGlue());

        JLabel planBadge = new JLabel("Storage: 1-Year Free Plan");
        planBadge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        planBadge.setForeground(new Color(107, 114, 128));
        planBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(planBadge);
        sidebar.add(Box.createVerticalStrut(10));

        JButton logoutBtn = createSideBtn("Logout", new Color(220, 38, 38));
        logoutBtn.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        sidebar.add(logoutBtn);

        root.add(sidebar, BorderLayout.WEST);

        // --- MAIN CONTENT AREA ---
        JPanel mainArea = new JPanel(new BorderLayout(12, 10));
        root.add(mainArea, BorderLayout.CENTER);

        JPanel topWrapper = new JPanel(new BorderLayout(0, 8));

        JPanel topNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton prevMonthBtn = new JButton("<");
        JButton nextMonthBtn = new JButton("Next Month >");
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        prevMonthBtn.addActionListener(e -> {
            YearMonth prev = currentMonth.minusMonths(1);
            if (prev.isBefore(YearMonth.now().minusYears(1))) {
                JOptionPane.showMessageDialog(this,
                        "Upgrade to Premium for more cloud storage to access data older than 1 year!",
                        "Premium Feature", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            currentMonth = prev;
            monthLabel.setText(currentMonth.toString());
            loadData();
        });
        nextMonthBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            monthLabel.setText(currentMonth.toString());
            loadData();
        });

        topNav.add(prevMonthBtn);
        topNav.add(monthLabel);
        topNav.add(nextMonthBtn);
        topWrapper.add(topNav, BorderLayout.NORTH);

        notificationBar.setBackground(new Color(34, 37, 42));
        notificationBar.setBorder(new EmptyBorder(8, 14, 8, 14));
        notificationBar.putClientProperty("FlatLaf.style", "arc: 10");
        notificationLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        notificationLabel.setForeground(new Color(156, 163, 175));
        notificationBar.add(notificationLabel, BorderLayout.WEST);
        notificationBar.setVisible(false);
        topWrapper.add(notificationBar, BorderLayout.SOUTH);

        mainArea.add(topWrapper, BorderLayout.NORTH);

        JPanel centerContent = new JPanel(new BorderLayout(12, 12));
        mainArea.add(centerContent, BorderLayout.CENTER);

        JPanel kpiRow = new JPanel(new GridLayout(1, 2, 12, 0));
        kpiRow.setPreferredSize(new Dimension(0, 110));

        JPanel balanceCard = createKpiCard("TOTAL BALANCE", balanceValue, new Color(52, 211, 153));
        JPanel expenseCard = createKpiCard("MONTHLY EXPENSE", expenseValue, new Color(248, 113, 113));

        budgetBar.setPreferredSize(new Dimension(0, 8));
        budgetBar.setStringPainted(false);
        budgetStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        budgetStatusLabel.setForeground(new Color(156, 163, 175));

        JPanel budgetBox = new JPanel(new BorderLayout(0, 4));
        budgetBox.setOpaque(false);
        budgetBox.add(budgetStatusLabel, BorderLayout.NORTH);
        budgetBox.add(budgetBar, BorderLayout.SOUTH);
        expenseCard.add(budgetBox, BorderLayout.SOUTH);

        kpiRow.add(balanceCard);
        kpiRow.add(expenseCard);

        JTable table = new JTable(tableModel);
        table.setRowSorter(sorter);
        table.setRowHeight(30);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(50, 53, 58));
        table.getColumnModel().getColumn(3).setCellRenderer(new AmountCellRenderer());

        searchField.setPreferredSize(new Dimension(0, 32));
        searchField.putClientProperty("JTextField.placeholderText", "Search transactions by category or note...");
        searchField.putClientProperty("FlatLaf.style", "arc: 10");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });

        JPanel tableSection = new JPanel(new BorderLayout(0, 8));
        tableSection.add(searchField, BorderLayout.NORTH);
        tableSection.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel leftMain = new JPanel(new BorderLayout(0, 12));
        leftMain.add(kpiRow, BorderLayout.NORTH);
        leftMain.add(tableSection, BorderLayout.CENTER);

        JPanel rightCharts = new JPanel(new GridLayout(2, 1, 0, 12));
        rightCharts.setPreferredSize(new Dimension(280, 0));
        rightCharts.add(chartPanel);
        rightCharts.add(dailyGraphPanel);

        centerContent.add(leftMain, BorderLayout.CENTER);
        centerContent.add(rightCharts, BorderLayout.EAST);

        // Bottom Input Ribbon
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 63, 65)),
                new EmptyBorder(12, 0, 0, 0)
        ));

        JPanel inputsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        typeBox.setPreferredSize(new Dimension(85, 32));
        catBox.setPreferredSize(new Dimension(135, 32));
        JButton addCatBtn = new JButton("+");
        addCatBtn.setPreferredSize(new Dimension(34, 32));
        addCatBtn.setToolTipText("Add Custom Category");
        addCatBtn.addActionListener(e -> addCustomCategory());

        amountField.setPreferredSize(new Dimension(75, 32));
        amountField.putClientProperty("JTextField.placeholderText", "0.00");
        descField.setPreferredSize(new Dimension(120, 32));
        descField.putClientProperty("JTextField.placeholderText", "e.g. Lunch");

        inputsPanel.add(new JLabel("Type:")); inputsPanel.add(typeBox);
        inputsPanel.add(new JLabel("Category:")); inputsPanel.add(catBox); inputsPanel.add(addCatBtn);
        inputsPanel.add(new JLabel("Amount:")); inputsPanel.add(amountField);
        inputsPanel.add(new JLabel("Note:")); inputsPanel.add(descField);

        JButton addBtn = new JButton("+ Add");
        addBtn.setPreferredSize(new Dimension(70, 32));
        addBtn.setBackground(new Color(59, 130, 246));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addBtn.addActionListener(e -> addTransaction());
        inputsPanel.add(addBtn);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton undoBtn = new JButton("Undo");
        JButton redoBtn = new JButton("Redo");
        undoBtn.setPreferredSize(new Dimension(60, 32));
        redoBtn.setPreferredSize(new Dimension(60, 32));
        undoBtn.addActionListener(e -> undo());
        redoBtn.addActionListener(e -> redo());
        actionPanel.add(undoBtn); actionPanel.add(redoBtn);

        bottom.add(inputsPanel, BorderLayout.WEST);
        bottom.add(actionPanel, BorderLayout.EAST);
        mainArea.add(bottom, BorderLayout.SOUTH);

        ensureDefaultAccount();
        loadCategories();
        loadData();
    }

    private ImageIcon createRobotIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Circular background
        g2.setColor(new Color(233, 213, 255));
        g2.fillOval(0, 0, size, size);

        // Robot head
        g2.setColor(new Color(88, 28, 135));
        g2.fillRoundRect(4, 6, size - 8, size - 10, 6, 6);

        // Antenna
        g2.fillRect(size / 2 - 1, 2, 2, 4);
        g2.fillOval(size / 2 - 2, 1, 4, 4);

        // Cute eyes
        g2.setColor(new Color(56, 189, 248));
        g2.fillOval(7, 10, 3, 3);
        g2.fillOval(size - 10, 10, 3, 3);

        // Smile
        g2.setColor(Color.WHITE);
        g2.drawArc(8, 11, size - 16, 4, 180, 180);

        g2.dispose();
        return new ImageIcon(img);
    }

    private void showNotification(String message, Color bg, Color fg) {
        notificationLabel.setText(message);
        notificationLabel.setForeground(fg);
        notificationBar.setBackground(bg);
        notificationBar.setVisible(true);
    }

    private void filterTable() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }

    private JButton createSideBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 14, 0, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.putClientProperty("FlatLaf.style", "arc: 10");
        return btn;
    }

    private JPanel createKpiCard(String title, JLabel valueLbl, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(new Color(34, 37, 42));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(12, 16, 12, 16)
        ));
        card.putClientProperty("FlatLaf.style", "arc: 14");

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(new Color(156, 163, 175));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLbl.setForeground(accent);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        return card;
    }

    private void setMonthlyBudget() {
        String input = JOptionPane.showInputDialog(this, "Set expense limit for " + currentMonth + " (Rs.):");
        if (input == null || input.trim().isEmpty()) return;
        try {
            BigDecimal limit = new BigDecimal(input.trim());
            String sql = "INSERT INTO Budgets (user_id, month_year, budget_limit) VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE budget_limit = ?";
            try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, currentMonth.toString());
                ps.setBigDecimal(3, limit);
                ps.setBigDecimal(4, limit);
                ps.executeUpdate();
                loadData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    private BigDecimal getMonthlyBudget() {
        String sql = "SELECT budget_limit FROM Budgets WHERE user_id = ? AND month_year = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, currentMonth.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal("budget_limit");
        } catch (SQLException ex) { ex.printStackTrace(); }
        return null;
    }

    private BigDecimal getCurrentMonthExpense() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM Transactions " +
                     "WHERE user_id = ? AND transaction_type = 'Expense' AND YEAR(transaction_date) = ? AND MONTH(transaction_date) = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, currentMonth.getYear());
            ps.setInt(3, currentMonth.getMonthValue());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException ex) { ex.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    private void deductExcessFromFuturePlans(BigDecimal excessDiff) {
        if (excessDiff.compareTo(BigDecimal.ZERO) <= 0) return;

        String selectSql = "SELECT plan_id, plan_name, spent_amount FROM Future_Plans " +
                           "WHERE user_id = ? AND spent_amount > 0 ORDER BY plan_id DESC LIMIT 1";
        String updateSql = "UPDATE Future_Plans SET spent_amount = spent_amount - ? WHERE plan_id = ?";

        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(selectSql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int planId = rs.getInt("plan_id");
                String planName = rs.getString("plan_name");
                BigDecimal currentSaved = rs.getBigDecimal("spent_amount");
                BigDecimal toDeduct = excessDiff.min(currentSaved);

                try (PreparedStatement upd = c.prepareStatement(updateSql)) {
                    upd.setBigDecimal(1, toDeduct);
                    upd.setInt(2, planId);
                    upd.executeUpdate();
                }

                BigDecimal remaining = currentSaved.subtract(toDeduct);
                showNotification(
                        "Budget exceeded by Rs. " + excessDiff + " — Deducted Rs. " + toDeduct + " from '" + planName + "' (Left: Rs. " + remaining + ")",
                        new Color(127, 29, 29), new Color(254, 202, 202)
                );
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadCategories() {
        catBox.removeAllItems();
        String sql = "SELECT category_id, category_name FROM Categories WHERE user_id IS NULL OR user_id = ? ORDER BY category_id ASC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                catBox.addItem(rs.getInt("category_id") + " - " + rs.getString("category_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading categories: " + ex.getMessage());
        }
    }

    private void addCustomCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter new category name (for " + typeBox.getSelectedItem() + "):");
        if (name == null || name.trim().isEmpty()) return;

        String sql = "INSERT INTO Categories (user_id, category_name, type) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name.trim());
            ps.setString(3, (String) typeBox.getSelectedItem());
            ps.executeUpdate();
            loadCategories();
            catBox.setSelectedIndex(catBox.getItemCount() - 1);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding category: " + ex.getMessage());
        }
    }

    private void addPocketMoney() {
        String input = JOptionPane.showInputDialog(this, "Enter pocket money for " + currentMonth + ":");
        if (input == null || input.trim().isEmpty()) return;
        try {
            BigDecimal amt = new BigDecimal(input.trim());
            Date txDate = Date.valueOf(currentMonth.equals(YearMonth.now()) ? LocalDate.now() : currentMonth.atDay(1));
            insertTx(1, amt, "Income", "Monthly Pocket Money", txDate, true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    private void addTransaction() {
        try {
            if (catBox.getSelectedItem() == null) return;
            BigDecimal amt = new BigDecimal(amountField.getText().trim());
            String type = (String) typeBox.getSelectedItem();
            int catId = Integer.parseInt(((String) catBox.getSelectedItem()).split(" - ")[0]);
            String desc = descField.getText().trim();
            Date txDate = Date.valueOf(currentMonth.equals(YearMonth.now()) ? LocalDate.now() : currentMonth.atDay(1));

            insertTx(catId, amt, type, desc, txDate, true);
            amountField.setText(""); descField.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount.");
        }
    }

    private void insertTx(int catId, BigDecimal amt, String type, String desc, Date date, boolean isNew) throws SQLException {
        BigDecimal prevExpense = "Expense".equals(type) && isNew ? getCurrentMonthExpense() : BigDecimal.ZERO;

        String sql = "INSERT INTO Transactions (user_id, account_id, category_id, amount, transaction_type, description, transaction_date) " +
                     "VALUES (?, (SELECT account_id FROM Accounts WHERE user_id = ? LIMIT 1), ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId); ps.setInt(2, userId); ps.setInt(3, catId);
            ps.setBigDecimal(4, amt); ps.setString(5, type); ps.setString(6, desc); ps.setDate(7, date);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                undoStack.push(new Tx(keys.getInt(1), catId, amt, type, desc, date));
                if (isNew) redoStack.clear();
            }
            loadData();

            if ("Expense".equals(type) && isNew) {
                BigDecimal budget = getMonthlyBudget();
                if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newExpense = prevExpense.add(amt);
                    BigDecimal excessBefore = prevExpense.subtract(budget).max(BigDecimal.ZERO);
                    BigDecimal excessAfter = newExpense.subtract(budget).max(BigDecimal.ZERO);
                    BigDecimal newlyExceeded = excessAfter.subtract(excessBefore);
                    if (newlyExceeded.compareTo(BigDecimal.ZERO) > 0) {
                        deductExcessFromFuturePlans(newlyExceeded);
                    }
                }
            }
        }
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        Tx tx = undoStack.pop();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM Transactions WHERE transaction_id = ?")) {
            ps.setInt(1, tx.id);
            ps.executeUpdate();
            redoStack.push(tx);
            loadData();
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Undo failed: " + ex.getMessage()); }
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        Tx tx = redoStack.pop();
        try { insertTx(tx.catId, tx.amt, tx.type, tx.desc, tx.date, false); }
        catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Redo failed: " + ex.getMessage()); }
    }

    private void ensureDefaultAccount() {
        String check = "SELECT account_id FROM Accounts WHERE user_id = ? LIMIT 1";
        String insert = "INSERT INTO Accounts (user_id, account_name, account_type, balance) VALUES (?, 'Cash', 'Cash', 0.00)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(check)) {
            ps.setInt(1, userId);
            if (!ps.executeQuery().next()) {
                try (PreparedStatement ins = c.prepareStatement(insert)) { ins.setInt(1, userId); ins.executeUpdate(); }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void loadData() {
        tableModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO, totalExpense = BigDecimal.ZERO;
        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        Map<Integer, Double> dailyTotals = new TreeMap<>();

        String sql = "SELECT t.transaction_date, t.transaction_type, c.category_name, t.amount, t.description " +
                     "FROM Transactions t JOIN Categories c ON t.category_id = c.category_id " +
                     "WHERE t.user_id = ? AND YEAR(t.transaction_date) = ? AND MONTH(t.transaction_date) = ? ORDER BY t.transaction_id DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, currentMonth.getYear()); ps.setInt(3, currentMonth.getMonthValue());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date txDate = rs.getDate(1);
                String type = rs.getString("transaction_type");
                String catName = rs.getString("category_name");
                BigDecimal amt = rs.getBigDecimal("amount");
                if (type.equals("Income")) {
                    total = total.add(amt);
                } else {
                    total = total.subtract(amt);
                    totalExpense = totalExpense.add(amt);
                    categoryTotals.merge(catName, amt.doubleValue(), Double::sum);
                    int day = txDate.toLocalDate().getDayOfMonth();
                    dailyTotals.merge(day, amt.doubleValue(), Double::sum);
                }
                tableModel.addRow(new Object[]{txDate, type, catName, amt, rs.getString(5)});
            }

            balanceValue.setText("Rs. " + total);
            expenseValue.setText("Rs. " + totalExpense);
            chartPanel.setData(categoryTotals);
            dailyGraphPanel.setData(currentMonth, dailyTotals);

            BigDecimal budget = getMonthlyBudget();
            if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
                int pct = totalExpense.multiply(BigDecimal.valueOf(100))
                        .divide(budget, 0, RoundingMode.HALF_UP).intValue();
                budgetBar.setValue(Math.min(pct, 100));
                budgetStatusLabel.setText(pct + "% of Rs. " + budget + " budget used");

                if (pct >= 100) {
                    budgetBar.setForeground(new Color(239, 68, 68));
                    expenseValue.setForeground(new Color(239, 68, 68));
                    showNotification(
                            "ALERT: Monthly budget exceeded! Spent Rs. " + totalExpense + " of Rs. " + budget + " (" + pct + "%)",
                            new Color(127, 29, 29), new Color(254, 202, 202)
                    );
                } else if (pct >= 80) {
                    budgetBar.setForeground(new Color(245, 158, 11));
                    expenseValue.setForeground(new Color(245, 158, 11));
                    showNotification(
                            "WARNING: You have used " + pct + "% of your Rs. " + budget + " monthly budget.",
                            new Color(120, 53, 15), new Color(253, 230, 138)
                    );
                } else {
                    budgetBar.setForeground(new Color(52, 211, 153));
                    expenseValue.setForeground(new Color(248, 113, 113));
                    notificationBar.setVisible(false);
                }
            } else {
                budgetBar.setValue(0);
                budgetStatusLabel.setText("No monthly budget set");
                expenseValue.setForeground(new Color(248, 113, 113));
                notificationBar.setVisible(false);
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage()); }
    }

    private static class AmountCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            int modelRow = table.convertRowIndexToModel(row);
            String type = (String) table.getModel().getValueAt(modelRow, 1);
            if ("Income".equals(type)) {
                setText("+ Rs. " + value);
                setForeground(new Color(52, 211, 153));
            } else {
                setText("- Rs. " + value);
                setForeground(new Color(248, 113, 113));
            }
            setFont(getFont().deriveFont(Font.BOLD));
            return this;
        }
    }

    private static class DonutChartPanel extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();
        private final Color[] palette = {
            new Color(59, 130, 246), new Color(16, 185, 129), new Color(245, 158, 11),
            new Color(239, 68, 68), new Color(168, 85, 247), new Color(236, 72, 153)
        };

        DonutChartPanel() {
            setBackground(new Color(34, 37, 42));
            setBorder(new EmptyBorder(12, 12, 12, 12));
            putClientProperty("FlatLaf.style", "arc: 14");
        }

        void setData(Map<String, Double> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(156, 163, 175));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("SPENDING BY CATEGORY", 16, 24);

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total <= 0) {
                g2.setColor(new Color(107, 114, 128));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.drawString("No expenses this month", 64, 120);
                g2.dispose();
                return;
            }

            int size = 120;
            int x = (getWidth() - size) / 2, y = 38;
            double startAngle = 90;
            int idx = 0;

            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double angle = (entry.getValue() / total) * 360.0;
                g2.setColor(palette[idx % palette.length]);
                g2.fillArc(x, y, size, size, (int) Math.round(startAngle), (int) Math.ceil(-angle));
                startAngle -= angle;
                idx++;
            }

            g2.setColor(getBackground());
            g2.fillOval(x + 28, y + 28, size - 56, size - 56);

            int ly = y + size + 20;
            idx = 0;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                if (ly > getHeight() - 10) break;
                g2.setColor(palette[idx % palette.length]);
                g2.fillRoundRect(16, ly - 9, 9, 9, 3, 3);
                g2.setColor(Color.WHITE);
                int pct = (int) Math.round((entry.getValue() / total) * 100);
                g2.drawString(entry.getKey() + " (" + pct + "%)", 32, ly);
                ly += 18;
                idx++;
            }
            g2.dispose();
        }
    }

    private static class DailyExpenseCardPanel extends JPanel {
        private YearMonth month = YearMonth.now();
        private Map<Integer, Double> dailyData = new TreeMap<>();
        private int hoveredDay = -1;

        DailyExpenseCardPanel() {
            setBackground(new Color(28, 34, 46));
            setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246, 90), 1, true));
            putClientProperty("FlatLaf.style", "arc: 16");
            ToolTipManager.sharedInstance().registerComponent(this);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int daysInMonth = month.lengthOfMonth();
                    int padL = 16, padR = 16;
                    int chartW = getWidth() - padL - padR;
                    if (chartW <= 0) return;

                    int relX = e.getX() - padL;
                    if (relX >= 0 && relX <= chartW && e.getY() >= 48 && e.getY() <= getHeight() - 24) {
                        int day = (int) ((relX / (double) chartW) * daysInMonth) + 1;
                        day = Math.max(1, Math.min(daysInMonth, day));
                        if (hoveredDay != day) {
                            hoveredDay = day;
                            double amt = dailyData.getOrDefault(day, 0.0);
                            setToolTipText(month + "-" + String.format("%02d", day) + ": Rs. " + String.format("%.2f", amt));
                            repaint();
                        }
                    } else if (hoveredDay != -1) {
                        hoveredDay = -1;
                        setToolTipText(null);
                        repaint();
                    }
                }
            });
        }

        void setData(YearMonth month, Map<Integer, Double> dailyData) {
            this.month = month;
            this.dailyData = dailyData;
            this.hoveredDay = -1;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(245, 158, 11));
            g2.fillRoundRect(16, 16, 22, 16, 4, 4);
            g2.setColor(new Color(156, 163, 175));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("DAILY EXPENSES", 46, 28);

            int daysInMonth = month.lengthOfMonth();
            double maxVal = dailyData.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

            int padL = 16, padR = 16, padT = 50, padB = 26;
            int chartW = getWidth() - padL - padR;
            int chartH = getHeight() - padT - padB;
            if (chartW <= 0 || chartH <= 0) { g2.dispose(); return; }

            g2.setColor(new Color(55, 65, 81));
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            double barSlot = chartW / (double) daysInMonth;
            int barW = Math.max(3, (int) Math.floor(barSlot) - 2);

            for (int d = 1; d <= daysInMonth; d++) {
                double val = dailyData.getOrDefault(d, 0.0);
                int barH = (maxVal > 0 && val > 0) ? Math.max(4, (int) ((val / maxVal) * chartH)) : 2;
                int x = padL + (int) ((d - 1) * barSlot);
                int y = padT + chartH - barH;

                if (d == hoveredDay) {
                    g2.setColor(new Color(96, 165, 250));
                } else if (val > 0) {
                    g2.setColor(new Color(59, 130, 246));
                } else {
                    g2.setColor(new Color(45, 52, 68));
                }
                g2.fillRoundRect(x, y, barW, barH, 3, 3);
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            if (hoveredDay != -1) {
                double amt = dailyData.getOrDefault(hoveredDay, 0.0);
                g2.setColor(new Color(147, 197, 253));
                g2.drawString("Day " + hoveredDay + ": Rs. " + String.format("%.2f", amt), 16, getHeight() - 8);
            } else {
                g2.setColor(new Color(107, 114, 128));
                g2.drawString("Hover over bars for daily total", 16, getHeight() - 8);
            }
            g2.dispose();
        }
    }
}