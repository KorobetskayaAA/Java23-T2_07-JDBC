package ru.teamscore.java23.booking;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class DatabaseConnector {
    private DatabaseConnector() {}

    private static Path filePath = Path.of("src","main", "resources", "sql.config");
    private static Properties getConnectionProperties() {
        Properties props = new Properties();
        try {
            List<String> jsonProps = Files.readAllLines(filePath);
            JSONObject jo = (JSONObject) new JSONParser().parse(String.join("\n", jsonProps));
            props.put("host", jo.containsKey("host") ? jo.get("host") : "localhost");
            props.put("port", jo.containsKey("port") ? jo.get("port") : 5432);
            props.put("database", jo.containsKey("database") ? jo.get("database") : "postgres");
            props.put("user", jo.get("user"));
            props.put("password", jo.get("password"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return props;
    }

    public static Connection connect() throws SQLException {
        Properties props = getConnectionProperties();
        String url = String.format("jdbc:postgresql://%s:%s/%s",
                props.get("host"), props.get("port"), props.get("database"));
        Connection conn = DriverManager.getConnection(url, props);
        if (conn != null) {
            System.out.println("Connected to database " + url);
        }
        return conn;
    }
}
