package com.mycompany.finance_management_system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/finance_manager";
    private static final String USER = "root"; // Change if you use a different MySQL user
    private static final String PASSWORD = "?????? put ur pass here :)"; // Put your MySQL password here

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}