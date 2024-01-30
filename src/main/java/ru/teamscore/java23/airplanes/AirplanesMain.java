package ru.teamscore.java23.airplanes;

import java.sql.*;

public class AirplanesMain {
    static String dbURL = "jdbc:postgresql://localhost:5432/demo";
    public static void main(String[] args) {
        // Не используйте учетки администратора в реальных приложениях!
        try (Connection connection = DriverManager.getConnection(dbURL, "postgres", "postgres")){
            System.out.println("Выполнено подключение к БД");

            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Максимальное число подключений: " + metaData.getMaxConnections());
            System.out.println("Поддержка транзакций: " + metaData.supportsTransactions());

            try (Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery("select * from demo.bookings.aircrafts")
            ) {
                while (results.next()) {
                    String model = results.getString("model");
                    Integer range = results.getInt("range");
                    System.out.printf("%d. %s - %d км\n", results.getRow(), model, range);
                }
            }
        } catch (SQLException e) {
            System.err.println("Произошла ошибка при работе к БД: " + e.getLocalizedMessage());
        }
    }
}
