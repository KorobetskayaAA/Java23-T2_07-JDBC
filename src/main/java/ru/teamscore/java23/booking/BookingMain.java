package ru.teamscore.java23.booking;

import com.google.gson.Gson;
import ru.teamscore.java23.booking.entities.BookingCreate;
import ru.teamscore.java23.booking.entities.BookingInfo;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public class BookingMain {
    public static void main(String[] args) throws SQLException {
        // считываем существующее бронирование
        try (var connection = DatabaseConnector.connect()) {
            BookingDao dao = new BookingDao(connection);
            // в обновленных версиях демо-базы такой id может отсутствовать!
            for (BookingInfo info : dao.getBookings("7508AB")) {
                System.out.println(info);
            }
        }

        // добавляем новое бронирование из файла JSON
        try (var connection = DatabaseConnector.connect()) {
            BookingDao dao = new BookingDao(connection);
            Path jsonPath = Path.of("src","main", "resources", "BookingToCreate.json");
            try (Reader jsonReader = Files.newBufferedReader(jsonPath)) {
                System.out.println("\n\nСоздание брони...");
                BookingCreate bookingToCreate = new Gson().fromJson(jsonReader, BookingCreate.class);
                if (!dao.createBooking(bookingToCreate)) {
                    System.err.println("Не удалось создать бронь " + bookingToCreate.getBookingRef());
                }
                for (BookingInfo info : dao.getBookings(bookingToCreate.getBookingRef())) {
                    System.out.println(info);
                }
                System.out.println("\n\nУдаление брони...");
                dao.deleteBooking(bookingToCreate.getBookingRef());
                System.out.println("\n\nУдаление завершено");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
