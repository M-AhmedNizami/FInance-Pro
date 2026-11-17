/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.finance_management_system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class CalculatorFrame extends JFrame {
    private JTextField display = new JTextField("0");
    private double num1 = 0;
    private String op = "";
    private boolean startNew = true;

    public CalculatorFrame() {
        setTitle("Calculator & Scratchpad");
        setSize(640, 380);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridLayout(1, 2, 12, 0));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        add(root);

        // --- LEFT: Calculator ---
        JPanel calcPanel = new JPanel(new BorderLayout(0, 10));
        display.setFont(new Font("Segoe UI", Font.BOLD, 24));
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setEditable(false);
        display.setPreferredSize(new Dimension(0, 50));
        calcPanel.add(display, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(5, 4, 6, 6));
        String[] keys = {
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "0", ".", "=", "+",
            "C"
        };
        for (String k : keys) {
            JButton btn = new JButton(k);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
            if (k.equals("=")) {
                btn.setBackground(new Color(59, 130, 246));
                btn.setForeground(Color.WHITE);
            } else if (k.equals("C")) {
                btn.setBackground(new Color(239, 68, 68));
                btn.setForeground(Color.WHITE);
            }
            btn.addActionListener(e -> onCalcClick(k));
            grid.add(btn);
        }
        calcPanel.add(grid, BorderLayout.CENTER);
        root.add(calcPanel);

        // --- RIGHT: Pencil & Eraser Scratchpad ---
        JPanel padWrapper = new JPanel(new BorderLayout(0, 8));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JToggleButton pencilBtn = new JToggleButton("Pencil", true);
        JToggleButton eraserBtn = new JToggleButton("Eraser");
        ButtonGroup bg = new ButtonGroup();
        bg.add(pencilBtn); bg.add(eraserBtn);
        tools.add(pencilBtn); tools.add(eraserBtn);

        ScratchPad pad = new ScratchPad(eraserBtn);
        padWrapper.add(tools, BorderLayout.NORTH);
        padWrapper.add(pad, BorderLayout.CENTER);
        root.add(padWrapper);
    }

    private void onCalcClick(String k) {
        if ("0123456789.".contains(k)) {
            if (startNew) { display.setText(k.equals(".") ? "0." : k); startNew = false; }
            else if (!(k.equals(".") && display.getText().contains("."))) display.setText(display.getText() + k);
        } else if ("+-*/".contains(k)) {
            num1 = Double.parseDouble(display.getText());
            op = k; startNew = true;
        } else if (k.equals("=")) {
            double num2 = Double.parseDouble(display.getText()), res = num2;
            switch (op) {
                case "+" -> res = num1 + num2;
                case "-" -> res = num1 - num2;
                case "*" -> res = num1 * num2;
                case "/" -> res = num2 == 0 ? 0 : num1 / num2;
            }
            display.setText(res == (long) res ? String.valueOf((long) res) : String.valueOf(res));
            startNew = true; op = "";
        } else if (k.equals("C")) {
            display.setText("0"); num1 = 0; op = ""; startNew = true;
        }
    }

    private static class ScratchPad extends JPanel {
        private BufferedImage img;
        private int lastX, lastY;
        private final Color bgColor = new Color(37, 39, 41);

        ScratchPad(JToggleButton eraserBtn) {
            setBorder(BorderFactory.createLineBorder(new Color(70, 73, 75)));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { lastX = e.getX(); lastY = e.getY(); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (img == null) return;
                    Graphics2D g = img.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (eraserBtn.isSelected()) {
                        g.setColor(bgColor);
                        g.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    } else {
                        g.setColor(Color.WHITE);
                        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    }
                    g.drawLine(lastX, lastY, e.getX(), e.getY());
                    g.dispose();
                    lastX = e.getX(); lastY = e.getY();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) {
                img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = img.createGraphics();
                g2.setColor(bgColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
            g.drawImage(img, 0, 0, null);
        }
    }
}