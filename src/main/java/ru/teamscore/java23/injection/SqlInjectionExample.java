package ru.teamscore.java23.injection;

import java.sql.*;

/**
 * Пример демонстрирует, как в необработанный запрос может
 * быть выполнена SQL-инъекция с:
 * - доступом к ограниченным данным
 * - удалением важных данных
 */
public class SqlInjectionExample {
    static String dbURL = "jdbc:postgresql://localhost:5432/test?user=postgres&password=postgres";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(dbURL)) {
            // создаем таблицу заново, т.к. дальше она будет удалена
            Statement createTableStatement = connection.createStatement();
            createTableStatement.executeUpdate("""
                    create table users (
                        id bigserial primary key,
                        name varchar(255) not null,
                        some_secret_info text null
                    )
                    """);
            System.out.println("Table user created");
            // заполняем данными
            Statement insertStatement = connection.createStatement();
            int insertedRowsCount = insertStatement.executeUpdate("""
                    insert into users (name, some_secret_info) values 
                    ('Irene87', 'id card'),
                    ('Alex', 'phone number'),
                    ('Bob', null)
                    """);
            System.out.println(insertedRowsCount + " rows inserted");


            // вместо поиска выполняем SQL-инъекцию
            System.out.println("\nGetting secret info user by name...");
            String searchWithInjection = "'abc' or 1 = 1";
            Statement statement = connection.createStatement();
            // запрос тоже слишком общий, возвращает много лишних данных
            ResultSet result = statement.executeQuery("select * from users where name = " +
                    searchWithInjection);
            while (result.next()) {
                ResultSetMetaData metaData = result.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    if (metaData.getColumnType(i) == Types.NVARCHAR ||
                            metaData.getColumnType(i) == Types.VARCHAR ||
                            metaData.getColumnType(i) == Types.CHAR) {
                        String value = result.getString(i);
                        if (!result.wasNull()) {
                            System.out.print(value);
                        }
                        System.out.print("\t");
                    }
                }
                System.out.println();
            }
            // вместо поиска выполняем SQL-инъекцию
            System.out.println("\nSetting secret info user by name...");
            // два запроса в одном
            String userNameToUpdateWithInjection = "'abc'; drop table users;";
            String secretToUpdate = "''";
            Statement statement2 = connection.createStatement();
            int result2 = statement2.executeUpdate("update users set some_secret_info = " +
                    secretToUpdate +
                    " where name = " +
                    userNameToUpdateWithInjection);
            System.out.println(result2 + " rows affected");

            DatabaseMetaData dbMetaData = connection.getMetaData();
            var tables = dbMetaData.getTables(null, "public", "users", new String[] {"TABLE"});
            if (!tables.next()) {
                System.out.println("Table users not found!");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
