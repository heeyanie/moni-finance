package app;

import java.sql.Connection;

public class DatabaseTest {

    public static void main(String[] args) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            System.out.println("DATABASE CONNECTED!");
            connection.close();
        } catch (Exception e) {
            System.out.println("DATABASE CONNECTION FAILED!");
            e.printStackTrace();
        }
    }
}