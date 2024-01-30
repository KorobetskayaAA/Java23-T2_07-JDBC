package ru.teamscore.java23.booking;

import ru.teamscore.java23.booking.entities.BookingCreate;
import ru.teamscore.java23.booking.entities.BookingInfo;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BookingDao {
    Connection connection;

    public BookingDao(Connection connection) {
        this.connection = connection;
    }

    private static String selectQuery = """
                select
                    b.book_ref
                  , b.book_date
                  , t.ticket_no
                  , t.passenger_name
                  , f.flight_no
                  , f.departure_airport
                  , f.arrival_airport
                  , tf.fare_conditions
                  , tf.amount\s
                from bookings.bookings b\s
                    join bookings.tickets t on b.book_ref = t.book_ref\s
                    join bookings.ticket_flights tf on tf.ticket_no = t.ticket_no
                    join bookings.flights f on tf.flight_id = f.flight_id\s
                where b.book_ref = ?
                """;
    public List<BookingInfo> getBookings(String bookingRef) {
        try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
            statement.setString(1, bookingRef);
            List<BookingInfo> bookings = new ArrayList<>();
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                bookings.add(new BookingInfo(
                        result.getString("book_ref"),
                        result.getObject("book_date", OffsetDateTime.class),
                        result.getString("ticket_no"),
                        result.getString("flight_no"),
                        result.getString("departure_airport"),
                        result.getString("arrival_airport"),
                        result.getString("passenger_name"),
                        result.getString("fare_conditions"),
                        result.getBigDecimal("amount")
                ));
            }
            return bookings;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean createBooking(BookingCreate booking) throws SQLException {
        // все части брони должны пройти одной транзакцией
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false); // SQLException - проброс, даже не начинаем транзакцию
        try {
            if (insertBooking(booking) != 1) {
                // не вставилась бронь - откат
                connection.rollback();
                return false;
            }

            if (insertTickets(booking) != booking.getTickets().size()) {
                // не все билеты - откат
                connection.rollback();
                return false;
            }

            // все закрепления за flight - одним batch-запросом
            Statement statement = connection.createStatement();
            for (BookingCreate.Ticket ticket : booking.getTickets()) {
                insertTicketFlights(ticket, statement);
            }
            int[] batchResult = statement.executeBatch();
            if (batchResult.length != booking.getTickets().size()) {
                connection.rollback();
                return false;
            }
            for (int i = 0; i < batchResult.length; i++) {
                if (batchResult[i] != booking.getTickets().get(i).getFlights().size()) {
                    // не все рейсы - откат
                    connection.rollback();
                    return false;
                }
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            // если не удалось выполнить какой-то запрос - откат
            connection.rollback();
            return false;
        } finally {
            // восстанавливаем режим записи транзакций без побочных эффектов
            connection.setAutoCommit(autoCommit);
        }
    }

    private static String insertBookingQuery = """
                insert into bookings.bookings values
                (? /*book_ref*/, ? /*book_date*/, ? /*total_amount*/)
                """;
    private int insertBooking(BookingCreate booking) throws SQLException {
        try (PreparedStatement insertBookingStatement = connection.prepareStatement(insertBookingQuery)) {
            insertBookingStatement.setString(1, booking.getBookingRef());
            insertBookingStatement.setObject(2, OffsetDateTime.parse(booking.getBookingDate()));
            insertBookingStatement.setBigDecimal(3, booking.getTotalAmount());
            int bookingInsertedCount = insertBookingStatement.executeUpdate();
            return bookingInsertedCount;
        }
    }

    private int insertTickets(BookingCreate booking) throws SQLException {
        String insertTicketsQuery = getInsertManyQuery("bookings.tickets",
                booking.getTickets(),
                t -> String.format("('%s'," + //ticket_no
                                "'%s'," + //book_ref
                                "'%s'," + //passenger_id
                                "'%s'," + //passenger_name
                                "'%s'" + //contact_data
                                ")",
                        t.getTicketNo(),
                        booking.getBookingRef(),
                        t.getPassengerId(),
                        t.getPassengerName(),
                        t.getContactData()
                )
        );
        try (PreparedStatement insertTicketsStatement = connection.prepareStatement(insertTicketsQuery)) {
            return insertTicketsStatement.executeUpdate();
        }
    }

    private void insertTicketFlights(BookingCreate.Ticket ticket, Statement statement) throws SQLException {
        String insertFlightsQuery = getInsertManyQuery("bookings.ticket_flights",
                ticket.getFlights(),
                f -> String.format("('%s'," + //ticket_no
                                "'%d'," + //flight_id
                                "'%s'," + //fare_conditions
                                "'%s'" + //amount
                                ")",
                        ticket.getTicketNo(),
                        f.getFlightId(),
                        f.getFare_conditions(),
                        f.getAmount().toPlainString()
                )
        );
        statement.addBatch(insertFlightsQuery);
    }

    private static <T> String getInsertManyQuery(String tableName, List<T> values, Function<T, String> mapper) {
        return String.format("insert into %s values %s",
                tableName,
                String.join(",",
                        values.stream().map(value -> mapper.apply(value)).toList()
                )
        );
    }

    private String deleteBookingQuery = """
            do $$
              declare
              _book_ref bpchar(6) := '%s';
              begin
                delete from bookings.ticket_flights tf
                where exists (select 1
               		         from bookings.tickets t
               		         where tf.ticket_no = t.ticket_no AND t.book_ref = _book_ref);
                delete from bookings.tickets t
                where t.book_ref = _book_ref;
                delete from bookings.bookings b
                where b.book_ref = _book_ref;
            end $$;
            """;
    public void deleteBooking(String bookingRef) {
        try (PreparedStatement deleteBookingStatement =
                     connection.prepareStatement(String.format(deleteBookingQuery, bookingRef))
        ) {
            deleteBookingStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
